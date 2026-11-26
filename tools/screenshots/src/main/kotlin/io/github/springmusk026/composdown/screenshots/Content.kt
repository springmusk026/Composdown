package io.github.springmusk026.composdown.screenshots

/** The Markdown shown in the generated images. */
internal object Content {
    val overview =
        """
        ## Release notes

        Composdown renders **CommonMark** and *GitHub Flavored Markdown* natively, with
        ~~a WebView~~ real Compose text you can select, theme and customize.

        - [x] Tables, task lists and footnotes[^1]
        - [x] Syntax highlighting, alerts and math
        - [ ] Your own design system

        | Platform | Targets | Status |
        | :------- | :------ | :----: |
        | Android | API 23+ | Stable |
        | iOS | arm64, simulator | Stable |
        | Desktop | JVM 17+ | Stable |

        ```kotlin
        @Composable
        fun Notes(markdown: String) {
            Composdown(markdown = markdown)
        }
        ```

        > [!TIP]
        > Replace how any block is drawn with `ComposdownComponents`.

        [^1]: Numbered and collected at the end, like on GitHub.
        """.trimIndent()

    const val CHAT_PROMPT = "How do I render a streamed reply?"

    val chatReply =
        """
        Use a **`ComposdownState`** and append tokens as they arrive:

        ```kotlin
        val state = rememberComposdownState()
        LaunchedEffect(reply) {
            reply.collect(state::append)
            state.finish()
        }
        Composdown(state = state)
        ```

        On each token, Composdown:

        1. Parses on a *background* dispatcher.
        2. Re-parses only the **unfinished end** of the text.
        3. Keeps completed blocks, so they are *not recomposed*.

        | Approach | 2,000 tokens |
        | :------- | -----------: |
        | Re-parse everything | 89 ms |
        | Composdown | 4.8 ms |

        > [!NOTE]
        > Half-written syntax like `**bold` never flickers.
        """.trimIndent()

    val customization =
        """
        ### Deploying the service

        Run the migration first, then roll out the new version:

        ```bash
        ./gradlew migrate
        kubectl rollout restart deployment/api
        ```

        > [!WARNING]
        > Rollouts during peak hours need approval from the on-call engineer.

        See the [runbook](https://example.com) for details.
        """.trimIndent()

    val rightToLeft =
        """
        ### Bidirectional text

        Each block is laid out in the direction of its own text:

        - שלום! זהו טקסט בעברית עם **הדגשה**
        - רשימות מופיעות בצד הנכון

        > مرحبا! الاقتباسات تظهر على **الجانب الصحيح**.

        Bullets, numbers and quote bars follow, without any configuration.
        """.trimIndent()
}
