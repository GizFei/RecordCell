package com.giz.recordcell.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.DatePicker
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.forEachIndexed
import androidx.recyclerview.widget.RecyclerView
import cn.bmob.v3.BmobUser
import cn.bmob.v3.datatype.BmobDate
import com.giz.android.toolkit.dp2pxSize
import com.giz.recordcell.R
import com.giz.recordcell.bmob.*
import com.giz.recordcell.data.formatCalendarText
import com.giz.recordcell.helpers.*
import com.giz.recordcell.widgets.CircularBottomSheetDialog
import com.giz.recordcell.widgets.ShadeTextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_new_daily_item.*
import java.util.*

class NewDailyItemActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val DAILY_MODE_CREATE = 0
        const val DAILY_MODE_MODIFY = 1
        const val DAILY_MODE_SEE = 2

        private const val EXTRA_DAILY_MODE = "DailyMode"
        private const val EXTRA_DAILY_ITEM = "DailyItem"

        fun newIntent(context: Context, mode: Int, dailyItem: DailyItem? = null) = Intent(context, NewDailyItemActivity::class.java)
            .apply {
                putExtra(EXTRA_DAILY_MODE, mode)
                putExtra(EXTRA_DAILY_ITEM, dailyItem)
            }
    }

    // 与日常事项有关的变量
    private var startTime = Date()
    private var endTime: Date? = null
    private var repeatType: Int = DailyItem.REPEAT_TYPE_DAY
    private var repeatDays = mutableListOf<Int>()

    private var dailyMode: Int = DAILY_MODE_CREATE
    private var dailyItem: DailyItem? = null
    private val currentUser: RecordUser by lazy { BmobUser.getCurrentUser(RecordUser::class.java) }

    private val noEndTimeText by lazy { resources.getString(R.string.new_daily_no_end_time_text) }
    private val repeatEverydayText by lazy { resources.getString(R.string.new_daily_repeat_everyday_text) }
    private val editModeTitle by lazy { resources.getString(R.string.new_daily_activity_name) }
    private val seeModeTitle by lazy { resources.getString(R.string.new_daily_activity_name_see) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_daily_item)

        // 获取模式，日常事项
        dailyMode = intent.getIntExtra(EXTRA_DAILY_MODE, DAILY_MODE_CREATE)
        dailyItem = intent.getSerializableExtra(EXTRA_DAILY_ITEM) as? DailyItem
        switchMode()

        setupListeners()
    }

    private fun setupListeners() {
        new_daily_start_time_tv.enableOnPressScaleTouchListener(0.8f, 200L, this)
        new_daily_end_time_tv.enableOnPressScaleTouchListener(0.8f, 200L, this)
        new_daily_repeat_time_tv.enableOnPressScaleTouchListener(0.8f, 200L, this)

        bindBackPressedIcon(new_daily_back_icon)
        new_daily_save_btn.setOnClickListener(this)
        new_daily_edit_btn.setOnClickListener(this)
        new_daily_calendar_btn.setOnClickListener(this)
        new_daily_delete_btn.setOnClickListener(this)
        new_daily_end_time_delete_btn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.new_daily_start_time_tv -> { setStartTime() }
            R.id.new_daily_end_time_tv -> { setEndTime() }
            R.id.new_daily_repeat_time_tv -> { setRepeatTime() }
            R.id.new_daily_save_btn -> {
                if(dailyMode == DAILY_MODE_CREATE){
                    saveDailyItem()
                }else if(dailyMode == DAILY_MODE_MODIFY) {
                    updateDailyItem()
                }
            }
            R.id.new_daily_edit_btn -> {
                if(dailyMode == DAILY_MODE_SEE) {
                    dailyMode = DAILY_MODE_MODIFY
                    switchMode()
                }
            }
            R.id.new_daily_calendar_btn -> {
                startActivity(DailyFinishCaseActivity.newIntent(this, dailyItem!!))
            }
            R.id.new_daily_delete_btn -> {
                MaterialAlertDialogBuilder(this, R.style.CustomDialog)
                    .setTitle("确定删除日常事项吗？")
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok){_, _ ->
                        dailyItem?.deleteDailyItem(this){ finish() }
                    }
                    .show()
            }
            R.id.new_daily_end_time_delete_btn -> {
                updateEndTime(null)
            }
        }
    }

    private fun switchMode() {
        when(dailyMode) {
            DAILY_MODE_CREATE, DAILY_MODE_MODIFY -> { // 新建模式，修改模式
                new_daily_activity_title.text = editModeTitle
                new_daily_save_btn.visibility = View.VISIBLE
                new_daily_action_btn_container.toggleVisibility(this){false}
                switchViewMode(true)

                if(dailyMode == DAILY_MODE_CREATE){
                    updateStartTime(startTime)
                }
                if(dailyMode == DAILY_MODE_MODIFY) {
                    fillViewContentFromDaily()
                }
                new_daily_item_name.requestFocus()
            }
            DAILY_MODE_SEE -> {
                new_daily_activity_title.text = seeModeTitle
                new_daily_save_btn.visibility = View.GONE
                switchViewMode(false)

                new_daily_item_name.setTextColor(Color.BLACK)
                new_daily_description.setTextColor(Color.BLACK)
                new_daily_action_btn_container.toggleVisibility(this){true}
                fillViewContentFromDaily()
            }
        }
    }

    private fun switchViewMode(enable: Boolean) {
        new_daily_item_name.isEnabled = enable
        new_daily_description.isEnabled = enable
        new_daily_start_time_tv.isEnabled = enable
        new_daily_end_time_tv.isEnabled = enable
        new_daily_repeat_time_tv.isEnabled = enable
    }

    // 填充内容
    private fun fillViewContentFromDaily() {
        new_daily_item_name.setText(dailyItem?.name)
        new_daily_description.setText(dailyItem?.desc)
        updateStartTime(dailyItem!!.startTime.fetchDate())
        updateEndTime(dailyItem?.endTime?.fetchDate())
        updateRepeatTime(dailyItem!!.repeatType, dailyItem!!.repeatDays)
    }

    // 更新开始时间
    private fun updateStartTime(date: Date) {
        startTime = date
        new_daily_start_time_tv.text = formatCalendarText(startTime)
    }
    // 更新结束时间
    private fun updateEndTime(date: Date?) {
        endTime = date
        new_daily_end_time_tv.text = if(endTime == null){
            noEndTimeText
        }else{
            formatCalendarText(endTime!!)
        }
        if(dailyMode != DAILY_MODE_SEE){
            new_daily_end_time_delete_btn.toggleVisibility(this){endTime != null}
        }
    }
    // 更新重复间隔
    private fun updateRepeatTime(type: Int, days: MutableList<Int>) {
        repeatType = type
        repeatDays = days
        when(repeatType) {
            DailyItem.REPEAT_TYPE_DAY -> {
                new_daily_repeat_time_tv.text = repeatEverydayText
            }
            DailyItem.REPEAT_TYPE_WEEK -> {
                var repeatStr = ""
                days.forEach { repeatStr += "${DailyItem.translateWeekNum(it)}，" }
                repeatStr = repeatStr.trim('，')
                new_daily_repeat_time_tv.text = repeatStr
            }
            DailyItem.REPEAT_TYPE_MONTH -> {
                var repeatStr = "每月"
                days.forEach { repeatStr += "$it," }
                repeatStr = repeatStr.trim(',') + "号"
                new_daily_repeat_time_tv.text = repeatStr
            }
        }
    }

    private fun setStartTime() {
        val datePicker = DatePicker(this).apply {
            minDate = Date().time
            updateDate(startTime.queryYear(), startTime.queryMonth() - 1, startTime.queryDay())
        }
        MaterialAlertDialogBuilder(this, R.style.CustomDialog)
            .setTitle("设置起始时间")
            .setView(datePicker)
            .setPositiveButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok){_, _ ->
                updateStartTime(Calendar.getInstance().apply {
                    set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
                }.time)
            }.show()
    }
    private fun setEndTime() {
        val datePicker = DatePicker(this).apply {
            minDate = startTime.time
            if(endTime != null){
                updateDate(endTime!!.queryYear(), endTime!!.queryMonth() - 1, endTime!!.queryDay())
            }
        }
        MaterialAlertDialogBuilder(this, R.style.CustomDialog)
            .setTitle("设置结束时间")
            .setView(datePicker)
            .setPositiveButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok){_, _ ->
                updateEndTime(Calendar.getInstance().apply {
                    set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
                }.time)
            }.show()
    }

    private fun setRepeatTime() {
        var type: Int = repeatType
        val days = repeatDays
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_select_repeat_time, null).apply {
            val container = findViewById<FrameLayout>(R.id.select_repeat_time_container)
            val typeContainer = findViewById<LinearLayout>(R.id.select_repeat_time_type_container)
            val weekNums = findViewById<LinearLayout>(R.id.select_repeat_time_week_nums)
            // 每周
            findViewById<ShadeTextView>(R.id.select_repeat_time_week_btn).apply {
                enableOnPressScaleTouchListener(minScale = 0.84f){
                    if(type != DailyItem.REPEAT_TYPE_WEEK){
                        days.clear()
                    }
                    type = DailyItem.REPEAT_TYPE_WEEK
                    typeContainer.animate().alpha(0f).setListener(object : AnimatorListenerAdapter(){
                        override fun onAnimationEnd(animation: Animator?) {
                            typeContainer.visibility = View.GONE
                        }
                    }).start()
                    weekNums.visibility = View.VISIBLE
                    ViewAnimationUtils.createCircularReveal(weekNums, (this.left + this.right)/2,
                        (this.bottom + this.top)/2, this.width / 2f, container.width.toFloat()).start()
                }
            }
            weekNums.forEachIndexed {idx, v ->
                if(repeatType == DailyItem.REPEAT_TYPE_WEEK){
                    v.isSelected = (idx + 1) in days
                }
                v.setOnClickListener {day ->
                    day.isSelected = !day.isSelected
                    if(day.isSelected) { days.add(idx + 1) } else days.remove(idx + 1)
                }
            }
            // 每月
            val monthNumRv = findViewById<RecyclerView>(R.id.select_repeat_time_month_num_rv).apply {
                val numList = IntRange(1, 31).toMutableList()
                adapter = object : CommonAdapter<Int>(this@NewDailyItemActivity, numList, R.layout.item_repeat_month_day){
                    override fun bindData(holder: CommonViewHolder, data: Int, position: Int) {
                        with(holder.itemView.findViewById<TextView>(R.id.item_text)){
                            if(repeatType == DailyItem.REPEAT_TYPE_MONTH){
                                isSelected = data in days
                            }
                            text = data.toString()
                            setOnClickListener {
                                this.isSelected = !this.isSelected
                                if(this.isSelected) { days.add(data) } else days.remove(data)
                            }
                        }
                    }
                }
            }
            findViewById<ShadeTextView>(R.id.select_repeat_time_month_btn).apply {
                enableOnPressScaleTouchListener(minScale = 0.84f) {
                    if(type != DailyItem.REPEAT_TYPE_MONTH){
                        days.clear()
                    }
                    type = DailyItem.REPEAT_TYPE_MONTH
                    ValueAnimator.ofInt(container.height, dp2pxSize(this@NewDailyItemActivity, 260f)).also {
                        it.addUpdateListener {
                            container.layoutParams = container.layoutParams.also { l -> l.height = it.animatedValue as Int }
                        }
                        it.doOnEnd {
                            monthNumRv.visibility = View.VISIBLE
                            ViewAnimationUtils.createCircularReveal(monthNumRv, (this.left + this.right)/2,
                                (this.bottom + this.top)/2, this.width / 2f, container.width.toFloat()).start()
                        }
                    }.start()
                    typeContainer.animate().alpha(0f).setListener(object : AnimatorListenerAdapter(){
                        override fun onAnimationEnd(animation: Animator?) {
                            typeContainer.visibility = View.GONE
                        }
                    }).start()
                }
            }
        }
        CircularBottomSheetDialog(this).apply {
            setContentView(view)
            findViewById<TextView>(R.id.select_repeat_time_save_btn)?.setOnClickListener {
                if(type == -1){
                    showToast("请选择一个重复间隔")
                }else{
                    if(days.isEmpty()){
                        showToast("至少选择一天")
                    }else{
                        days.sort() // 排序
                        updateRepeatTime(type, days)
                        dismiss()
                    }
                }
            }
            // 每天
            findViewById<ShadeTextView>(R.id.select_repeat_time_everyday_btn)?.enableOnPressScaleTouchListener(minScale = 0.84f) {
                updateRepeatTime(DailyItem.REPEAT_TYPE_DAY, days)
                dismiss()
            }
        }.show()
    }

    private fun saveDailyItem() {
        if(new_daily_item_name.text.isEmpty()){
            showToast("日常事项名称不能为空")
            return
        }
        hideSoftInputKeyboard(new_daily_item_name)
        val newDailyItem = DailyItem(
            currentUser,
            new_daily_item_name.text.toString(),
            new_daily_description.text.toString(),
            BmobDate(startTime),
            if(endTime == null) null else BmobDate(endTime),
            repeatType,
            repeatDays,
            mutableListOf()
        )
        newDailyItem.saveDailyItem(this){
            finish()
        }
    }

    private fun updateDailyItem() {
        if(dailyItem == null){
            showToast("更新失败，dailyItem为空")
            return
        }
        if(new_daily_item_name.text.isEmpty()){
            showToast("日常事项名称不能为空")
            return
        }
        hideSoftInputKeyboard(new_daily_item_name)
        dailyItem!!.apply {
            name = new_daily_item_name.text.toString()
            desc = new_daily_description.text.toString()
            startTime = BmobDate(this@NewDailyItemActivity.startTime)
            endTime = if(this@NewDailyItemActivity.endTime == null) null else BmobDate(this@NewDailyItemActivity.endTime)
            repeatType = this@NewDailyItemActivity.repeatType
            repeatDays = this@NewDailyItemActivity.repeatDays
        }
        // 处理完成情况
        val start = getYMDCalendarDate(startTime.queryYear(), startTime.queryMonth(), startTime.queryDay())
        val end = if(endTime == null) null else getYMDCalendarDate(endTime!!.queryYear(), endTime!!.queryMonth(), endTime!!.queryDay())
        val removeCases = mutableListOf<DailyItem.FinishCase>()
        dailyItem!!.finishCases.forEach {
            val d = getYMDCalendarDate(it.year, it.month, it.day)
            if(d.before(start) || d.after(end)){
                removeCases.add(it)
            }
        }
        dailyItem!!.apply {
            removeAll("finishCases", removeCases)
        }.updateDailyItem(this){ finish() }
    }

    override fun onBackPressed() {
        if(dailyMode != DAILY_MODE_SEE) {
            // 退出时询问是否保存
            MaterialAlertDialogBuilder(this, R.style.CustomDialog)
                .setTitle("确定不保存日常事项吗？")
                .setNegativeButton("不保存"){ _, _ ->
                    finish()
                }.setPositiveButton("继续编辑", null)
                .show()
        }else{
            finish()
        }
    }
}