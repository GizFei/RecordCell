package com.giz.recordcell.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cn.bmob.v3.Bmob
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.UpdateListener
import com.giz.recordcell.R
import com.giz.recordcell.bmob.APPLICATION_ID
import com.giz.recordcell.bmob.Todo
import com.giz.recordcell.helpers.printLog
import com.giz.recordcell.helpers.showToast
import kotlinx.android.synthetic.main.activity_todo_remind.*

class TodoRemindActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TODO = "TodoExtra"

        fun newIntent(context: Context, todo: Todo) = Intent(context, TodoRemindActivity::class.java).apply {
            putExtra(EXTRA_TODO, todo)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Bmob.initialize(this, APPLICATION_ID)

        setContentView(R.layout.activity_todo_remind)

        val todo = (intent?.getSerializableExtra(EXTRA_TODO) as? Todo)?.apply {
            todo_remind_name.text = this.itemName
            todo_remind_remark.text = this.remark
        }
        if(todo == null){
            printLog("待办事项为空")
        }else{
            printLog("提醒事项：$todo")
        }

        todo_remind_know_btn.enableOnPressScaleTouchListener(0.8f, 200L){
            finish()
        }
        todo_remind_finish_btn.enableOnPressScaleTouchListener(0.8f, 200L) {
            todo?.isFinished = true
            todo?.update(object : UpdateListener() {
                override fun done(p0: BmobException?) {
                    if(p0 == null){
                        showToast(R.string.bmob_update_success_text)
                        printLog("更新成功")
                        finish()
                    }else{
                        showToast(R.string.bmob_update_failure_text)
                        printLog("更新失败：$p0")
                    }
                }
            })
        }
    }


}