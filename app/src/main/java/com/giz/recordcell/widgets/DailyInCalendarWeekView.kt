package com.giz.recordcell.widgets

import android.content.Context
import android.graphics.*
import com.haibin.calendarview.Calendar

class DailyInCalendarWeekView(context: Context) : BaseCustomWeekView(context) {

    override fun onDrawScheme(canvas: Canvas?, calendar: Calendar?, x: Int) {
        val r = 10f
        calendar?.schemes?.forEachIndexed { idx, s ->
            schemePaint.color = s.shcemeColor
            canvas?.drawCircle(x + mItemWidth - r,  r * (2 * idx + 1) + 4f * idx, r, schemePaint)
        }
    }

}

/*
// 日期颜色
    private enum class DayColor(val color: Int) {
        NORMAL(Color.BLACK),        // 常规
        SELECTED(Color.WHITE),      // 选中
        OTHER(0xFFDDDDDD.toInt())   // 非本月
    }
    // 农历颜色
    private enum class LunarColor(val color: Int) {
        NORMAL(0xFFBBBBBB.toInt()),     // 常规
        SELECTED(Color.WHITE),          // 选中
        FESTIVAL(0xFF37CEFF.toInt()),   // 节日
        OTHER(0xFFDDDDDD.toInt())       // 非本月
    }
    // 日期数字画笔
    private val dayTextPaint = Paint().apply {
        color = DayColor.NORMAL.color
        textSize = 48f
        style = Paint.Style.FILL
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
    }
    // 农历画笔
    private val lunarTextPaint = Paint().apply {
        color = LunarColor.NORMAL.color
        textSize = 28f
        style = Paint.Style.FILL
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
    }
    private val minusRadius = 16f;
    private val selectedBgPaint = Paint().apply {
        color = 0xFF37CEFF.toInt()
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        style = Paint.Style.FILL
    }
    private val selectedShadePaint = Paint().apply {
        color = 0xC837CEFF.toInt()
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
    }
    private val strokePaint = Paint().apply {
        color = Color.GRAY
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        style = Paint.Style.STROKE
    }
    // 日期数字与节日文字之间的距离
    private val gapLength = 20f;

    override fun onDrawSelected(
        canvas: Canvas?,
        calendar: Calendar?,
        x: Int,
        hasScheme: Boolean
    ): Boolean {
        if(selectedShadePaint.maskFilter == null){
            selectedShadePaint.maskFilter = BlurMaskFilter(minusRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas?.drawCircle(x + mItemWidth / 2f,  mItemHeight / 2f, mItemWidth / 2f - minusRadius, selectedShadePaint)
        canvas?.drawCircle(x + mItemWidth / 2f, mItemHeight / 2f, mItemWidth / 2f - minusRadius, selectedBgPaint)
        return true
    }

    override fun onDrawText(
        canvas: Canvas?,
        calendar: Calendar?,
        x: Int,
        hasScheme: Boolean,
        isSelected: Boolean
    ) {
        // 日期数字
        if(!calendar!!.isCurrentMonth) { // 非本月
            dayTextPaint.color = DayColor.OTHER.color
        }else{
            dayTextPaint.color = if(isSelected) DayColor.SELECTED.color else DayColor.NORMAL.color
        }
        // 农历
        if(!calendar.isCurrentMonth){ // 非本月
            lunarTextPaint.color = LunarColor.OTHER.color
        }else{
            if(isSelected){
                lunarTextPaint.color = LunarColor.SELECTED.color
            }else{
                lunarTextPaint.color = if(isFestival(calendar)) LunarColor.FESTIVAL.color else LunarColor.NORMAL.color
            }
        }

        val day = calendar.day.toString()
        val festival = getFestival(calendar)

        val dayRect = Rect().also { dayTextPaint.getTextBounds(day, 0, day.length, it) }
        val festivalRect = Rect().also { lunarTextPaint.getTextBounds(festival, 0, festival.length, it) }

        val dayX = (mItemWidth - dayRect.width()) / 2f + x.toFloat()
        val dayY = (mItemHeight - dayRect.height() - festivalRect.height() - gapLength) / 2f + dayRect.height()

        val festivalX = (mItemWidth - festivalRect.width()) / 2f + x.toFloat()
        val festivalY = dayY + gapLength + festivalRect.height()

        // canvas?.drawRect(x.toFloat(), y.toFloat(), x.toFloat() + mItemWidth, y.toFloat() + mItemHeight, strokePaint)
        canvas?.drawText(day, dayX, dayY, dayTextPaint)
        canvas?.drawText(festival, festivalX, festivalY, lunarTextPaint)

        if(!isSelected && calendar.isCurrentDay) { // 今天未被选中时，圈出今天的日期
            canvas?.drawCircle(x + mItemWidth / 2f, mItemHeight / 2f, mItemWidth / 2f - minusRadius, strokePaint)
        }
    }
    private fun isFestival(calendar: Calendar) = !calendar.traditionFestival.isNullOrEmpty() ||
            !calendar.gregorianFestival.isNullOrEmpty()

    private val lunarList = arrayOf("初一", "初二")
    // 获得节日/农历，传统节日优先
    private fun getFestival(calendar: Calendar) = if(!calendar.traditionFestival.isNullOrEmpty()){
        calendar.traditionFestival
    }else if(!calendar.gregorianFestival.isNullOrEmpty()){
        calendar.gregorianFestival
    }else{
        calendar.lunar
    }

    override fun onDrawScheme(canvas: Canvas?, calendar: Calendar?, x: Int) {
        val r = 16f
        canvas?.drawCircle(x + mItemWidth - r, r, r, selectedBgPaint)
    }
 */