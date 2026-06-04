# Contributing to Au loup!

Thanks for your interest! The codebase is intentionally small and tidy — please
help keep it that way.

## Building

You need an LTS JDK (17 or 21) and the Android SDK. The easiest path is
[Android Studio](https://developer.android.com/studio); from the command line,
see the build section in the [README](README.md).

```sh
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # build a debug APK
```

## Before you open a PR

CI runs these on every push and pull request; please run them locally first:

```sh
./gradlew ktlintFormat   # auto-format Kotlin
./gradlew ktlintCheck    # style check
./gradlew lintDebug      # Android lint
./gradlew testDebugUnitTest
```

## Guidelines

- Keep pure logic (no Android dependencies) in the `telephony` package
  (`Prefixes.kt` / `Countries.kt`) and cover it with unit tests.
- Avoid adding dependencies unless they clearly earn their place.
- Match the style of the surrounding code; `ktlintFormat` handles the rest.

By contributing, you agree that your contributions are licensed under the
project's [GPL-3.0](LICENSE) license.
