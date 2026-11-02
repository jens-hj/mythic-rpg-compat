#!/usr/bin/env python3
"""Generate recipes, item models and recolour manifests from infusables.json.

Reads only JSON structure (item definitions, model parents/texture refs) and
texture *names/sizes* from the RPG Series jars. No artwork is copied: the
recoloured textures are produced at runtime by the client.

Two kinds of upgrade, each a "variant" with its own recoloured models/textures:
  * gems   (Mythic Upgrades): every infusable item, recipe = Mythic template + gem ingot
  * forges (Better Nether/End): Netherite class armor only, recipe from infusables.json
  * gem + forge pairs: class armor carrying both, two-tone; models only (no recipe)

Usage: scripts/gen_data.py [JAR ...]
Without arguments, jars are looked up in the Gradle cache (run a build first).
"""
import glob
import json
import os
import re
import shutil
import struct
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = "mythic_rpg_compat"
MAIN = os.path.join(ROOT, "src/main/resources")
CLIENT = os.path.join(ROOT, "src/client/resources")
SLOTS = ["head", "chest", "legs", "feet"]
GEM_TEMPLATE = "mythicupgrades:mythic_upgrade_smithing_template"
# Texture/model names that belong to upgradeable gear (vs. shared parents like "item/handheld").
MATERIAL = re.compile(r"netherite|ruby|aeternium|crystal")
BONUS_MATERIAL = re.compile(r"ruby|aeternium|crystal")


def load_jars(cfg, explicit):
    jars = {}
    for mod, m in cfg["mods"].items():
        if explicit:
            cands = [j for j in explicit if os.path.basename(j).startswith(mod) or m["modrinth"] in j]
        else:
            cands = glob.glob(os.path.expanduser(
                f"~/.gradle/caches/modules-2/files-2.1/maven.modrinth/{m['modrinth']}/*/*/*.jar"))
            cands = [c for c in cands if not c.endswith("-sources.jar")]
        if not cands:
            sys.exit(f"no jar found for {mod}; run ./gradlew build first or pass jar paths")
        jars[mod] = zipfile.ZipFile(sorted(cands)[-1])
    return jars


def read_json(jar, path):
    try:
        return json.loads(jar.read(path))
    except KeyError:
        return None


def png_size(jar, path):
    try:
        with jar.open(path) as f:
            head = f.read(24)
    except KeyError:
        return None
    return struct.unpack(">II", head[16:24])


def twin_for(jar, mod, kind, name):
    """(twin, mode) used to mask the recolourable parts of a texture.

    Netherite textures: the pre-netherite texture, masking grey netherite pixels ("grey").
    Ruby/Aeternium/Crystal textures: the netherite texture, masking every changed pixel ("diff").
    """
    src = f"assets/{mod}/textures/{kind}/{name}.png"
    size = png_size(jar, src)
    if size is None:
        sys.exit(f"missing texture {src}")
    if "netherite" in name:
        cands, mode = (name.replace("netherite_", "", 1), name.replace("netherite", "diamond", 1)), "grey"
    else:
        cands, mode = (BONUS_MATERIAL.sub("netherite", name, 1),), "diff"
    for cand in cands:
        if cand != name and png_size(jar, f"assets/{mod}/textures/{kind}/{cand}.png") == size:
            return f"{mod}:{kind}/{cand}", mode
    if mode == "diff":
        sys.exit(f"no netherite twin for {mod}:{kind}/{name}")
    return None, mode


def walk_models(node, fn):
    """Apply fn to every model reference string in an item model definition tree."""
    if isinstance(node, dict):
        for k, v in node.items():
            if k == "model" and isinstance(v, str):
                node[k] = fn(v)
            else:
                walk_models(v, fn)
    elif isinstance(node, list):
        for v in node:
            walk_models(v, fn)


def ns_split(ref, default):
    return ref.split(":", 1) if ":" in ref else (default, ref)


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def recipe(path, mods, template, base, addition, components):
    write(path, {
        "fabric:load_conditions": [{"condition": "fabric:all_mods_loaded", "values": mods}],
        "type": "minecraft:smithing_transform",
        "template": template,
        "base": base,
        "addition": addition,
        "result": {"id": base, "components": components},
    })


def main():
    cfg = json.load(open(os.path.join(ROOT, "infusables.json")))
    jars = load_jars(cfg, sys.argv[1:])
    gems = cfg["gems"]
    forges = cfg["forges"]
    forge_keys = [f"forged_{f}" for f in forges]

    out_dirs = [
        os.path.join(MAIN, f"data/{MOD}/recipe/infuse"),
        os.path.join(MAIN, f"data/{MOD}/recipe/forge"),
        os.path.join(CLIENT, f"assets/{MOD}/items"),
        os.path.join(CLIENT, f"assets/{MOD}/models"),
        os.path.join(CLIENT, f"assets/{MOD}/recolor"),
        os.path.join(CLIENT, "assets/minecraft/atlases"),
    ]
    for d in out_dirs:
        shutil.rmtree(d, ignore_errors=True)

    item_textures = {}   # "mod:item/x" -> entry
    armor_textures = {}  # "mod:armor/x" -> entry
    infusables = []      # (mod, item_id, armor_set | None, required mods)
    recipes = 0

    def entry(jar, mod, kind, name, forge):
        twin, mode = twin_for(jar, mod, kind, name)
        e = {"texture": f"{mod}:{kind}/{name}"}
        if twin:
            e["twin"] = twin
        if mode != "grey":
            e["mode"] = mode
        if forge:
            e["forge"] = True
        return e

    for mod, m in cfg["mods"].items():
        jar = jars[mod]
        for s in m["armor"]:
            armor_textures[f"{mod}:armor/{s}"] = entry(jar, mod, "armor", s, True)
            infusables += [(mod, f"{s}_{slot}", s, []) for slot in SLOTS]
        infusables += [(mod, w, None, []) for w in m["weapons"]]
        infusables += [(mod, w["id"], None, [w["requires"]]) for w in m.get("bonus_weapons", [])]

    for mod, item, armor_set, requires in infusables:
        jar = jars[mod]
        definition = read_json(jar, f"assets/{mod}/items/{item}.json")
        if definition is None:
            sys.exit(f"missing item definition {mod}:{item}")

        # Collect every model reachable from the definition, plus same-mod material parents.
        models = set()
        walk_models(definition, lambda ref: models.add(ref) or ref)
        pending = list(models)
        model_json = {}
        while pending:
            ref = pending.pop()
            ns, path = ns_split(ref, "minecraft")
            data = read_json(jars.get(ns, jar), f"assets/{ns}/models/{path}.json") if ns == mod else None
            if data is None:
                sys.exit(f"cannot read model {ref}")
            model_json[ref] = data
            parent = data.get("parent")
            if parent and ns_split(parent, "minecraft")[0] == mod and MATERIAL.search(parent) and parent not in model_json:
                models.add(parent)
                pending.append(parent)

        for ref, data in model_json.items():
            for tex in data.get("textures", {}).values():
                if tex.startswith("#"):
                    continue
                tns, tpath = ns_split(tex, "minecraft")
                kind, name = tpath.split("/", 1)
                key = f"{tns}:{tpath}"
                if tns == mod and MATERIAL.search(name):
                    if key not in item_textures:
                        item_textures[key] = entry(jar, mod, kind, name, armor_set is not None)
                    elif armor_set is not None:
                        item_textures[key]["forge"] = True

        def ours(variant, ref):
            ns, path = ns_split(ref, "minecraft")
            return f"{MOD}:{path.split('/', 1)[0]}/{variant}/{ns}/{path.split('/', 1)[1]}"

        # Class armor also gets the forge variants and the two-tone gem + forge pairs.
        variants = gems + (forge_keys + [f"{g}_{f}" for g in gems for f in forge_keys] if armor_set else [])
        for variant in variants:
            # Item model definition
            d = json.loads(json.dumps(definition))
            walk_models(d, lambda ref: ours(variant, ref) if ref in model_json else ref)
            write(os.path.join(CLIENT, f"assets/{MOD}/items/{variant}/{mod}/{item}.json"), d)

            # Models with retextured layers
            for ref, data in model_json.items():
                md = json.loads(json.dumps(data))
                if md.get("parent") in model_json:
                    md["parent"] = ours(variant, md["parent"])
                for k, tex in md.get("textures", {}).items():
                    tns, tpath = ns_split(tex, "minecraft")
                    if f"{tns}:{tpath}" in item_textures:
                        md["textures"][k] = ours(variant, f"{tns}:{tpath}")
                _, mpath = ns_split(ours(variant, ref), MOD)
                write(os.path.join(CLIENT, f"assets/{MOD}/models/{mpath}.json"), md)

        def components(variant, own):
            # The model/texture shown in recipe viewers; Visuals.apply switches to the two-tone
            # variant after crafting when the piece carries both a gem and a forge.
            c = {**own, "minecraft:item_model": f"{MOD}:{variant}/{mod}/{item}"}
            if armor_set:
                c["minecraft:custom_data"] = {
                    "armor_model_api": {"texture": f"{MOD}:textures/armor/{variant}/{mod}/{armor_set}.png"}
                }
            return c

        for gem in gems:
            recipe(os.path.join(MAIN, f"data/{MOD}/recipe/infuse/{mod}/{item}_{gem}.json"),
                   [mod, "mythicupgrades", *requires], GEM_TEMPLATE, f"{mod}:{item}",
                   f"mythicupgrades:{gem}_ingot", components(gem, {f"{MOD}:infusion": gem}))
            recipes += 1
        if armor_set:
            for name, f in forges.items():
                recipe(os.path.join(MAIN, f"data/{MOD}/recipe/forge/{mod}/{item}_{name}.json"),
                       [mod, f["requires"]], f["template"], f"{mod}:{item}", f["addition"],
                       components(f"forged_{name}", {f"{MOD}:forged": name}))
                recipes += 1

    variants = [{"key": g, "palette": f"mythicupgrades:textures/{g}_palette.png", "strip": True} for g in gems]
    for name, f in forges.items():
        ns, path = ns_split(f["palette"], "minecraft")
        variants.append({"key": f"forged_{name}", "palette": f"{ns}:textures/{path}.png", "forge": True})

    def entries(textures):
        return [textures[k] for k in sorted(textures)]

    write(os.path.join(CLIENT, "assets/minecraft/atlases/items.json"), {
        "sources": [{"type": f"{MOD}:gem_recolor", "variants": variants, "textures": entries(item_textures)}]
    })
    write(os.path.join(CLIENT, f"assets/{MOD}/recolor/armor.json"), {
        "variants": variants, "textures": entries(armor_textures)
    })

    no_twin = [k for k, e in {**item_textures, **armor_textures}.items() if "twin" not in e]
    print(f"{len(infusables)} infusables, {recipes} recipes, "
          f"{len(item_textures)} item textures, {len(armor_textures)} armor textures")
    if no_twin:
        print("no twin (colour-match fallback):", ", ".join(sorted(no_twin)))


if __name__ == "__main__":
    main()
