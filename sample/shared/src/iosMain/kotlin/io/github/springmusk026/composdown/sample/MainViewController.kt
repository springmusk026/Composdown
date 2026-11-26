package io.github.springmusk026.composdown.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** The sample application as a UIKit view controller, for the iOS app. */
@Suppress("FunctionName") // Called from Swift as MainViewControllerKt.MainViewController().
fun MainViewController(): UIViewController = ComposeUIViewController { SampleApp() }
