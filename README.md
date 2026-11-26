# Composdown

**A streaming-first Markdown renderer for Compose Multiplatform, built for AI chat and real-time apps.**

> **Status: in development.** Composdown is not published yet. The first release will be `0.1.0`.
> The APIs shown below are the planned design and may change before that release.

## Why Composdown?

Rendering Markdown that arrives token by token (from a language model, for example) is a different
problem from rendering a finished document. Most Markdown renderers parse the whole text again on every
change. During streaming that leads to:

- Work that grows with the length of the message on every token.
- Flicker and layout jumps as content above the streaming tail is rebuilt.
- Incomplete syntax rendered badly: unclosed code fences, half-written tables, and emphasis that
  flips between literal `**` and bold.

Composdown treats streaming as the main use case:

- **Incremental parsing.** Appended text is parsed from the last stable block boundary onward.
  Completed blocks are never parsed or recomposed again.
- **Graceful partial input.** Unclosed code fences render as open code blocks, partial tables show the
  rows received so far, and unterminated emphasis does not flicker.
- **Stable identity.** Every block has a stable key, so it works with `LazyColumn` and unchanged
  content is skipped by Compose.
- **Correctness.** Full CommonMark 0.31.2 compliance, verified by the official spec test suite on every
  platform, plus GitHub Flavored Markdown (tables, task lists, strikethrough, autolinks, footnotes)
  verified against the GFM spec and cmark-gfm's extension tests. GitHub alerts are available as an
  opt-in extension.

## Demo

<!-- A recording of the streaming chat sample will be added with the 0.1.0 release. -->

_A streaming demo recording will be added here with the first release._

## Supported platforms

| Platform | Targets |
|---|---|
| Android | API 23+ |
| iOS | `iosArm64`, `iosSimulatorArm64` (and `iosX64` for `composdown-core` only, see below) |
| Desktop | JVM 17+ |
| Web | `wasmJs` is planned |

Compose Multiplatform no longer publishes artifacts for `iosX64` (Intel-Mac iOS simulators), so the
Compose-based modules cannot support it. The pure-Kotlin `composdown-core` parser still does.

## Modules

| Module | Description |
|---|---|
| `composdown-core` | Markdown AST and incremental parser. Pure Kotlin, no UI dependencies. |
| `composdown-compose` | Compose Multiplatform renderer, Material 3 theme and streaming state holder. |
| `composdown-coil` | Image loading with Coil 3 (planned). |
| `composdown-highlight` | Syntax-highlighted code blocks (planned). |
| `composdown-math` | Inline and block math (planned). |

## Installation

Not yet published. Once `0.1.0` is released:

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.springmusk026:composdown-compose:0.1.0")
        }
    }
}
```

## Quick start (planned API)

Static content:

```kotlin
Composdown(markdown = "# Hello\n\nThis is **Composdown**.")
```

## Streaming usage (planned API)

```kotlin
val state = rememberComposdownState()

LaunchedEffect(Unit) {
    llm.stream(prompt).collect { token -> state.append(token) }
    state.finish()
}

Composdown(state = state, onLinkClick = { url -> openUrl(url) })
```

## Customization (planned API)

Composdown uses Material 3 defaults and a themeable `ComposdownTheme` (typography, colors and spacing
for each element). The composable for any block or inline type can be replaced, for example to add a
copy button to code blocks. Image loading is pluggable and has no default dependency.

## Comparison with other libraries

[multiplatform-markdown-renderer](https://github.com/mikepenz/multiplatform-markdown-renderer) is a
mature, widely used Compose Multiplatform Markdown renderer with a rich feature set, and is an
excellent choice for rendering complete documents. Composdown has a narrower focus: content that is
still being written. Its parser is designed for incremental re-parsing, and its rendering model keeps
completed content stable while new text arrives. Measured benchmark comparisons will be published with
the first release.

## Contributing

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) and the
[Code of Conduct](CODE_OF_CONDUCT.md). To report a security issue, see [SECURITY.md](SECURITY.md).

## License

```
Copyright 2026 Composdown contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
