### Added

- Added `radius`, `biome_radius`, `feature_radius` and `structure_radius` for filters.
- Added `feature` to filter replacements by placed feature type.
- Neighboring block checks now support block arrays, tags (`#tag`), and wildcards.
- Support for direction keywords in neighbor checks (`any`, `all`, `sides`/`horizontal`, `all_sides`/`all_horizontal`).
- Added config option to globally disable retrogen.

### Changed

- Improved rule parsing to be more forgiving with key names.
- Improve structure filtering.
- Code cleanups.

### Fixed

- Fixed `structures` filters being ignored for blocks placed during world generation.
- Fixed structure filters matching the whole structure bounding box on live placement instead of its individual pieces.
- Fixed issues with retrogen (thanks, @tazer!).
- Performance improvements.