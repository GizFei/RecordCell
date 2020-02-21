package com.giz.recordcell.data

import android.content.Context
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import cn.bmob.v3.listener.UpdateListener
import com.giz.recordcell.R
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.bmob.TaskBox
import com.giz.recordcell.helpers.*
import com.giz.recordcell.pagers.TaskBoxPagerItemView
import com.giz.recordcell.widgets.CircularBottomSheetDialog
import kotlinx.android.synthetic.main.dialog_new_taskbox.*
import java.lang.RuntimeException

/**
 * 新建、修改任务集
 */
class NewTaskBoxDialog(context: Context,
                       private val mode: Int,
                       private val user: RecordUser,
                       private val taskBox: TaskBox? = null) : CircularBottomSheetDialog(context, R.style.BottomSheetDialog_ShowSoftInput){

    companion object {
        const val TASKBOX_MODE_CREATE = 0 // 新建
        const val TASKBOX_MODE_MODIFY = 1 // 修改，taskBox不能为空
    }

    private var onDismissListener : () -> Unit = {}

    private var selectedBg = 0

    fun setOnNewTaskBoxDialogDismissListener(listener: () -> Unit){
        onDismissListener = listener
    }

    init {
        setContentView(R.layout.dialog_new_taskbox)
        setOnDismissListener {
            onDismissListener()
        }
        if(mode == TASKBOX_MODE_MODIFY){
            if(taskBox == null){
                context.printLog("修改任务集时，传入的任务集为空")
                throw RuntimeException("修改任务集时，传入的任务集为空")
            }
            new_taskbox_dialog_title.text = "修改任务集"
            selectedBg = taskBox.bgCode
            new_taskbox_title.setText(taskBox.title)
            new_taskbox_introduction.setText(taskBox.introduction)
        }
        enableExpanded()
    }

    init {
        //初始化背景列表
        findViewById<RecyclerView>(R.id.new_taskbox_bg_rv)?.adapter = object : CommonAdapter<Int>(context,
            TaskBoxPagerItemView.taskBoxBgImgMap.toMutableList(), R.layout.item_taskbox_bg){
            override fun bindData(holder: CommonViewHolder, data: Int, position: Int) {
                with(holder.itemView){
                    findViewById<ImageView>(R.id.item_taskbox_bg_img).apply {
                        setImageResource(data)
                        isSelected = position == selectedBg
                    }
                    setOnTouchListener(OnPressScaleChangeTouchListener())
                    setOnClickListener{
                        val oldSelectedBg = selectedBg
                        selectedBg = position
                        notifyItemChanged(oldSelectedBg)
                        notifyItemChanged(position)
                    }
                }
            }
        }
        findViewById<EditText>(R.id.new_taskbox_title)?.requestFocus()
        findViewById<TextView>(R.id.new_taskbox_save_btn)?.setOnClickListener {
            saveOrUpdateTaskBox()
        }
    }

    private fun saveOrUpdateTaskBox() {
        // 保存或更新任务集
        val title = findViewById<EditText>(R.id.new_taskbox_title)?.text.toString()
        val intro = findViewById<EditText>(R.id.new_taskbox_introduction)?.text.toString()
        if(title.isEmpty()){
            context.showToast("任务集名称不能为空")
            return
        }
        val newTaskBox = TaskBox(
            user,
            title,
            intro,
            selectedBg
        )
        if(mode == TASKBOX_MODE_CREATE){
            newTaskBox.save(object : SaveListener<String>() {
                override fun done(p0: String?, p1: BmobException?) {
                    if(p1 == null){
                        context.showToast(R.string.bmob_save_success_text)
                        dismiss()
                    }else{
                        context.showToast(R.string.bmob_save_failure_text)
                        context.printLog("保存失败：$p1")
                    }
                }
            })
        }else if(mode == TASKBOX_MODE_MODIFY){
            newTaskBox.update(taskBox!!.objectId, object : UpdateListener() {
                override fun done(p0: BmobException?) {
                    if(p0 == null){
                        context.showToast(R.string.bmob_update_success_text)
                        dismiss()
                    }else{
                        context.showToast(R.string.bmob_update_failure_text)
                        context.printLog("更新失败：$p0")
                    }
                }
            })
        }
    }
}



/*
private fun showNewTaskBoxDialog2(){
        val newDialog = CircularBottomSheetDialog(context).apply{
            setContentView(R.layout.dialog_new_taskbox)
            setOnDismissListener { queryTaskBoxes() }
        }
        var selectedBg = 0
        with(newDialog){
            findViewById<RecyclerView>(R.id.new_taskbox_bg_rv)?.adapter = object : CommonAdapter<Int>(context,
                TaskBoxPagerItemView.taskBoxBgImgMap.toMutableList(), R.layout.item_taskbox_bg){
                override fun bindData(holder: CommonViewHolder, data: Int, position: Int) {
                    with(holder.itemView){
                        findViewById<ImageView>(R.id.item_taskbox_bg_img).apply {
                            setImageResource(data)
                            isSelected = position == selectedBg
                        }
                        setOnTouchListener(OnPressScaleChangeTouchListener())
                        setOnClickListener{
                            val oldSelectedBg = selectedBg
                            selectedBg = position
                            notifyItemChanged(oldSelectedBg)
                            notifyItemChanged(position)
                        }
                    }
                }
            }
            findViewById<TextView>(R.id.new_taskbox_save_btn)?.setOnClickListener {
                // 保存任务集
                val titleEt = findViewById<EditText>(R.id.new_taskbox_title)
                val introEt = findViewById<EditText>(R.id.new_taskbox_introduction)
                val taskBox = TaskBox(
                    user,
                    titleEt?.text.toString(),
                    introEt?.text.toString(),
                    selectedBg
                )
                context.printLog(taskBox.toString())
                taskBox.save(object : SaveListener<String>() {
                    override fun done(p0: String?, p1: BmobException?) {
                        if(p1 == null){
                            context.showToast(R.string.bmob_save_success_text)
                            newDialog.dismiss()
                            context.printLog(p0.toString())
                        }else{
                            context.showToast(R.string.bmob_save_failure_text)
                            context.printLog("保存失败：$p1")
                        }
                    }
                })
            }
        }
        newDialog.show()
    }
 */