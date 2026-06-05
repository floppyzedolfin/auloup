# Au loup! <img src="docs/iris.svg" alt="Iris, the sleepy wolf" align="right" width="76">


[![CI](https://github.com/floppyzedolfin/auloup/actions/workflows/ci.yml/badge.svg)](https://github.com/floppyzedolfin/auloup/actions/workflows/ci.yml)

**English** · [Français](README.md)

Block incoming calls by **phone-number prefix**. You keep a list of prefixes
(e.g. `+1900`, `0900`); any incoming call whose number starts with one of them
is rejected automatically.

## The story of Iris

Iris was everyone's favourite culprit. Whenever Peter got bored he'd shout; the
whole village came running, pitchforks in hand — and it was always her, the
wolf, they came to hunt. Yet Iris had never so much as looked at the flock. All
she wanted was to sleep in peace, her muzzle resting on her paws.

Tired of being chased over alarms that were never hers, Iris turned the pack
around. Now she's the one keeping watch — not against the villagers, but
alongside everyone who's had enough of being harassed. When a cold-caller rings,
Iris opens one eye, and quiet returns.

Free and open source (GPL-3.0). Android first; iOS is planned (see
[Platform support](#platform-support)).

## Support the wolves

Au loup! is free and asks nothing for itself. If you'd like to give back, support
**real** wolves through any of these French associations that defend them (all
founding members of the [CAP Loup](https://www.cap-loup.fr/) collective):

- **[ASPAS](https://www.aspas-nature.org/)** — wild-animal protection; a
  public-interest body with independently audited accounts. Donate online:
  <https://www.aspas-nature.org/nous-soutenir/faire-un-don/>
- **[FERUS](https://www.ferus.fr/)** — the specialist for wolves, bears and
  lynx. Donate online: <https://www.ferus.fr/soutenez-nos-actions/dons-2>
- **[WWF France](https://www.wwf.fr/)** — global nature conservation; a
  public-interest foundation. Donate online: <https://faireundon.wwf.fr/>

All donate online by card; in France 66% of your gift is tax-deductible (a €5
donation costs you €1.70).

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
- Per-day and per-hour charts of blocked calls, on the main screen and per prefix.
- Group the list by country, collapsible, with a per-country total.
- Import **official** regulator block lists (France's ARCEP telemarketing
  ranges); imported prefixes are flagged *Official* vs. user-added ones.
- Optional silent status-bar notification when a call is blocked (toggleable).
- Settings page: change the app language and toggle notifications.

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
support JDK 25). Set `sdk.dir` in a `local.properties` file (Android Studio
writes this for you).

The simplest path is `make`, which finds a compatible JDK for you and writes the
APK to `app/build/outputs/apk/debug/auloup.apk`:

```sh
make            # build the debug APK (auloup.apk)
make install    # build and install on a connected device/emulator
make clean      # remove build outputs
```

If `make` can't find a JDK, point it at one: `make JAVA_HOME=/path/to/jdk-21`.

Or call Gradle directly (works on Windows too via `gradlew.bat`; point it at the
LTS JDK with `JAVA_HOME` or `org.gradle.java.home`):

```sh
./gradlew test           # run the JVM unit tests
./gradlew assembleDebug  # build a debug APK -> auloup.apk
./gradlew installDebug   # install on a connected device/emulator
```

### Build an APK and put it on your phone

This produces an installable file you can copy to an Android phone by hand — no
Android Studio, no USB debugging required.

**1. Prerequisites** (one-time): an LTS JDK (17 or 21 — *not* 25, which current
Gradle rejects) and the Android SDK. With Android Studio installed, both come
bundled. Without it, install the
[command-line tools](https://developer.android.com/tools), then:

```sh
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # path to your SDK
```

**2. Build the APK** from the project root:

```sh
make
```

The APK is written to:

```
app/build/outputs/apk/debug/auloup.apk
```

**3. Copy it to the phone.** Any transfer works — USB file copy (the phone shows
up as a drive; drop the APK into `Download/`), Quick Share / Nearby Share, email,
or a cloud drive.

**4. Install it on the phone.** Open the **Files** app, tap `auloup.apk` in
`Download/`, and confirm **Install**. The first time, Android asks to *Allow
install from this source* — toggle it on for Files (or your browser), then back
out and tap the APK again. (Settings path if needed: *Apps → Special app access
→ Install unknown apps*.)

**5. First run.** Open **Au loup!**, tap **Enable call blocking** and make it your
call-screening app, allow the notification prompt, then add a prefix.

> This is a **debug** APK signed with the throwaway debug key — perfect for
> installing on your own phone. For a Play Store release (signed `.aab`), see
> [RELEASE.md](RELEASE.md); build it with `make bundle`.

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

[GPL-3.0](LICENSE). © Au loup! contributors.
