package com.giz.recordcell.widgets

import android.content.Context
import android.graphics.Canvas
import com.giz.recordcell.helpers.printLog
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.YearView

class CustomYearView(context: Context) : YearView(context) {

    override fun onDrawMonth(
        canvas: Canvas?,
        year: Int,
        month: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        context.printLog("年视图，绘制月份。宽度：$width, 高度：$height")
    }

    override fun onDrawScheme(canvas: Canvas?, calendar: Calendar?, x: Int, y: Int) {

    }

    override fun onDrawSelected(
        canvas: Canvas?,
        calendar: Calendar?,
        x: Int,
        y: Int,
        hasScheme: Boolean
    ): Boolean {
        return true
    }

    override fun onDrawText(
        canvas: Canvas?,
        calendar: Calendar?,
        x: Int,
        y: Int,
        hasScheme: Boolean,
        isSelected: Boolean
    ) {

    }

    override fun onDrawWeek(canvas: Canvas?, week: Int, x: Int, y: Int, width: Int, height: Int) {

    }

}