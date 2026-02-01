# Reliable Replacer

<a href='https://files.minecraftforge.net'><img alt="forge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/forge_vector.svg"></a>
<a href='https://fabricmc.net'><img alt="fabric" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/fabric_vector.svg"></a>
<a href='https://neoforged.net/'><img alt="neoforge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg"></a>

A lightweight utility for replacing one block with another, both during worldgen and afterward.
This mod allows modpack creators to effortlessly ban items or restrict their
usage using standard JSON files, without the need for complex scripts.

## What About [Block Swap](https://modrinth.com/mod/block-swap)?

I'm glad you asked! I made Reliable Replacer because of some of my personal frustrations with Block Swap. Here are
some reasons to consider switching!

<details>
<summary>Why Reliable Replacer?</summary>

* Reliable Replacer is frequently updated and supports the latest versions, while as of the time of writing this, Block
  Swap is stuck on 1.20.1.
* Live config reloading! Run `/reload` and see your changes instantly take place.
* Advanced filtering:
    * Filter swaps by biome, dimensions, placed feature, coordinates, or structure!
    * Toggle retrogen on or off _per rule_.
    * Toggle player placement replacing _per rule_.
* Much more flexible configuration, supporting simple JSON configuration in user-defined folders and integrating with
  Cloth Config.
* Per-rule toggleable persistence:
    * Decide if replacement blocks should inherit the properties of the old block.
* Doesn't require any external libraries.

</details>

## Features

A full feature list is available on
the [Modded Minecraft Wiki](https://moddedmc.wiki/en/project/reliable-replacer/latest/docs/reliable-replacer/features).

For information and examples on how to use the mod, please also refer to
the [wiki](https://moddedmc.wiki/en/project/reliable-replacer/latest/docs/reliable-replacer/usage).

## Quick-Start

Create a file called `my_replacement.json`, or whatever else you'd like, in `config\reliable_replacer`:

```json
{
  "inputs": [
    "minecraft:dirt"
  ],
  "output": "minecraft:diamond_block",
  "biomes": [
    "minecraft:plains"
  ],
  "min_y": "64"
}
```

## License

[![Code license (MIT)](https://img.shields.io/badge/code%20license-MIT-green.svg?style=flat-square)](https://github.com/evanbones/Reliable-Replacer/blob/1.20.1/LICENSE)