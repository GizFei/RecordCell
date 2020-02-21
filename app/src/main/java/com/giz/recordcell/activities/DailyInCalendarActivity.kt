package com.giz.recordcell.activities

import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.BmobUser
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.giz.android.toolkit.dp2pxSize
import com.giz.recordcell.R
import com.giz.recordcell.bmob.*
import com.giz.recordcell.helpers.*
import com.giz.recordcell.widgets.ShadeImageButton
import com.giz.recordcell.widgets.WaitProgressDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView
import kotlinx.android.synthetic.main.activity_daily_in_calendar.*
import java.util.*

class DailyInCalendarActivity : AppCompatActivity() {

    private val currentUser: RecordUser by lazy { BmobUser.getCurrentUser(RecordUser::class.java) }

    private val itemList =  mutableListOf<DailyItem>()
    private var dailyAdapter: CommonMultiAdapter<Boolean, Pair<DailyItem, Boolean>>? = null

    private lateinit var mCalendarView: CalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_in_calendar)

        mCalendarView = findViewById(R.id.calendarView)
        mCalendarView.setOnCalendarSelectListener(object : CalendarView.OnCalendarSelectListener{
            override fun onCalendarSelect(calendar: Calendar?, isClick: Boolean) {
                calendar ?: return
                updateRecyclerView(calendar)
            }
            override fun onCalendarOutOfRange(calendar: Calendar?) {}
        })
        mCalendarView.setOnMonthChangeListener { year, month ->
            printLog("月份改变：$year,$month")
            addSchemes()
            setDateTv(year, month)
        }
        mCalendarView.post {
            setDateTv(mCalendarView.curYear, mCalendarView.curMonth)
        }
    }

    override fun onResume() {
        super.onResume()
        queryDailyItems()
    }

    private fun setDateTv(year: Int, month: Int) {
        daily_in_calendar_date_tv.text = "${year}年${month}月"
    }

    private fun queryDailyItems() {
        itemList.clear()
        val waitProgressDialog = WaitProgressDialog(this).apply { show() }
        val bmobQuery = BmobQuery<DailyItem>()
        bmobQuery.addWhereEqualTo("user", currentUser)
        bmobQuery.include("user")
        bmobQuery.findObjects(object : FindListener<DailyItem>() {
            override fun done(p0: MutableList<DailyItem>?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null){
                    p0 ?: return
                    itemList.addAll(p0)
                    updateRecyclerView(mCalendarView.selectedCalendar)
                    addSchemes()
                }else{
                    if(p1.errorCode == 9016){
                        showToast("查询日常项错误，请检查网络")
                    }else{
                        showToast("查询日常项错误")
                    }
                    printLog("查询日常项错误：$p1")
                }
            }
        })
    }

    private fun updateRecyclerView(calendar: Calendar) {
        printLog("updateRecyclerView:日期：$calendar, ${mCalendarView.selectedCalendar.isCurrentDay}")
        val calDate = getYMDCalendarDate(calendar.year, calendar.month, calendar.day)
        val taskGroup = itemList.filter { it.isTaskOfDate(calDate) }.groupBy { it.finishedTaskOfDate(calDate) }
        // 未完成的在前，完成的在后
        val itemPairs = mutableListOf<CommonMultiAdapter.MultiData>().apply {
            if(false in taskGroup.keys){
                add(CommonMultiAdapter.MultiData(false, CommonMultiAdapter.ViewType.HEADER))
            }
            taskGroup[false]?.map { CommonMultiAdapter.MultiData(Pair(it, false), CommonMultiAdapter.ViewType.ITEM) }?.
                also { this.addAll(it.toMutableList()) }
            if(true in taskGroup.keys){
                add(CommonMultiAdapter.MultiData(true, CommonMultiAdapter.ViewType.HEADER))
            }
            taskGroup[true]?.map { CommonMultiAdapter.MultiData(Pair(it, true), CommonMultiAdapter.ViewType.ITEM) }?.
                also { this.addAll(it.toMutableList()) }
        }
        if(dailyAdapter == null){
            dailyAdapter = object : CommonMultiAdapter<Boolean, Pair<DailyItem, Boolean>>(this, itemPairs,
                R.layout.item_daily_header, R.layout.item_daily_item){

                override fun bindHeaderData(holder: CommonMultiHeadHolder, data: Boolean, pos: Int) {
                    holder.itemView.findViewById<TextView>(R.id.daily_header_tv).text =
                        if(data){ "今日已完成" } else {"今日未完成"}
                }

                override fun bindItemData(holder: CommonMultiViewHolder, data: Pair<DailyItem, Boolean>, pos: Int) {
                    val dailyItem = data.first
                    val finished = data.second
                    with(holder.itemView) {
                        findViewById<CardView>(R.id.daily_item_cv).setCardBackgroundColor(0xFFF1F2F3.toInt())
                        findViewById<TextView>(R.id.daily_item_name).text = dailyItem.name
                        if(mCalendarView.selectedCalendar.isCurrentDay){
                            findViewById<ShadeImageButton>(R.id.daily_item_finish_btn).apply {
                                isEnabled = !finished
                                setOnClickListener {
                                    MaterialAlertDialogBuilder(context, R.style.CustomDialog)
                                        .setTitle("确定已完成今日[${dailyItem.name}]之事吗？")
                                        .setPositiveButton(android.R.string.ok) { _, _ ->
                                            addFinishCase(dailyItem)
                                        }
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .show()
                                }
                            }
                            findViewById<TextView>(R.id.daily_item_name).setTextColor(
                                if(finished) Color.GRAY else Color.BLACK
                            )
                        }else{
                            findViewById<ShadeImageButton>(R.id.daily_item_finish_btn).apply {
                                isEnabled = !finished
                                setOnClickListener {
                                    val today = getYMDCalendarDate(Date())
                                    if(getYMDCalendarDate(mCalendarView.selectedCalendar).before(today)){
                                        showToast("逝者如斯夫，往前看~")
                                    }else{
                                        showToast("明天尚未到来，专注当下~")
                                    }
                                }
                            }
                            findViewById<TextView>(R.id.daily_item_name).setTextColor(Color.BLACK)
                        }
                        setOnTouchListener(OnPressScaleChangeTouchListener())
                        setOnClickListener {
                            startActivity(NewDailyItemActivity.newIntent(this@DailyInCalendarActivity,
                                NewDailyItemActivity.DAILY_MODE_SEE, dailyItem))
                        }
                    }
                }
            }
            recyclerView.adapter = dailyAdapter
        }else{
            dailyAdapter?.updateData(itemPairs)
        }
    }

    private fun addSchemes() {
        val colorArray = arrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.GRAY, Color.BLACK)
        printLog("当前月份：${mCalendarView.curMonth}")
        mCalendarView.currentMonthCalendars?.forEach { c ->
            if(c.isCurrentMonth) {
                // printLog("添加本月计划：$c")
//                itemList.find { it.isTaskOfDate(getYMDCalendarDate(c.year, c.month, c.day)) }.let {
                val tmpSchemes = mutableListOf<Calendar.Scheme>()
                itemList.forEachIndexed {idx, it ->
                    if(it.isTaskOfDate(getYMDCalendarDate(c.year, c.month, c.day))){
                        tmpSchemes.add(Calendar.Scheme(colorArray[idx], it.name))
                    }
                    if(tmpSchemes.size > 5){ // 最多五个
                        return@forEachIndexed
                    }
                }
                c.schemes = tmpSchemes
                mCalendarView.addSchemeDate(c)
            }
        }
    }

    private fun addFinishCase(item: DailyItem) {
        val et = EditText(this).apply {
            requestFocus()
        }
        val linearLayout = FrameLayout(this).apply {
            setPadding(dp2pxSize(this@DailyInCalendarActivity, 20f), dp2pxSize(this@DailyInCalendarActivity, 8f),
                dp2pxSize(this@DailyInCalendarActivity, 20f), 0)
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
                item.add("finishCases", DailyItem.FinishCase(today.queryYear(),
                    today.queryMonth(), today.queryDay(), remark))
                item.updateDailyItem(this) {
                    queryDailyItems()
                }
            }
            .show()
    }
}