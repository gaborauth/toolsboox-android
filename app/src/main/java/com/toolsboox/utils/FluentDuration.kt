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

            val htmlMessage = StringBuilder()
            if (days > 0) {
                if (days > 1) {
                    htmlMessage.append(" ").append(days).append(" ")
                        .append(context.getString(R.string.util_fluent_days))
                } else {
                    htmlMessage.append(" ").append(days).append(" ").append(context.getString(R.string.util_fluent_day))
                }
                if (hours > 1) {
                    htmlMessage.append(" ").append(hours).append(" ")
                        .append(context.getString(R.string.util_fluent_hours))
                } else {
                    htmlMessage.append(" ").append(hours).append(" ")
                        .append(context.getString(R.string.util_fluent_hour))
                }
            } else if (hours > 0) {
                if (hours > 1) {
                    htmlMessage.append(" ").append(hours).append(" ")
                        .append(context.getString(R.string.util_fluent_hours))
                } else {
                    htmlMessage.append(" ").append(hours).append(" ")
                        .append(context.getString(R.string.util_fluent_hour))
                }
                if (minutes > 1) {
                    htmlMessage.append(" ").append(minutes).append(" ")
                        .append(context.getString(R.string.util_fluent_minutes))
                } else {
                    htmlMessage.append(" ").append(minutes).append(" ")
                        .append(context.getString(R.string.util_fluent_minute))
                }
            } else {
                if (minutes > 0) {
                    if (minutes > 1) {
                        htmlMessage.append(" ").append(minutes).append(" ")
                            .append(context.getString(R.string.util_fluent_minutes))
                    } else {
                        htmlMessage.append(" ").append(minutes).append(" ")
                            .append(context.getString(R.string.util_fluent_minute))
                    }
                }
                if (seconds > 1) {
                    htmlMessage.append(" ").append(seconds).append(" ")
                        .append(context.getString(R.string.util_fluent_seconds))
                } else {
                    htmlMessage.append(" ").append(seconds).append(" ")
                        .append(context.getString(R.string.util_fluent_second))
                }
            }

            return htmlMessage.trim().toString()
        }
    }
}
