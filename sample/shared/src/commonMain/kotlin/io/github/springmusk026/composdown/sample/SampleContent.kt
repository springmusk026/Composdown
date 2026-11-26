package io.github.springmusk026.composdown.sample

import io.github.springmusk026.composdown.compose.ComposdownDefaults
import io.github.springmusk026.composdown.core.MarkdownExtension

/** The sample renders GitHub Flavored Markdown with alerts and math. */
internal val SampleExtensions = ComposdownDefaults.Extensions + MarkdownExtension.MATH

/** Canned responses for the simulated chat, written the way language models format answers. */
internal object SampleContent {
    val prompts: List<Pair<String, String>> =
        listOf(
            "How do I stream Markdown in Compose?" to
                """
                ## Streaming Markdown with Composdown

                Use a **`ComposdownState`** and append tokens as they arrive:

                ```kotlin
                val state = rememberComposdownState()
                LaunchedEffect(response) {
                    response.tokens.collect { state.append(it) }
                    state.finish()
                }
                Composdown(state)
                ```

                What happens on each token:

                1. Text is appended and parsed on a *background* dispatcher.
                2. Only the **open end** of the document is parsed again.
                3. Completed blocks keep their instances, so they are not recomposed.

                > [!TIP]
                > In a chat, use `composdownItems` so that only visible blocks are composed.

                That's it! See the [project page](https://github.com/springmusk026/Composdown) for more.
                """.trimIndent(),
            "Compare a few sorting algorithms" to
                """
                Here is a comparison of common sorting algorithms[^1]:

                | Algorithm | Average | Worst | Stable |
                | :-------- | :-----: | :---: | :----: |
                | Quicksort | O(n log n) | O(n²) | No |
                | Merge sort | O(n log n) | O(n log n) | Yes |
                | Heapsort | O(n log n) | O(n log n) | No |
                | Insertion sort | O(n²) | O(n²) | Yes |

                - [x] Use **merge sort** when stability matters
                - [x] Use **quicksort** for fast in-memory sorting
                - [ ] ~~Bubble sort~~ is rarely the right choice

                > [!WARNING]
                > Worst cases matter for adversarial inputs.

                [^1]: Time complexities for *n* elements.
                """.trimIndent(),
            "Explain Euler's identity" to
                """
                **Euler's identity** connects five fundamental constants:

                ${'$'}${'$'}
                e^{i\pi} + 1 = 0
                ${'$'}${'$'}

                It follows from Euler's formula ${'$'}e^{ix} = \cos x + i \sin x${'$'} with ${'$'}x = \pi${'$'},
                since ${'$'}\cos \pi = -1${'$'} and ${'$'}\sin \pi = 0${'$'}.

                A quick numerical check in Python:

                ```python
                import cmath
                print(cmath.exp(1j * cmath.pi) + 1)  # about 1.2e-16j
                ```
                """.trimIndent(),
            "Write a greeting in Hebrew and Arabic" to
                """
                Right-to-left text is laid out from the right:

                שלום! זהו טקסט בעברית עם **הדגשה** ורשימה:

                - פריט ראשון
                - פריט שני

                مرحبا! هذا نص باللغة العربية مع *تأكيد*.

                > اقتباس باللغة العربية

                And back to English, with `inline code` and a www.example.com link.
                """.trimIndent(),
        )

    val showcase: String =
        """
        # Composdown showcase

        Every element Composdown renders, with **GitHub Flavored Markdown** and alerts.

        ## Text

        Paragraphs support *emphasis*, **strong emphasis**, ***both***, ~~strikethrough~~,
        `inline code`, [links](https://example.com), autolinks like https://example.com and
        www.example.com, and footnotes[^note].
        Line ends are soft breaks; a backslash\
        forces a hard break.

        ## Headings

        ### Level 3
        #### Level 4
        ##### Level 5
        ###### Level 6

        ## Lists

        - Bullet item
          - Nested item
            - Deeper item
        - [x] Completed task
        - [ ] Open task

        3. Ordered lists can start at any number
        4. And continue from there

        ## Quotes and alerts

        > A block quote can contain *any* block,
        > > including another quote.

        > [!NOTE]
        > Useful information.

        > [!TIP]
        > Helpful advice.

        > [!IMPORTANT]
        > Key information.

        > [!WARNING]
        > Urgent information.

        > [!CAUTION]
        > Risks of an action.

        ## Code

        ```kotlin
        fun main() {
            println("Hello, Composdown!")
        }
        ```

            Indented code blocks work too.

        ## Tables

        | Left | Center | Right |
        | :--- | :----: | ----: |
        | Text | *styled* | 1 |
        | More | `code` | 22 |

        ## Images

        ![The Composdown logo placeholder](https://picsum.photos/seed/composdown/800/400)

        ---

        Raw HTML such as <span>this</span> is shown as text, never interpreted.

        [^note]: Footnotes are collected at the end of the document.
        """.trimIndent()
}
