package com.toolsboox.ot

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.*

/**
 * JSON adapter of Locale class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class LocaleJsonAdapter {
    /**
     * Serialize to JSON.
     *
     * @param locale the locale
     * @return the JSON value
     */
    @ToJson
    fun toJson(locale: Locale): String {
        return locale.toLanguageTag()
    }

    /**
     * Serialize from JSON.
     *
     * @param languageTag the JSON value
     * @return the locale
     */
    @FromJson
    fun fromJson(languageTag: String): Locale {
        val locale = Locale.forLanguageTag(languageTag)
        if (locale.toLanguageTag() == "und") return Locale.getDefault()
        return locale
    }
}