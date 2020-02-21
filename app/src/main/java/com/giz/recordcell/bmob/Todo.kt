package com.giz.recordcell.bmob

import android.content.Context
import android.util.Log
import cn.bmob.v3.BmobObject
import cn.bmob.v3.datatype.BmobDate
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.UpdateListener
import com.giz.recordcell.R
import com.giz.recordcell.data.TodoRemindReceiver
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.WaitProgressDialog
import java.util.*

// 待办类
data class Todo(val user: RecordUser?,
                var itemName: String,  // 事项名称
                var remark: String,
                val createdTime: BmobDate,
                var remindTime: BmobDate?,
                var taskBox: TaskBox?,
                var isFinished: Boolean,
                var requestCode: Int
) : BmobObject(){

    constructor(): this(null, "", "", BmobDate(Date()), null, null, false, 0)
    constructor(t: Todo): this(t.user, t.itemName, t.remark, t.createdTime, t.remindTime, t.taskBox, t.isFinished, t.requestCode)

    override fun toString(): String = "用户${user?.username}创建的待办[$itemName], objectId：${objectId}," +
            "备注：$remark, 待办集：${taskBox?.title}, 创建时间：${createdTime.date}, 提醒时间：${remindTime?.date}, 是否完成：$isFinished"

    // 更新待办内容
    fun updateTodo(context: Context, onSuccess: () -> Unit) {
        val waitProgressDialog = WaitProgressDialog(context).also { it.show() }
        update(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    // 取消或设置闹钟
                    remindTime?.apply {
                        fetchDate().takeIf { it.after(Date()) }?.let {
                            if(isFinished){ // 取消
                                TodoRemindReceiver.stopAlarm(context, requestCode)
                            }else{ // 设置
                                TodoRemindReceiver.setupAlarm(context, it, this@Todo, requestCode)
                            }
                        }
                    }
                    context.showToast(R.string.bmob_update_success_text)
                    onSuccess()
                    Log.d("Todo", "更新待办成功: ${this@Todo}")
                }else{
                    context.showToast(R.string.bmob_update_failure_text)
                    Log.d("Todo", "更新Todo失败：$p0")
                }
            }
        })
    }

    // 删除待办
    fun deleteTodo(context: Context, onSuccess: () -> Unit){
        val waitProgressDialog = WaitProgressDialog(context).also { it.show() }
        delete(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    context.showToast(R.string.bmob_delete_success_text)
                    onSuccess()
                    // 删除闹钟
                    remindTime?.apply {
                        TodoRemindReceiver.stopAlarm(context, requestCode)
                    }
                    Log.d("Todo", "删除成功：${this@Todo}")
                }else{
                    context.showToast(R.string.bmob_delete_failure_text)
                    Log.d("Todo", "删除失败：$p0")
                }
            }
        })
    }
}




/*
// 更新云端的checkbox
    private fun updateTodo(to do: To do) {
        to do.update(to do.objectId, object : UpdateListener() {
            override fun done(p0: BmobException?) {
                if(p0 == null){
                    context.showToast(R.string.bmob_update_success_text)
                    printLog("更新成功: $to do")
                    updatePagerRecyclerView()
                }else{
                    context.showToast(R.string.bmob_update_failure_text)
                    printLog("更新Todo失败：$p0")
                }
            }
        })
    }
 */