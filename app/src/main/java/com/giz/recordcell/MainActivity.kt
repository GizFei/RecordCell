package com.giz.recordcell

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.forEach
import cn.bmob.v3.Bmob
import cn.bmob.v3.BmobUser
import com.giz.recordcell.activities.*
import com.giz.recordcell.bmob.APPLICATION_ID
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.data.NewTaskBoxDialog
import com.giz.recordcell.data.RecordCategory
import com.giz.recordcell.helpers.OnPressScaleChangeTouchListener
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.NewRecordBottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 0

        private fun printLog(msg: String) = Log.d(TAG, msg)
    }

    private var homeFragment: HomeFragment? = null
    private var allFragment: AllFragment? = null
    private var personFragment: PersonFragment? = null

    private val bottomBarBtnElevation by lazy { this.resources.getDimension(R.dimen.main_bottom_bar_button_elevation) }
    private val currentUser: RecordUser by lazy { BmobUser.getCurrentUser(RecordUser::class.java) }

    private var numberOfRequestPermission: Int = 0
    private val newRecordDialog by lazy {
        NewRecordBottomSheetDialog(this).apply {
            onItemClickListener = object : NewRecordBottomSheetDialog.OnRecordCategoryItemClickListener {
                override fun onItemClick(rc: RecordCategory?) {
                    onNewRecordItemClick(rc)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 请求相关动态权限
        if(!requestDynamicPermissions()){
            start()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus){
            val rect = Rect()
            window.decorView.getWindowVisibleDisplayFrame(rect)
            homeFragment?.setToolbarHeight(rect.top)
            allFragment?.setRootLayoutPaddingTop(rect.top)
        }
    }

    private fun start() {
        // 初始化Bmob，并检查是否有用户登录
        Bmob.initialize(this, APPLICATION_ID)
        if(!BmobUser.isLogin()){
            startActivity(LoginActivity.newIntent(this))
            finish()
        } else {
            setContentView(R.layout.activity_main)

            main_bottom_bar_home_btn.setOnClickListener(this)
            main_bottom_bar_home_btn.setOnTouchListener(OnPressScaleChangeTouchListener())
            main_bottom_bar_all_btn.setOnClickListener(this)
            main_bottom_bar_all_btn.setOnTouchListener(OnPressScaleChangeTouchListener())
            main_bottom_bar_person_btn.setOnClickListener(this)
            main_bottom_bar_person_btn.setOnTouchListener(OnPressScaleChangeTouchListener())
            main_bottom_bar_add_btn.setOnClickListener(this)

            setSelectBottomBarBtn(1)
            setFragment(1)
        }
    }

    private fun requestDynamicPermissions(): Boolean {
        val permissions = arrayListOf<String>()
        if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if(permissions.isNotEmpty()){
            numberOfRequestPermission = permissions.size
            requestPermissions(permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == PERMISSION_REQUEST_CODE) {
            if(grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // 权限通过
                start()
            }else{
                showToast(R.string.no_permission_text)
                finish()
            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    /**
     * 设置当前[Fragment]
     * @param i: 1, 2, 3
     */
    private fun setFragment(i: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        if (allFragment != null){
            transaction.hide(allFragment!!)
        }
        if (homeFragment != null){
            transaction.hide(homeFragment!!)
        }
        if (personFragment != null){
            transaction.hide(personFragment!!)
        }
        when(i) {
            1 -> {
                if(homeFragment == null){
                    homeFragment = HomeFragment.newInstance().also {
                        transaction.add(R.id.main_fragment_container, it)
                    }
                }
                transaction.show(homeFragment!!)
            }
            2 -> {
                if(allFragment == null){
                    allFragment = AllFragment.newInstance().also {
                        transaction.add(R.id.main_fragment_container, it)
                    }
                }
                transaction.show(allFragment!!)
            }
            3 -> {
                if(personFragment == null){
                    personFragment = PersonFragment.newInstance().also {
                        transaction.add(R.id.main_fragment_container, it)
                    }
                }
                transaction.show(personFragment!!)
            }
        }
        transaction.commit()
    }

    /**
     * 设置选中的底部工具栏按钮，改变其状态
     * @param pos 1, 2, 3
     */
    private fun setSelectBottomBarBtn(pos: Int) {
        main_bottom_bar_btn_container.forEach {
            it.elevation = 0f
            it.isSelected = false
        }
        with(main_bottom_bar_btn_container.getChildAt(pos - 1)){
            isSelected = true
            elevation = bottomBarBtnElevation
        }
    }

    // 底部工具栏按钮点击事件
    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.main_bottom_bar_home_btn -> {
                setSelectBottomBarBtn(1)
                setFragment(1)
            }
            R.id.main_bottom_bar_all_btn -> {
                setSelectBottomBarBtn(2)
                setFragment(2)
            }
            R.id.main_bottom_bar_person_btn -> {
                setSelectBottomBarBtn(3)
                setFragment(3)
            }
            R.id.main_bottom_bar_add_btn -> {
                showNewRecordBottomDialog()
            }
        }
    }

    /**
     * 弹出添加新记录的底部对话框
     */
    private fun showNewRecordBottomDialog() {
        newRecordDialog.show()
    }

    private fun onNewRecordItemClick(rc: RecordCategory?) = when(rc) {
        RecordCategory.LITTLE_NOTE -> {
            startActivity(Intent(this, NewLittleNoteActivity::class.java))
        }
        RecordCategory.TODO -> {
            startActivity(NewTodoActivity.newIntent(this, NewTodoActivity.TODO_MODE_CREATE))
        }
        RecordCategory.TASKBOX -> {
            NewTaskBoxDialog(this, NewTaskBoxDialog.TASKBOX_MODE_CREATE, currentUser).apply {
                setOnNewTaskBoxDialogDismissListener {
                    homeFragment?.taskBoxPagerItemView?.updatePagerRecyclerView()
                }
            }.show()
        }
        RecordCategory.DAILY -> {
            startActivity(Intent(this, NewDailyItemActivity::class.java))
        }
        RecordCategory.SCHEDULE -> {
            // startActivity(Intent(this, DailyInCalendarActivity::class.java))
        }
        RecordCategory.FAVORITE -> {
            collectClipboardText()
        }
        else -> Unit
    }

    // 收藏来自剪贴板的内容
    private fun collectClipboardText() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if(clipboard != null){
            if(clipboard.hasPrimaryClip() && clipboard.primaryClip!!.itemCount > 0){
                val shareContent = clipboard.primaryClip!!.getItemAt(0).text.toString().trim()
                MaterialAlertDialogBuilder(this)
                    .setTitle("收藏来自剪贴板的内容")
                    .setMessage(shareContent)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok){ _, _ ->
                        startActivity(CollectionActivity.newIntent(this, shareContent))
                    }.show()
            }else{
                showToast("剪贴板没有内容")
            }
        }else{
            showToast("未获取到剪贴板")
        }
    }

    // 底部工具栏滚动显示与隐藏
    var isBottomBarScrolling = false
    var isBottomBarHidden = false
    fun bottomBarSlideUp() {
        if(!isBottomBarHidden){
            return
        }
        // printLog("显示BottomBar")
        isBottomBarScrolling = true
        main_bottom_bar.animate().translationY(0f).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                isBottomBarScrolling = false
                isBottomBarHidden = false
            }
        })
    }
    fun bottomBarSlideDown() {
        if(isBottomBarHidden){
            return
        }
        // printLog("隐藏BottomBar")
        isBottomBarScrolling = true
        main_bottom_bar.animate().translationY(main_bottom_bar.height.toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                isBottomBarScrolling = false
                isBottomBarHidden = true
            }
        })
    }
}
