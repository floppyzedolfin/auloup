# Privacy Policy — Au loup!

_Last updated: 2026-06-03_

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
  app can show you per-prefix counts and charts.
- **Your settings** (app language, and whether to show a notification when a call
  is blocked).

This is stored using Android's standard on-device storage (Jetpack DataStore).
Removing a prefix deletes its blocked-call history. Uninstalling the app deletes
everything.

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
