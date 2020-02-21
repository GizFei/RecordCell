package com.giz.recordcell

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.get
import cn.bmob.v3.Bmob
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.BmobUser
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import cn.bmob.v3.listener.SaveListener
import com.giz.recordcell.bmob.APPLICATION_ID
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.bmob.Todo
import com.giz.recordcell.bmob.fetchDate
import com.giz.recordcell.data.TodoRemindReceiver
import com.giz.recordcell.helpers.OnPressScaleChangeTouchListener
import com.giz.recordcell.helpers.SharedPrefUtils
import com.giz.recordcell.helpers.clearText
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.ShadeView
import com.giz.recordcell.widgets.WaitProgressDialog
import kotlinx.android.synthetic.main.activity_login.*
import java.util.*
import kotlin.math.max

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        fun newIntent(context: Context) = Intent(context, LoginActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Bmob.initialize(this, APPLICATION_ID)

        setupListeners()
    }

    private fun setupListeners() {
        login_login_btn.setOnClickListener(this)
        login_login_btn.setOnTouchListener(OnPressScaleChangeTouchListener(shadeView = login_login_btn[0] as ShadeView))
        login_register_btn.setOnClickListener(this)
        login_register_btn.setOnTouchListener(OnPressScaleChangeTouchListener(shadeView = login_register_btn[0] as ShadeView))
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.login_login_btn -> {
                // 登录
                login()
            }
            R.id.login_register_btn -> {
                login_username_et.clearText()
                login_password_et.clearText()
                val transition = ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                    Pair<View, String>(login_username_et, resources.getString(R.string.share_username_et)),
                    Pair<View, String>(login_password_et, resources.getString(R.string.share_password_et)),
                    Pair<View, String>(login_register_btn, resources.getString(R.string.share_register_btn)),
                    Pair<View, String>(login_app_logo, resources.getString(R.string.share_app_logo))
                )
                startActivity(RegisterActivity.newIntent(this), transition.toBundle())
            }
        }
    }

    private fun login() {
        val waitDialog = WaitProgressDialog(this).apply { show() }
        val username = login_username_et.text.toString()
        val password = login_password_et.text.toString()
        if(username.isEmpty() && password.isEmpty()){
            waitDialog.dismiss()
            showToast(getString(R.string.login_wrong_string_hint))
            return
        }

        val user = RecordUser().apply {
            this.username = username
            this.setPassword(password)
        }
        user.login(object : SaveListener<RecordUser>() {
            override fun done(user: RecordUser?, e: BmobException?) {
                waitDialog.dismiss()
                if(e == null){
                    Log.d("LoginActivity", "user: $user")
                    Log.d("LoginActivity", "user: ${BmobUser.getCurrentUser(RecordUser::class.java)}")
                    // 首次登录，设置闹钟
                    setupAlarms(user)
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }else{
                    showToast("登录失败，请检查你的用户名和密码")
                }
            }
        })
    }

    // 设置闹钟，以及最大的requestCode
    private fun setupAlarms(user: RecordUser?) {
        user ?: return
        val todoQuery = BmobQuery<Todo>()
        todoQuery.addWhereEqualTo("user", user)
        todoQuery.include("user,taskBox")
        todoQuery.findObjects(object : FindListener<Todo>() {
            override fun done(p0: MutableList<Todo>?, p1: BmobException?) {
                if(p1 == null){
                    p0 ?: return
                    var maxRequestCode = 0
                    val nowTime = Date()
                    for(todo in p0){
                        maxRequestCode = max(maxRequestCode, todo.requestCode)
                        if(todo.remindTime != null){
                            val time = todo.remindTime!!.fetchDate()
                            if(time.after(nowTime)){ // 已过时间不设
                                TodoRemindReceiver.setupAlarm(this@LoginActivity, todo.remindTime!!.fetchDate(), todo, todo.requestCode)
                            }
                        }
                    }
                    // 存储最大的RequestCode
                    if(!SharedPrefUtils.containsUserPreferences(this@LoginActivity, user.objectId)){
                        SharedPrefUtils.setBroadCastRequestCode(this@LoginActivity, user.objectId, maxRequestCode)
                    }
                }
            }
        })
    }
}