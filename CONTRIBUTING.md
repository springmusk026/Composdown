# Contributing to Composdown

Thank you for your interest in improving Composdown. This guide explains how the project is organized,
how to build and test it, the standards every change is held to, and how changes are reviewed and
released.

By participating, you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md). To report a security
issue, follow [SECURITY.md](SECURITY.md) instead of opening an issue.

- [Ways to contribute](#ways-to-contribute)
- [Development setup](#development-setup)
- [Project layout](#project-layout)
- [Building and testing](#building-and-testing)
- [Quality gates](#quality-gates)
- [Making changes](#making-changes)
- [Commits and pull requests](#commits-and-pull-requests)
- [Releases](#releases)

## Ways to contribute

- **Report a bug** with the [bug report form](https://github.com/springmusk026/Composdown/issues/new/choose).
  The most useful reports include the Markdown input, the expected and actual rendering, and the
  platform. For parsing bugs, compare with the [CommonMark dingus](https://spec.commonmark.org/dingus/)
  or GitHub's rendering.
- **Propose a feature** with the feature request form. Describe the use case before the solution.
  Features that add public API are discussed in an issue before a pull request.
- **Improve documentation.** Corrections and clarifications to the guides in [`docs/`](docs) and to
  KDoc are always welcome, and need no prior discussion.
- **Fix an issue.** Issues labelled `good first issue` are a good starting point. Comment on the issue
  to avoid duplicate work.

## Development setup

| Requirement | Notes |
|---|---|
| JDK 17 or newer | CI uses JDK 21 |
| Android SDK | `ANDROID_HOME` set, or `sdk.dir` in `local.properties` |
| macOS with Xcode | Only for the iOS targets. On Linux and Windows, iOS tasks are skipped; CI runs them on macOS |
| XcodeGen | Only to run the iOS sample (`brew install xcodegen`) |

Open the project in Android Studio or IntelliJ IDEA with the Kotlin Multiplatform plugin. No other
setup is required: Gradle downloads every dependency.

## Project layout

| Path | Contents |
|---|---|
| `composdown-core` | Parser and document model: pure Kotlin, no UI dependencies |
| `composdown-core/spec` | CommonMark and GFM specification examples, from which tests are generated |
| `composdown-compose` | Compose Multiplatform renderer |
| `composdown-coil` | Coil-based image renderer |
| `composdown-highlight` | Highlights-based code highlighter |
| `benchmarks` | JMH parser benchmarks and the recorded [baseline](benchmarks/BASELINE.md) |
| `sample` | Demonstration app: `shared` UI, `desktop`, `android` and `ios` hosts |
| `tools/screenshots` | Generates the documentation images in `docs/images` |
| `build-logic` | Gradle convention plugins and code generation tasks |
| `config` | detekt configuration |
| `docs` | User guides |
| `scripts` | Scripts that regenerate checked-in data (Unicode tables, GFM examples) |

Dependency versions are declared only in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).
Build configuration shared by modules lives in the convention plugins, not in module build files.
[docs/architecture.md](docs/architecture.md) explains how the code is structured internally.

## Building and testing

```bash
./gradlew check
```

runs everything that CI runs on your platform: compilation, all tests, formatting, static analysis,
Android Lint and the API check. Narrower tasks for faster iteration:

| Task | Runs |
|---|---|
| `./gradlew :composdown-core:desktopTest` | Parser tests on the JVM (fastest feedback for parser work) |
| `./gradlew desktopTest` | All JVM tests, including Compose UI tests |
| `./gradlew testAndroidHostTest` | Android host tests |
| `./gradlew iosSimulatorArm64Test` | iOS simulator tests (macOS) |
| `./gradlew verifyRoborazziDesktop` | Screenshot tests |
| `./gradlew spotlessApply` | Formats the code |
| `./gradlew detekt` | Static analysis |
| `./gradlew apiCheck` / `apiDump` | Checks or records the public API |
| `./gradlew :benchmarks:benchmark` | Parser benchmarks |
| `./gradlew :sample:desktop:run` | Runs the sample app on Desktop |

## Quality gates

Every pull request must pass these checks in CI, on Linux and macOS:

| Gate | Standard |
|---|---|
| Formatting | ktlint (`ktlint_official`) with Compose rules, through Spotless |
| Static analysis | detekt with the configuration in `config/detekt`; Android Lint |
| Compiler | Warnings are errors |
| Public API | Explicit API mode: explicit visibility, explicit return types and KDoc on every public declaration |
| Binary compatibility | `apiCheck` against the committed dumps |
| Conformance | 100% of the CommonMark and GFM examples |
| Streaming | All streaming-equivalence tests |
| Rendering | Compose UI tests and screenshot tests |

A rule may be suppressed only with a comment explaining why the code is correct as written.

## Making changes

### General

- Keep code in `commonMain`. Use `expect`/`actual` only for capabilities that truly differ between
  platforms, and document the reason on the `expect` declaration.
- Prefer small, focused changes. A refactoring and a behavior change belong in separate commits.
- No placeholder code or `TODO` comments in merged code; open an issue instead.
- Update the relevant guide in `docs/` when you change behavior users rely on.

### Parser changes

- Write tests in `commonTest` with `kotlin.test`, so they run on every platform.
- Keep the specification suites at 100%. If a change alters which document is produced for some input,
  call it out in the pull request and in the changelog.
- The incremental engine must produce, after `finish()`, exactly the one-shot result for every way of
  splitting the input. Add the input that motivated your change to the streaming corpus.
- Avoid algorithms whose cost can grow faster than linearly with the input; add a case to
  `PathologicalInputTest` if a construct could be abused.
- Measure performance-sensitive changes with `./gradlew :benchmarks:benchmark` before and after, on
  the same machine, and include the numbers in the pull request.

### Rendering changes

- Cover behavior with Compose UI tests in `composdown-compose/src/desktopTest`.
- For visual changes, record new screenshots with `./gradlew :composdown-compose:recordRoborazziDesktop`,
  review every changed image in `composdown-compose/src/desktopTest/snapshots`, and commit them.
- If the change is visible in the documentation images, regenerate them as described in
  [`tools/screenshots`](tools/screenshots/README.md).
- Preserve accessibility semantics, and go through the relevant items of the
  [manual checklist](docs/accessibility.md#manual-verification-checklist).
- Check that completed blocks are still skipped during streaming
  (`completedBlocksAreNotRecomposedWhileTextIsAppended`).

### Public API changes

Public API is a long-term commitment. Before adding or changing it:

1. Open an issue describing the use case and the proposed API.
2. Follow the existing conventions: Compose API guidelines for composables, `ComposdownDefaults` for
   defaults, classes with value equality (not data classes) for configuration, internal constructors for
   model types.
3. Run `./gradlew apiDump` and commit the updated dumps, so the change is visible in review. Kotlin/Native
   dumps are exact only when generated on macOS; if CI's `apiCheck` disagrees with a dump made on Linux,
   use the dump CI reports.

## Commits and pull requests

- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`,
  `perf:`, `refactor:`, `test:`, `docs:`, `build:`, `ci:`, `chore:`, optionally with a scope
  (`fix(core): ...`). Mark breaking changes with `!` or a `BREAKING CHANGE:` footer.
- Every commit should build and pass the tests.
- Add an entry under **Unreleased** in [CHANGELOG.md](CHANGELOG.md) for any user-visible change.
- Fill in the pull request template, including how you verified the change.
- A maintainer reviews every pull request. Expect questions; they are about the code, not about you.

## Releases

Composdown follows [Semantic Versioning](https://semver.org/). Before 1.0, minor releases may contain
breaking changes, which are always listed in the changelog. Releases are made from `main` by a
maintainer: the changelog is finalized, the version is tagged, and CI publishes the artifacts.
