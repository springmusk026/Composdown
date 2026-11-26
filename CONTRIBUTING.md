# Contributing to Composdown

Thanks for your interest in improving Composdown. This document explains how to build the project,
the standards every change is held to, and how to submit your work.

By participating, you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Prerequisites

- JDK 17 or newer (CI uses JDK 21).
- The Android SDK, with `ANDROID_HOME` set or `sdk.dir` in `local.properties`.
- macOS with Xcode to build and test the iOS targets. On Linux and Windows the iOS tasks are skipped
  automatically. CI runs them on a macOS runner for every pull request.

## Project layout

| Module | Purpose |
|---|---|
| `composdown-core` | Pure Kotlin Multiplatform: Markdown AST, incremental parser, CommonMark/GFM spec tests. No UI dependencies. |
| `composdown-compose` | Compose Multiplatform renderer, theme and streaming state holder. |
| `build-logic` | Gradle convention plugins shared by all modules. |

Library dependency versions live in `gradle/libs.versions.toml`. Shared build configuration lives in
the convention plugins under `build-logic/convention`, not in individual module build files.

## Building and testing

```bash
./gradlew check          # everything: compile, tests, Spotless, detekt, API check
./gradlew desktopTest    # Desktop (JVM) tests
./gradlew testAndroidHostTest   # Android host (JVM) tests
./gradlew iosSimulatorArm64Test # iOS simulator tests (macOS only)
```

## Code standards

- **Formatting:** ktlint (`ktlint_official` style) with the Compose rules ruleset, applied through
  Spotless. Run `./gradlew spotlessApply` before committing.
- **Static analysis:** detekt, configured in `config/detekt/detekt.yml`. Any rule override must carry a
  comment explaining why.
- **Warnings are errors.** Do not suppress a warning without a comment that justifies it.
- **Explicit API mode** is enabled in every library module. Every public declaration needs an explicit
  visibility modifier, an explicit return type, and KDoc.
- **Keep code in `commonMain`.** Use `expect`/`actual` only when a capability truly differs per
  platform, and document the reason on the `expect` declaration.
- **No placeholder code or TODOs** in merged code. Open an issue instead.

## Public API changes

The public ABI of every module is recorded in `api/` dumps (JVM and KLib) and checked by `apiCheck`.
If you change public API on purpose, run `./gradlew apiDump` and commit the updated dumps, so the change
is visible in review. KLib dumps are only exact when generated on macOS; if you are on Linux and the
CI `apiCheck` job disagrees with your dump, use the dump produced by CI.

## Tests

- Parser logic: `kotlin.test` in `commonTest`, so it runs on every platform.
- Parser changes must keep the CommonMark and GFM spec suites at 100% and must not break the
  streaming-equivalence tests: feeding a document in arbitrary chunks must produce the same result as
  parsing it in one go.
- Rendering changes: Compose UI tests and, where visual, updated screenshot goldens.

## Commits and pull requests

- Commits follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`,
  `docs:`, `test:`, `build:`, `ci:`, `refactor:`, `perf:`, `chore:`). Use a `!` or a
  `BREAKING CHANGE:` footer for breaking changes.
- Keep commits small and focused; each one should build and pass tests.
- Add an entry under **Unreleased** in [CHANGELOG.md](CHANGELOG.md) for any user-visible change.
- Fill in the pull request template, including how you verified the change.

## Versioning

Composdown follows [Semantic Versioning](https://semver.org/). Before 1.0.0, minor releases may contain
breaking changes; these are always called out in the changelog.
