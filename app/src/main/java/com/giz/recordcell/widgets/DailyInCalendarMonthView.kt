package com.giz.recordcell.widgets

import android.content.Context
import android.graphics.*
import com.haibin.calendarview.Calendar

class DailyInCalendarMonthView(context: Context) : BaseCustomMonthView(context) {

    /**
     * 绘制标记的日期,这里可以是背景色，标记色什么的
     *
     * @param canvas   canvas
     * @param calendar 日历calendar
     * @param x        日历Card x起点坐标
     * @param y        日历Card y起点坐标
     */
    override fun onDrawScheme(canvas: Canvas?, calendar: Calendar?, x: Int, y: Int) {
        val r = 10f
//        canvas?.drawCircle(x + mItemWidth - r, y + r, r, selectedBgPaint)
        calendar?.schemes?.forEachIndexed { idx, s ->
            schemePaint.color = s.shcemeColor
            canvas?.drawCircle(x + mItemWidth - r, y + r * (2 * idx + 1) + 4f * idx, r, schemePaint)
        }
    }

}