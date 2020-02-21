package com.giz.recordcell.bmob

import android.content.Context
import android.util.Log
import cn.bmob.v3.BmobObject
import cn.bmob.v3.datatype.BmobDate
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import cn.bmob.v3.listener.UpdateListener
import com.giz.recordcell.R
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.WaitProgressDialog
import java.util.*

data class LittleNote(val user: RecordUser?,
                      var content: String,
                      val createdTime: BmobDate,
                      var taskBox: TaskBox?,
                      var history: MutableList<String>
) : BmobObject() {

    constructor() : this(null, "", BmobDate(Date()), null, mutableListOf<String>())

    companion object {
        const val HISTORY_TIME = "editTime" // 时间是String形式的
        const val HISTORY_CONTENT = "content"
    }

    // 更新
    fun updateNote(context: Context, onSuccess: () -> Unit) {
        val waitProgressDialog = WaitProgressDialog(context)
        update(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    context.showToast(R.string.bmob_update_success_text)
                    onSuccess()
                    Log.d("Note", "更新待办成功: ${this@LittleNote}")
                }else{
                    context.showToast(R.string.bmob_update_failure_text)
                    Log.d("Note", "更新Note失败：$p0")
                }
            }
        })
    }

    // 保存
    fun saveNote(context: Context, onSuccess: () -> Unit) {
        val waitProgressDialog = WaitProgressDialog(context).also { it.show() }
        save(object : SaveListener<String>() {
            override fun done(p0: String?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null){
                    context.showToast(R.string.bmob_save_success_text)
                    onSuccess()
                    Log.d("Note", "保存成功：${this@LittleNote}")
                }else{
                    context.showToast(R.string.bmob_save_failure_text)
                    Log.d("Note", "保存失败：$p1")
                }
            }
        })
    }

    // 删除待办
    fun deleteNote(context: Context, onSuccess: () -> Unit){
        val waitProgressDialog = WaitProgressDialog(context).also { it.show() }
        delete(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    context.showToast(R.string.bmob_delete_success_text)
                    onSuccess()
                    Log.d("Note", "删除成功：${this@LittleNote}")
                }else{
                    context.showToast(R.string.bmob_delete_failure_text)
                    Log.d("Note", "删除失败：$p0")
                }
            }
        })
    }

}