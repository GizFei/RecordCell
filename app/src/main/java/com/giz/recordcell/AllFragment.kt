package com.giz.recordcell

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import cn.bmob.v3.BmobUser
import com.giz.recordcell.activities.CollectionActivity
import com.giz.recordcell.activities.NewDailyItemActivity
import com.giz.recordcell.activities.NewLittleNoteActivity
import com.giz.recordcell.activities.NewTodoActivity
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.data.FloatingWindowService
import com.giz.recordcell.data.NewTaskBoxDialog
import com.giz.recordcell.data.RecordCategory
import com.giz.recordcell.helpers.CommonAdapter
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.ShadeImageButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_all.*

class AllFragment : Fragment() {

    companion object {
        private const val TAG = "AllFragment"
        private fun printLog(msg: String) = Log.d(TAG, msg)

        fun newInstance() = AllFragment()
    }

    private lateinit var mainActivity: MainActivity
    private val functionItemList = mutableListOf(
        FunctionItem(RecordCategory.LITTLE_NOTE.desc, R.drawable.ic_notebook, 0xFF26c6da.toInt(), 0x5C26c6da.toInt()),
        FunctionItem(RecordCategory.TODO.desc, R.drawable.ic_check, 0xFFffd200.toInt(), 0x5CFFD200.toInt()),
        FunctionItem(RecordCategory.TASKBOX.desc, R.drawable.ic_box, 0xFFbe63f9.toInt(), 0x5CBE63F9.toInt()),
        FunctionItem(RecordCategory.SCHEDULE.desc, R.drawable.ic_calendar, 0xFF26c6da.toInt(), 0x5C26c6da.toInt()),
        FunctionItem(RecordCategory.ARTICLE.desc, R.drawable.ic_paper, 0xFF26c6da.toInt(), 0x5C26c6da.toInt()),
        FunctionItem(RecordCategory.DAILY.desc, R.drawable.ic_brightness, 0xFFffd200.toInt(), 0x5CFFD200.toInt()),
        FunctionItem(RecordCategory.FAVORITE.desc, R.drawable.ic_folders, 0xFFbe63f9.toInt(), 0x5CBE63F9.toInt()),
        FunctionItem("悬浮便签编辑框", R.drawable.ic_notebook, 0xFF26c6da.toInt(), 0x5C26c6da.toInt())
    )

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_all, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        all_functions_recycler_view.adapter = object : CommonAdapter<FunctionItem>(mainActivity,
            functionItemList, R.layout.item_all_functions) {

            override fun bindData(holder: CommonViewHolder, data: FunctionItem, position: Int) {
                with(holder.itemView){
                    findViewById<ShadeImageButton>(R.id.item_function_icon).apply {
                        setImageResource(data.icon)
                        solidColor = data.bgColor
                        setShadeColor(data.shadeColor)
                        setOnClickListener {
                            onItemClick(data.name)
                        }
                    }
                    findViewById<TextView>(R.id.item_function_text).text = data.name
                }
            }
        }
    }

    private fun onItemClick(data: String) {
        if(data == "悬浮便签编辑框"){
            if(!Settings.canDrawOverlays(mainActivity)){
                mainActivity.showToast("请先开启权限")
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${mainActivity.packageName}")))
            }
            if(!FloatingWindowService.isStarted){
                mainActivity.startService(Intent(mainActivity, FloatingWindowService::class.java))
            }
            return
        }
        when(RecordCategory.getCategoryFromDesc(data)){
            RecordCategory.LITTLE_NOTE -> {
                startActivity(Intent(mainActivity, NewLittleNoteActivity::class.java))
            }
            RecordCategory.TODO -> {
                startActivity(NewTodoActivity.newIntent(mainActivity, NewTodoActivity.TODO_MODE_CREATE))
            }
            RecordCategory.TASKBOX -> {
                NewTaskBoxDialog(mainActivity, NewTaskBoxDialog.TASKBOX_MODE_CREATE,
                    BmobUser.getCurrentUser(RecordUser::class.java)).show()
            }
            RecordCategory.SCHEDULE -> {
                mainActivity.showToast("日程功能等待开发~")
            }
            RecordCategory.ARTICLE -> {
                mainActivity.showToast("文章功能等待开发~")
            }
            RecordCategory.DAILY -> {
                startActivity(Intent(mainActivity, NewDailyItemActivity::class.java))
            }
            RecordCategory.FAVORITE -> {
                collectClipboardText()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if(mainActivity.hasWindowFocus()){
            val rect = Rect()
            mainActivity.window.decorView.getWindowVisibleDisplayFrame(rect)
            setRootLayoutPaddingTop(rect.top)
        }
    }

    // 收藏来自剪贴板的内容
    private fun collectClipboardText() {
        val clipboard = mainActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if(clipboard != null){
            if(clipboard.hasPrimaryClip() && clipboard.primaryClip!!.itemCount > 0){
                val shareContent = clipboard.primaryClip!!.getItemAt(0).text.toString().trim()
                MaterialAlertDialogBuilder(mainActivity)
                    .setTitle("收藏来自剪贴板的内容")
                    .setMessage(shareContent)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok){ _, _ ->
                        startActivity(CollectionActivity.newIntent(mainActivity, shareContent))
                    }.show()
            }else{
                mainActivity.showToast("剪贴板没有内容")
            }
        }else{
            mainActivity.showToast("未获取到剪贴板")
        }
    }

    private var hasSetStatusBarHeight = false
    fun setRootLayoutPaddingTop(padding: Int) {
        if(!hasSetStatusBarHeight){
            printLog("更新Top：$padding")
            all_rootLayout.updatePadding(top = padding)
            hasSetStatusBarHeight = true
        }
    }

    data class FunctionItem(val name: String,
                            val icon: Int,
                            val bgColor: Int,
                            val shadeColor: Int)
}