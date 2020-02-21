package com.giz.recordcell.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.text.HtmlCompat
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.bmob.v3.BmobUser
import cn.bmob.v3.datatype.BmobDate
import com.giz.android.toolkit.dp2pxSize
import com.giz.recordcell.bmob.LittleNote
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.bmob.TaskBox
import com.giz.recordcell.bmob.fetchDate
import com.giz.recordcell.data.TaskBoxesSelectionDialog
import com.giz.recordcell.data.formatNoteDateText
import com.giz.recordcell.widgets.MarkdownTextEditor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_new_little_note.*
import org.json.JSONObject
import java.util.*

import com.giz.recordcell.R
import com.giz.recordcell.helpers.*
import com.giz.recordcell.widgets.CircularBottomSheetDialog
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

class NewLittleNoteActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val NOTE_MODE_CREATE = 0
        const val NOTE_MODE_MODIFY = 1
        const val NOTE_MODE_SEE = 2

        private const val EXTRA_NOTE_MODE = "NoteModeExtra"
        private const val EXTRA_NOTE_ITEM = "NoteItemExtra"

        fun newIntent(context: Context, mode: Int, note: LittleNote? = null) = Intent(context, NewLittleNoteActivity::class.java)
            .apply {
                putExtra(EXTRA_NOTE_MODE, mode)
                putExtra(EXTRA_NOTE_ITEM, note)
            }
    }

    private val createdTime = Date()
    private var taskBox: TaskBox? = null

    private var noteMode: Int = NOTE_MODE_CREATE
    private var littleNote: LittleNote? = null
    private val currentUser: RecordUser by lazy {
        BmobUser.getCurrentUser(RecordUser::class.java)
    }
    private lateinit var markDownEditor: MarkdownTextEditor

    private val noTaskBoxText by lazy { resources.getString(R.string.new_todo_taskbox_default) }
    private val editModeTitle by lazy { resources.getString(R.string.new_note_activity_edit_title) }
    private val seeModeTitle by lazy { resources.getString(R.string.new_note_activity_see_title) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_little_note)

        // 获取模式，便签
        noteMode = intent.getIntExtra(EXTRA_NOTE_MODE, NOTE_MODE_CREATE)
        littleNote = intent.getSerializableExtra(EXTRA_NOTE_ITEM) as? LittleNote

        // 添加webView
        if(noteMode == NOTE_MODE_CREATE){
            markDownEditor = MarkdownTextEditor(this, MarkdownTextEditor.MARKDOWN_MODE_EDIT)
        }else{
            markDownEditor = MarkdownTextEditor(this, MarkdownTextEditor.MARKDOWN_MODE_SEE,
                littleNote!!.content)
        }
        markDownEditor.setOnPageFinishedListener {
            if(noteMode == NOTE_MODE_SEE){
                markDownEditor.getTextLength {
                    new_note_text_counter.text = resources.getString(R.string.new_note_text_counter, it)
                }
            }
            if(noteMode == NOTE_MODE_CREATE){
                new_note_text_counter.text = resources.getString(R.string.new_note_text_counter, 0)
                // markDownEditor.setMaxLength(10) 最大长度
                markDownEditor.setOnTextLengthChangeListener {
                    runOnUiThread {
                        new_note_text_counter.text = resources.getString(R.string.new_note_text_counter, it)
                    }
                }
            }
        }
        new_note_editorContainer.addView(markDownEditor.getEditorView(), ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)) // 编辑器占满剩余部分

        switchMode()
        setupListeners()
    }

    private fun setupListeners() {
        new_note_taskbox_tv.enableOnPressScaleTouchListener(0.8f, 200L, this)
        bindBackPressedIcon(new_note_back_icon)
        new_note_save_btn.setOnClickListener(this)
        new_note_edit_btn.setOnClickListener(this)
        new_note_delete_btn.setOnClickListener(this)
        new_note_delete_taskbox_btn.setOnClickListener(this)
        new_note_menu_btn.setOnClickListener(this)

        new_note_editorContainer.post {
            val initBottom = new_note_editorContainer.bottom
            new_note_editorContainer.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                // printLog("改变布局：$bottom, $oldBottom, $initBottom")
                new_note_taskbox_container.toggleVisibility(this) { bottom >= initBottom } // 软键盘收起时显示，弹出时隐藏
            }
        }
        markDownEditor.setOnEditorScrollChangeListener {
            if(noteMode == NOTE_MODE_SEE) {
                if(it > 0) { // 向上滚动，隐藏
                    new_note_taskbox_container.toggleVisibility(this){ false }
                    new_note_action_btn_container.toggleVisibility(this){ false }
                }else{ // 向下滚动
                    new_note_taskbox_container.toggleVisibility(this){ true }
                    new_note_action_btn_container.toggleVisibility(this){ true }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.new_note_taskbox_tv -> { setTaskBox() }
            R.id.new_note_delete_taskbox_btn -> { updateTaskBox(null) }
            R.id.new_note_save_btn -> {
                if(noteMode == NOTE_MODE_CREATE){
                    saveNote()
                }else{
                    updateNote()
                }
            }
            R.id.new_note_delete_btn -> {
                MaterialAlertDialogBuilder(this, R.style.CustomDialog)
                    .setTitle("确定删除该条便签吗？")
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok){_, _ ->
                        littleNote?.deleteNote(this){ finish() }
                    }
                    .show()
            }
            R.id.new_note_edit_btn -> {
                if(noteMode == NOTE_MODE_SEE){
                    noteMode = NOTE_MODE_MODIFY
                    switchMode()
                }
            }
            R.id.new_note_menu_btn -> {
                showNotePopupMenu()
            }
        }
    }

    // 切换模式
    private fun switchMode(){
        when(noteMode){
            NOTE_MODE_CREATE, NOTE_MODE_MODIFY -> { // 新建模式，修改模式
                new_note_activity_title.text = editModeTitle
                new_note_create_time_tv.text = formatNoteDateText(createdTime)
                new_note_menu_btn.visibility = View.GONE
                new_note_save_btn.visibility = View.VISIBLE
                new_note_action_btn_container.visibility = View.GONE
                switchViewMode(true)

                if(noteMode == NOTE_MODE_MODIFY){
                    fillViewContentFromNote() // 重新填充一遍内容
                    // 获得焦点，弹出软键盘
                    markDownEditor.switchMode(MarkdownTextEditor.MARKDOWN_MODE_EDIT)
                    markDownEditor.setOnTextLengthChangeListener {
                        new_note_text_counter.text = resources.getString(R.string.new_note_text_counter, it)
                    }
                }
            }
            NOTE_MODE_SEE -> {
                new_note_activity_title.text = seeModeTitle
                new_note_save_btn.visibility = View.GONE
                new_note_menu_btn.visibility = View.VISIBLE
                switchViewMode(false)

                new_note_action_btn_container.visibility = View.VISIBLE
                fillViewContentFromNote()
            }
        }
    }
    private fun switchViewMode(enable: Boolean){
        new_note_taskbox_tv.isEnabled = enable
    }

    private fun fillViewContentFromNote() {
        new_note_create_time_tv.text = formatNoteDateText(littleNote?.createdTime!!.fetchDate())
        updateTaskBox(littleNote?.taskBox)
    }

    // 弹出菜单，分享与查看历史版本
    private fun showNotePopupMenu() {
        PopupMenu(this, new_note_menu_btn).apply {
            menuInflater.inflate(R.menu.menu_new_note, menu)
            setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.menu_new_note_share -> {
                        val shareContent = HtmlCompat.fromHtml(littleNote!!.content, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                        startActivity(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareContent)
                        })
                    }
                    R.id.menu_new_note_history -> {
                        showHistoryNotes()
                    }
                }
                true
            }
            val field = PopupMenu::class.declaredMemberProperties.find { it.name == "mPopup" }
            field?.let {
                it.isAccessible = true
                val helper = it.get(this) as MenuPopupHelper
                helper.setForceShowIcon(true)
            }
        }.show()
    }
    private fun showHistoryNotes() {
        val historyObjList = mutableListOf<JSONObject>()
        try {
            historyObjList.addAll(littleNote!!.history.map {
                JSONObject(it)
            })
        }catch (joe: Exception) {
            printLog("读写Json时错误：$joe")
        }
        if(historyObjList.isEmpty()){
            showToast("没有便签历史")
            return
        }
        val titleTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.gravity = Gravity.CENTER
            }
            setTextColor(Color.BLACK)
            setText(R.string.dialog_note_history_title)
            textSize = 16f
        }
        val historyRv = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.gravity = Gravity.CENTER
                it.updateMargins(top = dp2pxSize(this@NewLittleNoteActivity, 6f))
            }
            layoutManager = LinearLayoutManager(this@NewLittleNoteActivity, RecyclerView.VERTICAL, false)
            adapter = object : CommonAdapter<JSONObject>(this@NewLittleNoteActivity, historyObjList,
                R.layout.item_little_note) {
                override fun bindData(holder: CommonViewHolder, data: JSONObject, position: Int) {
                    with(holder.itemView){
                        try {
                            findViewById<TextView>(R.id.note_content_tv).text = HtmlCompat.fromHtml(data.getString(LittleNote.HISTORY_CONTENT),
                                HtmlCompat.FROM_HTML_MODE_COMPACT).trim()
                            findViewById<TextView>(R.id.note_updated_time_tv).text =
                                formatNoteDateText(Date(BmobDate.getTimeStamp(data.getString(LittleNote.HISTORY_TIME))))
                        } catch (e: Exception) {
                            printLog("读写Json时错误：$e")
                        }
                        findViewById<CardView>(R.id.note_cardView).setCardBackgroundColor(0xFFF1F2F3.toInt())
                        setOnTouchListener(OnPressScaleChangeTouchListener())
                        setOnClickListener {
                            showHistoryNoteDetail(data)
                        }
                    }
                }
            }
        }
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(titleTv)
            addView(historyRv)
        }
        CircularBottomSheetDialog(this).apply {
            setContentViewWithTopIndicator(linearLayout)
        }.show()
    }
    private fun showHistoryNoteDetail(obj: JSONObject) {
        var content = ""
        var time = ""
        try {
            content = obj.getString(LittleNote.HISTORY_CONTENT)
            time = obj.getString(LittleNote.HISTORY_TIME)
            printLog("Html内容：$content")
        }catch (e: Exception) {
            printLog("读写Json时错误：$e")
        }
        val editor = MarkdownTextEditor(this@NewLittleNoteActivity, MarkdownTextEditor.MARKDOWN_MODE_SEE, content)
        val timeTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.updateMargins(left = dp2pxSize(this@NewLittleNoteActivity, 18f), top = dp2pxSize(this@NewLittleNoteActivity, 12f))
            }
            setTextColor(0xFF8B8B8B.toInt())
            text = formatNoteDateText(Date(BmobDate.getTimeStamp(time)))
            textSize = 12f
        }
        CircularBottomSheetDialog(this).apply {
            setContentViewWithTopIndicator(LinearLayout(this@NewLittleNoteActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(editor.getEditorView())
                addView(timeTv)
            })
        }.show()
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
        new_note_taskbox_tv.text = taskBox?.title?.let {
            if(it.length > 8){
                "${it.substring(0, 8)}..."
            }else{ it }
        } ?: noTaskBoxText
        if(noteMode != NOTE_MODE_SEE){
            new_note_delete_taskbox_btn.toggleVisibility(this) { taskBox != null }
        }
    }

    // 保存便签
    private fun saveNote() {
        markDownEditor.getHTML {
            printLog("HTML内容：[$it]")
            if(it == "<div></div>"){
                showToast("便签内容不能为空")
                return@getHTML
            }
            val newNote = LittleNote(
                currentUser,
                it,
                BmobDate(createdTime),
                taskBox,
                mutableListOf<String>()
            )
            newNote.saveNote(this){
                finish() // 保存成功退出
            }
        }
    }


    // 更新便签
    private fun updateNote() {
        if(littleNote == null){
            showToast("更新失败，Todo为空")
            return
        }
        markDownEditor.getHTML {
            printLog("HTML内容：$it")
            if(it == "<div></div>"){
                showToast("便签内容不能为空")
                return@getHTML
            }
            littleNote?.apply {
                printLog("新文本：[$it],旧文本：[$content]")
                if(it != content){
                    try {
                        // 添加历史
                        val historyObj = JSONObject().apply {
                            if(history.size == 0){
                                put(LittleNote.HISTORY_TIME, createdTime.date)
                            }else{
                                put(LittleNote.HISTORY_TIME, updatedAt)
                            }
                            put(LittleNote.HISTORY_CONTENT, content)
                        }
                        history.add(historyObj.toString())
                    }catch (e: Exception){
                        printLog("添加历史错误：$e")
                    }
                }
                content = it
                if(this@NewLittleNoteActivity.taskBox == null){
                    remove("taskBox")
                }else{
                    this.taskBox = this@NewLittleNoteActivity.taskBox
                }
            }?.updateNote(this){ finish() }
        }
    }

    override fun onBackPressed() {
        if(noteMode != NOTE_MODE_SEE){
            // 退出时询问是否保存
            MaterialAlertDialogBuilder(this, R.style.CustomDialog)
                .setTitle("确定不保存待办事项吗？")
                .setNegativeButton("不保存"){ _, _ ->
                    finish()
                }.setPositiveButton("继续编辑", null)
                .show()
        }else{
            finish()
        }
    }
}