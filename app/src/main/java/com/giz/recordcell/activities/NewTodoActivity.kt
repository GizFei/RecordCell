package com.giz.recordcell.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import cn.bmob.v3.BmobUser
import cn.bmob.v3.datatype.BmobDate
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import com.giz.recordcell.R
import com.giz.recordcell.bmob.*
import com.giz.recordcell.data.TaskBoxesSelectionDialog
import com.giz.recordcell.data.TodoRemindReceiver
import com.giz.recordcell.data.formatNoteDateText
import com.giz.recordcell.helpers.*
import com.giz.recordcell.widgets.DateTimePickerDialog
import com.giz.recordcell.widgets.WaitProgressDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_new_todo.*
import kotlinx.android.synthetic.main.activity_new_todo.new_todo_taskbox_tv
import java.util.*

class NewTodoActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val TODO_MODE_CREATE = 0
        const val TODO_MODE_MODIFY = 1
        const val TODO_MODE_SEE = 2

        const val RESULT_CODE_OK = 200
        const val RESULT_CODE_CANCEL = 404

        private const val EXTRA_TODO_MODE = "TodoMode"
        private const val EXTRA_TODO_ITEM = "TodoItem"

        fun newIntent(context: Context, mode: Int, todo: Todo? = null) = Intent(context, NewTodoActivity::class.java)
            .apply {
                putExtra(EXTRA_TODO_MODE, mode)
                putExtra(EXTRA_TODO_ITEM, todo)
            }
    }

    private val createdTime = Date()
    private var remindTime: Date? = null
    private var taskBox: TaskBox? = null

    private var todoMode: Int = TODO_MODE_CREATE
    private var theTodo: Todo? = null
    private lateinit var currentUser: RecordUser

    private val noRemindTimeText by lazy { resources.getString(R.string.new_todo_no_remind_time_text) }
    private val noTaskBoxText by lazy { resources.getString(R.string.new_todo_taskbox_default) }
    private val editModeTitle by lazy { resources.getString(R.string.new_todo_activity_name) }
    private val seeModeTitle by lazy { resources.getString(R.string.new_todo_activity_see_name) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_todo)

        currentUser = BmobUser.getCurrentUser(RecordUser::class.java)
        // 获取模式，待办事项
        todoMode = intent.getIntExtra(EXTRA_TODO_MODE, TODO_MODE_CREATE)
        theTodo = intent.getSerializableExtra(EXTRA_TODO_ITEM) as? Todo
        switchMode()

        setupListeners()
    }

    private fun setupListeners() {
        new_todo_remind_time_tv.enableOnPressScaleTouchListener(0.8f, 200L, this)
        new_todo_taskbox_tv.enableOnPressScaleTouchListener(0.8f, 200L, this)
        new_todo_back_icon.setOnClickListener(this)
        new_todo_save_btn.setOnClickListener(this)
        new_todo_edit_btn.setOnClickListener(this)
        new_todo_delete_btn.setOnClickListener(this)
        new_todo_delete_remind_btn.setOnClickListener(this)
        new_todo_delete_taskbox_btn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.new_todo_remind_time_tv -> { setRemindTime() }
            R.id.new_todo_taskbox_tv -> { setTaskBox() }
            R.id.new_todo_delete_remind_btn -> { updateRemindTime(null) }
            R.id.new_todo_delete_taskbox_btn -> { updateTaskBox(null) }
            R.id.new_todo_save_btn -> {
                if(todoMode == TODO_MODE_CREATE){
                    saveTodo()
                }else if(todoMode == TODO_MODE_MODIFY){
                    updateTodo()
                }
            }
            R.id.new_todo_back_icon -> { confirmBackPressed() }

            R.id.new_todo_delete_btn -> {
                MaterialAlertDialogBuilder(this, R.style.CustomDialog)
                    .setTitle("确定删除待办事项吗？")
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok){_, _ ->
                        theTodo?.deleteTodo(this){ finishNewTodoActivity(true) }
                    }
                    .show()
            }
            R.id.new_todo_edit_btn -> {
                if(todoMode == TODO_MODE_SEE){
                    todoMode = TODO_MODE_MODIFY
                    switchMode()
                }
            }
        }
    }

    // 切换模式
    private fun switchMode(){
        when(todoMode){
            TODO_MODE_CREATE, TODO_MODE_MODIFY -> { // 新建模式，修改模式
                new_todo_activity_title.text = editModeTitle
                new_todo_create_time_tv.text = formatNoteDateText(createdTime)
                new_todo_save_btn.visibility = View.VISIBLE
                new_todo_action_btn_container.toggleVisibility(this){false}
                switchViewMode(true)

                if(todoMode == TODO_MODE_MODIFY){
                    fillViewContentFromTodo() // 重新填充一遍内容
                }
                // 获得焦点，弹出软键盘
                new_todo_item_name.requestFocus()
                new_todo_item_name.setSelection(new_todo_item_name.text.toString().length)
                showSoftInputKeyboard(new_todo_item_name)
            }
            TODO_MODE_SEE -> {
                new_todo_activity_title.text = seeModeTitle
                new_todo_save_btn.visibility = View.GONE
                switchViewMode(false)

                new_todo_item_name.setTextColor(Color.BLACK)
                new_todo_remark.setTextColor(Color.BLACK)
                new_todo_action_btn_container.toggleVisibility(this){true}
                fillViewContentFromTodo()
            }
        }
    }
    private fun switchViewMode(enable: Boolean){
        new_todo_item_name.isEnabled = enable
        new_todo_remark.isEnabled = enable
        new_todo_taskbox_tv.isEnabled = enable
        new_todo_remind_time_tv.isEnabled = enable
    }

    // 将待办事项的内容填充到视图中
    private fun fillViewContentFromTodo() {
        new_todo_create_time_tv.text = formatNoteDateText(theTodo?.createdTime?.fetchDate()!!)
        new_todo_item_name.setText(theTodo?.itemName)
        new_todo_remark.setText(theTodo?.remark)
        updateRemindTime(theTodo?.remindTime?.fetchDate())
        updateTaskBox(theTodo?.taskBox)
    }

    // 设置任务集
    private fun setTaskBox() {
        TaskBoxesSelectionDialog(this, currentUser, taskBox).apply {
            setOnTaskBoxClickListener {
                updateTaskBox(it)
            }
        }.showDialog()
    }

    // 更新当前待办所属任务集
    private fun updateTaskBox(tb: TaskBox?){
        taskBox = tb
        new_todo_taskbox_tv.text = taskBox?.title?.let {
            if(it.length > 10){
                "${it.substring(0, 10)}..."
            }else{ it }
        } ?: noTaskBoxText
        if(todoMode != TODO_MODE_SEE){
            new_todo_delete_taskbox_btn.toggleVisibility(this) { taskBox != null }
        }
    }

    // 更新当前待办的提醒时间
    private fun updateRemindTime(date: Date?) {
        remindTime = date
        new_todo_remind_time_tv.text = if(remindTime == null) {
            noRemindTimeText
        } else{
            formatNoteDateText(remindTime!!)
        }
        if(todoMode != TODO_MODE_SEE){
            new_todo_delete_remind_btn.toggleVisibility(this) { remindTime != null }
        }
    }

    // 设置提醒时间
    private fun setRemindTime() {
        val dateTimePickerDialog = DateTimePickerDialog(this, remindTime ?: Date()){
                updateRemindTime(it)
            }
        dateTimePickerDialog.show()
        // printLog("更改提醒时间")
    }

    // 保存待办
    private fun saveTodo() {
        if(new_todo_item_name.text.isEmpty()){
            showToast("待办事项名称不能为空")
            return
        }
        hideSoftInputKeyboard(new_todo_item_name)
        val waitProgressDialog = WaitProgressDialog(this).apply { show() }
        val newTodo = Todo(
            currentUser,
            new_todo_item_name.text.toString(),
            new_todo_remark.text.toString(),
            BmobDate(createdTime),
            if(remindTime == null) null else BmobDate(remindTime),
            taskBox,
            false,
            SharedPrefUtils.getBroadCastRequestCode(this@NewTodoActivity, currentUser.objectId)
        )
        newTodo.save(object : SaveListener<String>() {
            override fun done(p0: String?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null){
                    // 保存成功
                    showToast(R.string.bmob_save_success_text)
                    // 注册闹钟
                    remindTime?.let{
                        TodoRemindReceiver.setupAlarm(this@NewTodoActivity, it, newTodo,
                            newTodo.requestCode)
                    }
                    finishNewTodoActivity(true)
                }else{
                    showToast(R.string.bmob_save_failure_text)
                    printLog("保存失败：$p1")
                }
            }
        })
    }

    // 更新待办
    private fun updateTodo() {
        if(theTodo == null) {
            showToast("更新失败，Todo为空")
            return
        }
        if(new_todo_item_name.text.isEmpty()){
            showToast("待办事项名称不能为空")
            return
        }
        theTodo?.apply {
            itemName = new_todo_item_name.text.toString()
            remark = new_todo_remark.text.toString()
            if(this@NewTodoActivity.remindTime == null){
                remove("remindTime")
            }else{
                this.remindTime = BmobDate(this@NewTodoActivity.remindTime)
            }
            if(this@NewTodoActivity.taskBox == null){
                remove("taskBox")
            }else{
                this.taskBox = this@NewTodoActivity.taskBox
            }
        }?.updateTodo(this){ finishNewTodoActivity(true) }
    }

    override fun onBackPressed() {
        confirmBackPressed()
    }

    private fun finishNewTodoActivity(refresh: Boolean){
        if(refresh){
            setResult(RESULT_CODE_OK)
        }else{
            setResult(RESULT_CODE_CANCEL)
        }
        finish()
    }

    private fun confirmBackPressed() {
        if(todoMode == TODO_MODE_SEE){
            finishNewTodoActivity(false)
        }else{
            // 退出时询问是否保存
            MaterialAlertDialogBuilder(this, R.style.CustomDialog)
                .setTitle("确定不保存待办事项吗？")
                .setNegativeButton("不保存"){ _, _ ->
                    finishNewTodoActivity(false)
                }.setPositiveButton("继续编辑", null)
                .show()
        }
    }
}