package io.github.springmusk026.composdown.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import io.github.springmusk026.composdown.compose.ComposdownDefaults

/**
 * The configuration provided by the enclosing entry point, or the defaults when a block is rendered
 * on its own.
 */
@Composable
@ReadOnlyComposable
internal fun currentEnvironment(): ComposdownEnvironment =
    LocalComposdownEnvironment.current
        ?: ComposdownEnvironment(
            ComposdownDefaults.theme(),
            ComposdownDefaults.Components,
            imageRenderer = null,
            onLinkClick = null,
        )
