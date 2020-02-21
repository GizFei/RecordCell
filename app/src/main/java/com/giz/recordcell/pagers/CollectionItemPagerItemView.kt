package com.giz.recordcell.pagers

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.android.volley.Response
import com.giz.recordcell.R
import com.giz.recordcell.activities.CollectionDetailActivity
import com.giz.recordcell.bmob.*
import com.giz.recordcell.data.SourceApp
import com.giz.recordcell.data.formatCalendarText
import com.giz.recordcell.helpers.CommonMultiAdapter
import com.giz.recordcell.helpers.OnPressScaleChangeTouchListener
import com.giz.recordcell.helpers.printLog
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.RoundedImageView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

class CollectionItemPagerItemView(private val context: Context,
                                  private val user: RecordUser) : BaseRecyclerViewPagerItemView(context) {

    private var collectionAdapter: CommonMultiAdapter<Date, CollectionItem>? = null

    override fun updatePagerRecyclerView() {
        mSwipeRefreshLayout.isRefreshing = true

        BmobQuery<CollectionItem>().apply {
            addWhereEqualTo("user", user)
            addQueryKeys("title,url,sourceApp,coverUrl,collectedTime") // 不包含富文本项，保证带宽
            order("-collectedTime") // 按最新修改时间降序排列
        }.findObjects(object : FindListener<CollectionItem>() {
            override fun done(p0: MutableList<CollectionItem>?, p1: BmobException?) {
                mSwipeRefreshLayout.isRefreshing = false
                if(p1 == null){
                    p0 ?: return
                    updateAdapter(p0)
                }else{
                    if(p1.errorCode == 9016){
                        context.showToast("查询收藏项错误，请检查网络")
                    }else{
                        context.showToast("查询收藏项错误")
                    }
                    context.printLog("查询收藏项错误：$p1")
                }
            }
        })
    }

    private fun updateAdapter(list: MutableList<CollectionItem>) {
        val collectionPairs = mutableListOf<CommonMultiAdapter.MultiData>()
        list.groupBy { getYMDCalendarDate(it.collectedTime.fetchDate()) }.forEach {
            collectionPairs.add(CommonMultiAdapter.MultiData(it.key, CommonMultiAdapter.ViewType.HEADER))
            it.value.forEach { item ->
                collectionPairs.add(CommonMultiAdapter.MultiData(item, CommonMultiAdapter.ViewType.ITEM))
            }
        }
        if(collectionAdapter == null) {
            collectionAdapter = object : CommonMultiAdapter<Date, CollectionItem>(context, collectionPairs,
                R.layout.item_daily_header, R.layout.item_collection_item) {
                override fun bindHeaderData(holder: CommonMultiHeadHolder, data: Date, pos: Int) {
                    holder.itemView.findViewById<TextView>(R.id.daily_header_tv).text = formatCalendarText(data)
                }

                override fun bindItemData(holder: CommonMultiViewHolder, data: CollectionItem, pos: Int) {
                    with(holder.itemView){
                        findViewById<TextView>(R.id.collection_item_title).text = data.title
                        findViewById<ImageView>(R.id.collection_item_icon).setImageDrawable(
                            getAppIcon(context, SourceApp.getAppPackageName(data.sourceApp))
                        )
                        findViewById<RoundedImageView>(R.id.collection_item_coverImg).apply {
                            if(data.coverUrl.isEmpty()){
                                visibility = View.GONE
                            }else{
                                visibility = View.VISIBLE
                                WebImageUtils.downloadImage(context, data.coverUrl, object : Response.Listener<Bitmap> {
                                    override fun onResponse(response: Bitmap?) {
                                        if(response != null){
                                            setImageBitmap(response)
                                        }else{
                                            visibility = View.GONE
                                        }
                                    }
                                }, width, height)
                            }
                        }
                        setOnTouchListener(OnPressScaleChangeTouchListener())
                        setOnClickListener {
                            context.startActivity(CollectionDetailActivity.newIntent(context, data))
                        }
                        setOnLongClickListener {
                            showPopupMenu(data, this)
                            true
                        }
                    }
                }
            }
            mRecyclerView.adapter = collectionAdapter
        }else{
            collectionAdapter?.updateData(collectionPairs)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun showPopupMenu(item: CollectionItem, anchorView: View) {
        val popupMenu = PopupMenu(context, anchorView, Gravity.TOP).apply {
            menuInflater.inflate(R.menu.menu_collection_item, menu) // 填充菜单
            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.menu_collection_item_delete -> {
                        MaterialAlertDialogBuilder(context, R.style.CustomDialog)
                            .setTitle("确定删除收藏[${item.title}]吗？")
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok){_, _ ->
                                item.deleteCollection(context){ updatePagerRecyclerView() }
                            }
                            .show()
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

    /**
     * 获得应用的图标
     * @param context 上下文
     * @param packageName 应用包名
     * @return 图标Drawable
     */
    private fun getAppIcon(context: Context, packageName: String): Drawable? {
        try {
            val packageManager = context.packageManager
            val info = packageManager.getApplicationInfo(packageName, 0)
            return info.loadIcon(packageManager)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("CommonUtil", "getAppIcon: Name Not Found")
        }
        return null
    }

}