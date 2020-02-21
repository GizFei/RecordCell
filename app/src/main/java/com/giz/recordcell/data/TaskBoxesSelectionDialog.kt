package com.giz.recordcell.data

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.giz.android.toolkit.dp2px
import com.giz.recordcell.R
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.bmob.TaskBox
import com.giz.recordcell.helpers.CommonAdapter
import com.giz.recordcell.helpers.OnPressScaleChangeTouchListener
import com.giz.recordcell.helpers.printLog
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.pagers.TaskBoxPagerItemView
import com.giz.recordcell.widgets.CircularBottomSheetDialog
import com.giz.recordcell.widgets.WaitProgressDialog

class TaskBoxesSelectionDialog(context: Context,
                               private val user: RecordUser,
                               private val selectedTaskBox: TaskBox?) : CircularBottomSheetDialog(context) {

    private var onTaskBoxClickListener: (TaskBox) -> Unit = {}

    fun setOnTaskBoxClickListener(listener: (TaskBox) -> Unit) {
        onTaskBoxClickListener = listener
    }

    // 查询所有任务集
    private fun queryTaskBoxes(){
        val waitProgressDialog = WaitProgressDialog(context).apply { show() }
        val bmobQuery = BmobQuery<TaskBox>()
        bmobQuery.addWhereEqualTo("user", user)
        bmobQuery.findObjects(object : FindListener<TaskBox>() {
            override fun done(p0: MutableList<TaskBox>?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null){
                    showTaskBoxesDialog(p0)
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

    // 显示任务集列表
    private fun showTaskBoxesDialog(list: MutableList<TaskBox>?) {
        val boxList: MutableList<TaskBox> = list ?: mutableListOf()
        boxList.add(TaskBox(RecordUser(), "", "", 0)) // 最后一个显示添加按钮视图

        val titleTv = TextView(context).apply {
            text = "加入任务集"
            setTextColor(Color.BLACK)
            textSize = 16f
            val dp8 = dp2px(context, 8f).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp8, 0, 0, dp8)
            }
        }
        val recyclerView = RecyclerView(context).apply{
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = object : CommonAdapter<TaskBox>(context, boxList, R.layout.item_taskbox){
                private fun isLast(pos: Int) = pos == itemCount - 1

                override fun bindData(holder: CommonViewHolder, data: TaskBox, position: Int) {
                    if(isLast(position)){ //末位
                        with(holder.itemView){
                            findViewById<ImageView>(R.id.item_taskbox_new_btn).visibility = View.VISIBLE
                            findViewById<ConstraintLayout>(R.id.item_taskbox_container).visibility = View.GONE
                            setOnTouchListener(OnPressScaleChangeTouchListener(duration = 200L))
                            setOnClickListener{
                                dismiss()
                                showNewTaskBoxDialog()
                            }
                        }
                    }else{
                        with(holder.itemView) {
                            findViewById<ImageView>(R.id.item_taskbox_new_btn).visibility = View.GONE
                            findViewById<ConstraintLayout>(R.id.item_taskbox_container).apply {
                                visibility = View.VISIBLE
                                isSelected = (selectedTaskBox != null && selectedTaskBox.objectId == data.objectId)
                            }
                            findViewById<ImageView>(R.id.item_taskbox_icon).setImageResource(
                                TaskBoxPagerItemView.taskBoxBgImgMap[data.bgCode])
                            findViewById<TextView>(R.id.item_taskbox_title).text = data.title
                            findViewById<TextView>(R.id.item_taskbox_intro).text = data.introduction
                            setOnTouchListener(OnPressScaleChangeTouchListener(duration = 200L))
                            setOnClickListener{
                                dismiss()
                                onTaskBoxClickListener(data)
                            }
                        }
                    }
                }
            }
        }
        val ll = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(titleTv)
            addView(recyclerView)
        }
        setContentViewWithTopIndicator(ll)
        show()
    }

    fun showDialog() {
        queryTaskBoxes()
    }

    // 新建任务集
    private fun showNewTaskBoxDialog(){
        NewTaskBoxDialog(context, NewTaskBoxDialog.TASKBOX_MODE_CREATE, user).apply {
            setOnNewTaskBoxDialogDismissListener {
                queryTaskBoxes()
            }
        }.show()
    }
}





/*
    // 设置任务集
    private fun setTaskBox() {
        val waitProgressDialog = WaitProgressDialog(this).apply { show() }
        val bmobQuery = BmobQuery<TaskBox>()
        bmobQuery.addWhereEqualTo("user", currentUser)
        bmobQuery.findObjects(object : FindListener<TaskBox>() {
            override fun done(p0: MutableList<TaskBox>?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null){
                    showTaskBoxesDialog(p0)
                }else{
                    showTaskBoxesDialog(null)
                    printLog("查询任务集错误：$p1")
                }
            }
        })
        printLog("更改任务集")
    }

    // 显示任务集
    private fun showTaskBoxesDialog(l: MutableList<TaskBox>?) {
        val boxList: MutableList<TaskBox> = l ?: mutableListOf()
        boxList.add(TaskBox(RecordUser(), "", "")) // 最后一个显示添加按钮视图
        val dialog = CircularBottomSheetDialog(this@NewTodoActivity)

        val titleTv = TextView(this).apply {
            text = "加入任务集"
            setTextColor(Color.BLACK)
            textSize = 16f
            val dp8 = dp2px(this@NewTodoActivity, 8f).toInt()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp8, 0, 0, dp8)
            }
        }
        val recyclerView = RecyclerView(this).apply{
            layoutManager = LinearLayoutManager(this@NewTodoActivity, RecyclerView.VERTICAL, false)
            adapter = object : CommonAdapter<TaskBox>(this@NewTodoActivity, boxList, R.layout.item_taskbox){
                private fun isLast(pos: Int) = pos == itemCount - 1

                override fun bindData(holder: CommonViewHolder, data: TaskBox, position: Int) {
                    if(isLast(position)){ //末位
                        with(holder.itemView){
                            findViewById<ImageView>(R.id.item_taskbox_new_btn).visibility = View.VISIBLE
                            findViewById<ConstraintLayout>(R.id.item_taskbox_container).visibility = View.GONE
                            setOnTouchListener(OnPressScaleChangeTouchListener(duration = 200L))
                            setOnClickListener{
                                dialog.dismiss()
                                showNewTaskboxDialog()
                            }
                        }
                    }else{
                        with(holder.itemView) {
                            findViewById<ImageView>(R.id.item_taskbox_new_btn).visibility = View.GONE
                            findViewById<ConstraintLayout>(R.id.item_taskbox_container).apply {
                                visibility = View.VISIBLE
                                isSelected = (taskBox != null && taskBox?.objectId == data.objectId)
                            }
                            findViewById<TextView>(R.id.item_taskbox_title).text = data.title
                            findViewById<TextView>(R.id.item_taskbox_intro).text = data.introduction
                            setOnTouchListener(OnPressScaleChangeTouchListener(duration = 200L))
                            setOnClickListener{
                                dialog.dismiss()
                                updateTaskBox(data)
                            }
                        }
                    }
                }
            }
        }
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(titleTv)
            addView(recyclerView)
        }
        dialog.setContentView(ll)
        dialog.show()
    }

    // 新建任务集
    private fun showNewTaskboxDialog(){
        val newDialog = CircularBottomSheetDialog(this@NewTodoActivity).apply{
            setContentView(R.layout.dialog_new_taskbox)
            setOnDismissListener { setTaskBox() }
        }
        with(newDialog){
            findViewById<TextView>(R.id.new_taskbox_save_btn)?.setOnClickListener {
                // 保存任务集
                val titleEt = findViewById<EditText>(R.id.new_taskbox_title)
                val introEt = findViewById<EditText>(R.id.new_taskbox_introduction)
                val taskBox = TaskBox(
                    currentUser,
                    titleEt?.text.toString(),
                    introEt?.text.toString()
                )
                printLog(taskBox.toString())
                taskBox.save(object : SaveListener<String>() {
                    override fun done(p0: String?, p1: BmobException?) {
                        if(p1 == null){
                            showToast("保存成功")
                            newDialog.dismiss()
                            printLog(p0.toString())
                        }else{
                            showToast("保存失败")
                            printLog("保存失败：$p1")
                        }
                    }
                })
            }
        }
        newDialog.show()
    }

     */