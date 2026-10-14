# VichGram — clean Android base

This branch, `dsh-clean-telegram-base`, is a cleanup starting point for integrating a proper Telegram Android client implementation. It is **not yet a functional messenger**.

## Retained essentials

- Android Gradle project structure and app configuration.
- VichGram application ID, label, and app icon resource.
- Android manifest, launcher activity, and FileProvider path configuration.
- GitHub Actions workflows for Android APK builds and TDLib package preparation.
- Environment/Actions-secret based Telegram API credential configuration (values must never be committed).
- Release/build pipeline and stable signing-key cache configuration.

## Removed from this branch

The previous hand-built `MainActivity` chat UI and its custom chat-list/settings behavior were replaced with a minimal launcher placeholder. No claim is made that Telegram login, chats, history, or messaging work in this placeholder.

## Next integration steps

1. Bring in and review the official Telegram Android client source and its license/build requirements.
2. Decide the correct architecture and dependencies; do not assume the existing TDLib-only scaffold is the same as the official Telegram Android app.
3. Integrate in small commits and build after each step.
4. Test authentication and real chats on a device before claiming functionality.

## Build

GitHub Actions workflows are retained. The Android workflow expects the TDLib artifact from the `build-tdlib.yml` workflow. Telegram API credentials, when needed, belong in repository Actions secrets:

- `VICHGRAM_TG_API_ID`
- `VICHGRAM_TG_API_HASH`

Never place credential values in source files or commit history.
