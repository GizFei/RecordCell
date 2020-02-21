package com.giz.recordcell.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import cn.bmob.v3.BmobBatch
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.BmobUser
import cn.bmob.v3.datatype.BatchResult
import cn.bmob.v3.datatype.BmobDate
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import cn.bmob.v3.listener.QueryListListener
import cn.bmob.v3.listener.QueryListener
import com.giz.recordcell.R
import com.giz.recordcell.bmob.LittleNote
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.bmob.TaskBox
import com.giz.recordcell.bmob.Todo
import com.giz.recordcell.data.NewTaskBoxDialog
import com.giz.recordcell.data.formatNoteDateText
import com.giz.recordcell.helpers.*
import com.giz.recordcell.pagers.TaskBoxPagerItemView
import com.giz.recordcell.widgets.CircularBottomSheetDialog
import com.giz.recordcell.widgets.WaitProgressDialog
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_taskbox_detail.*
import java.util.*

class TaskBoxDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_TASKBOX = "TaskBoxExtra"
        fun newIntent(context: Context, taskBox: TaskBox) = Intent(context, TaskBoxDetailActivity::class.java).apply {
            putExtra(EXTRA_TASKBOX, taskBox)
        }
    }

    private lateinit var taskBox: TaskBox
    private val currentUser: RecordUser by lazy { BmobUser.getCurrentUser(RecordUser::class.java) }

    private var todoAdapter: CommonAdapter<Todo>? = null
    private var noteAdapter: CommonAdapter<LittleNote>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_taskbox_detail)

        val tb = intent?.getSerializableExtra(EXTRA_TASKBOX) as TaskBox?
        if(tb == null){
            showToast("任务集为空")
            finish()
        }else{
            taskBox = tb
            printLog("收到任务集：$taskBox")
        }

        setupListeners()
        fillViewContent()
    }

    override fun onResume() {
        super.onResume()
        printLog("任务集详情活动恢复")
        updateTodoRecyclerView()
        updateNoteRecyclerView()
    }

    private fun fillViewContent() {
        taskbox_detail_title.text = taskBox.title
        taskbox_detail_introduction.text = taskBox.introduction
        taskbox_detail_bg.setImageResource(TaskBoxPagerItemView.taskBoxBgImgMap[taskBox.bgCode])

//        updateTodoRecyclerView()
//        updateNoteRecyclerView()
    }

    private fun setupListeners() {
        taskbox_detail_app_bar.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            var isShown = false
            override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
                // taskbox_detail_title.translationX = (abs(verticalOffset) / taskbox_detail_app_bar.totalScrollRange.toFloat()) * dp42
                if(verticalOffset == -taskbox_detail_app_bar.totalScrollRange){
                    if(!isShown){
                        isShown = true
                        ValueAnimator.ofArgb(Color.TRANSPARENT, Color.WHITE).apply {
                            addUpdateListener {
                                taskbox_detail_toolbar.setBackgroundColor(it.animatedValue as Int)
                            }
                            this.addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    if(isShown)
                                        taskbox_detail_toolbar.title = taskBox.title
                                }
                            })
                        }.start()
                    }
                }else{
                    if(isShown){
                        ValueAnimator.ofArgb(Color.WHITE, Color.TRANSPARENT).apply {
                            addUpdateListener {
                                taskbox_detail_toolbar.setBackgroundColor(it.animatedValue as Int)
                            }
                        }.start()
                        isShown = false
                        taskbox_detail_toolbar.title = ""
                    }
                }
            }
        })
        taskbox_detail_toolbar.setNavigationOnClickListener { onBackPressed() }
        taskbox_detail_bg.setOnClickListener {
            if(taskbox_detail_bg.alpha == 0f){
                taskbox_detail_bg.animate().alpha(1.0f)
                ValueAnimator.ofArgb(Color.BLACK, Color.WHITE).apply {
                    addUpdateListener {
                        changeTaskBoxContentTvColor(it.animatedValue as Int)
                    }
                }.start()
            }else{
                taskbox_detail_bg.animate().alpha(0f)
                ValueAnimator.ofArgb(Color.WHITE, Color.BLACK).apply {
                    addUpdateListener {
                        changeTaskBoxContentTvColor(it.animatedValue as Int)
                    }
                }.start()
            }
        }
        taskbox_detail_title.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(taskBox.title)
                .setMessage(taskBox.introduction)
                .show()
        }
        taskbox_detail_delete_btn.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.CustomDialog)
                .setTitle("确定删除任务集[${taskBox.title}]吗？")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    taskBox.deleteTaskBox(this){ finish() }
                }
                .show()
        }
        taskbox_detail_modify_btn.setOnClickListener {
            NewTaskBoxDialog(this, NewTaskBoxDialog.TASKBOX_MODE_MODIFY, currentUser, taskBox).apply {
                setOnNewTaskBoxDialogDismissListener {
                    val taskBoxQuery = BmobQuery<TaskBox>()
                    taskBoxQuery.include("user")
                    taskBoxQuery.getObject(taskBox.objectId, object : QueryListener<TaskBox>() {
                        override fun done(p0: TaskBox?, p1: BmobException?) {
                            if(p1 == null){
                                p0?.let {
                                    taskBox = it
                                    fillViewContent()
                                }
                            }else{
                                showToast("查询失败")
                                printLog("查询任务集失败：$p1")
                            }
                        }
                    })
                }
            }.show()
        }
        taskbox_detail_add_todo_btn.setOnClickListener {
            showAddOrRemoveTodosDialog(true)
        }
        taskbox_detail_remove_todo_btn.setOnClickListener {
            showAddOrRemoveTodosDialog(false)
        }
        taskbox_detail_add_note_btn.setOnClickListener {
            showAddOrRemoveNotesDialog(true)
        }
        taskbox_detail_remove_note_btn.setOnClickListener {
            showAddOrRemoveNotesDialog(false)
        }
    }

    private fun changeTaskBoxContentTvColor(color: Int){
        taskbox_detail_title.setTextColor(color)
        taskbox_detail_title.compoundDrawableTintList = com.giz.android.toolkit.getColorStateList(color)
        taskbox_detail_introduction.setTextColor(color)
    }

    // 更新待办列表
    private fun updateTodoRecyclerView() {
        val todoQuery = BmobQuery<Todo>()
        todoQuery.addWhereEqualTo("taskBox", taskBox)
        todoQuery.include("taskBox")
        todoQuery.findObjects(object : FindListener<Todo>() {
            override fun done(p0: MutableList<Todo>?, p1: BmobException?) {
                if(p1 == null){
                    p0 ?: return
                    if(todoAdapter == null){
                        todoAdapter = object : CommonAdapter<Todo>(this@TaskBoxDetailActivity, p0, R.layout.item_todo) {
                            override fun bindData(holder: CommonViewHolder, data: Todo, position: Int) {
                                with(holder.itemView.findViewById<MaterialCheckBox>(R.id.item_todo_checkbox)){
                                    setOnCheckedChangeListener(null)
                                    isChecked = data.isFinished
                                    setOnCheckedChangeListener { _, isChecked ->
                                        data.isFinished = isChecked
                                        data.updateTodo(this@TaskBoxDetailActivity){
                                            updateTodoRecyclerView()
                                        }
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
                                        startActivity(NewTodoActivity.newIntent(this@TaskBoxDetailActivity,
                                            NewTodoActivity.TODO_MODE_SEE, data))
                                    }
                                    setOnLongClickListener {
                                        val menuTitle = "移出待办集"
                                        PopupMenu(this@TaskBoxDetailActivity, this).apply {
                                            menu.add(menuTitle)
                                            setOnMenuItemClickListener {
                                                if(it.title == menuTitle){
                                                    data.remove("taskBox")
                                                    data.updateTodo(this@TaskBoxDetailActivity){ updateTodoRecyclerView() }
                                                }
                                                true
                                            }
                                        }.show()
                                        true
                                    }
                                    findViewById<TextView>(R.id.item_todo_remark).apply {
                                        text = data.remark
                                        toggleVisibility { data.remark.isNotEmpty() }
                                    }
                                }
                            }
                        }
                        taskbox_detail_todo_rv.adapter = todoAdapter
                    }else{
                        todoAdapter?.updateData(p0)
                    }
                }
            }
        })
    }

    // 更新便签列表
    private fun updateNoteRecyclerView() {
        val noteQuery = BmobQuery<LittleNote>()
        noteQuery.addWhereEqualTo("taskBox", taskBox)
        noteQuery.include("user,taskBox")
        noteQuery.order("-updatedAt") // 按最新修改时间降序排列
        noteQuery.findObjects(object : FindListener<LittleNote>() {
            override fun done(p0: MutableList<LittleNote>?, p1: BmobException?) {
                if (p1 == null) {
                    p0 ?: return
                    updateNoteAdapter(p0)
                } else {
                    if (p1.errorCode == 9016) {
                        showToast("查询便签错误，请检查网络")
                    } else {
                        showToast("查询便签错误")
                    }
                    printLog("查询便签错误：$p1")
                }
            }
        })
    }
    private fun updateNoteAdapter(list: MutableList<LittleNote>){
        if(noteAdapter == null){
            noteAdapter = object : CommonAdapter<LittleNote>(this, list, R.layout.item_little_note){
                override fun bindData(holder: CommonViewHolder, data: LittleNote, position: Int) {
                    with(holder.itemView){
                        findViewById<TextView>(R.id.note_content_tv).text = // Compact：简洁的文本
                            HtmlCompat.fromHtml(data.content, HtmlCompat.FROM_HTML_MODE_COMPACT).trim()
                        findViewById<TextView>(R.id.note_updated_time_tv).text = formatNoteDateText(
                            Date(BmobDate.getTimeStamp(data.updatedAt))
                        )
                        setOnTouchListener(OnPressScaleChangeTouchListener())
                        setOnClickListener {
                            startActivity(NewLittleNoteActivity.newIntent(this@TaskBoxDetailActivity,
                                NewLittleNoteActivity.NOTE_MODE_SEE, data))
                        }
                        setOnLongClickListener {
                            val menuTitle = "移出待办集"
                            PopupMenu(this@TaskBoxDetailActivity, this).apply {
                                menu.add(menuTitle)
                                setOnMenuItemClickListener {
                                    if(it.title == menuTitle){
                                        data.remove("taskBox")
                                        data.updateNote(this@TaskBoxDetailActivity){ updateNoteRecyclerView() }
                                    }
                                    true
                                }
                            }.show()
                            true
                        }
                    }
                }
            }
            taskbox_detail_note_rv.adapter = noteAdapter
        }else{
            noteAdapter?.updateData(list)
        }
    }

    // 添加或移除待办
    private fun showAddOrRemoveTodosDialog(add: Boolean) {
        val waitProgressDialog = WaitProgressDialog(this).apply { show() }
        val todoQuery = BmobQuery<Todo>()
        if(add){
            todoQuery.addWhereNotEqualTo("taskBox", taskBox)
        }else{
            todoQuery.addWhereEqualTo("taskBox", taskBox)
        }
        todoQuery.include("user,taskBox")
        todoQuery.findObjects(object : FindListener<Todo>() {
            override fun done(p0: MutableList<Todo>?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null){
                    p0 ?: return
                    if(p0.size == 0){
                        showToast("该任务集没有待办事项")
                        return
                    }
                    val selectedTodo = mutableListOf<Todo>()
                    val view = LayoutInflater.from(this@TaskBoxDetailActivity).inflate(R.layout.dialog_select_todo, null).apply {
                        findViewById<RecyclerView>(R.id.select_todo_rv).adapter = object : CommonAdapter<Todo>(
                            this@TaskBoxDetailActivity, p0, R.layout.item_todo
                        ){
                            override fun bindData(holder: CommonViewHolder, data: Todo, position: Int) {
                                with(holder.itemView){
                                    val checkBox = findViewById<MaterialCheckBox>(R.id.item_todo_checkbox)
                                    findViewById<TextView>(R.id.item_todo_name).text = data.itemName
                                    findViewById<TextView>(R.id.item_todo_remark).text = data.remark
                                    findViewById<CardView>(R.id.item_todo_cardView).setCardBackgroundColor(0xFFF1F2F3.toInt())
                                    setOnTouchListener(OnPressScaleChangeTouchListener())
                                    setOnClickListener {
                                        checkBox.isChecked = !checkBox.isChecked
                                        if(checkBox.isChecked){
                                            selectedTodo.add(data)
                                        }else{
                                            selectedTodo.remove(data)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    CircularBottomSheetDialog(this@TaskBoxDetailActivity).apply {
                        setContentView(view)
                        findViewById<TextView>(R.id.select_todo_dialog_title)?.setText(
                            if(add) R.string.select_todo_dialog_title_add else R.string.select_todo_dialog_title_remove
                        )
                        findViewById<TextView>(R.id.select_todo_save_btn)?.setOnClickListener {
                            if(add){
                                addTodosIntoTaskBox(selectedTodo, this)
                            }else{
                                removeTodosFromTaskBox(selectedTodo, this)
                            }
                        }
                    }.show()
                }
            }
        })
    }
    private fun addTodosIntoTaskBox(todoList: MutableList<Todo>, dialog: CircularBottomSheetDialog){
        val waitProgressDialog = WaitProgressDialog(this).apply { show() }
        todoList.map {
            it.taskBox = taskBox
            it
        }.also {
            BmobBatch().updateBatch(it).doBatch(object : QueryListListener<BatchResult>() {
                override fun done(p0: MutableList<BatchResult>?, p1: BmobException?) {
                    waitProgressDialog.dismiss()
                    dialog.dismiss()
                    if(p1 == null){
                        updateTodoRecyclerView()
                        showToast("添加成功")
                    }else{
                        printLog("更新失败：$p1")
                    }
                }
            })
        }
    }
    private fun removeTodosFromTaskBox(todoList: MutableList<Todo>, dialog: CircularBottomSheetDialog){
        val waitProgressDialog = WaitProgressDialog(this).apply { show() }
        Thread(Runnable {
            todoList.forEach {
                it.remove("taskBox")
                it.updateSync()
            }
            waitProgressDialog.dismiss()
            dialog.dismiss()
            updateTodoRecyclerView()
            showToast("移除成功")
        }).start()
    }

    // 添加或移除便签
    private fun showAddOrRemoveNotesDialog(add: Boolean) {
        val waitProgressDialog = WaitProgressDialog(this).apply { show() }
        val noteQuery = BmobQuery<LittleNote>()
        if(add){
            noteQuery.addWhereNotEqualTo("taskBox", taskBox)
        }else{
            noteQuery.addWhereEqualTo("taskBox", taskBox)
        }
        noteQuery.include("user,taskBox")
        noteQuery.findObjects(object : FindListener<LittleNote>(){
            override fun done(p0: MutableList<LittleNote>?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                p0 ?: return
                if(p0.size == 0){
                    showToast("该任务集没有便签")
                    return
                }
                val selectedNote = mutableListOf<LittleNote>()
                val view = LayoutInflater.from(this@TaskBoxDetailActivity).inflate(R.layout.dialog_select_note, null).apply {
                    findViewById<RecyclerView>(R.id.select_note_rv).adapter = object : CommonAdapter<LittleNote>(
                        this@TaskBoxDetailActivity, p0, R.layout.item_little_note
                    ){
                        override fun bindData(holder: CommonViewHolder, data: LittleNote, position: Int) {
                            with(holder.itemView){
                                findViewById<CardView>(R.id.note_cardView).setCardBackgroundColor(0xFFF1F2F3.toInt())
                                findViewById<TextView>(R.id.note_content_tv).text = // Compact：简洁的文本
                                    HtmlCompat.fromHtml(data.content, HtmlCompat.FROM_HTML_MODE_COMPACT).trim()
                                findViewById<TextView>(R.id.note_updated_time_tv).text = formatNoteDateText(Date(BmobDate.getTimeStamp(data.updatedAt)))
                                val checkBox = findViewById<MaterialCheckBox>(R.id.note_check_box).apply {
                                    visibility = View.VISIBLE
                                }
                                setOnTouchListener(OnPressScaleChangeTouchListener())
                                setOnClickListener {
                                    checkBox.isChecked = !checkBox.isChecked
                                    if(checkBox.isChecked){
                                        selectedNote.add(data)
                                    }else{
                                        selectedNote.remove(data)
                                    }
                                }
                            }
                        }
                    }
                    findViewById<TextView>(R.id.select_note_dialog_title).setText(
                        if(add) R.string.select_note_dialog_title_add else R.string.select_note_dialog_title_remove
                    )
                }
                CircularBottomSheetDialog(this@TaskBoxDetailActivity).apply {
                    setContentView(view)
                    findViewById<TextView>(R.id.select_note_save_btn)?.setOnClickListener {
                        if(add){
                            addNotesIntoTaskBox(selectedNote, this)
                        }else{
                            removeNotesFromTaskBox(selectedNote, this)
                        }
                    }
                }.show()
            }
        })
    }
    private fun addNotesIntoTaskBox(noteList: MutableList<LittleNote>, dialog: CircularBottomSheetDialog){
        val waitProgressDialog = WaitProgressDialog(this).apply { show() }
        noteList.map {
            it.taskBox = taskBox
            it
        }.also {
            BmobBatch().updateBatch(it).doBatch(object : QueryListListener<BatchResult>() {
                override fun done(p0: MutableList<BatchResult>?, p1: BmobException?) {
                    waitProgressDialog.dismiss()
                    dialog.dismiss()
                    if(p1 == null){
                        updateNoteRecyclerView()
                        showToast("添加成功")
                    }else{
                        printLog("更新失败：$p1")
                    }
                }
            })
        }
    }
    private fun removeNotesFromTaskBox(noteList: MutableList<LittleNote>, dialog: CircularBottomSheetDialog){
        val waitProgressDialog = WaitProgressDialog(this).apply { show() }
        Thread(Runnable {
            noteList.forEach {
                it.remove("taskBox")
                it.updateSync()
            }
            waitProgressDialog.dismiss()
            dialog.dismiss()
            updateNoteRecyclerView()
            showToast("移除成功")
        }).start()
    }
}