package io.github.springmusk026.composdown.compose.internal

/**
 * Marks a class for the Poko compiler plugin, which generates `equals`, `hashCode` and `toString`
 * from the primary constructor properties.
 *
 * Theme and configuration classes need structural equality: they are typically created during
 * composition, and equal values must not invalidate everything that depends on them.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
internal annotation class Poko
