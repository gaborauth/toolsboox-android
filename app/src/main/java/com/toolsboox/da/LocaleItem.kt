package com.toolsboox.da

/**
 * Locale item data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
data class LocaleItem(
    val languageTag: String,
    val displayName: String
) {
    override fun toString(): String = "$languageTag - $displayName"
}
