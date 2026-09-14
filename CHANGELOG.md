# Changelog for Cast API

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0] - 2026-09-15

### Added

- Added `launchDefaultMediaReceiver()`.
- Added `CHANGELOG.md`.

### Changed

- Renamed `getApplications()` to `getRunningApplications()`.
- Reorganized `Metadata` class structure.
- Made message parsing more lenient.
- Allowed non-standard `SUBTITLE` as an alias for `SUBTITLES` when deserializing `TrackSubType`.

### Fixed

- Renamed a forgotten instance of `launch()` to `launchApplication()`.
- Fixed various JavaDoc inconsistencies.

## [0.1.2] - 2025-11-06

### Changed

- Allowed null `MediaVolume` in `MediaStatus`.
- Updated dependencies to the latest versions that support Java 7.

### Fixed

- Made exception handling slightly more robust.
- Fixed too early registration attempts for discovered cast devices and added logging to discovery.

## [0.1.1] - 2022-10-28

### Changed

- Updated logback.

### Fixed

- Various minor fixes.
- Minor logging and exception handling tweaks.
- Fixed `X509TrustAllManager`.

## [0.1.0] - 2021-07-26

### First release

- Requires Java 7 or later.

[Unreleased]: https://github.com/DigitalMediaServer/Cast-API/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/DigitalMediaServer/Cast-API/compare/v0.1.2...v0.2.0
[0.1.2]: https://github.com/DigitalMediaServer/Cast-API/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/DigitalMediaServer/Cast-API/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/DigitalMediaServer/Cast-API/releases/tag/v0.1.0
