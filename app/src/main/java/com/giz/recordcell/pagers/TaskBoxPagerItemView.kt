package com.giz.recordcell.pagers

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.giz.android.toolkit.dp2pxSize
import com.giz.recordcell.R
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.bmob.TaskBox
import com.giz.recordcell.data.NewTaskBoxDialog
import com.giz.recordcell.helpers.CommonAdapter
import com.giz.recordcell.helpers.OnPressScaleChangeTouchListener
import com.giz.recordcell.helpers.printLog
import com.giz.recordcell.helpers.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

class TaskBoxPagerItemView(private val context: Context,
                           private val user: RecordUser) : BaseRecyclerViewPagerItemView(context) {

    companion object {
        // 背景图数组
        val taskBoxBgImgMap = intArrayOf(
            R.raw.undraw_slider,     R.raw.undraw_books,        R.raw.undraw_moonlight,
            R.raw.undraw_cup_of_tea, R.raw.undraw_xmas_snowman, R.raw.undraw_breakfast_psiw,
            R.raw.undraw_new_decade, R.raw.undraw_santa_visit,  R.raw.undraw_decorative_friends
        )
    }

    private var taskBoxAdapter: CommonAdapter<TaskBox>? = null

    private var onTaskBoxItemClickListener: (TaskBox) -> Unit = {}
    private var onTaskBoxDeleteListener: () -> Unit = {}

    fun setOnTaskBoxItemClickListener(listener: (TaskBox) -> Unit){
        onTaskBoxItemClickListener = listener
    }

    fun setonTaskBoxDeleteListener(listener: () -> Unit){
        onTaskBoxDeleteListener = listener
    }

    override fun updatePagerRecyclerView() {
        mSwipeRefreshLayout.isRefreshing = true

        val bmobQuery = BmobQuery<TaskBox>()
        bmobQuery.addWhereEqualTo("user", user)
        bmobQuery.include("user")
        bmobQuery.findObjects(object : FindListener<TaskBox>() {
            override fun done(p0: MutableList<TaskBox>?, p1: BmobException?) {
                mSwipeRefreshLayout.isRefreshing = false
                if(p1 == null){
                    p0 ?: return
                    updateAdapter(p0)
                }else{
                    if(p1.errorCode == 9016){
                        context.showToast("查询任务集错误，请检查网络")
                    }else{
                        context.showToast("查询任务集错误")
                    }
                    context.printLog("查询任务集错误：$p1")
                }
            }
        })
    }

    private fun updateAdapter(list: MutableList<TaskBox>) {
        if(taskBoxAdapter == null){
            taskBoxAdapter = object : CommonAdapter<TaskBox>(context, list, R.layout.item_taskbox){
                override fun bindData(holder: CommonViewHolder, data: TaskBox, position: Int) {
                    with(holder.itemView) {
                        layoutParams = LinearLayout.LayoutParams(layoutParams.width, layoutParams.height).apply {
                            setMargins(dp2pxSize(context, 16f), dp2pxSize(context, 8f),
                                dp2pxSize(context, 16f), dp2pxSize(context, 8f))
                        }
                        findViewById<ConstraintLayout>(R.id.item_taskbox_container).setBackgroundColor(
                            Color.WHITE)
                        findViewById<ImageView>(R.id.item_taskbox_icon).setImageResource(taskBoxBgImgMap[data.bgCode])
                        findViewById<TextView>(R.id.item_taskbox_title).text = data.title
                        findViewById<TextView>(R.id.item_taskbox_intro).text = data.introduction
                        setOnTouchListener(OnPressScaleChangeTouchListener(duration = 200L))
                        setOnClickListener{
                            onTaskBoxItemClickListener(data)
                        }
                        setOnLongClickListener {
                            showPopupMenu(data, it)
                            true
                        }
                    }
                }
            }
            mRecyclerView.adapter = taskBoxAdapter
        }else{
            taskBoxAdapter?.updateData(list)
        }
    }

    // 长按待办事项弹出菜单
    @Suppress("UNCHECKED_CAST")
    private fun showPopupMenu(taskBox: TaskBox, anchorView: View) {
        val popupMenu = PopupMenu(context, anchorView).apply {
            menuInflater.inflate(R.menu.menu_taskbox_item, menu) // 填充菜单
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_taskbox_delete -> {
                        MaterialAlertDialogBuilder(context, R.style.CustomDialog)
                            .setTitle("确定删除任务集[${taskBox.title}]吗？")
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                taskBox.deleteTaskBox(context){
                                    updatePagerRecyclerView()
                                    onTaskBoxDeleteListener()
                                }
                            }
                            .show()
                    }
                    R.id.menu_taskbox_modify -> {
                        NewTaskBoxDialog(context, NewTaskBoxDialog.TASKBOX_MODE_MODIFY, user, taskBox).apply {
                            setOnNewTaskBoxDialogDismissListener {
                                updatePagerRecyclerView()
                            }
                        }.show()
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
}