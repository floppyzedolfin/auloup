# CallBloker

[![CI](https://github.com/floppyzedolfin/callbloker/actions/workflows/ci.yml/badge.svg)](https://github.com/floppyzedolfin/callbloker/actions/workflows/ci.yml)

Block incoming calls by **phone-number prefix**. You keep a list of prefixes
(e.g. `+1900`, `0900`); any incoming call whose number starts with one of them
is rejected automatically.

Free and open source (GPL-3.0). Android first; iOS is planned (see
[Platform support](#platform-support)).

## Status

Early MVP:

- Maintain a persistent list of blocked prefixes, chosen via a country picker
  (flag + calling code) plus a national-prefix field (at least 3 digits).
- Understands each country's national (trunk) prefix, so entering `01 60` in
  France is the same as `+33 1 60`, `07…` in the UK is `+44 7…`, and an incoming
  call matches whether its caller ID arrives in national or international form.
- Reject incoming calls that match — silently, with no entry in your call log.
- Count how many calls each prefix has blocked. When several prefixes match a
  call, the most specific (longest) one is credited.
- Tap a prefix to see the history of calls it blocked (number + time).
- Optional silent status-bar notification when a call is blocked (toggleable).

No accounts, no network, no tracking. Your prefix list never leaves the device.

## Platform support

| Platform | Prefix blocking | How |
| --- | --- | --- |
| **Android** (10 / API 29+) | ✅ Full | A [`CallScreeningService`](https://developer.android.com/reference/android/telecom/CallScreeningService) runs our code on every incoming call and matches the prefix at call time. |
| **iOS** | ⏳ Planned, limited | iOS call blocking (`CallKit` Call Directory) only accepts a static list of **full numbers** — the OS does the matching and app code never runs at call time, so true prefix matching isn't possible. iOS support will land separately with a different, full-number model. |

This is why the project is Android-first: prefix blocking is a first-class
feature there, and a fundamentally constrained one on iOS.

## How it works

```
MainActivity (Compose UI)  ─┐
                            ├─►  PrefixRepository  ──►  DataStore (on-device)
PrefixCallScreeningService ─┘            ▲
        (system-invoked)                 │
                                   Prefixes (pure matching logic)
```

- `Prefixes.kt` — pure, Android-free matching logic (`normalize`, `longestMatch`, `isBlocked`); unit-tested.
- `Countries.kt` — ISO→calling-code data; country names from `Locale`, flags as emoji.
- `PrefixRepository.kt` — persists the prefixes, the blocked-call history, and the notify preference (Jetpack DataStore). Counts are derived from the history.
- `PrefixCallScreeningService.kt` — the system binds this on each incoming call; it rejects matches and records the block.
- `Notifications.kt` — the silent "call blocked" notification channel and poster.
- `MainActivity.kt` — the Compose screens: the main list (enable blocking, toggle notifications, add/remove prefixes, see counts) and a per-prefix blocked-call history.

To block calls, the app must be granted the **call-screening role**
(`RoleManager.ROLE_CALL_SCREENING`); the UI prompts for this.

## Build & run

### With Android Studio (recommended)

1. Install [Android Studio](https://developer.android.com/studio). It bundles a
   compatible JDK, the Android SDK, and an emulator.
2. **Open** this folder. Let it sync Gradle.
3. Pick a device/emulator (Android 10+) and press **Run**.
4. In the app, tap **Enable call blocking** and grant the role, then add a prefix.

### From the command line

Requires the Android SDK and an LTS JDK (17 or 21 — current Gradle does not yet
support JDK 25). Point Gradle at it with `JAVA_HOME` or `org.gradle.java.home`,
and set `sdk.dir` in a `local.properties` file (Android Studio writes this for
you).

```sh
./gradlew test           # run the JVM unit tests
./gradlew assembleDebug  # build a debug APK
./gradlew installDebug   # install on a connected device/emulator
```

## Localization

The UI is fully translatable — all strings live in `res/values/strings.xml`,
counts use Android plurals (with the correct CLDR categories per language), and
country names are localized automatically via `Locale`. On Android 13+ the
language can be picked per-app in system settings.

Available in **English** plus **62 other languages** (Spanish, the Portuguese
variants, French, German, Chinese, Japanese, Arabic, Hindi, Russian, and many
more — see `res/values-*/`).

> ⚠️ The non-English/French translations are **machine-generated** as a
> starting point and have **not yet been reviewed by native speakers**.
> Corrections via PR are very welcome.

To add or improve a language, copy `app/src/main/res/values/strings.xml` to
`values-<code>/strings.xml`, translate the values (using the right plural
categories for that language), and add the locale to
`res/xml/locales_config.xml`.

## Contributing

Contributions welcome — the codebase is intentionally small and tidy. See
[CONTRIBUTING.md](CONTRIBUTING.md). In short: keep pure logic in `Prefixes.kt` /
`Countries.kt` with tests, and run `./gradlew ktlintFormat ktlintCheck lintDebug
testDebugUnitTest` before opening a PR (CI runs the same).

## License

[GPL-3.0](LICENSE). © CallBloker contributors.
