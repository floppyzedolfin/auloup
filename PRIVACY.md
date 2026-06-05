# Privacy Policy — Au loup!

_Last updated: 2026-06-05_

**Au loup!** ("the app") is a call-blocking app for Android. This policy explains
what the app does and does not do with your information. The short version: the
app collects nothing, sends nothing, and has no servers.

## What we collect

**Nothing.** The app has no accounts, no analytics, no advertising, no crash
reporting, and makes no network connections of any kind. We, the developers,
never receive any data from the app.

## What stays on your device

All data the app uses is created and stored **only on your device**, and never
leaves it:

- **The list of phone-number prefixes** you choose to block.
- **A local history of blocked calls** (the caller's number and the time) so the
  app can show you per-prefix counts and a monthly calendar.
- **Your settings** (app language, theme, and whether to show a notification when
  a call is blocked).

This is stored using Android's standard on-device storage (Jetpack DataStore).
Removing a prefix deletes its blocked-call history, and uninstalling the app
deletes everything. The app also **disables Android's automatic backup**
(`allowBackup="false"`), so this data is never copied to any cloud.

## Your data rights (GDPR)

Au loup! is designed so EU data-protection law (the GDPR) is a non-issue:
nothing leaves your device, so the developer never holds your data and is not a
"data controller" for it — running the app on your own phone is personal /
household use.

The only personal data involved (a caller's number while a call is screened, and
the prefixes and history you save) is processed **locally**, solely to do the one
thing you asked: block calls. There is no profiling, no advertising, no
third-party sharing, and no international data transfer.

You keep full control:

- **Access & portability** — all your data is the prefix list and blocked-call
  history shown in the app, on your device.
- **Erasure** — remove a prefix to delete its blocked-call history, or uninstall
  the app to erase everything. (No cloud copy exists to delete.)

## Permissions the app uses, and why

- **Call screening role** (`RoleManager.ROLE_CALL_SCREENING`): Android lets one
  app screen incoming calls. When you grant this role, the system asks the app —
  on each incoming call — whether to allow or reject it. The app only compares
  the incoming number against your prefix list, on your device. It does **not**
  read your call log or contacts, and it does not have access to call audio.
- **Post notifications** (`POST_NOTIFICATIONS`, Android 13+): only used to show
  the optional "call blocked" notification, which you can turn off.

## Children's privacy

The app collects no data from anyone, including children.

## Changes to this policy

If this policy ever changes, the updated version will be published at the same
location with a new "Last updated" date.

## Contact

Questions about privacy: floppyzedolfin@gmail.com
