### Changed

- Large worldgen performance and memory improvements.
- Removed filtering by feature for simplicity and performance.
    - This is a relatively niche usecase compared to structure/biome filtering, and incurred a _heavy_ memory overhead.
      Similar results are possible using Vanilla datapacks.

### Fixed

- Fix y-level handling on structure filters.
- General backend refactors/cleanups.