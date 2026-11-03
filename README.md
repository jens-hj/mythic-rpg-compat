# RPG Gear Upgrades

*(formerly Mythic RPG Compat; the mod id is still `mythic_rpg_compat`)*

Fabric mod for Minecraft 26.3 that gives **RPG Series** class gear endgame upgrades from other mods.
Every integration is optional and turns on when its mod is installed.

## Mythic gem infusion ([Mythic Upgrades](https://modrinth.com/mod/mythic-upgrades))

Smithing table: Mythic Upgrade Smithing Template + RPG item + gem ingot (ametrine, aquamarine,
citrine, jade, peridot, ruby, sapphire or topaz). You get the same item back, plus:

- **Mythic effects.** Infused armor counts as that gem's Mythic armor piece for set effects
  (partial sets work too). Infused weapons trigger the gem's on-hit effect, just like a Mythic sword.
- **Better stats.** Infused armor gets its own class attributes (ranged damage, spell power, ...)
  boosted by 25% and +1 armor toughness.
- **Room for an extra socket** (with the Jewelry mod). Infused gear accepts one more Socket Mount.

Works on Netherite RPG armor and weapons, and on the RPG Series' own Ruby/Aeternium/Crystal weapons.
Infusing again with a different gem swaps the gem.

## Ruby and Aeternium forging ([Better Nether](https://modrinth.com/mod/betternether), [Better End](https://modrinth.com/mod/betterend))

Netherite class armor gets the tiers the RPG Series already has for weapons:

| Tier | Template | Addition |
|---|---|---|
| Ruby | `minecraft:netherite_upgrade_smithing_template` | `betternether:nether_ruby` |
| Aeternium | `betterend:plate_upgrade_smithing_template` | `betterend:aeternium_forged_plate` |

Forged armor gets the material's armor stats over Netherite (read from Fireruby/Aeternium armor),
+1 toughness and +15% on its own class attributes, and the netherite parts take the material's colour.

A piece can be forged *and* infused, in either order: bonuses add up and the piece gets a two-tone look (forge colours on the dark shades, gem colours on the light ones).

## Supported gear

All Netherite-tier armor sets and weapons from Archers, Paladins & Priests, Wizards and
Rogues & Warriors, plus their Ruby/Aeternium/Crystal weapons. See [`infusables.json`](infusables.json).

## Requirements

- Fabric Loader, Fabric API
- Armor Model API (comes with the RPG Series)
- Any of Mythic Upgrades 5.1.1+, Better Nether, Better End; recipes for missing mods are skipped

## Limitations

- Infused weapons trigger their gem's on-hit effect on melee hits, and also:
  - **Arrows and bolts** from an infused bow or crossbow, when fully drawn (crossbow bolts always
    count). The gem comes from the weapon that fired the arrow.
  - **Spells**, on the spell's direct target only (not splash or area victims), at most once per
    target per second. The gem comes from the main-hand item.

## Config

`config/rpg_gear_upgrades.json` (created on first start) scales the particles of Mythic Upgrades'
gem effects: `mythic_particles.<gem>` per gem (`1` = Mythic's default, `0` = none), or
`mythic_particles.override` for all gems at once. Server-side; restart to apply.

With [Mod Menu](https://modrinth.com/mod/modmenu) and
[Cloth Config](https://modrinth.com/mod/cloth-config) installed, the same settings are on the mod's
config screen (as percentages), applied right away in singleplayer.

## How it works

- Data components `mythic_rpg_compat:infusion` (gem) and `mythic_rpg_compat:forged` (tier) store
  the upgrades. Recipes are plain `minecraft:smithing_transform`, so recipe viewers show them.
- Stat bonuses are computed at runtime from the other mods' armor items, never their classes.
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
./gradlew runClientGameTest -PnoMythic            # same, without Mythic Upgrades installed
MYTHIC_RPG_GALLERY=1 ./gradlew runClientGameTest  # renders the Modrinth gallery to build/run/clientGameTest/gallery
```

Publishing to Modrinth (token from `$MODRINTH_TOKEN` or `~/.config/modrinth-token`):

```sh
./gradlew modrinth -PmodrinthDryRun   # print what would be uploaded
./gradlew modrinth                    # upload the version, changelog from CHANGELOG.md
./gradlew modrinthSyncBody            # push docs/MODRINTH.md as the project description
```

After editing `infusables.json`, regenerate the recipes and models with `python3 scripts/gen_data.py`
(it reads the RPG jars from the Gradle cache, so build once first).

## License

MIT
