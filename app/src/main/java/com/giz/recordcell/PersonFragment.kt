package com.giz.recordcell

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.BmobUser
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import cn.bmob.v3.listener.UpdateListener
import com.android.volley.Response
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.bmob.Todo
import com.giz.recordcell.bmob.WebImageUtils
import com.giz.recordcell.bmob.fetchDate
import com.giz.recordcell.data.FunctionListOrderDialog
import com.giz.recordcell.data.TodoRemindReceiver
import com.giz.recordcell.helpers.FileUriUtils
import com.giz.recordcell.helpers.OnPressScaleChangeTouchListener
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.RoundedImageView
import com.giz.recordcell.widgets.WaitProgressDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_person.*
import java.util.*
import kotlin.math.max

class PersonFragment : PreferenceFragmentCompat() {

    companion object {
        private const val TAG = "PersonFragment"
        private const val AVATAR_IMAGE_REQUEST_CODE = 0
        private const val HEADER_BG_IMAGE_REQUEST_CODE = 1
        fun newInstance() = PersonFragment()

        private fun printLog(msg: String) = Log.d(TAG, msg)
    }

    private lateinit var mainActivity: MainActivity
    private lateinit var currentUser: RecordUser

    private lateinit var avatarImg: RoundedImageView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        currentUser = BmobUser.getCurrentUser(RecordUser::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_person, container, false).apply {
        val settingView = super.onCreateView(inflater, container, savedInstanceState)
        this.findViewById<FrameLayout>(R.id.person_setting_container).addView(settingView)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 头像
        avatarImg = view.findViewById(R.id.person_avatar)
        avatarImg.post { fetchAvatarImg() }
        person_username.text = currentUser.username
        person_introduction.text = currentUser.introduction
        fetchHeaderBgImg()

        avatarImg.setOnTouchListener(OnPressScaleChangeTouchListener())
        avatarImg.setOnLongClickListener {
            // 更改头像
            queryPhonePhotos(AVATAR_IMAGE_REQUEST_CODE)
            true
        }
        person_edit_header_bg_btn.setOnTouchListener(OnPressScaleChangeTouchListener(0.8f))
        person_edit_header_bg_btn.setOnClickListener {
            queryPhonePhotos(HEADER_BG_IMAGE_REQUEST_CODE)
        }
        person_logout_btn.setOnTouchListener(OnPressScaleChangeTouchListener(0.8f))
        person_logout_btn.setOnClickListener {
            // 登出
            MaterialAlertDialogBuilder(mainActivity)
                .setTitle("确认退出登录吗？")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok){ _, _ ->
                    BmobUser.logOut()
                    // 清除所有闹钟
                    stopAlarms()
                    startActivity(LoginActivity.newIntent(mainActivity))
                    mainActivity.finish()
                }.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == AVATAR_IMAGE_REQUEST_CODE || requestCode == HEADER_BG_IMAGE_REQUEST_CODE) {
            val uri = data?.data
            val path = FileUriUtils.getFilePathByUri(mainActivity, uri)
            printLog(path ?: "NULL")
            if (path != null) {
                if(requestCode == AVATAR_IMAGE_REQUEST_CODE){
                    updateAvatarImg(path)
                }else if(requestCode == HEADER_BG_IMAGE_REQUEST_CODE){
                    updateHeaderBgImg(path)
                }
            } else {
                mainActivity.showToast("未获取到图片")
                printLog(data.toString())
            }
        }
    }

    // 创建设置菜单
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        printLog("创建设置菜单")
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if(preference?.key == "record_home_function_list") {
            // 主页功能列表排序
            FunctionListOrderDialog(requireContext(), currentUser)
        }
        return super.onPreferenceTreeClick(preference)
    }

    // 获取头像图片
    private fun fetchAvatarImg() {
        printLog("头像网址：${currentUser.avatar}。${avatarImg.width}")
        WebImageUtils.downloadImage(mainActivity, currentUser.avatar, object : Response.Listener<Bitmap> {
            override fun onResponse(response: Bitmap?) {
                if(response != null){
                    printLog("获得图片：${response.width}, ${response.height}")
                    avatarImg.setImageBitmap(response)
                }
            }
        }, avatarImg.width, avatarImg.height)
    }

    // 获取背景图
    private fun fetchHeaderBgImg() {
        WebImageUtils.downloadImage(mainActivity, currentUser.headerBg, object : Response.Listener<Bitmap> {
            override fun onResponse(response: Bitmap?) {
                if(response != null){
                    printLog("获得图片：${response.width}, ${response.height}")
                    person_header_bg.setImageBitmap(response)
                }
            }
        })
    }

    /**
     * 更新头像
     * @param path 本地图片路径
     */
    private fun updateAvatarImg(path: String) {
        if(!FileUriUtils.judgeFileSize(path, 1)){
            // 大于1M
            mainActivity.showToast("图片不能大于1M")
            return
        }
        val waitDialog = WaitProgressDialog(mainActivity).apply { show() }
        Thread(Runnable {
            try {
                // 更改头像
                val imgObject = WebImageUtils.uploadImage(path)
                if(imgObject.getBoolean("success")){
                    val imgUrl = imgObject.getJSONObject("data").getString("url")
                    val hash = imgObject.getJSONObject("data").getString("hash")
                    printLog("新头像网址：$imgUrl, hash: $hash")
                    if(currentUser.avatarHash != WebImageUtils.DEFAULT_AVATAR_HASH){ // 旧头像不是默认头像，删除原先的头像
                        printLog("删除头像Hash: ${currentUser.avatarHash}")
                        WebImageUtils.deleteImage(currentUser.avatarHash)
                    }
                    currentUser.avatar = imgUrl
                    currentUser.avatarHash = hash
                    currentUser.update(object : UpdateListener() {
                        override fun done(p0: BmobException?) {
                            waitDialog.dismiss()
                            if(p0 == null){
                                mainActivity.showToast("更改头像成功")
                                fetchAvatarImg()
                            }else{
                                mainActivity.showToast("更改失败：$p0")
                                printLog(p0.toString())
                            }
                        }
                    })
                }else{
                    waitDialog.dismiss()
                    if(imgObject.has("code") && imgObject.getString("code") == "image_repeated"){
                        mainActivity.showToast("头像修改失败：图片已存在")
                    }else if(imgObject.has("errorMsg")){
                        mainActivity.showToast(imgObject.getString("errorMsg"))
                    }else{
                        mainActivity.showToast("头像修改失败")
                    }
                }
            } catch (e: Exception) {
                waitDialog.dismiss()
                printLog(e.toString())
                mainActivity.showToast("头像修改失败：未知错误")
            }
        }).start()
    }

    /**
     * 更新背景图
     * @param path 本地图片路径
     */
    private fun updateHeaderBgImg(path: String){
        if(!FileUriUtils.judgeFileSize(path, 2)){
            // 大于1M
            mainActivity.showToast("图片不能大于2M")
            return
        }
        val waitDialog = WaitProgressDialog(mainActivity).apply { show() }
        Thread(Runnable {
            try {
                // 更改头像
                val imgObject = WebImageUtils.uploadImage(path)
                if(imgObject.getBoolean("success")){
                    val imgUrl = imgObject.getJSONObject("data").getString("url")
                    val hash = imgObject.getJSONObject("data").getString("hash")
                    printLog("新背景图网址：$imgUrl, hash: $hash")
                    if(currentUser.headerBgHash != WebImageUtils.DEFAULT_HEADER_BG_HASH){ // 旧背景图不是默认背景，删除原先的背景
                        printLog("删除头像Hash: ${currentUser.headerBgHash}")
                        WebImageUtils.deleteImage(currentUser.headerBgHash)
                    }
                    currentUser.headerBg = imgUrl
                    currentUser.headerBgHash = hash
                    currentUser.update(object : UpdateListener() {
                        override fun done(p0: BmobException?) {
                            waitDialog.dismiss()
                            if(p0 == null){
                                mainActivity.showToast("更改背景图成功")
                                fetchHeaderBgImg()
                            }else{
                                mainActivity.showToast("更改失败：$p0")
                                printLog(p0.toString())
                            }
                        }
                    })
                }else{
                    waitDialog.dismiss()
                    if(imgObject.has("code") && imgObject.getString("code") == "image_repeated"){
                        mainActivity.showToast("背景修改失败：图片已存在")
                    }else if(imgObject.has("errorMsg")){
                        mainActivity.showToast(imgObject.getString("errorMsg"))
                    }else{
                        mainActivity.showToast("背景修改失败")
                    }
                }
            } catch (e: Exception) {
                waitDialog.dismiss()
                printLog(e.toString())
                mainActivity.showToast("背景修改失败：未知错误")
            }
        }).start()
    }

    private fun queryPhonePhotos(requestCode: Int){
        val imgIntent = Intent(Intent.ACTION_GET_CONTENT)
        imgIntent.type = "image/*"
        startActivityForResult(imgIntent, requestCode)
    }

    private fun stopAlarms() {
        val todoQuery = BmobQuery<Todo>()
        todoQuery.addWhereEqualTo("user", currentUser)
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
                            if(time.after(nowTime)){ // 已过时间不取消
                                TodoRemindReceiver.stopAlarm(mainActivity, todo.requestCode)
                            }
                        }
                    }
                }
            }
        })
    }
}