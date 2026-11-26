# Composdown sample

A Compose Multiplatform application that demonstrates every Composdown feature on Android, iOS and
Desktop from one shared UI. Use it to evaluate the library, to reproduce issues, and to verify
rendering and accessibility by hand.

## Screens

**Chat.** Choose a prompt, and a canned response is streamed at a realistic pace (about 30 tokens per
second), as a language model would produce it. Responses cover code with syntax highlighting, tables,
math, alerts and right-to-left text. Messages are rendered with `ComposdownState` and
`composdownItems` in a `LazyColumn`, which is the recommended setup for chat interfaces.

**Showcase.** Every element Composdown renders, from a static document: headings, emphasis, lists and
task lists, quotes and alerts, code, tables, footnotes, math and images. Use it to review themes and to
go through the [accessibility checklist](../docs/accessibility.md#manual-verification-checklist).

Both screens follow the system's light or dark mode.

## Modules

| Module | Platform | Contents |
|---|---|---|
| `sample/shared` | All | The application UI (`SampleApp`), sample content and the simulated token stream |
| `sample/desktop` | Desktop (JVM) | Window host |
| `sample/android` | Android | Activity host |
| `sample/ios` | iOS | SwiftUI host and XcodeGen project specification |

## Running

**Desktop**

```bash
./gradlew :sample:desktop:run
```

**Android** (with a device or emulator connected)

```bash
./gradlew :sample:android:installDebug
```

**iOS** (macOS with Xcode). The Xcode project is generated with
[XcodeGen](https://github.com/yonaskolb/XcodeGen):

```bash
brew install xcodegen
```

```bash
cd sample/ios && xcodegen && open ComposdownSample.xcodeproj
```

Building the app in Xcode runs `./gradlew :sample:shared:embedAndSignAppleFrameworkForXcode`, which
builds the Kotlin framework for the selected simulator or device.

## Configuration used

The sample renders with `ComposdownDefaults.Extensions` plus `MarkdownExtension.MATH`,
`CoilImageRenderer` for images and `HighlightsCodeHighlighter` for code. See
[`SampleContent.kt`](shared/src/commonMain/kotlin/io/github/springmusk026/composdown/sample/SampleContent.kt)
for the content and [`ChatScreen.kt`](shared/src/commonMain/kotlin/io/github/springmusk026/composdown/sample/ChatScreen.kt)
for the streaming setup.
