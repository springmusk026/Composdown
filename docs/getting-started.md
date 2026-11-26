# Getting started

This guide takes you from an empty project to rendered Markdown on Android, iOS and Desktop.

- [Requirements](#requirements)
- [Add the dependency](#add-the-dependency)
- [Render your first document](#render-your-first-document)
- [Choose the right entry point](#choose-the-right-entry-point)
- [Platform notes](#platform-notes)
- [Next steps](#next-steps)

## Requirements

| | Minimum |
|---|---|
| Kotlin | 2.4 |
| Compose Multiplatform | 1.12 |
| Android | API 23, built with JVM target 17 |
| iOS | `iosArm64` or `iosSimulatorArm64` (the sample app targets iOS 15) |
| Desktop | JVM 17 |
| Web | `wasmJs`, in browsers with WebAssembly GC support (Chrome 119, Firefox 120, Safari 18.2) |

`composdown-core` has no Compose dependency and can be used in any Kotlin Multiplatform project,
including server and command-line applications.

## Add the dependency

> Composdown has not yet been published. The coordinates below take effect with the `0.1.0` release
> to Maven Central. Until then, run `./gradlew publishToMavenLocal` in a clone of this repository and
> add `mavenLocal()` to your project's repositories, or try the [sample app](../sample).

With a version catalog:

```toml
# gradle/libs.versions.toml
[versions]
composdown = "0.1.0"

[libraries]
composdown-compose = { module = "io.github.springmusk026:composdown-compose", version.ref = "composdown" }
composdown-coil = { module = "io.github.springmusk026:composdown-coil", version.ref = "composdown" }
composdown-highlight = { module = "io.github.springmusk026:composdown-highlight", version.ref = "composdown" }
```

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.composdown.compose)
            implementation(libs.composdown.coil)      // optional: images
            implementation(libs.composdown.highlight) // optional: syntax highlighting
        }
    }
}
```

`composdown-compose` brings in `composdown-core` transitively. Add `composdown-core` on its own only if
you need the parser without Compose.

## Render your first document

```kotlin
@Composable
fun ReleaseNotes() {
    val markdown = """
        # Release 2.3

        This release adds **offline mode** and fixes several issues:

        - [x] Sync conflicts are resolved automatically
        - [x] Faster startup on older devices
        - [ ] Tablet layout (next release)

        > [!NOTE]
        > Offline mode is enabled by default for new installations.
    """.trimIndent()

    MaterialTheme {
        Composdown(
            markdown = markdown,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        )
    }
}
```

The output uses the colors and typography of the surrounding `MaterialTheme`, follows light and dark
mode, and supports text selection. Web, email and phone links open in the platform's handler.

## Choose the right entry point

| Your content | Use | Guide |
|---|---|---|
| A string that changes rarely | `Composdown(markdown = ...)` | [Rendering](rendering.md) |
| A long document, or many documents in a list | `MarkdownParser().parse(...)` once, then `composdownItems` in a `LazyColumn` | [Rendering](rendering.md#long-documents-and-lists) |
| Text that grows while displayed (streams, live editors, collaborative content) | `ComposdownState` | [Live content](streaming.md) |
| A document you parse yourself, for example in a view model | `Composdown(document = ...)` | [Parser](parser.md) |
| No UI, only the document tree | `composdown-core` | [Parser](parser.md) |

## Platform notes

**Android.** No setup is required. Remote images through `composdown-coil` need Coil's network module
(for example `io.coil-kt.coil3:coil-network-okhttp`) and the `INTERNET` permission.

**iOS.** Use Composdown inside a `ComposeUIViewController`. The Compose-based modules do not support
the `iosX64` (Intel simulator) target, because Compose Multiplatform no longer publishes it;
`composdown-core` does. The [sample](../sample) contains a complete SwiftUI host.

**Desktop.** No setup is required. Remote images need a Coil network module such as
`coil-network-ktor3` with a Ktor engine.

**Web.** Use Composdown inside `ComposeViewport`. Browsers give Compose no system fonts to fall back
on, so bundle the fonts your content needs as Compose resources:

- a monospace font for code and math, set through `ComposdownDefaults.typography(codeBlock = ...,
  inlineCode = ..., math = ..., mathBlock = ...)`, as the sample's `SampleTheme.kt` does;
- fonts for scripts other than Latin, Greek and Cyrillic (for example Noto Sans Hebrew or Arabic), if
  your content uses them.

Remote images need `coil-network-ktor3` with the `ktor-client-js` engine, and the image server must
allow cross-origin requests.

## Next steps

- [Customization](customization.md): match your design system.
- [Rendering](rendering.md): links, images, code, math and selection.
- [Compatibility](compatibility.md): what is supported, precisely.
