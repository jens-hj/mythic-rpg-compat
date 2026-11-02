#!/usr/bin/env python3
"""Generate recipes, item models and recolour manifests from infusables.json.

Reads only JSON structure (item definitions, model parents/texture refs) and
texture *names/sizes* from the RPG Series jars. No artwork is copied: the
recoloured textures are produced at runtime by the client.

Usage: scripts/gen_data.py [JAR ...]
Without arguments, jars are looked up in the Gradle cache (run a build first).
"""
import glob
import json
import os
import shutil
import struct
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = "mythic_rpg_compat"
MAIN = os.path.join(ROOT, "src/main/resources")
CLIENT = os.path.join(ROOT, "src/client/resources")
SLOTS = ["head", "chest", "legs", "feet"]
TEMPLATE = "mythicupgrades:mythic_upgrade_smithing_template"


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
    """Pre-netherite texture with the same size, used to mask the netherite parts."""
    src = f"assets/{mod}/textures/{kind}/{name}.png"
    size = png_size(jar, src)
    if size is None:
        sys.exit(f"missing texture {src}")
    for cand in (name.replace("netherite_", "", 1), name.replace("netherite", "diamond", 1)):
        if cand != name and png_size(jar, f"assets/{mod}/textures/{kind}/{cand}.png") == size:
            return f"{mod}:{kind}/{cand}"
    return None


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


def main():
    cfg = json.load(open(os.path.join(ROOT, "infusables.json")))
    jars = load_jars(cfg, sys.argv[1:])
    gems = cfg["gems"]

    out_dirs = [
        os.path.join(MAIN, f"data/{MOD}/recipe/infuse"),
        os.path.join(CLIENT, f"assets/{MOD}/items"),
        os.path.join(CLIENT, f"assets/{MOD}/models"),
        os.path.join(CLIENT, f"assets/{MOD}/recolor"),
        os.path.join(CLIENT, "assets/minecraft/atlases"),
    ]
    for d in out_dirs:
        shutil.rmtree(d, ignore_errors=True)

    item_textures = {}   # "mod:item/x" -> twin | None
    armor_textures = {}  # "mod:armor/x" -> twin | None
    infusables = []      # (mod, item_id, armor_set | None)
    recipes = 0

    for mod, m in cfg["mods"].items():
        jar = jars[mod]
        for s in m["armor"]:
            armor_textures[f"{mod}:armor/{s}"] = twin_for(jar, mod, "armor", s)
            infusables += [(mod, f"{s}_{slot}", s) for slot in SLOTS]
        infusables += [(mod, w, None) for w in m["weapons"]]

    for mod, item, armor_set in infusables:
        jar = jars[mod]
        definition = read_json(jar, f"assets/{mod}/items/{item}.json")
        if definition is None:
            sys.exit(f"missing item definition {mod}:{item}")

        # Collect every model reachable from the definition, plus same-mod netherite parents.
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
            if parent and ns_split(parent, "minecraft")[0] == mod and "netherite" in parent and parent not in model_json:
                models.add(parent)
                pending.append(parent)

        for ref, data in model_json.items():
            for tex in data.get("textures", {}).values():
                if tex.startswith("#"):
                    continue
                tns, tpath = ns_split(tex, "minecraft")
                kind, name = tpath.split("/", 1)
                if tns == mod and "netherite" in name and f"{tns}:{tpath}" not in item_textures:
                    item_textures[f"{tns}:{tpath}"] = twin_for(jar, mod, kind, name)

        def ours(gem, ref):
            ns, path = ns_split(ref, "minecraft")
            return f"{MOD}:{path.split('/', 1)[0]}/{gem}/{ns}/{path.split('/', 1)[1]}"

        for gem in gems:
            # Item model definition
            d = json.loads(json.dumps(definition))
            walk_models(d, lambda ref: ours(gem, ref) if ref in model_json else ref)
            write(os.path.join(CLIENT, f"assets/{MOD}/items/{gem}/{mod}/{item}.json"), d)

            # Models with retextured layers
            for ref, data in model_json.items():
                md = json.loads(json.dumps(data))
                if md.get("parent") in model_json:
                    md["parent"] = ours(gem, md["parent"])
                for k, tex in md.get("textures", {}).items():
                    tns, tpath = ns_split(tex, "minecraft")
                    if f"{tns}:{tpath}" in item_textures:
                        md["textures"][k] = ours(gem, f"{tns}:{tpath}")
                _, mpath = ns_split(ours(gem, ref), MOD)
                write(os.path.join(CLIENT, f"assets/{MOD}/models/{mpath}.json"), md)

            # Smithing recipe
            components = {
                f"{MOD}:infusion": gem,
                "minecraft:item_model": f"{MOD}:{gem}/{mod}/{item}",
            }
            if armor_set:
                components["minecraft:custom_data"] = {
                    "armor_model_api": {"texture": f"{MOD}:textures/armor/{gem}/{mod}/{armor_set}.png"}
                }
            write(os.path.join(MAIN, f"data/{MOD}/recipe/infuse/{mod}/{item}_{gem}.json"), {
                "fabric:load_conditions": [{"condition": "fabric:all_mods_loaded", "values": [mod]}],
                "type": "minecraft:smithing_transform",
                "template": TEMPLATE,
                "base": f"{mod}:{item}",
                "addition": f"mythicupgrades:{gem}_ingot",
                "result": {"id": f"{mod}:{item}", "components": components},
            })
            recipes += 1

    def entries(textures):
        return [{"texture": t, **({"twin": tw} if tw else {})} for t, tw in sorted(textures.items())]

    write(os.path.join(CLIENT, "assets/minecraft/atlases/items.json"), {
        "sources": [{"type": f"{MOD}:gem_recolor", "gems": gems, "textures": entries(item_textures)}]
    })
    write(os.path.join(CLIENT, f"assets/{MOD}/recolor/armor.json"), {
        "gems": gems, "textures": entries(armor_textures)
    })

    no_twin = [t for t, tw in {**item_textures, **armor_textures}.items() if tw is None]
    print(f"{len(infusables)} infusables, {recipes} recipes, "
          f"{len(item_textures)} item textures, {len(armor_textures)} armor textures")
    if no_twin:
        print("no twin (colour-match fallback):", ", ".join(sorted(no_twin)))


if __name__ == "__main__":
    main()
