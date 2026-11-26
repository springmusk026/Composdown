package io.github.springmusk026.composdown.core.internal.inline

import io.github.springmusk026.composdown.core.internal.text.isUnicodePunctuation
import io.github.springmusk026.composdown.core.internal.text.isUnicodeWhitespace

/**
 * Whether a run of emphasis delimiters (`*` or `_`) can open and/or close emphasis, decided by the
 * characters around it as the CommonMark "left-flanking" and "right-flanking" rules describe.
 */
internal class DelimiterRun private constructor(
    val canOpen: Boolean,
    val canClose: Boolean,
) {
    companion object {
        /**
         * @param before the code point before the run, or a line feed at the start of the text.
         * @param after the code point after the run, or a line feed at the end of the text.
         */
        fun classify(
            delimiterChar: Char,
            before: Int,
            after: Int,
        ): DelimiterRun {
            val afterIsWhitespace = isUnicodeWhitespace(after)
            val afterIsPunctuation = isUnicodePunctuation(after)
            val beforeIsWhitespace = isUnicodeWhitespace(before)
            val beforeIsPunctuation = isUnicodePunctuation(before)
            val leftFlanking = !afterIsWhitespace && (!afterIsPunctuation || beforeIsWhitespace || beforeIsPunctuation)
            val rightFlanking = !beforeIsWhitespace && (!beforeIsPunctuation || afterIsWhitespace || afterIsPunctuation)
            return if (delimiterChar == '_') {
                // `_` cannot open or close emphasis inside a word.
                DelimiterRun(
                    canOpen = leftFlanking && (!rightFlanking || beforeIsPunctuation),
                    canClose = rightFlanking && (!leftFlanking || afterIsPunctuation),
                )
            } else {
                DelimiterRun(canOpen = leftFlanking, canClose = rightFlanking)
            }
        }
    }
}
