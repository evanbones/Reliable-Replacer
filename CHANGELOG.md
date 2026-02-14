# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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