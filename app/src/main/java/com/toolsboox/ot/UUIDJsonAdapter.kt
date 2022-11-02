package com.toolsboox.ot

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.*

/**
 * JSON adapter of UUID class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class UUIDJsonAdapter {
    /**
     * Serialize to JSON.
     *
     * @param uuid the UUID
     * @return the JSON value
     */
    @ToJson
    fun toJson(uuid: UUID): String {
        return uuid.toString()
    }

    /**
     * Serialize from JSON.
     *
     * @param uuid the JSON value
     * @return the locale
     */
    @FromJson
    fun fromJson(uuid: String): UUID {
        return UUID.fromString(uuid)
    }
}