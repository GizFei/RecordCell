package com.giz.recordcell.bmob

import cn.bmob.v3.datatype.BmobDate
import java.util.*

const val APPLICATION_ID = "779a0c145eb68fff2e3bf9f14863c5b3"

fun BmobDate.fetchDate(): Date = Date(BmobDate.getTimeStamp(date))

fun Date.toCalendar(): Calendar = Calendar.getInstance().also { it.time = this }
// 获得年份
fun Calendar.getYear() = this.get(Calendar.YEAR)
fun Date.queryYear() = this.toCalendar().getYear()
// 获得月份（1-12）
fun Calendar.getMonth() = this.get(Calendar.MONTH) + 1
fun Date.queryMonth() = this.toCalendar().getMonth()
// 获得日子
fun Calendar.getDay() = this.get(Calendar.DAY_OF_MONTH)
fun Date.queryDay() = this.toCalendar().getDay()
// 获得星期（1-7，周日-周六）
fun Calendar.getDayOfWeek() = this.get(Calendar.DAY_OF_WEEK)
fun Date.queryDayOfWeek() = this.toCalendar().getDayOfWeek()
// 获取只包含年月日的日期，month:1-12
fun getYMDCalendarDate(year: Int, month: Int, day: Int): Date = Calendar.getInstance().also {
    it.set(year, month - 1, day, 0, 0, 0)
    it.set(Calendar.MILLISECOND, 0)
}.time
fun getYMDCalendarDate(date: Date): Date = Calendar.getInstance().also {
    it.set(date.queryYear(), date.queryMonth() - 1, date.queryDay(), 0, 0, 0)
    it.set(Calendar.MILLISECOND, 0)
}.time
fun getYMDCalendarDate(calendar: com.haibin.calendarview.Calendar) = getYMDCalendarDate(calendar.year,
    calendar.month, calendar.day)