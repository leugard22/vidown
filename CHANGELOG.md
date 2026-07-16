# Changelog

All notable changes to this project will be documented in this file.

## [1.0.20260716] - 2026-07-16

### Added

- **Material 3 Settings Rework**: Grouped configuration parameters inside elevated card layouts, added inline animated selection dropdowns, and cleaned up accounting widgets.
- **Storage Access Framework (SAF) Support**: Custom download destination configuration, allowing direct write access to external folders (like SD cards) with fallback media folder handling.
- **Concurrent Downloads Scheduler**: Parallel downloader queue executing downloads in concurrent coroutines up to a user-configured limit.
- **Actionable Progress Notifications**: Aggregate progress bar notifications featuring quick actions (`PLAY` completed files, `SHARE` documents, `RETRY` failed tasks).
- **Secure YouTube Login**: Firefox Desktop User-Agent routing split inside the login WebView to bypass Google's "disallowed_useragent" blocker.
- **TikTok Login Redirect Fix**: Mobile Safari User-Agent configuration to force mobile-optimized pages, coupled with custom URI scheme overrides (`tiktok://`, `intent://`) to prevent WebView context freezes.
- **Subtitle Language Customization**: Expandable dialog setting to choose specific subtitle tracks (e.g. English, French, Spanish) or define custom codes.
- **Download Speed Limit Throttling**: Support for limiting active task speeds (Unlimited, 100 KB/s, 500 KB/s, 1 MB/s, 2 MB/s, 5 MB/s).
- **GitHub Actions Workflows**: Automatic build verification on branch pushes (`build.yml`) and automated signing + assets distribution on version tag pushes (`release.yml`).
- **Tag-based Versioning**: Automated project versionCode (number of tags + 1) and versionName (latest tag name).
