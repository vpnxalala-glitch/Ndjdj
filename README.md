# VichGram

VichGram is an Android project with a neon cyberpunk interface and a floating assistant/browser window. The current build includes an embedded Telegram Web entry point inside the floating window. This is a web client, not a native TDLib implementation; native Telegram API integration, background update handling, and full multi-account management remain future work.

## Current features
- Floating overlay window with adjustable width, height, and bubble size
- Embedded browser, web-based AI entry point, and Telegram Web in a floating window
- App launcher and text/voice commands
- Quick links to Android settings
- Phone-number format hints and links to public search services
- Neon-inspired icon and rounded UI cards

## Build
GitHub Actions builds the debug APK on pushes to `main` and publishes a prerelease when the build succeeds. Download the latest APK from the repository Releases page.

## Planned
- Telegram client using TDLib and the official Telegram API
- Multi-account support, chat folders, privacy options, and local AI utilities
- Expanded neon theme and settings

A Telegram `api_id` and `api_hash` are required for the client integration. Keep credentials out of source control and public logs.
