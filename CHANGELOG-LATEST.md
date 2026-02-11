### Added

- Rules can now be restricted to specific block properties (e.g., only replacing `half=upper` for flowers or `lit=true`
  for furnaces).
- Added the ability to trigger replacements based on adjacent blocks (e.g., only replace if there is air above or water
  below).
- Introduced a probability field to allow for non-deterministic swaps.
- Added a `remove` shorthand to rules to easily clear blocks without manually defining `minecraft:air` as the output.