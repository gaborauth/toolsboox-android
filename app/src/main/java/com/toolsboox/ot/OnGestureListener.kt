package com.toolsboox.ot

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs
import kotlin.math.atan2

class OnGestureListener : GestureDetector.SimpleOnGestureListener() {
    companion object {
        /**
         * The swipes directions array.
         */
        private val swipeDirections = arrayOf(0, 0, 0, 0)

        /**
         * Swipe not finished yet
         */
        const val NONE = -1

        /**
         * Left to right swipe.
         */
        const val LTR = 0

        /**
         * Right to left swipe.
         */
        const val RTL = 1

        /**
         * Up to down swipe.
         */
        const val UTD = 2

        /**
         * Down to up swipe.
         */
        const val DTU = 3
    }

    fun onTouchEvent(gd: GestureDetectorCompat, v: View, e: MotionEvent): Int {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> swipeDirections.fill(0)
            MotionEvent.ACTION_UP -> {
                if (swipeDirections.max() > 0) return swipeDirections.indexOf(swipeDirections.maxOrNull())
            }
        }

        gd.onTouchEvent(e)
        return NONE
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (e1 == null) return false

        if (abs(distanceX) + abs(distanceY) < 10) return false

        val angle = Math.toDegrees(atan2((e1.y - e2.y).toDouble(), (e2.x - e1.x).toDouble())).toFloat()
        if (angle > -45 && angle <= 45) {
            swipeDirections[LTR]++
            return true
        }
        if (angle >= 135 && angle < 180 || angle < -135 && angle > -180) {
            swipeDirections[RTL]++
            return true
        }
        if (angle < -45 && angle >= -135) {
            swipeDirections[UTD]++
            return true
        }
        if (angle > 45 && angle <= 135) {
            swipeDirections[DTU]++
            return true
        }
        return false
    }
}