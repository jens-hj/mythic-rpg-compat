Endgame upgrades for the RPG Series gear by [ZsoltMolnarrr](https://modrinth.com/user/ZsoltMolnarrr) ([Archers](https://modrinth.com/mod/archers), [Paladins & Priests](https://modrinth.com/mod/paladins-and-priests), [Wizards](https://modrinth.com/mod/wizards), [Rogues & Warriors](https://modrinth.com/mod/rogues-and-warriors)), tying it into [Mythic Upgrades](https://modrinth.com/mod/mythic-upgrades), [Better Nether](https://modrinth.com/mod/betternether) and [Better End](https://modrinth.com/mod/betterend).

Normally your Netherite class gear is a dead end. Mythic Upgrades only works on vanilla Netherite, and Better Nether/End's Ruby and Aeternium tiers only exist for their own tools and armor. This mod gives RPG class gear a way forward, while keeping everything that makes it class gear.

Every integration is **optional**: install whichever of these mods you play with and the matching upgrades appear.

## Mythic gem infusion *(needs Mythic Upgrades)*

Smithing Table: **Mythic Upgrade Smithing Template** + **RPG item** + **Mythic gem ingot** (Ametrine, Aquamarine, Citrine, Jade, Peridot, Ruby, Sapphire or Topaz).

- **Gem effects.** Infused **armor** counts as that gem's Mythic armor for set effects, including partial sets and sets mixed with real Mythic pieces. Infused **weapons** trigger the gem's on-hit effect, like a Mythic sword:
  - **Melee** hits, as with Mythic's own weapons.
  - **Arrows and bolts** from infused bows and crossbows, when fully drawn (crossbow bolts always count).
  - **Spells** cast with an infused staff or weapon, on the spell's direct target (not splash or area victims), at most once per target per second.
- **Stats.** Armor gets **+25% on its own class attributes** (Ranged Damage, Spell Power, Draw Speed, …) and **+1 Armor Toughness**.
- **Extra socket.** With [Jewelry](https://modrinth.com/mod/jewelry), infused gear accepts **one more Socket Mount**.
- Works on all Netherite RPG armor and weapons, *and* on the RPG Series' own **Ruby, Aeternium and Crystal weapons**.
- Infuse again with a different ingot to swap the gem.

## Ruby & Aeternium forging *(needs Better Nether / Better End)*

The RPG Series already has Ruby and Aeternium **weapons**. This adds the matching tiers for Netherite class **armor**:

| Tier | Template | Addition |
|---|---|---|
| **Ruby** (Better Nether) | Netherite Upgrade Smithing Template | Nether Ruby |
| **Aeternium** (Better End) | Plate Upgrade Smithing Template | Aeternium Forged Plate |

- **Stats.** Each tier adds its material's armor stats over Netherite (read from Better Nether's Fireruby and Better End's Aeternium armor), **+1 Armor Toughness**, and **+15% on the armor's own class attributes**, the same step the RPG Series uses from Netherite to Ruby/Aeternium weapons.
- **Looks.** The dark netherite parts turn ruby red or Aeternium teal.

## Stacking

A piece can be **both forged and infused**, in either order. The bonuses add up (e.g. +40% class attributes and +2 toughness), the tooltip shows both, and the piece gets a **two-tone look**: Ruby or Aeternium on the darker shades, the gem's colour on the lighter ones.

## Coloured gear

The dark netherite parts of the item icon *and* the worn armor are recoloured in the gem's or material's palette. On Ruby/Aeternium/Crystal weapons the ruby, Aeternium or crystal parts take the gem's colour. Cloth, gold and leather keep their original colours. The item keeps its name, enchantments, trims, sockets and class stats.

All recipes show up in REI, JEI and EMI as normal smithing recipes.

## Supported gear

- [Archers](https://modrinth.com/mod/archers): Ranger armor, longbow, shortbow, heavy and rapid crossbow, spear, plus Ruby/Aeternium/Crystal variants
- [Paladins & Priests](https://modrinth.com/mod/paladins-and-priests): Crusader armor, Prior robes, claymore, great hammer, mace, holy staff and wand, kite shield, plus Ruby/Aeternium variants
- [Wizards](https://modrinth.com/mod/wizards): Arcane, Fire and Frost robes, staves, wands, plus the Ruby fire staff and the Crystal arcane staff
- [Rogues & Warriors](https://modrinth.com/mod/rogues-and-warriors): Assassin and Berserker armor, dagger, double axe, glaive, sickle, plus Ruby/Aeternium variants

## Config: fewer Mythic particles

Mythic Upgrades' gem effects (Jade's trail and aura, Topaz and Arcane waves, Ice Bomb rings, …) can spawn a lot of particles. `config/rpg_gear_upgrades.json` scales them:

```json
"mythic_particles": {
  "override": null,
  "ametrine": 1.0, "aquamarine": 1.0, "citrine": 1.0, "jade": 0.25,
  "peridot": 1.0, "ruby": 1.0, "topaz": 1.0, "necoium": 1.0
}
```

- `1` keeps Mythic's amount, `0` hides them, `0.25` keeps about a quarter.
- Set `override` to a number to use it for every gem instead of the per-gem values.
- Particles are spawned by the server, so on a server, edit the **server's** config (restart to apply). In singleplayer, your own.

## Requirements

- Fabric Loader + Fabric API
- Armor Model API (installed with the RPG Series)
- Any of: [Mythic Upgrades](https://modrinth.com/mod/mythic-upgrades) 5.1.1+, [Better Nether](https://modrinth.com/mod/betternether), [Better End](https://modrinth.com/mod/betterend)
- Optional: [Jewelry](https://modrinth.com/mod/jewelry) for the extra socket

Install on **both the server and the client**.

## Good to know

- The recoloured textures are generated on your machine from the mods you have installed. No RPG Series artwork is included in this mod, so it also works with resource packs that retexture the RPG gear.
- Formerly *Mythic RPG Compat*. Gear upgraded with older versions keeps working.
