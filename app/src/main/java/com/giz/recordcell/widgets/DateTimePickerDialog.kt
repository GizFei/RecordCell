package com.giz.recordcell.widgets

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.viewpager.widget.PagerAdapter
import com.giz.recordcell.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import java.util.*

class DateTimePickerDialog(context: Context, date: Date, onDateOkListener: (Date) -> Unit = {})
    : MaterialAlertDialogBuilder(context, R.style.CustomDialog) {

    private val datePicker = DatePicker(context)
    private val timePicker = TimePicker(context)

    init {
        // 设置时间
        val calendar = Calendar.getInstance().apply { time = date }
        datePicker.init(
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        , null)
        datePicker.minDate = date.time // 不能选取当前时间之前的日期

        timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
        timePicker.minute = calendar.get(Calendar.MINUTE)
        timePicker.setIs24HourView(true)
    }

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_date_time_picker, null)
        val viewPager = view.findViewById<AutoHeightViewPager>(R.id.dialog_datetime_picker_vp)
        viewPager?.adapter = DateTimePickerVpAdapter()
        view.findViewById<TabLayout>(R.id.dialog_datetime_picker_tabs)?.setupWithViewPager(viewPager)

        setTitle("设置提醒时间")
        setView(view)
        setPositiveButton(android.R.string.ok){_, _ -> onDateOkListener(getCurrentDateTime()) }
        setNegativeButton(android.R.string.cancel, null)
    }

    private fun getCurrentDateTime(): Date = Calendar.getInstance().apply {
        set(datePicker.year, datePicker.month, datePicker.dayOfMonth, timePicker.hour, timePicker.minute, 0)
    }.time

    private inner class DateTimePickerVpAdapter: PagerAdapter() {

        override fun getCount(): Int = 2

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val view: View
            if(position == 0){
                // 日期选择器
                view = datePicker
            }else{
                // 时间选择器
                view = timePicker
            }
            container.addView(view)

            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`

        override fun getPageTitle(position: Int): CharSequence? = if(position == 0){ "日期" }else{ "时间" }
    }

}