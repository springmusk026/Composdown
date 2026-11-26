package io.github.springmusk026.composdown.core.streaming

/** Realistic documents of the kind language models produce, for streaming tests. */
internal object StreamingCorpus {
    val documents: List<String> =
        listOf(
            """
            # Setting up the project

            First, install the dependencies. You need **Kotlin 2.4** and a recent *JDK*.

            1. Clone the repository.
            2. Run the build:

               ```bash
               ./gradlew build
               ```

            3. Open the project in your IDE.

            > [!TIP]
            > Use the `--offline` flag if you have no network access.

            | Command | Purpose |
            | :------ | ------: |
            | `build` | Compiles and tests |
            | `check` | Runs linters too |

            See the [documentation][docs] and the footnote[^1] for details.

            - [x] Install
            - [ ] Configure
              - nested *item* with `code`

            [docs]: https://example.com/docs "Docs"
            [^1]: Footnotes can appear **anywhere** in the document.
            """.trimIndent(),
            """
            Here is a function:

            ```kotlin
            fun greet(name: String): String {
                return "Hello, ${'$'}name!"
            }
            ```

            And some ~~deleted~~ text with a www.example.com link, an email@example.com address,
            and a [reference] that is defined later.

            ***

            Text with a hard break  
            and a soft break, `inline code`, <span>raw HTML</span> and &copy; entities.

                indented code block

            Final paragraph with [^late] and [^early] footnotes.

            [^early]: Defined first.
            [^late]: Defined second.
            [reference]: /target
            """.trimIndent(),
            "* a\n* b\n\n  continued\n* c\n\n> quote\nlazy\n> > nested\n\n" +
                "<div>\nhtml block\n</div>\n\nSetext\n======\n\n- [ ] task\n",
        )
}
