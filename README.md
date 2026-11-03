# AURA PULSE

A customizable Android live-island style companion app inspired by compact live activity capsules.

## Device compatibility goal

- Target Android 6.0 (API 23) and newer where device APIs allow.
- Layouts should adapt to different screen sizes and densities.
- Overlay, notification-listener, media-session and battery features must request the relevant Android permissions/access and use fallbacks when a manufacturer or Android version restricts background behavior.
- No app can guarantee identical behavior on every Android phone; OEM battery management and permission policies differ.

## Build

GitHub Actions builds a debug APK on every push to `main` and when manually triggered. Open **Actions → Android APK Build** and download the `aura-pulse-debug-apk` artifact after a successful run.

## Current foundation

- Dark premium AURA PULSE home screen
- Island-style preview capsule
- Feature cards for music, charging, notifications and timers
- Android 6+ minimum SDK baseline
- Automated Android debug APK build

This is an independent Android app inspired by the general live-island concept; it does not copy Apple's or Islandify's branding or proprietary implementation. Full live features are being added in stages.
