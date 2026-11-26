# Composdown sample

A shared Compose Multiplatform application with two screens:

- **Chat**: choose a prompt to stream a canned response at a realistic pace (about 30 tokens per
  second), rendered with `ComposdownState` and `composdownItems` in a `LazyColumn`.
- **Showcase**: every element Composdown renders, from static text.

| Module | Platform | Run |
|---|---|---|
| `sample/shared` | All | The application UI (`SampleApp`) |
| `sample/desktop` | Desktop (JVM) | `./gradlew :sample:desktop:run` |
| `sample/android` | Android | `./gradlew :sample:android:installDebug` |
| `sample/ios` | iOS | See below |

## iOS

The iOS app is a SwiftUI shell around `MainViewController()` from the shared framework. Its Xcode
project is generated with [XcodeGen](https://github.com/yonaskolb/XcodeGen) on macOS:

```bash
brew install xcodegen
cd sample/ios && xcodegen
open ComposdownSample.xcodeproj
```

Building the app runs `./gradlew :sample:shared:embedAndSignAppleFrameworkForXcode` to build the
Kotlin framework for the selected simulator or device.
