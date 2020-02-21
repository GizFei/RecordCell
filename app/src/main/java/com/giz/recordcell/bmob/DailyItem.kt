package com.giz.recordcell.bmob

import android.content.Context
import cn.bmob.v3.BmobObject
import cn.bmob.v3.datatype.BmobDate
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import cn.bmob.v3.listener.UpdateListener
import com.giz.recordcell.R
import com.giz.recordcell.helpers.printLog
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.WaitProgressDialog
import java.io.Serializable
import java.util.*

data class DailyItem(val user: RecordUser?,
                     var name: String,
                     var desc: String,
                     var startTime: BmobDate,
                     var endTime: BmobDate?,  // 为空表示无限期
                     var repeatType: Int,
                     var repeatDays: MutableList<Int>,
                     var finishCases: MutableList<FinishCase>) : BmobObject() {

    companion object {
        // 重复间隔：天，周，月
        const val REPEAT_TYPE_DAY = 1;
        const val REPEAT_TYPE_WEEK = 2;
        const val REPEAT_TYPE_MONTH = 3;

        fun translateWeekNum(day: Int) = when(day){
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> "未知"
        }
    }

    constructor(): this(null, "", "", BmobDate(Date()), null, REPEAT_TYPE_DAY,
        mutableListOf(), mutableListOf())

    data class FinishCase(val year: Int,
                          val month: Int, // 1-12
                          val day: Int,   // 1-31
                          var remark: String // 心得
    ): Serializable {
        override fun hashCode(): Int {
            return super.hashCode()
        }
        override fun equals(other: Any?): Boolean {
            return other is FinishCase && (other.year == year &&  other.month == month
                    && other.day == day)
        }
    }

    // 保存
    fun saveDailyItem(context: Context, onSuccess: () -> Unit) {
        val waitProgressDialog = WaitProgressDialog(context).also { it.show() }
        save(object : SaveListener<String>() {
            override fun done(p0: String?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null){
                    context.showToast(R.string.bmob_save_success_text)
                    onSuccess()
                    context.printLog("保存成功：${this@DailyItem}")
                }else{
                    context.showToast(R.string.bmob_save_failure_text)
                    context.printLog("保存失败：$p1")
                }
            }
        })
    }

    // 更新
    fun updateDailyItem(context: Context, onSuccess: () -> Unit) {
        val waitProgressDialog = WaitProgressDialog(context)
        update(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    context.showToast(R.string.bmob_update_success_text)
                    onSuccess()
                    context.printLog("更新日常成功: ${this@DailyItem}")
                }else{
                    context.showToast(R.string.bmob_update_failure_text)
                    context.printLog("更新日常失败：$p0")
                }
            }
        })
    }

    // 删除
    fun deleteDailyItem(context: Context, onSuccess: () -> Unit){
        val waitProgressDialog = WaitProgressDialog(context).also { it.show() }
        delete(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    context.showToast(R.string.bmob_delete_success_text)
                    onSuccess()
                    context.printLog("删除成功：${this@DailyItem}")
                }else{
                    context.showToast(R.string.bmob_delete_failure_text)
                    context.printLog("删除失败：$p0")
                }
            }
        })
    }

    // 判断今天是否有该任务
    fun isTodayTask(): Boolean = isTaskOfDate(Date())

    fun isTaskOfDate(date: Date): Boolean {
        val today = getYMDCalendarDate(date)
        val start = getYMDCalendarDate(startTime.fetchDate())
        val end = if(endTime == null) null else getYMDCalendarDate(endTime!!.fetchDate())
        // printLog("$name: ${today.queryDayOfWeek()}")
        return if(today.after(start) || today == start){
            when(repeatType) {
                REPEAT_TYPE_DAY -> {
                    return if(endTime != null){
                        today.before(end) || today == end
                    }else{
                        true
                    }
                }
                REPEAT_TYPE_WEEK -> {
                    if(endTime != null){
                        if(today.before(end) || today == end){
                            today.queryDayOfWeek() in repeatDays
                        }else{
                            false
                        }
                    }else{ // 长期
                        today.queryDayOfWeek() in repeatDays
                    }
                }
                REPEAT_TYPE_MONTH -> {
                    if(endTime != null){
                        if(today.before(end) || today == end){
                            today.queryDay() in repeatDays
                        }else{
                            false
                        }
                    }else{ // 长期
                        today.queryDay() in repeatDays
                    }
                }
                else -> false
            }
        }else{
            false
        }
    }

    fun finishedTodayTask() : Boolean {
        val todayCase = Date().let {
            FinishCase(it.queryYear(), it.queryMonth(), it.queryDay(), "")
        }
        return if(isTodayTask()){
            finishCases.find {
                it == todayCase
            } != null
        }else{
            false
        }
    }

    fun finishedTaskOfDate(date: Date): Boolean {
        val dateCase = FinishCase(date.queryYear(), date.queryMonth(), date.queryDay(), "")
        return if(isTaskOfDate(date)){
            finishCases.find {
                it == dateCase
            } != null
        }else{
            false
        }
    }

    fun getRepeatDatesOfMonth(year: Int, month: Int): MutableList<Date> {
        val dateList = mutableListOf<Date>()
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }
        do {
            if(isTaskOfDate(cal.time)){
                dateList.add(cal.time)
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }while (cal.getMonth() == month)
        return dateList
    }
}