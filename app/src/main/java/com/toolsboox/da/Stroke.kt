package com.toolsboox.da

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.moshi.JsonClass
import java.lang.reflect.Type
import java.util.*

/**
 * Stroke data class.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
@JsonClass(generateAdapter = true)
data class Stroke(
    var strokeId: UUID,
    var strokePoints: List<StrokePoint>
) {
    companion object {
        /**
         * Deep copy of strokes.
         *
         * @param strokes the source list
         * @return the destination list
         */
        fun listDeepCopy(strokes: List<Stroke>): List<Stroke> {
            val listType: Type = object : TypeToken<List<Stroke>>() {}.type
            val gson = Gson()
            val json: String = gson.toJson(strokes, listType)
            return gson.fromJson(json, listType)
        }
    }
}