package com.toolsboox.plugin.calendar.ot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import com.toolsboox.R
import com.toolsboox.da.Stroke
import com.toolsboox.ot.Creator
import com.toolsboox.ot.OnGestureListener
import com.toolsboox.plugin.calendar.CalendarNavigator
import com.toolsboox.plugin.calendar.da.v2.CalendarDay
import com.toolsboox.plugin.calendar.ui.CalendarDayFragment
import java.time.LocalDate

/**
 * Create daily health template of calendar plugin.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class HealthDayPage {

    companion object {
        // Cell width
        private const val cew = 325.0f

        // Cell height
        private const val ceh = 50.0f

        // Left offset
        private const val lo = (1404.0f - 4 * cew) / 2.0f

        // Top offset
        private const val to = (1872.0f - 35 * ceh) / 2.0f

        /**
         * Process touch event on the calendar page and navigate to the view of calendar.
         *
         * @param view the surface view
         * @param motionEvent the motion event
         * @param gestureResult the gesture result
         * @param fragment the parent fragment
         * @param calendarDay the calendar data class
         * @return true
         */
        fun onTouchEvent(
            view: View, motionEvent: MotionEvent, gestureResult: Int,
            fragment: CalendarDayFragment, calendarDay: CalendarDay
        ): Boolean {
            if (motionEvent.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER) return true

            val year = calendarDay.year
            val month = calendarDay.month
            val day = calendarDay.day
            val locale = calendarDay.locale

            val localDate = LocalDate.of(year, month, day)

            when (gestureResult) {
                OnGestureListener.LTR -> {
                    CalendarNavigator.toDayPage(fragment, localDate.minusDays(1L), CalendarDay.HEALTH_V1_STYLE)
                    return true
                }

                OnGestureListener.RTL -> {
                    CalendarNavigator.toDayPage(fragment, localDate.plusDays(1L), CalendarDay.HEALTH_V1_STYLE)
                    return true
                }

                OnGestureListener.UTD -> {
                    CalendarNavigator.toWeekPage(fragment, localDate, locale)
                    return true
                }

                OnGestureListener.DTU -> {
                    CalendarNavigator.toDayNote(fragment, localDate, "0")
                    return true
                }
            }

            return true
        }

        /**
         * Draw the daily health template of calendar plugin.
         *
         * @param context the context
         * @param canvas the canvas
         * @param calendarDay data class
         */
        fun drawPage(
            context: Context, canvas: Canvas, calendarDay: CalendarDay
        ) {
            val startDayText = context.getString(R.string.calendar_health_day_start_it)
            val duringDayText = context.getString(R.string.calendar_health_day_do_it)
            val endDayText = context.getString(R.string.calendar_health_day_rate_it)

            val wakeUpText = context.getString(R.string.calendar_health_day_wake_up)
            val sleepText = context.getString(R.string.calendar_health_day_sleep)
            val weightText = context.getString(R.string.calendar_health_day_weight)

            val breakfastText = context.getString(R.string.calendar_health_day_breakfast)
            val lunchText = context.getString(R.string.calendar_health_day_lunch)
            val dinnerText = context.getString(R.string.calendar_health_day_dinner)
            val snacksText = context.getString(R.string.calendar_health_day_snacks)
            val activityText = context.getString(R.string.calendar_health_day_activity)

            val waterText = context.getString(R.string.calendar_health_day_water_dl)

            val motivationText = context.getString(R.string.calendar_health_day_motivation)
            val energyText = context.getString(R.string.calendar_health_day_energy)
            val happinessText = context.getString(R.string.calendar_health_day_happiness)

            val stressText = context.getString(R.string.calendar_health_day_stress)
            val conditionText = context.getString(R.string.calendar_health_day_condition)
            val moodText = context.getString(R.string.calendar_health_day_mood)

            val painText = context.getString(R.string.calendar_health_day_pain)
            val productivityText = context.getString(R.string.calendar_health_day_productivity)
            val overallText = context.getString(R.string.calendar_health_day_overall)

            val strokes = calendarDay.calendarStrokes[CalendarDay.HEALTH_V1_STYLE] ?: listOf()

            if (!calendarDay.calendarValues.containsKey(CalendarDay.HEALTH_V1_STYLE)) {
                calendarDay.calendarValues[CalendarDay.HEALTH_V1_STYLE] = mutableMapOf()
            }
            val healthValues = calendarDay.calendarValues[CalendarDay.HEALTH_V1_STYLE]!!.toMutableMap()

            canvas.drawRect(0.0f, 0.0f, 1404.0f, 1872.0f, Creator.fillWhite)
            canvas.drawRect(lo, to, lo + 4 * cew, to + 35 * ceh, Creator.lineDefaultBlack)

            canvas.drawRect(0.0f, to + 0 * ceh, lo, to + 6 * ceh, Creator.fillGrey80)
            canvas.save()
            canvas.rotate(-90.0f, lo - 5.0f, to + 3 * ceh)
            canvas.drawText(startDayText, lo - 5.0f, to + 3 * ceh - 5.0f, Creator.textDefaultWhiteCenter)
            canvas.restore()

            drawWakeUp(canvas, lo, to + 0 * ceh, wakeUpText)
            drawSleep(canvas, lo, to + 2 * ceh, sleepText)
            drawWeight(canvas, lo, to + 4 * ceh, weightText)

            healthValues["motivation"] = drawTenPointGrid(canvas, strokes, lo + 2 * cew, to, motivationText)
            healthValues["energy"] = drawTenPointGrid(canvas, strokes, lo + 2 * cew, to + 2 * ceh, energyText)
            healthValues["happiness"] = drawTenPointGrid(canvas, strokes, lo + 2 * cew, to + 4 * ceh, happinessText)

            canvas.drawLine(0 + 0 * cew, to + 6 * ceh + 0.0f, lo + 4 * cew, to + 6 * ceh + 0.0f, Creator.lineDefaultBlack)
            canvas.drawLine(0 + 0 * cew, to + 6 * ceh + 2.0f, lo + 4 * cew, to + 6 * ceh + 2.0f, Creator.lineDefaultBlack)

            canvas.drawRect(0.0f, to + 6 * ceh, lo, to + 22 * ceh, Creator.fillGrey80)
            canvas.save()
            canvas.rotate(-90.0f, lo - 5.0f, to + 14 * ceh)
            canvas.drawText(duringDayText, lo - 5.0f, to + 14 * ceh - 5.0f, Creator.textDefaultWhiteCenter)
            canvas.restore()

            healthValues["breakfast"] = drawCalories(canvas, strokes, lo + 0 * cew, to + 6 * ceh, 2 * ceh, breakfastText)
            healthValues["lunch"] = drawCalories(canvas, strokes, lo + 0 * cew, to + 8 * ceh, 2 * ceh, lunchText)
            healthValues["dinner"] = drawCalories(canvas, strokes, lo + 0 * cew, to + 10 * ceh, 2 * ceh, dinnerText)
            val foodCals = listOfNotNull(healthValues["breakfast"], healthValues["lunch"], healthValues["dinner"]).toMutableList()
            healthValues["snacks"] = drawCalories(canvas, strokes, lo + 0 * cew, to + 12 * ceh, 4 * ceh, snacksText, foodCals)
            healthValues["activity"] = drawCalories(canvas, strokes, lo + 0 * cew, to + 16 * ceh, 4 * ceh, activityText, foodCals, true)

            canvas.drawLine(lo + 0 * cew, to + 20 * ceh + 0.0f, lo + 4 * cew, to + 20 * ceh + 0.0f, Creator.lineDefaultBlack)
            canvas.drawLine(lo + 0 * cew, to + 20 * ceh + 2.0f, lo + 4 * cew, to + 20 * ceh + 2.0f, Creator.lineDefaultBlack)

            healthValues["water"] = drawWater(canvas, strokes, lo, to + 20 * ceh, waterText)

            canvas.drawLine(0 + 0 * cew, to + 22 * ceh + 0.0f, lo + 4 * cew, to + 22 * ceh + 0.0f, Creator.lineDefaultBlack)
            canvas.drawLine(0 + 0 * cew, to + 22 * ceh + 2.0f, lo + 4 * cew, to + 22 * ceh + 2.0f, Creator.lineDefaultBlack)

            canvas.drawRect(0.0f, to + 22 * ceh, lo, to + 35 * ceh, Creator.fillGrey80)
            canvas.save()
            canvas.rotate(-90.0f, lo - 5.0f, to + 28.5f * ceh)
            canvas.drawText(endDayText, lo - 5.0f, to + 28.5f * ceh - 5.0f, Creator.textDefaultWhiteCenter)
            canvas.restore()

            healthValues["stress"] = drawTenPointGrid(canvas, strokes, lo + 0 * cew, to + 22 * ceh, stressText)
            healthValues["condition"] = drawTenPointGrid(canvas, strokes, lo + 0 * cew, to + 24 * ceh, conditionText)
            healthValues["mood"] = drawTenPointGrid(canvas, strokes, lo + 0 * cew, to + 26 * ceh, moodText)

            healthValues["pain"] = drawTenPointGrid(canvas, strokes, lo + 2 * cew, to + 22 * ceh, painText)
            healthValues["productivity"] = drawTenPointGrid(canvas, strokes, lo + 2 * cew, to + 24 * ceh, productivityText)
            healthValues["overall"] = drawTenPointGrid(canvas, strokes, lo + 2 * cew, to + 26 * ceh, overallText)

            canvas.drawLine(lo + 0 * cew, to + 28 * ceh + 0.0f, lo + 4 * cew, to + 28 * ceh + 0.0f, Creator.lineDefaultBlack)
            canvas.drawLine(lo + 0 * cew, to + 28 * ceh + 2.0f, lo + 4 * cew, to + 28 * ceh + 2.0f, Creator.lineDefaultBlack)

            // Notes grid
            for (i in 28..34) {
                if (i % 2 == 0) {
                    canvas.drawRect(
                        lo + 0 * cew, to + i * ceh, lo + 2 * cew, to + i * ceh + ceh,
                        Creator.fillGrey20
                    )
                }
                canvas.drawLine(
                    lo + 0 * cew, to + i * ceh, lo + 2 * cew, to + i * ceh,
                    Creator.lineDefaultBlack
                )
            }

            canvas.drawLine(lo + 2 * cew, to + 28 * ceh, lo + 2 * cew, to + 35 * ceh, Creator.lineDefaultBlack)

            // Tasks grid
            for (i in 28..34) {
                if (i % 2 == 0) {
                    canvas.drawRect(
                        lo + 2 * cew, to + i * ceh, lo + 4 * cew, to + i * ceh + ceh,
                        Creator.fillGrey20
                    )
                }
                canvas.drawLine(
                    lo + 2 * cew, to + i * ceh, lo + 4 * cew, to + i * ceh,
                    Creator.lineDefaultBlack
                )
                canvas.drawRect(
                    lo + 2 * cew + 10.0f, to + i * ceh + 10.0f, lo + 2 * cew + 40.0f, to + i * ceh + 40.0f,
                    Creator.lineDefaultGrey50
                )
            }

            calendarDay.calendarValues[CalendarDay.HEALTH_V1_STYLE] = healthValues.toMap()
        }

        private fun drawWakeUp(canvas: Canvas, xo: Float, yo: Float, text: String) {
            canvas.drawLine(xo, yo + 2 * ceh, xo + 2 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 0 * cew, yo, xo + 0 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 1 * cew, yo, xo + 1 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 2 * cew, yo, xo + 2 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 2 * cew - 2 * ceh, yo, xo + 2 * cew - 2 * ceh, yo + 2 * ceh, Creator.lineDefaultBlack)

            canvas.drawText(text, xo + 20.0f, yo + 2 * ceh - 35.0f, Creator.textDefaultBlack)
        }

        private fun drawSleep(canvas: Canvas, xo: Float, yo: Float, text: String) {
            canvas.drawLine(xo, yo + 2 * ceh, xo + 2 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 0 * cew, yo, xo + 0 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 1 * cew, yo, xo + 1 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 2 * cew, yo, xo + 2 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 2 * cew - 2 * ceh, yo, xo + 2 * cew - 2 * ceh, yo + 2 * ceh, Creator.lineDefaultBlack)

            canvas.drawText(text, xo + 20.0f, yo + 2 * ceh - 35.0f, Creator.textDefaultBlack)
        }

        private fun drawWeight(canvas: Canvas, xo: Float, yo: Float, text: String) {
            canvas.drawLine(xo, yo + 2 * ceh, xo + 2 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 0 * cew, yo, xo + 0 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 1 * cew, yo, xo + 1 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 2 * cew, yo, xo + 2 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 2 * cew - 2 * ceh, yo, xo + 2 * cew - 2 * ceh, yo + 2 * ceh, Creator.lineDefaultBlack)

            canvas.drawText(text, xo + 20.0f, yo + 2 * ceh - 35.0f, Creator.textDefaultBlack)
        }

        /**
         * Draw and calculates the calories grid and return the value if checked.
         *
         * @param canvas the canvas
         * @param strokes the strokes
         * @param xo the left X coordinate
         * @param yo the top Y coordinate
         * @param height the height of the grid
         * @param text the text
         * @param calories the calculated calories
         * @param negative display and calculate negative calories
         * @return the checked value or null
         */
        private fun drawCalories(
            canvas: Canvas, strokes: List<Stroke>, xo: Float, yo: Float, height: Float, text: String,
            calories: MutableList<Float> = mutableListOf(), negative: Boolean = false
        ): Float? {
            canvas.drawLine(xo + 1 * cew, yo + 0 * ceh, xo + 1 * cew, yo + height, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 4 * cew - 4 * ceh, yo + 0 * ceh, xo + 4 * cew - 4 * ceh, yo + height, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 0 * cew, yo + height, xo + 4 * cew, yo + height, Creator.lineDefaultBlack)
            canvas.drawText(text, xo + 20.0f, yo + 2 * ceh - 35.0f, Creator.textDefaultBlack)

            var result: Float? = null
            val xgo = (2 * cew - 2 * ceh)
            val xgw = (2 * cew - 2 * ceh) / 20.0f
            val ygw = xgw
            for (i in 0..19) {
                canvas.drawLine(xo + xgo + i * xgw, yo, xo + xgo + i * xgw, yo + ygw, Creator.lineDefaultBlack)
                if (checkStroke(strokes, xo + xgo + i * xgw, yo, xgw, ygw)) {
                    result = (result ?: 0.0f) + 100.0f
                }
            }
            if (negative) {
                result?.let { calories.add(-it) }
            } else {
                result?.let { calories.add(it) }
            }

            canvas.drawLine(xo + xgo + 0 * xgw, yo + ygw, xo + xgo + 20 * xgw, yo + ygw, Creator.lineDefaultBlack)
            if (height > 2 * ceh) {
                canvas.drawRect(xo + 4 * cew - 4 * ceh, yo + 2 * ceh, xo + 4 * cew, yo + 4 * ceh, Creator.fillGrey20)
                canvas.drawLine(xo + 4 * cew - 4 * ceh, yo + height / 2, xo + 4 * cew, yo + height / 2, Creator.lineDefaultBlack)
                if (calories.isNotEmpty()) {
                    canvas.drawText("${calories.sum().toInt()}", xo + 4 * cew - 2 * ceh, yo + 4 * ceh - 25.0f, Creator.text60BlackCenter)
                }
            }

            if (result != null) {
                if (negative) {
                    canvas.drawText("-${result.toInt()}", xo + 4 * cew - 2 * ceh, yo + 2 * ceh - 25.0f, Creator.text60BlackCenter)
                } else {
                    canvas.drawText("${result.toInt()}", xo + 4 * cew - 2 * ceh, yo + 2 * ceh - 25.0f, Creator.text60BlackCenter)
                }
            }

            return result
        }

        /**
         * Draw the water grid and return the value if checked.
         *
         * @param canvas the canvas
         * @param strokes the strokes
         * @param xo the left X coordinate
         * @param yo the top Y coordinate
         * @param text the text
         * @return the checked value or null
         */
        private fun drawWater(canvas: Canvas, strokes: List<Stroke>, xo: Float, yo: Float, text: String): Float? {
            canvas.drawLine(xo + 1 * cew, yo + 0 * ceh, xo + 1 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 4 * cew - 4 * ceh, yo + 0 * ceh, xo + 4 * cew - 4 * ceh, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawText(text, xo + 20.0f, yo + 2 * ceh - 35.0f, Creator.textDefaultBlack)

            var result: Float? = null
            val xgw = (3 * cew - 4 * ceh) / 10.0f
            val ygw = 2.0f * ceh / 3.0f
            for (i in 0..9) {
                val glassPath = Path()
                glassPath.moveTo(xo + cew + i * xgw + 10.0f, yo + 0 * ceh + 10.0f)
                glassPath.lineTo(xo + cew + i * xgw + xgw - 10.0f, yo + 0 * ceh + 10.0f)
                glassPath.lineTo(xo + cew + i * xgw + xgw - 25.0f, yo + 2 * ceh - 10.0f)
                glassPath.lineTo(xo + cew + i * xgw + 25.0f, yo + 2 * ceh - 10.0f)
                glassPath.lineTo(xo + cew + i * xgw + 10.0f, yo + 0 * ceh + 10.0f)
                canvas.drawPath(glassPath, Creator.fillGrey20)

                canvas.drawLine(xo + 1 * cew + i * xgw + xgw, yo + 0 * ceh, xo + 1 * cew + i * xgw + xgw, yo + 2 * ceh, Creator.lineDefaultBlack)
                canvas.drawLine(xo + 1 * cew + i * xgw, yo + 1 * ygw, xo + 1 * cew + i * xgw + xgw, yo + 1 * ygw, Creator.lineDefaultGrey50)
                canvas.drawLine(xo + 1 * cew + i * xgw, yo + 2 * ygw, xo + 1 * cew + i * xgw + xgw, yo + 2 * ygw, Creator.lineDefaultGrey50)

                if (checkStroke(strokes, xo + cew + i * xgw, yo + 0 * ceh + 0 * ygw, xgw, ygw)) {
                    result = (result ?: 0.0f) + 1.0f
                }
                if (checkStroke(strokes, xo + cew + i * xgw, yo + 0 * ceh + 1 * ygw, xgw, ygw)) {
                    result = (result ?: 0.0f) + 1.0f
                }
                if (checkStroke(strokes, xo + cew + i * xgw, yo + 0 * ceh + 2 * ygw, xgw, ygw)) {
                    result = (result ?: 0.0f) + 1.0f
                }
            }

            if (result != null) {
                canvas.drawText("${result.toInt()}", xo + 4 * cew - 2 * ceh, yo + 2 * ceh - 25.0f, Creator.text60BlackCenter)
            }

            return result
        }

        /**
         * Draw the ten point grid and return the value if checked.
         *
         * @param canvas the canvas
         * @param strokes the strokes
         * @param xo the left X coordinate
         * @param yo the top Y coordinate
         * @param text the text
         * @return the checked value or null
         */
        private fun drawTenPointGrid(canvas: Canvas, strokes: List<Stroke>, xo: Float, yo: Float, text: String): Float? {
            canvas.drawLine(xo, yo + 2 * ceh, xo + 2 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 0 * cew, yo, xo + 0 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 1 * cew, yo, xo + 1 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 2 * cew, yo, xo + 2 * cew, yo + 2 * ceh, Creator.lineDefaultBlack)
            canvas.drawLine(xo + 2 * cew - 2 * ceh, yo, xo + 2 * cew - 2 * ceh, yo + 2 * ceh, Creator.lineDefaultBlack)

            val xgw = (cew - 2 * ceh) / 5.0f
            canvas.drawLine(xo + cew + 1 * xgw, yo, xo + cew + 1 * xgw, yo + 2 * ceh, Creator.lineDefaultGrey50)
            canvas.drawLine(xo + cew + 2 * xgw, yo, xo + cew + 2 * xgw, yo + 2 * ceh, Creator.lineDefaultGrey50)
            canvas.drawLine(xo + cew + 3 * xgw, yo, xo + cew + 3 * xgw, yo + 2 * ceh, Creator.lineDefaultGrey50)
            canvas.drawLine(xo + cew + 4 * xgw, yo, xo + cew + 4 * xgw, yo + 2 * ceh, Creator.lineDefaultGrey50)
            canvas.drawLine(xo + cew, yo + ceh, xo + cew + 5 * xgw, yo + ceh, Creator.lineDefaultGrey50)

            var result: Float? = null
            canvas.drawText(text, xo + 20.0f, yo + 2 * ceh - 35.0f, Creator.textDefaultBlack)
            for (i in 0..4) {
                canvas.drawText("$i", xo + cew + (i + 0.5f) * xgw, yo + 1 * ceh - 10.0f, Creator.textDefaultGray20Center)
                if (checkStroke(strokes, xo + cew + i * xgw, yo + 0 * ceh, xgw, ceh)) {
                    result = i.toFloat()
                }
            }
            for (i in 5..9) {
                canvas.drawText("$i", xo + cew + (i - 4.5f) * xgw, yo + 2 * ceh - 10.0f, Creator.textDefaultGray20Center)
                if (checkStroke(strokes, xo + cew + (i - 5) * xgw, yo + 1 * ceh, xgw, ceh)) {
                    result = i.toFloat()
                }
            }

            if (result != null) {
                canvas.drawText("${result.toInt()}", xo + 2 * cew - ceh, yo + 2 * ceh - 25.0f, Creator.text60BlackCenter)
            }

            return result
        }

        /**
         * Check the stroke in boundary.
         *
         * @param strokes the strokes
         * @param xo the left X coordinate
         * @param yo the top Y coordinate
         * @param width the width of the area
         * @param height the height of the area
         * @return true if there is a stroke point in the boundary
         */
        private fun checkStroke(strokes: List<Stroke>, xo: Float, yo: Float, width: Float, height: Float): Boolean {
            strokes.forEach { stroke ->
                val numberOf = stroke.strokePoints
                    .filter { p -> xo < p.x }.filter { p -> xo + width > p.x }
                    .filter { p -> yo < p.y }.count { p -> yo + height > p.y }
                if (numberOf > 0) return true
            }

            return false
        }
    }
}
