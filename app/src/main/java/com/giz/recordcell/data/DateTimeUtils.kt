package com.giz.recordcell.data

import android.text.format.DateFormat
import java.util.*

// 时间日期相关的工具函数

// 获取日期中的月份
fun getMonthFromDate(date: Date): Int = Calendar.getInstance().apply { time = date }
    .run { get(Calendar.MONTH) + 1 }

// 获取日期中的年份
fun getYearFromDate(date: Date): Int = Calendar.getInstance().apply { time = date }
    .run { get(Calendar.YEAR) }

// 判断一个日期是否为今年
fun isInCurrentYear(date: Date): Boolean {
    return getYearFromDate(Date()) == getYearFromDate(date)
}

// 判断两个日期是否是同一个月（同年）
fun isSameMonth(date1: Date, date2: Date): Boolean = getYearFromDate(date1) == getYearFromDate(date2)
        && getMonthFromDate(date1) == getMonthFromDate(date2)

// 合成描述日期的文本
fun formatNoteDateText(date: Date): String {
    return if (isInCurrentYear(date)) {
        DateFormat.format("MM月dd日 kk:mm", date).toString()
    } else {
        DateFormat.format("yyyy年MM月dd日 kk:mm", date).toString()
    }
}
// 合成只有年月日的日期文本
fun formatCalendarTextAlwaysWithYear(date: Date): String = DateFormat.format("yyyy年MM月dd日", date).toString()
fun formatCalendarText(date: Date): String = if(isInCurrentYear(date)) {
    DateFormat.format("MM月dd日", date).toString()
}else{
    DateFormat.format("yyyy年MM月dd日", date).toString()
}
fun formatNoteDateTextAlwaysWithYear(date: Date): String = DateFormat.
    format("yyyy年MM月dd日 kk:mm", date).toString()
fun formatCollectedTimeText(date: Date): String = if(isInCurrentYear(date)){
    DateFormat.format("MM-dd kk:mm", date).toString()
}else{
    DateFormat.format("yyyy-MM-dd kk:mm", date).toString()
}


// 获得如“2020年1月”一样的日期格式（年月）
fun formMonthText(date: Date): String {
    val monthStrings = arrayOf(
        "一月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "十一月", "十二月"
    )
    val month = getMonthFromDate(date)
    return if (isInCurrentYear(date)) {
        monthStrings[month - 1]
    } else {
        DateFormat.format("yyyy年MM月", date).toString()
    }
}

// 获得到月的整点日期，如“2020年1月1日 0:0:0:00”
fun getDateByMonthUnit(date: Date): Date = Calendar.getInstance().also { it.time = date }.apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.run { time }
