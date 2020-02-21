package com.giz.recordcell.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.haibin.calendarview.Calendar

class DailyFinishCaseWeekView(context: Context) : BaseCustomWeekView(context) {

    private val todoBgPaint = Paint().apply {
        color = Color.GRAY
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val finishOrNotBgPaint = Paint().apply {
        color = Color.BLUE
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        style = Paint.Style.FILL
    }

    override fun onDrawSelected(
        canvas: Canvas?,
        calendar: Calendar?,
        x: Int,
        hasScheme: Boolean
    ): Boolean {
        return if(hasScheme){
            changeSelectedTextColor = false
            selectedBgPaint.style = Paint.Style.STROKE
            selectedBgPaint.strokeWidth = 6f
            canvas?.drawRoundRect(x.toFloat(), 4f, x + mItemWidth.toFloat(),
                mItemHeight.toFloat() - 4f, 16f, 16f, selectedBgPaint)
            true
        }else{
            changeSelectedTextColor = true
            selectedBgPaint.style = Paint.Style.FILL
            super.onDrawSelected(canvas, calendar, x, hasScheme)
        }
    }

    override fun onDrawText(
        canvas: Canvas?,
        calendar: Calendar?,
        x: Int,
        hasScheme: Boolean,
        isSelected: Boolean
    ) {
        changeToWhiteText = if(hasScheme){
            calendar!!.scheme != "Todo"
        }else{
            false
        }
        super.onDrawText(canvas, calendar, x, hasScheme, isSelected)
    }

    // 绘制标记
    override fun onDrawScheme(canvas: Canvas?, calendar: Calendar?, x: Int) {
        calendar ?: return
        when(calendar.scheme) {
            // 今天之后未完成
            "Todo" -> canvas?.drawCircle(x + mItemWidth / 2f, y + mItemHeight / 2f, mItemWidth / 2f - minusRadius, todoBgPaint)
            "Finish", "NotFinish" -> {
                finishOrNotBgPaint.color = calendar.schemeColor
                canvas?.drawCircle(x + mItemWidth / 2f, y + mItemHeight / 2f, mItemWidth / 2f - minusRadius, finishOrNotBgPaint)
            }
        }
    }

}