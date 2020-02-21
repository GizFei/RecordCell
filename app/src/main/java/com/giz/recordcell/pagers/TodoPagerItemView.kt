package com.giz.recordcell.pagers

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.preference.PreferenceManager
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.giz.recordcell.R
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.bmob.Todo
import com.giz.recordcell.data.TaskBoxesSelectionDialog
import com.giz.recordcell.helpers.*
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

class TodoPagerItemView(private val context: Context,
                        private val currentUser: RecordUser): BaseRecyclerViewPagerItemView(context) {

    private var todoAdapter: CommonAdapter<Todo>? = null
    private var onTodoItemClickListener = { _: Todo -> Unit }
    private var finishedTodoVisibility = false
    private var showRemark = true

    fun setOnTodoItemClickListener(listener: (Todo) -> Unit){
        onTodoItemClickListener = listener
    }

    private val visibilityBtn: ImageView

    private fun toggleIcon(visibility: Boolean) {
        visibilityBtn.setImageResource(if(visibility) R.drawable.ic_visibility
        else R.drawable.ic_visibility_off)
    }

    init {
        val headerView = LayoutInflater.from(context).inflate(R.layout.pager_header_todo, null).apply {
            visibilityBtn = findViewById(R.id.pager_header_todo_visibility)

            finishedTodoVisibility = !PreferenceManager.getDefaultSharedPreferences(this@TodoPagerItemView.context)
                .getBoolean("todo_hide_finished", false).also { toggleIcon(!it) }

            visibilityBtn.apply {
                setOnClickListener {
                    finishedTodoVisibility = !finishedTodoVisibility
                    toggleIcon(finishedTodoVisibility)
                    updatePagerRecyclerView()
                }
            }
        }
        addHeaderView(headerView)
    }

    // 更新Todo待办列表
    override fun updatePagerRecyclerView() {
        // 是否显示备注
        showRemark = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("todo_show_remark",true)
        //finishedTodoVisibility = !PreferenceManager.getDefaultSharedPreferences(context)
          //  .getBoolean("todo_hide_finished", false).also { toggleIcon(!it) }
        // printLog("是否显示备注：$showRemark, 隐藏已完成：$finishedTodoVisibility")

        mSwipeRefreshLayout.isRefreshing = true

        val bmobQuery = BmobQuery<Todo>()
        bmobQuery.addWhereEqualTo("user", currentUser)
        bmobQuery.include("user,taskBox")
        bmobQuery.findObjects(object : FindListener<Todo>() {
            override fun done(p0: MutableList<Todo>?, p1: BmobException?) {
                mSwipeRefreshLayout.isRefreshing = false
                if(p1 == null){
                    p0 ?: return
                    if(!finishedTodoVisibility){
                        p0.filterNot { it.isFinished }.also {
                            p0.clear()
                            p0.addAll(it)
                        }
                    }
                    if(todoAdapter == null){
                        todoAdapter = object : CommonAdapter<Todo>(context, p0, R.layout.item_todo) {
                            override fun bindData(holder: CommonViewHolder, data: Todo, position: Int) {
                                with(holder.itemView.findViewById<MaterialCheckBox>(R.id.item_todo_checkbox)){
                                    setOnCheckedChangeListener(null)
                                    isChecked = data.isFinished
                                    setOnCheckedChangeListener { _, isChecked ->
                                        data.isFinished = isChecked
                                        data.updateTodo(context) { updatePagerRecyclerView() }
                                    }
                                }
                                with(holder.itemView){
                                    findViewById<TextView>(R.id.item_todo_name).apply {
                                        text = data.itemName
                                        if(data.isFinished){
                                            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                                            setTextColor(Color.GRAY)
                                        }else{
                                            paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                                            setTextColor(Color.BLACK)
                                        }
                                    }
                                    setOnTouchListener(
                                        OnPressScaleChangeTouchListener(minScale = 0.92f,
                                            targetView = holder.itemView)
                                    )
                                    setOnClickListener {
                                        onTodoItemClickListener(data)
                                    }
                                    setOnLongClickListener {
                                        // 长按弹出菜单
                                        showPopupMenu(data, holder.itemView)
                                        true
                                    }
                                    findViewById<TextView>(R.id.item_todo_remark).apply {
                                        if(showRemark){
                                            text = data.remark
                                            toggleVisibility { data.remark.isNotEmpty() }
                                        }else {
                                            toggleVisibility { false }
                                        }
                                    }
                                }
                            }
                        }
                        mRecyclerView.adapter = todoAdapter
                    }else{
                        todoAdapter?.updateData(p0)
                    }
                }else{
                    printLog("加载待办列表失败：$p1")
                }
            }
        })
    }

    // 长按待办事项弹出菜单
    @Suppress("UNCHECKED_CAST")
    private fun showPopupMenu(data: Todo, anchorView: View) {
        val popupMenu = PopupMenu(context, anchorView).apply {
            menuInflater.inflate(R.menu.menu_todo_item, menu) // 填充菜单
            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.menu_todo_delete -> {
                        MaterialAlertDialogBuilder(context, R.style.CustomDialog)
                            .setTitle("确定删除待办事项[${data.itemName}]吗？")
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok){_, _ ->
                                data.deleteTodo(context){ updatePagerRecyclerView() }
                            }
                            .show()
                    }
                    R.id.menu_todo_add_to_taskbox -> {
                        TaskBoxesSelectionDialog(context, currentUser, data.taskBox).apply {
                            setOnTaskBoxClickListener {
                                data.taskBox = it
                                data.updateTodo(context){ updatePagerRecyclerView() }
                            }
                        }.showDialog()
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