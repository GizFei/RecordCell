package com.giz.recordcell.bmob

import android.content.Context
import android.util.Log
import cn.bmob.v3.BmobObject
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.UpdateListener
import com.giz.recordcell.R
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.WaitProgressDialog

data class TaskBox(val user: RecordUser?,
                   val title: String,
                   val introduction: String,
                   val bgCode: Int // 背景图代号，从0 - 8
) : BmobObject(){

    constructor(): this(null, "", "", 0)

    override fun toString(): String = "用户${user?.username}创建待办集[$title]，简介：$introduction，背景图为：$bgCode"

    // 删除任务集，同时删除所有与之有关的便签、待办事项
    fun deleteTaskBox(context: Context, onSuccess: () -> Unit) {
        Log.d("TaskBox", "删除任务集：$this")
        val waitProgressDialog = WaitProgressDialog(context).apply { show() }
        Thread(Runnable {
            deleteTodosOfTaskBox(this)
            deleteNotesOfTaskBox(this)

            delete(object : UpdateListener() {
                override fun done(p0: BmobException?) {
                    waitProgressDialog.dismiss()
                    if(p0 == null){
                        context.showToast(R.string.bmob_delete_success_text)
                        onSuccess()
                    }else{
                        context.showToast(R.string.bmob_delete_failure_text)
                        Log.d("TaskBox", "删除失败：$p0")
                    }
                }
            })
        }).start()
    }
    private fun deleteTodosOfTaskBox(tb: TaskBox) {
        val todoQuery = BmobQuery<Todo>()
        todoQuery.addWhereEqualTo("taskBox", tb)
        todoQuery.include("user,taskBox")
        val todoList = todoQuery.findObjectsSync(Todo::class.java)
        todoList.forEach{
            it.remove("taskBox")
            it.updateSync() // 同步删除
        }
    }
    private fun deleteNotesOfTaskBox(tb: TaskBox) {
        val noteQuery = BmobQuery<LittleNote>()
        noteQuery.addWhereEqualTo("taskBox", tb)
        noteQuery.include("user,taskBox")
        val noteList = noteQuery.findObjectsSync(LittleNote::class.java)
        noteList.forEach {
            it.remove("taskBox")
            it.updateSync() // 同步删除
        }
    }
}