# Persian Agent — Android assistant

A Persian-first Android voice assistant with a lightweight, local command engine.

## Android app
The native Android app is in [android/](android/). It includes Persian speech input/output, contact calling with confirmation, SMS composer handoff, reminders, web search, app launching, bounded local memory, and opt-in reading/replying to supported message notifications.

## Build and install
See [Android setup and limitations](android/README.md). GitHub Actions builds a debug APK artifact; a successful CI build verifies compilation, not real-device behavior.

## Important limitations
- The Android engine is command/rule based; a full local large language model is not bundled.
- On-device Persian speech recognition and Persian text-to-speech depend on device/provider language support.
- Message reading requires explicit notification access. Quick replies work only when the message app exposes a direct-reply action.
- Always-on wake-word behavior and real-device compatibility are not guaranteed.

The original lightweight web app remains at the repository root. No API keys should be committed to this public repository.
