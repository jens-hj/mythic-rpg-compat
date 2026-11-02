# Mythic RPG Compat

Fabric mod for Minecraft 26.3 that lets you **infuse Netherite RPG Series gear with
[Mythic Upgrades](https://modrinth.com/mod/mythic-upgrades) gems**.

Put a Netherite RPG piece in a smithing table with the Mythic Upgrade Smithing Template and a
gem ingot (ametrine, aquamarine, citrine, jade, peridot, ruby, sapphire or topaz). You get the
same item back, plus:

- **Mythic effects.** Infused armor counts as that gem's Mythic armor piece for set effects
  (partial sets work too). Infused weapons trigger the gem's on-hit effect, just like a Mythic sword.
- **Your class gear stays intact.** The look, name, ranged/spell stats, enchantments and trims
  are all kept. Mythic's armor and toughness are added on top of the item's own (they currently
  match Netherite's).
- **Gem colours.** The dark netherite parts of the item icon and the worn armor are recoloured
  with the gem's palette.

Infusing an already infused piece with a different gem swaps the gem.

## Supported gear

All Netherite-tier armor sets and weapons from Archers, Paladins & Priests, Wizards and
Rogues & Warriors. See [`infusables.json`](infusables.json) for the list.

## Requirements

- Fabric Loader, Fabric API
- Mythic Upgrades 5.1.1+
- Armor Model API (comes with the RPG Series)
- Any of the RPG Series mods; recipes for mods you don't have are skipped

## Limitations

- Mythic Upgrades only triggers weapon effects on direct melee hits. Arrows from infused bows
  and crossbows, and most spells, don't trigger gem effects.

## How it works

- A data component `mythic_rpg_compat:infusion` stores the gem. Recipes are plain
  `minecraft:smithing_transform`, so recipe viewers show them.
- A [MixinSquared](https://github.com/Bawnorton/MixinSquared) hook into Mythic Upgrades' own
  armor/on-hit handlers makes infused stacks report the matching Mythic item.
- Recoloured textures are **generated at runtime** from the textures of your installed mods
  (a custom atlas sprite source for icons, dynamic textures for worn armor). No RPG Series art
  is shipped in this mod.

## Development

```sh
nix develop                      # JDK 25, Python + Pillow, GPU libs for the dev client
./gradlew build
./gradlew runClient
./gradlew runClientGameTest      # end-to-end test, saves screenshots to build/run/clientGameTest/screenshots
```

After editing `infusables.json`, regenerate the recipes and models with `python3 scripts/gen_data.py`
(it reads the RPG jars from the Gradle cache, so build once first).

## License

MIT
