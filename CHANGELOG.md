# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.7] - 2026-06-28

### Fixed

- Fixed issues with loot modification.

## [1.5.6] - 2026-06-27

### Changed

- Add support for an array of `match_ids` in Reliable Replacer.

## [1.5.5] - 2026-06-04

### Fixed

- Hopefully fixed No Man's Land crash.

## [1.5.4] - 2026-06-03

### Fixed

- Fixed crash when features are placed outside `WorldGenRegion` bounds during world generation (@Rozarke).
- Fixed log spam caused by early loading.

## [1.5.3] - 2026-06-01

### Fixed

- Fixed issues with C2ME.

## [1.5.2] - 2026-06-01

### Fixed

- Fixed issues with feature blocks not swapping across chunk boundaries.
- Fudged tree biomes at biome borders, so now leaf replacements (for example) will evaluate as true as
  long as the trunk is in the targeted biome.

## [1.5.1] - 2026-05-28

### Fixed

- Fix log spam with certain NBT-related rules.

## [1.5.0] - 2026-05-28

### Fixed

- Fixed double chests not forming after swaps.
- Hopefully fixed issues with structure filters not fully replacing structure blocks.
- Performance and stability enhancements.

## [1.4.0] - 2026-05-01

### Fixed

- Fixed issues with swapping block entities.

## [1.3.0] - 2026-02-28

### Added

### Added

- Added NBT replacement inside inventories.
- Added logic for item randomization with NBT replacement.

## [1.2.10] - 2026-02-23

### Changed

- Output blocks in rules are now validated and will throw a warning to the console if invalid.

## [1.2.9] - 2026-02-22

### Fixed

- Fixed crash with certain mods that simulate level rendering (like GuideME).

## [1.2.8] - 2026-02-22

### Fixed

- Improved worldgen and placement handling for multiblocks (doors, tall flowers, etc.)
    - This means multiblock rules should work seamlessly in simple input/output swaps without any additional filtering.

## [1.2.7] - 2026-02-21

### Fixed

- Added explicit support for replacing air.
    - **NOTE**: While this is _technically_ supported, it can/will cause worldgen lag when used on a large scale. Be
      careful!

## [1.2.6] - 2026-02-19

### Added

- Missing ID swaps now support structure repaletting.

## [1.2.5] - 2026-02-18

### Fixed

- Config now generates on game startup, instead of on world load.

## [1.2.4] - 2026-02-16

### Fixed

- Fixed issue when using `output_nbt` and `structure` filters together.

## [1.2.3] - 2026-02-15

### Added

- Added a filter for `input_nbt` for targetting specific blocks with NBT.
- Added a filter for `output_nbt` for placing blocks with NBT.

## [1.2.2] - 2026-02-14

### Fixed

- Fixed intial worldgen replacement sometimes missing blocks.
- Fixed randomization affecting all vertically stacked blocks.

### Added

- Added `additional_blocks` to rules for placing or removing multi-block structures (like doors, beds, or tall grass) at
  specific offsets.
- Added logging for missing block IDs in replacement rules.

## [1.2.1] - 2026-02-13

### Added

- Added `outputs` to replacement rules, allowing you to replace input blocks with a random output from an array of
  blocks.

## [1.2.0] - 2026-02-13

### Fixed

- Fixed visual flicker when placing blocks.

### Added

- Added `output_state_properties` to replacement rules, allowing you to enforce specific properties on the output
  block (e.g., forcing `axis=y` for vertical logs).
- Added `randomize_properties` to replacement rules, enabling randomization of block properties (e.g., randomizing
  rotation or facing direction).
- Added tag support for inputs (e.g. `#minecraft:logs`).
- Added the `self`/`this` keyword to the output field to allow modifying the properties of the original block
  without changing its type.
- Added smart defaults in the output field: if output is omitted, the mod now assumes a 1-to-1 mapping.

## [1.1.0] - 2026-02-11

### Added

- Rules can now be restricted to specific block properties (e.g., only replacing `half=upper` for flowers or `lit=true`
  for furnaces).
- Added the ability to trigger replacements based on adjacent blocks (e.g., only replace if there is air above or water
  below).
- Introduced a probability field to allow for non-deterministic swaps.
- Added a `remove` shorthand to rules to easily clear blocks without manually defining `minecraft:air` as the output.

## [1.0.4] - 2026-02-07

### Changed

- Large worldgen performance and memory improvements.
- Removed filtering by feature for simplicity and performance.
    - This is a relatively niche usecase compared to structure/biome filtering, and incurred a _heavy_ memory overhead.
      Similar results are possible using Vanilla datapacks.

### Fixed

- Fix y-level handling on structure filters.
- General backend refactors/cleanups.

## [1.0.3h] - 2026-02-07

### Changed

- Affecting player placed blocks is disabled by default on structure, biome, and feature filters, unless specifically
  enabled with `"player_blocks": "true"`.

### Fixed

- Fixed structure filters not working until a world reload.

## [1.0.3] - 2026-02-07

### Changed

- Performance improvements.
- Retrogen is disabled by default on structure, biome, and feature filters, unless specifically enabled with
  `"retrogen": "true"`.

### Fixed

- Fixed structure filters not working properly with multiple rules.

## [1.0.2] - 2026-02-04

### Added

- Added support for retaining NBT data with `keep_nbt` (default true).

### Changed

- Rename `apply_to_player_placement` to `player_blocks`.

## [1.0.1h] - 2026-02-02

### Fixed

- Fixed missing refmap crash on Fabric.

## [1.0.1] - 2026-02-02

### Added

- Create default example config, if none exists already.
- Create default `swapper.json` to easily migrate from Block Swap.

## [1.0.0] - 2026-01-31

* Initial release.