package io.github.springmusk026.composdown.sample

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/** Average delay between tokens: about 30 tokens per second, a typical language model speed. */
private const val AVERAGE_TOKEN_DELAY_MS = 33L
private const val JITTER_MS = AVERAGE_TOKEN_DELAY_MS / 2

/** Splits text like a language model tokenizer would, roughly: words, whitespace and punctuation. */
private val TOKEN = Regex("""\s+|\w+|[^\w\s]""")

/**
 * Emits [text] in token-sized pieces at a realistic, slightly irregular pace, the way a language
 * model streams a response.
 */
internal fun simulatedTokenStream(
    text: String,
    random: Random = Random.Default,
): Flow<String> =
    flow {
        for (match in TOKEN.findAll(text)) {
            // Vary each delay by up to half the average either way.
            delay(random.nextLong(AVERAGE_TOKEN_DELAY_MS - JITTER_MS, AVERAGE_TOKEN_DELAY_MS + JITTER_MS))
            emit(match.value)
        }
    }
