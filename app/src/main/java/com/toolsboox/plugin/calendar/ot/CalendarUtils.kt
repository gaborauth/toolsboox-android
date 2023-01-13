package com.toolsboox.plugin.calendar.ot

import android.content.SharedPreferences
import androidx.constraintlayout.widget.ConstraintSet
import com.toolsboox.R
import com.toolsboox.databinding.FragmentCalendarBinding
import javax.inject.Inject

/**
 * Common calendar utils.
 *
 * @author <a href="mailto:gabor.auth@toolsboox.com">Gábor AUTH</a>
 */
class CalendarUtils @Inject constructor() {

    /**
     * The shared preferences.
     */
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    /**
     * Update toolbar side.
     *
     * @param binding the binding
     * @param switch switch side
     */
    fun updateToolbar(binding: FragmentCalendarBinding, switch: Boolean = false) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.drawingLayout)

        val calendarSide = sharedPreferences.getString("calendarToolbarSide", "LEFT")
        val newCalendarSide = if ("LEFT" == calendarSide) {
            if (switch) "RIGHT" else "LEFT"
        } else {
            if (switch) "LEFT" else "RIGHT"
        }

        if ("RIGHT" == newCalendarSide) {
            if (switch) {
                sharedPreferences.edit().putString("calendarToolbarSide", "RIGHT").apply()
            }

            constraintSet.connect(R.id.navigatorImageView, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.navigatorImageView, ConstraintSet.END, R.id.toolbarDrawing, ConstraintSet.START)
            constraintSet.connect(R.id.toolbarDrawing, ConstraintSet.START, R.id.templateImageView, ConstraintSet.END)
            constraintSet.connect(R.id.toolbarDrawing, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.templateImageView, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.templateImageView, ConstraintSet.END, R.id.toolbarDrawing, ConstraintSet.START)
            constraintSet.connect(R.id.surfaceView, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.surfaceView, ConstraintSet.END, R.id.toolbarDrawing, ConstraintSet.START)
        } else {
            if (switch) {
                sharedPreferences.edit().putString("calendarToolbarSide", "LEFT").apply()
            }

            constraintSet.connect(R.id.navigatorImageView, ConstraintSet.START, R.id.toolbarDrawing, ConstraintSet.END)
            constraintSet.connect(R.id.navigatorImageView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.toolbarDrawing, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.toolbarDrawing, ConstraintSet.END, R.id.templateImageView, ConstraintSet.START)
            constraintSet.connect(R.id.templateImageView, ConstraintSet.START, R.id.toolbarDrawing, ConstraintSet.END)
            constraintSet.connect(R.id.templateImageView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.surfaceView, ConstraintSet.START, R.id.toolbarDrawing, ConstraintSet.END)
            constraintSet.connect(R.id.surfaceView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        }

        constraintSet.applyTo(binding.drawingLayout)
    }
}