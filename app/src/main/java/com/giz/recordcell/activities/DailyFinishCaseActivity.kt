package com.giz.recordcell.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.QueryListener
import com.giz.android.toolkit.dp2pxSize
import com.giz.recordcell.R
import com.giz.recordcell.bmob.*
import com.giz.recordcell.helpers.*
import com.giz.recordcell.widgets.WaitProgressDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView
import kotlinx.android.synthetic.main.activity_daily_finish_case.*
import java.util.*

class DailyFinishCaseActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_DAILY_ITEM = "DailyItem"

        fun newIntent(context: Context, item: DailyItem) = Intent(context, DailyFinishCaseActivity::class.java)
            .apply {
                putExtra(EXTRA_DAILY_ITEM, item)
            }
    }

    private lateinit var dailyItem: DailyItem
    private lateinit var calendarView: CalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_finish_case)

        dailyItem = intent.getSerializableExtra(EXTRA_DAILY_ITEM) as DailyItem

        calendarView = findViewById(R.id.daily_finish_case_calendarView)
        calendarView.apply {
            setOnCalendarSelectListener(object : CalendarView.OnCalendarSelectListener {
                override fun onCalendarSelect(calendar: Calendar?, isClick: Boolean) {
                    calendar ?: return
                    setFinishCaseRemark(calendar)
                }

                override fun onCalendarOutOfRange(calendar: Calendar?) {}
            })
            setOnMonthChangeListener { year, month ->
                addSchemes()
                setDateTv(year, month)
            }
            post {
                addSchemes()
                setDateTv(calendarView.curYear, calendarView.curMonth)
                setFinishCaseRemark(calendarView.selectedCalendar)
            }
        }
        daily_finish_case_edit_btn.setOnClickListener {
            editFinishCase()
        }
        daily_finish_case_save_btn.setOnClickListener {
            saveFinishCase()
        }
    }

    private fun setDateTv(year: Int, month: Int) {
        daily_finish_case_date_tv.text = "${year}年${month}月"
    }

    // 更新活动中的DailyItem
    private fun refreshDailyItem() {
        val waitProgressDialog = WaitProgressDialog(this).apply { show() }
        val dailyQuery = BmobQuery<DailyItem>()
        dailyQuery.getObject(dailyItem.objectId, object : QueryListener<DailyItem>() {
            override fun done(p0: DailyItem?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null){
                    if(p0 == null){
                        printLog("日常为空")
                    }else{
                        dailyItem = p0
                        setFinishCaseRemark(calendarView.selectedCalendar)
                        addSchemes()
                    }
                }else{
                    showToast("刷新失败")
                    printLog("刷新失败, $p1")
                }
            }
        })
    }

    // 填充某个日期的打卡心得
    private fun setFinishCaseRemark(calendar: Calendar) {
        val calDate = getYMDCalendarDate(calendar)
        if(dailyItem.isTaskOfDate(calDate)){ // 该日期包含该日常
            daily_item_cv.toggleVisibility(this) {true}
            daily_item_name.text = dailyItem.name

            if(calendar.isCurrentDay){
                daily_item_finish_btn.setOnClickListener {
                    MaterialAlertDialogBuilder(this, R.style.CustomDialog)
                        .setTitle("确定已完成今日[${dailyItem.name}]之事吗？")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            addFinishCase(dailyItem)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
            }else{
                daily_item_finish_btn.setOnClickListener {
                    val today = getYMDCalendarDate(Date())
                    if(calDate.before(today)){
                        showToast("逝者如斯夫，往前看~")
                    }else{
                        showToast("明天尚未到来，专注当下~")
                    }
                }
            }

            dailyItem.finishCases.find {
                it.year == calendar.year && it.month == calendar.month && it.day == calendar.day
            }.let {
                if(it == null){
                    daily_finish_case_remark.clearText()
                    daily_item_finish_btn.isEnabled = true
                    daily_item_name.setTextColor(Color.BLACK)
                }else{ // 已完成
                    daily_finish_case_remark.setText(it.remark)
                    daily_item_finish_btn.isEnabled = false
                    daily_item_name.setTextColor(Color.GRAY)
                }
            }
        }else{
            daily_item_cv.toggleVisibility(this) {false}
        }
    }

    // 在日历上展示日常情况
    private fun addSchemes() {
        val today = getYMDCalendarDate(Date())
        calendarView.currentMonthCalendars?.forEach { c ->
            if(c.isCurrentMonth) {
                val date = getYMDCalendarDate(c.year, c.month, c.day)
                if(dailyItem.isTaskOfDate(date)){
                    if(date.after(today)){ // 今天之后的任务，待完成
                        c.apply {
                            scheme = "Todo"
                            schemeColor = Color.GRAY
                        }
                    }else{
                        if(dailyItem.finishedTaskOfDate(date)){ // 今天之前，已完成
                            c.apply {
                                scheme = "Finish"
                                schemeColor = resources.getColor(R.color.colorAccent, null)
                            }
                        }else{ // 今天之后，未完成
                            c.apply {
                                scheme = "NotFinish"
                                schemeColor = 0xFFAAAAAA.toInt()
                            }
                        }
                    }
                    calendarView.addSchemeDate(c)
                }
            }
        }
    }

    // 修改打卡心得
    private fun editFinishCase() {
        val selectedDate = getYMDCalendarDate(calendarView.selectedCalendar)

        if(dailyItem.finishedTaskOfDate(selectedDate)){
            daily_finish_case_save_btn.toggleVisibility(this) { true }
            daily_finish_case_remark.apply {
                isEnabled = true
                requestFocus()
                showSoftInputKeyboard(daily_finish_case_remark)
            }
        }else{
            showToast("请先完成今日日常")
        }
    }
    private fun saveFinishCase() {
        daily_finish_case_save_btn.toggleVisibility(this){ false }
        daily_finish_case_remark.isEnabled = false

        val calendar = calendarView.selectedCalendar
        dailyItem.finishCases.find {
            it.year == calendar.year && it.month == calendar.month && it.day == calendar.day
        }?.let {
            val idx = dailyItem.finishCases.indexOf(it)
            // printLog("第$idx 个打卡心得")
            dailyItem.finishCases[idx].remark = daily_finish_case_remark.text.toString()
            dailyItem.updateDailyItem(this) {
                refreshDailyItem()
            }
        }
    }

    // 添加完成心得
    private fun addFinishCase(item: DailyItem) {
        val et = EditText(this).apply {
            requestFocus()
        }
        val linearLayout = FrameLayout(this).apply {
            setPadding(dp2pxSize(this@DailyFinishCaseActivity, 20f), dp2pxSize(this@DailyFinishCaseActivity, 8f),
                dp2pxSize(this@DailyFinishCaseActivity, 20f), 0)
            addView(et)
        }
        val today = Date()
        var remark = ""
        MaterialAlertDialogBuilder(this, R.style.CustomDialog)
            .setTitle("添加心得")
            .setView(linearLayout)
            .setPositiveButton(android.R.string.ok){ _, _ ->
                remark = et.text.toString()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener {
                hideSoftInputKeyboard(et)
                item.add("finishCases", DailyItem.FinishCase(today.queryYear(),
                    today.queryMonth(), today.queryDay(), remark))
                item.updateDailyItem(this) {
                    refreshDailyItem()
                }
            }
            .show()
    }
}