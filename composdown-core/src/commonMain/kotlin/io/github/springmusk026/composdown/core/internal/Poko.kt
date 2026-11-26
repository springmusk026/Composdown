package io.github.springmusk026.composdown.core.internal

/**
 * Marks a class for the Poko compiler plugin, which generates `equals`, `hashCode` and `toString`
 * from the primary constructor properties.
 *
 * Composdown uses this instead of `data class` for public model types so that `copy` and
 * `componentN` functions do not become part of the public, binary-compatible API.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
internal annotation class Poko
