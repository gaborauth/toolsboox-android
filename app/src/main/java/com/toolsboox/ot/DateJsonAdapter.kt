package com.toolsboox.ot

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.*

/**
 * JSON adapter of Date class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class DateJsonAdapter {
    /**
     * Serialize to JSON.
     *
     * @param date the date
     * @return the JSON value
     */
    @ToJson
    fun toJson(date: Date): Long {
        return date.time
    }

    /**
     * Serialize from JSON.
     *
     * @param json the JSON value
     * @return the date
     */
    @FromJson
    fun fromJson(json: Long): Date {
        return Date(json)
    }
}