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