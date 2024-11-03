# Change Log
All notable changes to this project will be documented in this file.

## [v1.0.2] - 2024-11-03

### Added
-- 
### Changed
Updated HotlistSourceType enum values in the SDK:
- Renamed `PASSED_ONLY` to `EMBEDDED`: Still uses the database sent via intent.
- Renamed `LOADED_ONLY` to `LOCAL`: Still uses the hotlists loaded within the Sentinel/Legion app.
- Renamed `BOTH` to `ALL`: Continues to use both the database sent via intent and the hotlists loaded within the Sentinel/Legion app.

### Fixed
-- 