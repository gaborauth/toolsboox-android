package com.toolsboox.utils

import android.content.Context
import com.toolsboox.R
import java.util.*

/**
 * Convert duration to fluent duration.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class FluentDuration {

    companion object {

        /**
         * Duration to fluent string converter.
         *
         * @param context the context
         * @param startTimeInclusive the start time (inclusive)
         * @param endTimeExclusive the end time (inclusive)
         */
        fun convert(context: Context, startTimeInclusive: Date?, endTimeExclusive: Date?): String {
            if (startTimeInclusive == null || endTimeExclusive == null) {
                return context.getString(R.string.util_fluent_unknown)
            }

            return convert(context, endTimeExclusive.time - startTimeInclusive.time)
        }

        /**
         * Duration to fluent string converter.
         *
         * @param context the context
         * @param timestamp the timestamp
         */
        fun convert(context: Context, timestamp: Date?): String {
            if (timestamp == null) {
                return context.getString(R.string.util_fluent_unknown)
            }

            return convert(context, Date().time - timestamp.time)
        }

        /**
         * Duration to fluent string converter.
         *
         * @param context the context
         * @param totalMilliSeconds total milliseconds
         */
        fun convert(context: Context, totalMilliSeconds: Long?): String {
            if (totalMilliSeconds == null) {
                return context.getString(R.string.util_fluent_unknown)
            }

            val totalSeconds = totalMilliSeconds / 1000L
            val days = totalSeconds / 86400
            val hours = (totalSeconds % 86400) / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            val result = StringBuilder()
            if (days > 0) {
                result.append(" $days ")
                if (days > 1) {
                    result.append(context.getString(R.string.util_fluent_days))
                } else {
                    result.append(context.getString(R.string.util_fluent_day))
                }
                result.append(" $hours ")
                if (hours > 1) {
                    result.append(context.getString(R.string.util_fluent_hours))
                } else {
                    result.append(context.getString(R.string.util_fluent_hour))
                }
            } else if (hours > 0) {
                result.append(" $hours ")
                if (hours > 1) {
                    result.append(context.getString(R.string.util_fluent_hours))
                } else {
                    result.append(context.getString(R.string.util_fluent_hour))
                }
                result.append(" $minutes ")
                if (minutes > 1) {
                    result.append(context.getString(R.string.util_fluent_minutes))
                } else {
                    result.append(context.getString(R.string.util_fluent_minute))
                }
            } else {
                if (minutes > 0) {
                    result.append(" $minutes ")
                    if (minutes > 1) {
                        result.append(context.getString(R.string.util_fluent_minutes))
                    } else {
                        result.append(context.getString(R.string.util_fluent_minute))
                    }
                }
                result.append(" $seconds ")
                if (seconds > 1) {
                    result.append(context.getString(R.string.util_fluent_seconds))
                } else {
                    result.append(context.getString(R.string.util_fluent_second))
                }
            }

            return result.trim().toString()
        }
    }
}
