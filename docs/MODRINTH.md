Adds compat between [Mythic Upgrades](https://modrinth.com/mod/mythic-upgrades) and the RPG Series base mods like [Archers (RPG Series)](https://modrinth.com/mod/archers) by [ZsoltMolnarrr](https://modrinth.com/user/ZsoltMolnarrr).

Normally your Netherite class gear is a dead end. Mythic Upgrades only works on vanilla Netherite, so your Netherite Ranger armor or Netherite Claymore can never become Mythic. This mod lets you **infuse** them with Mythic gems while keeping everything that makes them class gear.

## How it works

Use a **Smithing Table** with:

1. **Mythic Upgrade Smithing Template**
2. **A Netherite RPG Series item** (armor or weapon)
3. **A Mythic gem ingot**: Ametrine, Aquamarine, Citrine, Jade, Peridot, Ruby, Sapphire or Topaz

You get the same item back, now infused. Infuse it again with a different ingot to swap the gem.

## What infusion gives you

- **Mythic gem effects**
  - Infused **armor** counts as that gem's Mythic armor for set effects. A full set gets the full effect, and partial sets get a partial one. You can mix infused RPG pieces with real Mythic pieces.
  - Infused **weapons** trigger the gem's on-hit effect, just like a Mythic sword.
- **Better stats.** Infused armor gets **+25% on its own class attributes** (Ranged Damage, Spell Power, Draw Speed, …) and **+1 Armor Toughness**.
- **Room for an extra socket.** With [Jewelry](https://modrinth.com/mod/jewelry), infused gear accepts **one more Socket Mount** than normal.
- **Gem-coloured gear.** The dark netherite parts of the item icon *and* the worn armor are recoloured in the gem's palette. Cloth, gold, leather and gems keep their original colours.
- **Nothing is lost.** The item keeps its look, name, enchantments, trims, socketed gems and class stats.

## Supported gear

All Netherite-tier armor and weapons from:

- [Archers](https://modrinth.com/mod/archers): Ranger armor, longbow, shortbow, heavy and rapid crossbow, spear
- [Paladins & Priests](https://modrinth.com/mod/paladins-and-priests): Crusader armor, Prior robes, claymore, great hammer, mace, holy staff and wand, kite shield
- [Wizards](https://modrinth.com/mod/wizards): Arcane, Fire and Frost robes, staves, wands
- [Rogues & Warriors](https://modrinth.com/mod/rogues-and-warriors): Assassin and Berserker armor, dagger, double axe, glaive, sickle

You only need the RPG mods you actually play with. Recipes for missing mods are skipped. All recipes show up in REI, JEI and EMI as normal smithing recipes.

## Requirements

- Fabric Loader + Fabric API
- [Mythic Upgrades](https://modrinth.com/mod/mythic-upgrades) 5.1.1+
- Armor Model API (installed with the RPG Series)
- Optional: [Jewelry](https://modrinth.com/mod/jewelry) for the extra socket

Install on **both the server and the client**.

## Good to know

- Mythic Upgrades only triggers weapon gem effects on **direct melee hits**, so arrows from infused bows and crossbows (and most spells) don't trigger them. Your bows still get the colours and the infusion.
- The recoloured textures are generated on your machine from the mods you have installed. No RPG Series artwork is included in this mod, so it also works with resource packs that retexture the RPG gear.
