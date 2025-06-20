package com.example.triptogether.ui

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.triptogether.R

object MainScreenUiHelper {

    fun styleSelectedTab(context: Context, selected: TextView, unselected: TextView) {
        selected.setTextColor(ContextCompat.getColor(context, R.color.black))
        selected.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        selected.setTypeface(null, Typeface.BOLD)

        unselected.setTextColor(ContextCompat.getColor(context, R.color.gray))
        unselected.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        unselected.setTypeface(null, Typeface.NORMAL)
    }
}
