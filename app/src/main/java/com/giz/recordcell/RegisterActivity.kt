package com.giz.recordcell

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import cn.bmob.v3.Bmob
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import com.giz.recordcell.bmob.APPLICATION_ID
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.helpers.OnPressScaleChangeTouchListener
import com.giz.recordcell.helpers.isEmail
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.ShadeView
import com.giz.recordcell.widgets.WaitProgressDialog
import kotlinx.android.synthetic.main.activity_register.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class RegisterActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        fun newIntent(context: Context) = Intent(context, RegisterActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        Bmob.initialize(this, APPLICATION_ID)

        setupListeners()
    }

    private fun setupListeners() {
        register_back_btn.setOnClickListener(this)
        register_register_btn.setOnClickListener(this)

        register_register_btn.setOnTouchListener(OnPressScaleChangeTouchListener(shadeView = register_register_btn[0] as ShadeView))
        register_username_et.addTextChangedListener(object : CustomTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val username = register_username_et.text.toString()
                // 判断用户名格式
                if(username.contains(Regex("[@/]"))){
                    register_username_layout.error = resources.getString(R.string.wrong_username_hint)
                }else{
                    if(register_username_layout.error != ""){
                        register_username_layout.error = ""
                    }
                }
            }
        })
        register_email_et.addTextChangedListener(object : CustomTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if(register_email_layout.error != "") {
                    register_email_layout.error = ""
                }
            }
        })
        register_password_et.addTextChangedListener(object : CustomTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if(register_confirm_psd_layout.error != "") {
                    register_confirm_psd_layout.error = ""
                }
            }
        })
        register_confirm_psd_et.addTextChangedListener(object : CustomTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if(register_confirm_psd_layout.error != "") {
                    register_confirm_psd_layout.error = ""
                }
            }
        })
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.register_back_btn -> { onBackPressed() }
            R.id.register_register_btn -> {
                // 注册
                register()
            }
        }
    }

    private fun register() {
        val username = register_username_et.text.toString()
        val email = register_email_et.text.toString()
        val psd = register_password_et.text.toString()
        val confirmPsd = register_confirm_psd_et.text.toString()
        // 判断用户名格式
        if(username.isEmpty() || username.isBlank() || username.contains(Regex("[@/]"))){
            register_username_layout.error = resources.getString(R.string.wrong_username_hint)
            return
        }
        // 判断邮箱格式
        if(!email.isEmail()) {
            register_email_layout.error = resources.getString(R.string.wrong_email_hint)
            return
        }
        // 判断两次密码是否相同，长度大于8，不能有空格
        if(psd != confirmPsd || psd.length < 8 || !psd.isNotBlank()){
            register_confirm_psd_layout.error = resources.getString(R.string.wrong_password_hint)
            return
        }
        // 注册
        val waitDialog = WaitProgressDialog(this).apply { show() }
        Log.d("RegisterActivity", "[${username}], [${email}], [$psd]")
        val user = RecordUser().apply {
            this.username = username
            this.setPassword(psd)
            this.email = email
        }
        user.signUp(object : SaveListener<RecordUser>() {
            override fun done(user: RecordUser?, e: BmobException?) {
                waitDialog.dismiss()
                if(e == null) {
                    showToast("注册成功")
                    onBackPressed()
                }else{
                    e.printStackTrace()
                    Log.e("RegisterActivity", e.toString())
                    if(e.errorCode == 202){
                        showToast("用户名已存在")
                    }else if(e.errorCode == 203){
                        showToast("邮箱已被注册")
                    }else{
                        showToast("注册失败")
                    }
                }
            }
        })
    }

    @Throws(Exception::class)
    private fun createAvatarFile(){
        val avatarFile = File(filesDir.path, "default_avatar.jpg") // 默认头像
        if(!avatarFile.exists()){
            if(avatarFile.createNewFile()){
                val inputStream = resources.openRawResource(R.raw.default_avatar)
                val os = FileOutputStream(avatarFile)

                val byteArray = ByteArray(2048)
                var len = inputStream.read(byteArray)
                while (len > -1){
                    os.write(byteArray, 0, len)
                    len = inputStream.read(byteArray)
                }
                os.close()
                inputStream.close()
            }
        }
    }

    @Throws(Exception::class)
    private fun createHeaderBgFile(){
        val headerFile = File(filesDir.path, "default_header.jpg") // 默认头像
        if(!headerFile.exists()){
            if(headerFile.createNewFile()){
                val inputStream = resources.openRawResource(R.raw.default_header)
                val os = FileOutputStream(headerFile)

                val byteArray = ByteArray(2048)
                var len = inputStream.read(byteArray)
                while (len > -1){
                    os.write(byteArray, 0, len)
                    len = inputStream.read(byteArray)
                }
                os.close()
                inputStream.close()
            }
        }
    }

    private open class CustomTextWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

}