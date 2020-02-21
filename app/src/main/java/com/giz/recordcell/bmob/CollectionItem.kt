package com.giz.recordcell.bmob

import android.content.Context
import cn.bmob.v3.BmobObject
import cn.bmob.v3.datatype.BmobDate
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import cn.bmob.v3.listener.UpdateListener
import com.giz.recordcell.R
import com.giz.recordcell.helpers.printLog
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.WaitProgressDialog
import java.util.*

data class CollectionItem(var user: RecordUser,
                          var title: String,
                          var collectedTime: BmobDate,
                          var sourceApp: String,
                          var url: String,
                          var coverUrl: String,
                          var richContent: String,  // 文章内容
                          var authorName: String,   // 作者名
                          var avatarUrl: String     // 作者头像链接
) : BmobObject() {

    constructor(): this(RecordUser(), "", BmobDate(Date()), "", "", "", "", "佚名", "")

    // 保存
    fun saveCollection(context: Context, onSuccess: () -> Unit, onFail: () -> Unit = {}) {
        val waitProgressDialog = WaitProgressDialog(context).also { it.show() }
        save(object : SaveListener<String>() {
            override fun done(p0: String?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null){
                    context.showToast(R.string.bmob_save_success_text)
                    onSuccess()
                    context.printLog("保存成功：${this@CollectionItem}")
                }else{
                    context.showToast(R.string.bmob_save_failure_text)
                    onFail()
                    context.printLog("保存失败：$p1")
                }
            }
        })
    }

    // 更新
    fun updateCollection(context: Context, onSuccess: () -> Unit) {
        val waitProgressDialog = WaitProgressDialog(context)
        update(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    context.showToast(R.string.bmob_update_success_text)
                    onSuccess()
                    context.printLog("更新日常成功: ${this@CollectionItem}")
                }else{
                    context.showToast(R.string.bmob_update_failure_text)
                    context.printLog("更新日常失败：$p0")
                }
            }
        })
    }

    // 删除
    fun deleteCollection(context: Context, onSuccess: () -> Unit){
        val waitProgressDialog = WaitProgressDialog(context).also { it.show() }
        delete(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    context.showToast(R.string.bmob_delete_success_text)
                    onSuccess()
                    context.printLog("删除成功：${this@CollectionItem}")
                }else{
                    context.showToast(R.string.bmob_delete_failure_text)
                    context.printLog("删除失败：$p0")
                }
            }
        })
    }

    override fun toString(): String = "CollectionItem(${title}, ${collectedTime.date}, ${sourceApp}, ${url}, ${coverUrl})"
}