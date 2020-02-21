package com.giz.recordcell.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.QueryListener
import com.android.volley.Response
import com.giz.android.toolkit.isUrl
import com.giz.recordcell.R
import com.giz.recordcell.bmob.CollectionItem
import com.giz.recordcell.bmob.WebImageUtils
import com.giz.recordcell.bmob.fetchDate
import com.giz.recordcell.data.SourceApp
import com.giz.recordcell.data.formatCollectedTimeText
import com.giz.recordcell.helpers.isUrl
import com.giz.recordcell.helpers.printLog
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.BottomArcBackgroundDrawable
import com.giz.recordcell.widgets.NestedScrollWebView
import com.giz.recordcell.widgets.WaitProgressDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_collection_detail.*
import org.jsoup.Jsoup

class CollectionDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_COLLECTION_ITEM = "CollectionExtra"

        fun newIntent(context: Context, collectionItem: CollectionItem) = Intent(context, CollectionDetailActivity::class.java).apply {
            putExtra(EXTRA_COLLECTION_ITEM, collectionItem)
        }
    }

    private lateinit var collectionItem: CollectionItem
    private lateinit var collectionWebView: NestedScrollWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection_detail)

        collectionItem = intent.getSerializableExtra(EXTRA_COLLECTION_ITEM) as CollectionItem
        collectionWebView = findViewById(R.id.collection_detail_viewer)
        fillBasicContent()

        val waitProgressDialog = WaitProgressDialog(this).apply { show() }
        val collectionQuery = BmobQuery<CollectionItem>()
        collectionQuery.getObject(collectionItem.objectId, object : QueryListener<CollectionItem>() {
            override fun done(p0: CollectionItem?, p1: BmobException?) {
                waitProgressDialog.dismiss()
                if(p1 == null && p0 != null){
                    collectionItem = p0
                    fillViewContent()
                }else{
                    showToast("查询收藏项失败")
                    printLog("查询收藏失败：$p1")
                }
            }
        })
    }

    // 填充基本内容
    private fun fillBasicContent() {
        // 弧形背景
        collection_detail_layout.background = BottomArcBackgroundDrawable.builder()
            .fgColor(resources.getColor(R.color.colorPrimary, null))
            .build()
        // 基本信息
        collection_detail_title.text = collectionItem.title
        collection_detail_date.text = formatCollectedTimeText(collectionItem.collectedTime.fetchDate())
        collection_detail_source_icon.setImageDrawable(getAppIcon(this, SourceApp.getAppPackageName(collectionItem.sourceApp)))
        collection_detail_app_bar.invalidate()
    }

    // 填充内容
    private fun fillViewContent() {
        // 作者信息和文章内容
        setAuthorInfo()
        showRichContent()
        // 底部工具栏监听
        collection_detail_bab.setNavigationOnClickListener { onBackPressed() }
        collection_detail_bab.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.collection_detail_share_btn -> {
                    val shareContent = "[${collectionItem.sourceApp}]${collectionItem.title} ${collectionItem.url}"
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareContent)
                    }.let { i -> startActivity(i) }
                }
                R.id.collection_detail_delete_btn -> {
                    MaterialAlertDialogBuilder(this, R.style.CustomDialog)
                        .setTitle("确定删除收藏[${collectionItem.title}]吗？")
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok){_, _ ->
                            collectionItem.deleteCollection(this){ finish() }
                        }
                        .show()
                }
            }
            true
        }
    }

    // 展示收藏内容
    private fun showRichContent() {
        // 去年标签之间的'\n\t'，不然会报错
        // logAllContent("CollectionDetail,处理后的文本：", processRichContent())
        setupWebView(processRichContent())
    }

    // 初始化网络视图
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(text: String) {
        with(collectionWebView){
            settings.javaScriptEnabled = true
            addJavascriptInterface(AndroidJsMethod(), "AndroidJs")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    collectionWebView.evaluateJavascript("javascript:setContent('$text')"){}
                }
            }

            loadUrl("file:///android_asset/collectionViewer.html")

            setOnLongClickListener {
                printLog("点击WebView")
                this.hitTestResult?.apply {
                    if(type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE){
                        showToast("图片链接：${extra}")
                    }
                }
                true
            }
        }
    }

    // 处理富文本
    private fun processRichContent() = when(SourceApp.getSourceAppFromName(collectionItem.sourceApp)){
        SourceApp.ZhiHu -> { // 知乎
            // 处理图片元素<img src="data:image/svg+settings;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\'
            // width=\'198\' height=\'136\'></svg>" data-rawwidth="198" data-rawheight="136" data-size="normal"
            // class="content_image lazy" width="198" data-actualsrc="https://pic4.zhimg.com/50/v2-d85ab32a1ce1a59a34e2df88a8b16f39_hd.jpg">
            val document = Jsoup.parse(collectionItem.richContent) // 把内容包裹起来，变成：<html><head></head><body>内容</body></html>
            document.select("img").forEach {
                if(!isUrl(it.attr("src"), true)){
                    if(it.attr("data-actualsrc").isUrl(true)){
                        it.attr("src", it.attr("data-actualsrc"))
                    }else{
                        it.remove()
                    }
                }
            }
            // printLog("知乎内页内容：${document.selectFirst("body").html()}")
            document.selectFirst("body").html().replace(Regex("[\\n\\r]+"), "")
            // collectionItem.richContent.replace(Regex("[\\n\\r]+"), "")
                // .replace("'", "\\'") 给标签中的单引号加斜杠，不会与最外层包裹的单引号引起冲突
        }
        SourceApp.DouBan -> { // 豆瓣
            val document = Jsoup.parse(collectionItem.richContent)
            document.select("a.view-large").remove() // 去掉“查看原图”的链接
            document.selectFirst("body").html().replace(Regex("[\\n\\r]+"), "")
        }
        SourceApp.WeiBo -> { // 微博
            val document = Jsoup.parse(collectionItem.richContent)
            document.select("img").forEach {
                if(!it.attr("src").isUrl()){
                    it.attr("src", "https:${it.attr("src")}")
                }
            }
            document.selectFirst("body").html().replace(Regex("[\\n\\r]+"), "")
        }
        SourceApp.WeiXin -> { // 微信
            val document = Jsoup.parse(collectionItem.richContent)
            document.select("iframe").forEach {// 处理视频
                if(it.hasAttr("data-src")){
                    val dataSrc = Uri.parse(it.attr("data-src"))
                    val newSrc = Uri.parse("https://v.qq.com/txp/iframe/player.html")
                        .buildUpon()
                        .appendQueryParameter("origin", "https://mp.weixin.qq.com")
                        .appendQueryParameter("vid", dataSrc.getQueryParameter("vid"))
                        .appendQueryParameter("autoplay", "false")
                        .appendQueryParameter("full", "false")
                        .appendQueryParameter("show1080p", "true")
                        .appendQueryParameter("isDebugIframe", "false")
                        .toString()
                    it.attr("src", newSrc)
                    it.attr("allowfullscreen", "true")
                    it.attr("frameborder", "0")
                }else{
                    it.remove()
                }
            }
            document.select("img").forEach {
                if(it.attr("data-src").isUrl(true)){
                    it.attr("src", it.attr("data-src"))
                    it.removeAttr("style") // 保证图片比例正确
                }
            }
            document.selectFirst("body").html().replace(Regex("[\\n\\r']+"), "")
        }
        else -> collectionItem.richContent.replace(Regex("[\\n\\r]+"), "")
    }

    // 在布局中填充作者信息
    private fun setAuthorInfo() {
        collection_detail_authorName.text = collectionItem.authorName
        if((collectionItem.sourceApp == SourceApp.DouBan._name && !collectionItem.url.contains("note")
            && !collectionItem.url.contains("status")) || (collectionItem.sourceApp == SourceApp.WeiXin._name))
        { // 豆瓣其他，微信
            // printLog("微信头像")
            collection_detail_avatar.setImageDrawable(getAppIcon(this, SourceApp.getAppPackageName(collectionItem.sourceApp)))
        }else{
            WebImageUtils.downloadImage(this, collectionItem.avatarUrl, object : Response.Listener<Bitmap> {
                override fun onResponse(response: Bitmap?) {
                    response?.let { collection_detail_avatar.setImageBitmap(response) }
                }
            }, collection_detail_avatar.width, collection_detail_avatar.height)
        }
    }

    // js调用android方法的接口
    private inner class AndroidJsMethod {
        @JavascriptInterface
        fun print(){}

        // 点击图片
        @JavascriptInterface
        fun imgClick(url: String){
            printLog("图片链接：$url")
            showToast("图片链接：$url")
        }
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

    override fun onBackPressed() {
        if(collectionWebView.canGoBack()){
            collectionWebView.goBack()
        }else{
            super.onBackPressed()
        }
    }
}