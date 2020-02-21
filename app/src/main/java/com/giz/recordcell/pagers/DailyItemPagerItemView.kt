package com.giz.recordcell.pagers

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.giz.android.toolkit.dp2pxSize
import com.giz.recordcell.R
import com.giz.recordcell.activities.DailyInCalendarActivity
import com.giz.recordcell.activities.NewDailyItemActivity
import com.giz.recordcell.bmob.*
import com.giz.recordcell.helpers.*
import com.giz.recordcell.widgets.CircularBottomSheetDialog
import com.giz.recordcell.widgets.ShadeImageButton
import com.giz.recordcell.widgets.WaitProgressDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

class DailyItemPagerItemView(private val context: Context,
                             private val user: RecordUser) : BaseRecyclerViewPagerItemView(context){

    private var dailyItemAdapter: CommonMultiAdapter<Boolean, Pair<DailyItem, Boolean>>? = null

    private var onDailyItemClickListener: (DailyItem) -> Unit = {}

    init {
        val headerView = LayoutInflater.from(context).inflate(R.layout.pager_header_daily_item, null)
        headerView.apply {
            findViewById<ImageView>(R.id.pager_header_daily_in_calendar).setOnClickListener {
                context.startActivity(Intent(context, DailyInCalendarActivity::class.java))
            }
            findViewById<ImageView>(R.id.pager_header_daily_show_all).setOnClickListener {
                showAllDailyItems()
            }
        }.also { addHeaderView(it) }
    }

    fun setOnDailyItemClickListener(listener: (DailyItem) -> Unit) {
        onDailyItemClickListener = listener
    }

    override fun updatePagerRecyclerView() {
        mSwipeRefreshLayout.isRefreshing = true

        val bmobQuery = BmobQuery<DailyItem>()
        bmobQuery.addWhereEqualTo("user", user)
        bmobQuery.include("user")
        bmobQuery.findObjects(object : FindListener<DailyItem>() {
            override fun done(p0: MutableList<DailyItem>?, p1: BmobException?) {
                mSwipeRefreshLayout.isRefreshing = false
                if(p1 == null){
                    p0 ?: return
                    updateAdapter(p0)
                }else{
                    if(p1.errorCode == 9016){
                        context.showToast("查询日常项错误，请检查网络")
                    }else{
                        context.showToast("查询日常项错误")
                    }
                    context.printLog("查询日常项错误：$p1")
                }
            }
        })
    }

    private fun updateAdapter(list: MutableList<DailyItem>) {
        val taskGroup = list.filter { it.isTodayTask() }.groupBy { it.finishedTodayTask() }
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

        if(dailyItemAdapter == null){
            dailyItemAdapter = object : CommonMultiAdapter<Boolean, Pair<DailyItem, Boolean>>(context, itemPairs,
                R.layout.item_daily_header, R.layout.item_daily_item) {
                override fun bindItemData(holder: CommonMultiViewHolder, data: Pair<DailyItem, Boolean>, pos: Int) {
                    val dailyItem = data.first
                    val finished = data.second
                    with(holder.itemView) {
                        findViewById<TextView>(R.id.daily_item_name).text = dailyItem.name
                        findViewById<ShadeImageButton>(R.id.daily_item_finish_btn).setOnClickListener {
                            MaterialAlertDialogBuilder(context, R.style.CustomDialog)
                                .setTitle("确定已完成今日[${dailyItem.name}]之事吗？")
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    addFinishCase(dailyItem)
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                        }
                        findViewById<ShadeImageButton>(R.id.daily_item_finish_btn)
                            .isEnabled = !finished
                        findViewById<TextView>(R.id.daily_item_name).setTextColor(
                            if(finished) Color.GRAY else Color.BLACK
                        )
                        setOnTouchListener(OnPressScaleChangeTouchListener())
                        setOnClickListener { onDailyItemClickListener(dailyItem) }
                        setOnLongClickListener {
                            showPopupMenu(dailyItem, this)
                            true
                        }
                    }
                }

                override fun bindHeaderData(holder: CommonMultiHeadHolder, data: Boolean, pos: Int) {
                    holder.itemView.findViewById<TextView>(R.id.daily_header_tv).text =
                        if(data){ "今日已完成" } else {"今日未完成"}
                }
            }
            mRecyclerView.adapter = dailyItemAdapter
        }else{
            dailyItemAdapter?.updateData(itemPairs)
        }
    }

    // 添加完成心得
    private fun addFinishCase(item: DailyItem) {
        val et = EditText(context).apply {
            requestFocus()
        }
        val linearLayout = FrameLayout(context).apply {
            setPadding(dp2pxSize(context, 20f), dp2pxSize(context, 8f), dp2pxSize(context, 20f), 0)
            addView(et)
        }
        val today = Date()
        var remark = ""
        MaterialAlertDialogBuilder(context, R.style.CustomDialog)
            .setTitle("添加心得")
            .setView(linearLayout)
            .setPositiveButton(android.R.string.ok){ _, _ ->
                remark = et.text.toString()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener {
                item.add("finishCases", DailyItem.FinishCase(today.queryYear(),
                    today.queryMonth(), today.queryDay(), remark))
                item.updateDailyItem(context) {
                    updatePagerRecyclerView()
                }
            }
            .show()
    }

    // 长按待办事项弹出菜单
    @Suppress("UNCHECKED_CAST")
    private fun showPopupMenu(item: DailyItem, anchorView: View) {
        val popupMenu = PopupMenu(context, anchorView).apply {
            menuInflater.inflate(R.menu.menu_daily_item, menu) // 填充菜单
            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.menu_daily_item_delete -> {
                        MaterialAlertDialogBuilder(context, R.style.CustomDialog)
                            .setTitle("确定删除日常[${item.name}]吗？")
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok){_, _ ->
                                item.deleteDailyItem(context){ updatePagerRecyclerView() }
                            }
                            .show()
                    }
                    R.id.menu_daily_item_modify -> {
                        context.startActivity(NewDailyItemActivity.newIntent(context, NewDailyItemActivity.DAILY_MODE_SEE,
                            item))
                    }
                }
                true
            }
        }
        // 显示图标
        popupMenu.apply {
            val field = this::class.declaredMemberProperties.find { it.name == "mPopup" } as? KProperty1<PopupMenu, *>
            field?.apply {
                isAccessible = true
                val helper = get(popupMenu) as MenuPopupHelper
                helper.setForceShowIcon(true)
            }
        }.show()
    }

    // 显示所有日常
    private fun showAllDailyItems() {
        val waitProgressDialog = WaitProgressDialog(context).apply { show() }
        val dailyQuery = BmobQuery<DailyItem>()
        dailyQuery.addWhereEqualTo("user", user)
        dailyQuery.include("user")
        dailyQuery.findObjects(object : FindListener<DailyItem>() {
            override fun done(p0: MutableList<DailyItem>?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null){
                    p0 ?: return
                    val dialog = CircularBottomSheetDialog(context)
                    val titleTv = TextView(context).apply {
                        val dp16 = dp2pxSize(context, 16f)
                        setMarginsWithWrapContent(dp16, 0, dp16, dp16 / 2)
                        text = "全部日常"
                        setTextColor(Color.BLACK)
                        textSize = 16f
                    }
                    RecyclerView(context).apply {
                        layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                        adapter = object : CommonAdapter<DailyItem>(context, p0, R.layout.item_daily_item) {
                            override fun bindData(holder: CommonViewHolder, data: DailyItem, position: Int) {
                                with(holder.itemView){
                                    findViewById<CardView>(R.id.daily_item_cv).setCardBackgroundColor(0xFFF1F2F3.toInt())
                                    findViewById<ShadeImageButton>(R.id.daily_item_finish_btn).visibility = View.INVISIBLE
                                    findViewById<TextView>(R.id.daily_item_name).text = data.name
                                    setOnTouchListener(OnPressScaleChangeTouchListener())
                                    setOnClickListener {
                                        dialog.dismiss()
                                        context.startActivity(NewDailyItemActivity.newIntent(context,
                                            NewDailyItemActivity.DAILY_MODE_SEE, data))
                                    }
                                }
                            }
                        }
                    }.also {
                        val ll = LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER_HORIZONTAL
                            addView(titleTv)
                            addView(it)
                        }
                        dialog.apply {
                            setContentViewWithTopIndicator(ll)
                        }.show()
                    }
                }else{
                    context.showToast("查询失败")
                    context.printLog("查询失败：$p1")
                }
            }
        })
    }

}