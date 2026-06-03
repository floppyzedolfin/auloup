# Releasing Au loup! to Google Play

This is the end-to-end checklist for publishing to the Play Store. The repo is
already set up to produce a signed release bundle; the remaining steps are an
account, a signing key, and the store listing.

## 1. One-time prerequisites

- [ ] A **Google Play Developer account** (one-time US$25): <https://play.google.com/console/signup>.
- [ ] Accept the Developer Distribution Agreement in the Play Console.
- [ ] An LTS JDK (17 or 21) and the Android SDK — same as for normal builds
      (`make` finds the JDK automatically; see README).

## 2. Create your upload key (do this once, keep it forever)

Google Play uses **Play App Signing**: Google holds the real app-signing key, and
you sign uploads with your own **upload key**. If you lose the upload key you can
ask Google to reset it, but treat it as precious anyway.

Generate the upload keystore at the repo root:

```sh
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

It will ask for a keystore password, a key password, and your name/org. Then tell
the build about it by copying the template and filling it in:

```sh
cp keystore.properties.example keystore.properties
# edit keystore.properties: set storePassword, keyPassword, keyAlias=upload,
# storeFile=upload-keystore.jks
```

> **Both `upload-keystore.jks` and `keystore.properties` are gitignored** and must
> never be committed. Back them up somewhere safe and private (a password
> manager, an encrypted drive). Without `keystore.properties` the release build
> falls back to the debug key, which the Play Store will reject.

## 3. Build the release bundle

```sh
make bundle      # -> app/build/outputs/bundle/release/app-release.aab
```

Play requires an **`.aab`** (Android App Bundle), not an APK. The build already
applies R8 code/resource shrinking (the release is ~3 MB). To smoke-test the
release build on a device first:

```sh
make release     # signed release APK you can sideload
```

## 4. Create the app in Play Console

- [ ] **Create app** → name **Au loup!**, default language, app type **App**,
      **Free**.
- [ ] Enroll in **Play App Signing** (default for new apps) and upload the AAB to
      an **Internal testing** track first, then promote to Production.

## 5. Store listing copy (ready to paste)

**App name** (max 30 chars):

```
Au loup! — Call Blocker
```

**Short description** (max 80 chars):

```
Block spam calls by phone-number prefix. Private, offline, open source.
```

**Full description** (max 4000 chars):

```
Au loup! blocks unwanted incoming calls by phone-number PREFIX, not just full
numbers. Keep a list of prefixes (for example a country's telemarketing range,
or an area code) and any call whose number starts with one of them is rejected
automatically.

• Block by prefix — add a country (flag + calling code) and a national prefix.
• Understands each country's trunk prefix, so "01 60" in France and "+33 1 60"
  are the same, and a call matches whether its caller ID arrives in national or
  international form.
• See how many calls each prefix has blocked, with per-day and per-hour charts.
• Tap a prefix to view the calls it blocked, with numbers formatted the way each
  country writes them.
• Import official regulator block lists (e.g. France's ARCEP telemarketing
  ranges), shown as "Official" vs. your own entries.
• Optional silent notification when a call is blocked.
• Available in English and 60+ other languages.

PRIVACY: Au loup! has no accounts, no ads, no tracking, and makes no network
connections. Your prefix list and block history never leave your device.

Au loup! is guarded by Iris, a sleepy she-wolf: silence the callers who keep
crying wolf, and she finally gets to sleep.

Free and open source (GPL-3.0). Source: https://github.com/floppyzedolfin/auloup
```

- [ ] **Category**: Tools (alternative: Communication).
- [ ] **Contact email**: confirm before publishing (privacy policy currently
      lists `floppyzedolfin@gmail.com`).

## 6. Graphics assets (you must supply these)

Play requires, at minimum:

- [ ] **App icon** — 512 × 512 px, 32-bit PNG. Generate from the in-app logo
      (`app/src/main/res/drawable/ic_logo.xml` / `ic_launcher_foreground.xml`),
      e.g. via Android Studio → right-click `res` → New → Image Asset, or export
      the vector to a 512 PNG.
- [ ] **Feature graphic** — 1024 × 500 px PNG/JPG.
- [ ] **Phone screenshots** — at least 2 (up to 8), 16:9 or 9:16, min 320 px.
      You can capture these from a device/emulator:
      `adb exec-out screencap -p > screenshot.png`. Good candidates: the main
      list grouped by country, and a per-prefix stats page.

## 7. Privacy policy (required)

- [ ] Host **PRIVACY.md** at a public URL and paste that URL into the Play
      Console. Easiest option: enable GitHub Pages, or link directly to the file
      on GitHub (e.g. `https://github.com/floppyzedolfin/auloup/blob/main/PRIVACY.md`).

## 8. Data safety form

Answer (matches PRIVACY.md):

- [ ] **Does your app collect or share any user data?** → **No.**
- [ ] No data types collected, none shared, no third parties.
- [ ] Data is processed only on-device.

## 9. Content rating & policy questionnaires

- [ ] **Content rating**: a utility/communication app with no objectionable
      content — answer "No" to all violence/sexual/etc. questions → rating
      "Everyone".
- [ ] **Target audience**: not directed at children (general audience).
- [ ] **Ads**: app contains no ads → "No".
- [ ] **Government apps / financial / health**: No.

### Permissions note

The app uses the **CallScreeningService role** and `POST_NOTIFICATIONS`. It does
**not** request `READ_CALL_LOG`, `READ_PHONE_STATE`, or `READ_CONTACTS`, so the
Play **Permissions Declaration Form** for Call Log / SMS access does **not**
apply. If Play ever asks about call/phone permissions, the honest answer is that
the app only uses the call-screening role to reject calls and never reads the
call log or contacts (see PRIVACY.md). Be ready to explain this in a review.

## 10. Versioning for future updates

Each Play upload needs a higher `versionCode`. In `app/build.gradle.kts`:

- `versionCode` — integer, **increment by 1 every release** (currently `1`).
- `versionName` — human-readable string shown to users (currently `1.0.0`).

## Quick reference

| Item | Value |
| --- | --- |
| Application ID | `com.floppyzedolfin.auloup` |
| Min SDK / Target SDK | 29 / 35 |
| Release artifact | `app/build/outputs/bundle/release/app-release.aab` (`make bundle`) |
| Signing | upload key via gitignored `keystore.properties` (debug fallback if absent) |
| Privacy policy | `PRIVACY.md` (host at a public URL) |
