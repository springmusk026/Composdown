# Performance

Composdown is designed to render on the first frame and to stay smooth while content changes. This
page describes what makes it fast, how performance is measured, and what applications can do.

- [What the library does](#what-the-library-does)
- [Parser benchmarks (JVM)](#parser-benchmarks-jvm)
- [Android Macrobenchmarks](#android-macrobenchmarks)
- [Baseline Profiles](#baseline-profiles)
- [Guidance for applications](#guidance-for-applications)

## What the library does

| Technique | Effect |
|---|---|
| Linear-time parser with no regular expressions on hot paths | Parsing a 73 KB document takes about 2 ms on a desktop JVM |
| Incremental parsing of growing text | Each update re-parses only the unfinished block, so its cost does not depend on document length |
| Stable block instances and value-equal configuration | Compose skips completed blocks entirely while text is appended |
| One `Text` per block for inline content | Layout node count grows with blocks, not with inline elements |
| Background parsing in `ComposdownState` | Streamed text never parses on the main thread |
| Intrinsic table measurement | Each table cell is measured once |
| Lazy list integration | Long documents compose only the visible blocks |
| Baseline Profiles shipped in the Android artifacts | Rendering code is precompiled at install time instead of interpreted on first use |

## Parser benchmarks (JVM)

The [`benchmarks`](../benchmarks) module measures the parser with JMH through kotlinx-benchmark:

```bash
./gradlew :benchmarks:benchmark
```

Recorded results and the machine they were measured on are in
[`benchmarks/BASELINE.md`](../benchmarks/BASELINE.md). Compare changes on the same machine, before and
after.

| Benchmark | What it measures |
|---|---|
| `parseLargeDocument` | One-shot parse of a 73 KB document covering every construct |
| `streamTokensIncrementally` | 2,000 tokens appended one at a time, with a document produced after each |
| `streamTokensAfterLongDocument` | The same stream appended after the 73 KB document |
| `streamTokensWithFullReparse` | The same stream, re-parsing the whole text on every token (the naive approach, for reference) |

## Android Macrobenchmarks

[`benchmarks/macrobenchmark`](../benchmarks/macrobenchmark) measures the [sample app](../sample) on a
device, with release builds, as users experience it:

| Benchmark | Metric | Scenario |
|---|---|---|
| `StartupBenchmark.startupWithoutPrecompilation` | Time to initial display | Cold start with no precompiled code (the first launch after install without profiles) |
| `StartupBenchmark.startupWithBaselineProfile` | Time to initial display | Cold start with the Baseline Profile applied |
| `RenderingBenchmark.scrollShowcase` | Frame durations and overruns | Scrolling through a long document with every kind of element |
| `RenderingBenchmark.streamChatResponse` | Frame durations and overruns | A chat response streamed at about 30 tokens per second |

Run them with a device or emulator (API 28 or newer) connected:

```bash
./gradlew :benchmarks:macrobenchmark:connectedBenchmarkReleaseAndroidTest
```

Results are written to `benchmarks/macrobenchmark/build/outputs/connected_android_test_additional_output/`,
including Perfetto traces of every iteration. For reliable numbers, use a physical device with a
locked clock if possible, keep the screen on, and close other applications. Emulator results are only
useful for comparing runs with each other; on an emulator, add
`-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR`.

The [Android performance workflow](../.github/workflows/android-performance.yml) runs the benchmarks
and generates the profile on an emulator on demand. Reference results from physical devices will be
published with the first release.

## Baseline Profiles

Android runs code through an interpreter and just-in-time compiler until it has been used enough to be
compiled ahead of time. A Baseline Profile lists code to compile at install time instead, which removes
that warm-up from the first launches and first renders.

**Library profiles.** `composdown-core` and `composdown-compose` ship profiles in their Android
artifacts (`src/androidMain/baselineProfiles`). They cover all of both libraries, because every part of
the parser and renderer runs when a document is shown. The Android Gradle plugin merges them into the
profile of any application that depends on Composdown; nothing needs to be configured.

**Application profiles.** Applications get the most benefit from generating a profile for their own
user journeys, which then covers Composdown as used by the app, together with Compose and the app's
own code. The sample shows the setup:

- [`BaselineProfileGenerator`](../benchmarks/macrobenchmark/src/main/kotlin/io/github/springmusk026/composdown/benchmark/BaselineProfileGenerator.kt)
  drives the app through startup, a long document and a streamed response.
- The `androidx.baselineprofile` plugin in the app and the benchmark module wires generation into the
  build:

  ```bash
  ./gradlew :sample:android:generateBaselineProfile
  ```

- `androidx.profileinstaller` installs the profile for apps that are not installed from Google Play
  (sideloaded builds, and the benchmarks themselves).

Profiles are only used by release builds. Debug builds are not representative of performance.

## Guidance for applications

- **Parse once.** Parse static content with `MarkdownParser` outside composition (for example in a
  view model) and render the `MarkdownDocument`, especially when the same text is shown repeatedly.
- **Use lazy lists for long content.** Render long documents and feeds with `composdownItems`, keyed by
  a stable id per document.
- **Use `ComposdownState` for growing text.** It parses off the main thread and combines updates that
  arrive faster than they can be shown.
- **Keep configuration stable.** Create `ComposdownComponents`, image renderers and highlighters once
  (at the top level or with `remember`). A new lambda or instance on every recomposition makes every
  block recompose.
- **Keep highlighting cheap.** A `CodeHighlighter` runs on every update of a streamed code block.
- **Measure release builds** with Macrobenchmark, as in the sample, rather than judging performance
  from debug builds.
