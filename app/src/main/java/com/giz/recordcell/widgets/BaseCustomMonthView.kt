package com.giz.recordcell.widgets

import android.content.Context
import android.graphics.*
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.MonthView

open class BaseCustomMonthView(context: Context) : MonthView(context) {

    protected var changeSelectedTextColor = true
    protected var changeToWhiteText = false
    // 日期颜色
    protected enum class DayColor(val color: Int) {
        NORMAL(Color.BLACK),        // 常规
        SELECTED(Color.WHITE),      // 选中
        OTHER(0xFFDDDDDD.toInt())   // 非本月
    }
    // 农历颜色
    protected enum class LunarColor(val color: Int) {
        NORMAL(0xFFBBBBBB.toInt()),     // 常规
        SELECTED(Color.WHITE),          // 选中
        FESTIVAL(0xFF37CEFF.toInt()),   // 节日
        OTHER(0xFFDDDDDD.toInt())       // 非本月
    }
    // 日期数字画笔
    protected val dayTextPaint = Paint().apply {
        color = DayColor.NORMAL.color
        textSize = 48f
        style = Paint.Style.FILL
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
    }
    // 农历画笔
    protected val lunarTextPaint = Paint().apply {
        color = LunarColor.NORMAL.color
        textSize = 28f
        style = Paint.Style.FILL
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
    }
    protected val minusRadius = 16f;
    // 选中背景笔刷
    protected val selectedBgPaint = Paint().apply {
        color = 0xFF37CEFF.toInt()
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        style = Paint.Style.FILL
    }
    // 选中背景阴影笔刷
    protected val selectedShadePaint = Paint().apply {
        color = 0xC837CEFF.toInt()
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        maskFilter = BlurMaskFilter(minusRadius, BlurMaskFilter.Blur.NORMAL)
    }
    protected val strokePaint = Paint().apply {
        color = Color.GRAY
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        style = Paint.Style.STROKE
    }
    protected val schemePaint = Paint().apply {
        color = 0xFF37CEFF.toInt()
        flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        style = Paint.Style.FILL
    }
    // 日期数字与节日文字之间的距离
    protected val gapLength = 20f;

    /**
     * 绘制日历文本
     *
     * @param canvas     canvas
     * @param calendar   日历calendar
     * @param x          日历Card x起点坐标
     * @param y          日历Card y起点坐标
     * @param hasScheme  是否是标记的日期
     * @param isSelected 是否选中
     */
    override fun onDrawText(
        canvas: Canvas?,
        calendar: Calendar?,
        x: Int,
        y: Int,
        hasScheme: Boolean,
        isSelected: Boolean
    ) {
        // 日期数字
        if(!calendar!!.isCurrentMonth) { // 非本月
            dayTextPaint.color = DayColor.OTHER.color
        }else{
            dayTextPaint.color = if(isSelected && changeSelectedTextColor) DayColor.SELECTED.color else DayColor.NORMAL.color
        }
        // 农历
        if(!calendar.isCurrentMonth){ // 非本月
            lunarTextPaint.color = LunarColor.OTHER.color
        }else{
            if(isSelected && changeSelectedTextColor){
                lunarTextPaint.color = LunarColor.SELECTED.color
            }else{
                lunarTextPaint.color = if(isFestival(calendar)) LunarColor.FESTIVAL.color else LunarColor.NORMAL.color
            }
        }
        // 改变成白色
        if(changeToWhiteText){
            dayTextPaint.color = Color.WHITE
            lunarTextPaint.color = Color.WHITE
        }

        val day = calendar.day.toString()
        val festival = getFestival(calendar)

        val dayRect = Rect().also { dayTextPaint.getTextBounds(day, 0, day.length, it) }
        val festivalRect = Rect().also { lunarTextPaint.getTextBounds(festival, 0, festival.length, it) }

        val dayX = (mItemWidth - dayRect.width()) / 2f + x.toFloat()
        val dayY = (mItemHeight - dayRect.height() - festivalRect.height() - gapLength) / 2f + y.toFloat() + dayRect.height()

        val festivalX = (mItemWidth - festivalRect.width()) / 2f + x.toFloat()
        val festivalY = dayY + gapLength + festivalRect.height()

        // 绘制文本
        canvas?.drawText(day, dayX, dayY, dayTextPaint)
        canvas?.drawText(festival, festivalX, festivalY, lunarTextPaint)

        if(!isSelected && calendar.isCurrentDay) { // 今天未被选中时，圈出今天的日期
            canvas?.drawCircle(x + mItemWidth / 2f, y + mItemHeight / 2f, mItemWidth / 2f - minusRadius, strokePaint)
        }
    }
    private fun isFestival(calendar: Calendar) = !calendar.traditionFestival.isNullOrEmpty() ||
            !calendar.gregorianFestival.isNullOrEmpty()

    // 获得节日/农历，传统节日优先
    private fun getFestival(calendar: Calendar) = if(!calendar.traditionFestival.isNullOrEmpty()){
        calendar.traditionFestival
    }else if(!calendar.gregorianFestival.isNullOrEmpty()){
        calendar.gregorianFestival
    }else{
        calendar.lunar
    }

    /**
     * 绘制选中的日子
     *
     * @param canvas    canvas
     * @param calendar  日历日历calendar
     * @param x         日历Card x起点坐标
     * @param y         日历Card y起点坐标
     * @param hasScheme hasScheme 非标记的日期
     * @return 返回true 则绘制onDrawScheme，因为这里背景色不是是互斥的，所以返回true
     */
    override fun onDrawSelected(
        canvas: Canvas?,
        calendar: Calendar?,
        x: Int,
        y: Int,
        hasScheme: Boolean
    ): Boolean {
        canvas?.drawCircle(x + mItemWidth / 2f, y + mItemHeight / 2f, mItemWidth / 2f - minusRadius, selectedShadePaint)
        canvas?.drawCircle(x + mItemWidth / 2f, y + mItemHeight / 2f, mItemWidth / 2f - minusRadius, selectedBgPaint)
        return true
    }

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
        canvas?.drawCircle(x + mItemWidth - r, y + r, r, schemePaint)
    }

}