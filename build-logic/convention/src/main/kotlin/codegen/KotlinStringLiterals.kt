package codegen

/**
 * Returns [value] as a Kotlin string literal (including the surrounding quotes) that contains only
 * printable ASCII, so generated sources are independent of file encoding and invisible characters
 * stay visible in review.
 */
internal fun kotlinStringLiteral(value: String): String =
    buildString(value.length + 2) {
        append('"')
        for (c in value) {
            when (c) {
                '\\' -> {
                    append("\\\\")
                }

                '"' -> {
                    append("\\\"")
                }

                '$' -> {
                    append("\\$")
                }

                '\n' -> {
                    append("\\n")
                }

                '\t' -> {
                    append("\\t")
                }

                '\r' -> {
                    append("\\r")
                }

                else -> {
                    if (c.code in 0x20..0x7E) {
                        append(c)
                    } else {
                        append("\\u").append(
                            c.code
                                .toString(16)
                                .uppercase()
                                .padStart(4, '0'),
                        )
                    }
                }
            }
        }
        append('"')
    }
