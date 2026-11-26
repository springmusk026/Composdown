package io.github.springmusk026.composdown.sample

/**
 * Test tags of the sample's interactive elements. On Android they are exposed as resource ids, so
 * that the macrobenchmarks and the Baseline Profile generator can find them with UI Automator.
 */
internal object SampleTags {
    const val CHAT_TAB = "chat_tab"
    const val SHOWCASE_TAB = "showcase_tab"
    const val CHAT_LIST = "chat_list"
    const val SHOWCASE = "showcase"

    /** The chip that sends the prompt at [index]. */
    fun prompt(index: Int): String = "prompt_$index"
}
