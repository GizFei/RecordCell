package com.giz.recordcell.pagers

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.text.HtmlCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.datatype.BmobDate
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.giz.android.toolkit.dp2pxSize
import com.giz.recordcell.R
import com.giz.recordcell.bmob.LittleNote
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.data.TaskBoxesSelectionDialog
import com.giz.recordcell.data.formatNoteDateText
import com.giz.recordcell.helpers.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.*
import kotlin.math.min
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

class LittleNotePagerItemView(private val context: Context,
                              private val user: RecordUser
) : BaseRecyclerViewPagerItemView(context) {

    private var noteAdapter: CommonAdapter<LittleNote>? = null

    init {
        mRecyclerView.layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL)
        mRecyclerView.updatePadding(left = dp2pxSize(context, 8f), right = dp2pxSize(context, 8f))
    }

    private var onNoteItemClickListener: (LittleNote) -> Unit = {}

    fun setOnNoteItemClickListener(listener: (LittleNote) -> Unit){
        onNoteItemClickListener = listener
    }

    override fun updatePagerRecyclerView() {
        mSwipeRefreshLayout.isRefreshing = true

        val noteQuery = BmobQuery<LittleNote>()
        noteQuery.addWhereEqualTo("user", user)
        noteQuery.include("user,taskBox")
        noteQuery.order("-updatedAt") // 按最新修改时间降序排列
        noteQuery.findObjects(object : FindListener<LittleNote>() {
            override fun done(p0: MutableList<LittleNote>?, p1: BmobException?) {
                mSwipeRefreshLayout.isRefreshing = false
                if(p1 == null){
                    p0 ?: return
                    updateAdapter(p0)
                }else{
                    if(p1.errorCode == 9016){
                        context.showToast("查询便签错误，请检查网络")
                    }else{
                        context.showToast("查询便签错误")
                    }
                    context.printLog("查询便签错误：$p1")
                }
            }
        })
    }

    private fun updateAdapter(list: MutableList<LittleNote>) {
        if(noteAdapter == null){
            noteAdapter = object : CommonAdapter<LittleNote>(context, list, R.layout.item_little_note){
                override fun bindData(holder: CommonViewHolder, data: LittleNote, position: Int) {
                    with(holder.itemView){
                        val noImg = Jsoup.parse(data.content).apply {
                            select("img").forEach {
                                it.replaceWith(Element("span").apply { text("[图片]") })
                            }
                        }.body().html() // 替换图片标签
                        findViewById<TextView>(R.id.note_content_tv).text = // Compact：简洁的文本
                            HtmlCompat.fromHtml(noImg, HtmlCompat.FROM_HTML_MODE_COMPACT).trim()
                        findViewById<TextView>(R.id.note_updated_time_tv).text = formatNoteDateText(Date(BmobDate.getTimeStamp(data.updatedAt)))
                        setOnTouchListener(OnPressScaleChangeTouchListener())
                        setOnClickListener {
                            onNoteItemClickListener(data)
                        }
                        setOnLongClickListener {
                            showPopupMenu(data, this)
                            true
                        }
                    }
                }
            }
            mRecyclerView.adapter = noteAdapter
        }else{
            noteAdapter?.updateData(list)
        }
    }

    // 长按待办事项弹出菜单
    @Suppress("UNCHECKED_CAST")
    private fun showPopupMenu(note: LittleNote, anchorView: View) {
        val popupMenu = PopupMenu(context, anchorView).apply {
            menuInflater.inflate(R.menu.menu_littlenote_item, menu) // 填充菜单
            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.menu_note_delete -> {
                        val summary = HtmlCompat.fromHtml(note.content, 0).toString()
                            .replace(Regex("\\s+"), "").let {
                                it.substring(0, min(10, it.length))
                            }
                        MaterialAlertDialogBuilder(context, R.style.CustomDialog)
                            .setTitle("确定删除便签[$summary...]吗？")
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok){_, _ ->
                                note.deleteNote(context){ updatePagerRecyclerView() }
                            }
                            .show()
                    }
                    R.id.menu_note_add_to_taskbox -> {
                        TaskBoxesSelectionDialog(context, user, note.taskBox).apply {
                            setOnTaskBoxClickListener {
                                note.taskBox = it
                                note.updateNote(this@LittleNotePagerItemView.context){ updatePagerRecyclerView() }
                            }
                        }.showDialog()
                    }
                }
                true
            }
        }
        // 显示图标
        popupMenu.apply {
            val field = this::class.declaredMemberProperties.find { it.name == "mPopup" } as? KProperty1<PopupMenu, *>
            field?.apply {
                isAccessible = true
                val helper = get(popupMenu) as MenuPopupHelper
                helper.setForceShowIcon(true)
            }
        }.show()
    }

}