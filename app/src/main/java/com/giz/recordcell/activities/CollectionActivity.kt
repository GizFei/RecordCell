package com.giz.recordcell.activities

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.transition.Fade
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import cn.bmob.v3.Bmob
import cn.bmob.v3.BmobUser
import com.giz.android.toolkit.fetchHtml
import com.giz.android.toolkit.isUrl
import com.giz.android.toolkit.logAllContent
import com.giz.recordcell.R
import com.giz.recordcell.bmob.APPLICATION_ID
import com.giz.recordcell.bmob.CollectionItem
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.data.ShareContentParser
import com.giz.recordcell.data.SourceApp
import com.giz.recordcell.helpers.printLog
import com.giz.recordcell.helpers.showToast
import kotlinx.android.synthetic.main.activity_collection.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern


class CollectionActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_CLIPTEXT = "ClipText" // 来自剪贴板的文本

        fun newIntent(context: Context, text: String) = Intent(context, CollectionActivity::class.java)
            .apply {
                putExtra(EXTRA_CLIPTEXT, text)
            }
    }

    private var mCollectionItem: CollectionItem? = null
    private val currentUser: RecordUser by lazy { BmobUser.getCurrentUser(RecordUser::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bmob.initialize(this, APPLICATION_ID)
        setContentView(R.layout.activity_collection)

        window.enterTransition = Fade()
        window.exitTransition = Fade()
    }

    override fun onResume() {
        super.onResume()
        // 布局动画
        collection_ll.animate().translationY(0f).setDuration(800).start()
        AnimationUtils.loadAnimation(this, R.anim.progress_rotate_forever).apply {
            interpolator = FastOutSlowInInterpolator()
        }.also { collection_progress.startAnimation(it) }

        collectItem()
    }

    /**
     * 收藏的主函数
     */
    private fun collectItem() {
        if(intent != null){
            intent.apply {
                if(intent.action != null && intent.action == Intent.ACTION_SEND
                    && intent.clipData != null){
                    // 来自“更多”的分享
                    val shareContent = intent.clipData!!.getItemAt(0).text.toString()

                    mCollectionItem = ShareContentParser.formCollectionItem(shareContent)
                    if(mCollectionItem != null){
                        if(mCollectionItem?.sourceApp != SourceApp.UNKNOWN._name){
                            // 获取网页内容，抓取封面图片网址，和其他细节
                            mCollectionItem!!.user = currentUser
                            CollectionUrlTask().execute(mCollectionItem!!.url)
                        }else{
                            showToast("未知来源")
                            finish()
                        }
                    }else{
                        showToast("内容分析失败")
                        finish()
                    }
                }else if(intent.hasExtra(EXTRA_CLIPTEXT)){
                    // 来自剪贴板的文本
                    fetchClipboardText(intent.getStringExtra(EXTRA_CLIPTEXT))
                }else{
                    showToast("来源不是网址或未知")
                    finish()
                }
            }
        }else{
            showToast("Intent为空")
            finish()
        }
    }

    private inner class CollectionUrlTask : AsyncTask<String, Unit, String>() {
        override fun doInBackground(vararg params: String): String? {
            // 抓取网页内容
            try {
                val connection = getAvailableConnection(params[0].trim())
                if(connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val result = fetchHtml(connection)
                    if(mCollectionItem!!.sourceApp == SourceApp.DouBan._name
                        && mCollectionItem!!.url.contains("dispatch")){
                        // 来自手机端的豆瓣特殊处理
                        Pattern.compile("(?:.*)h5url : '(.*)'.replace(?:.*)").matcher(result).apply {
                            if(find()){
                                // 新的网址
                                val newUrl = group(1).replace("&amp;", "&")
                                    .replace("m.douban.com", "www.douban.com")
                                val newConnection = getAvailableConnection(newUrl)
                                mCollectionItem?.url = newUrl
                                if(newConnection.responseCode == HttpURLConnection.HTTP_OK){
                                    return fetchHtml(newConnection)
                                }
                            }
                        }
                    }else{
                        return result
                    }
                }else{
                    printLog("doInBackground: 网络连接错误")
                }
            } catch (e: Exception) {
                printLog("网络连接错误：${e.message}")
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            // 处理网页内容-抓取图片链接或是标题
            if(result == null){
                printLog("onPostExecute: 空Html")
                finish()
                return
            }
            val document = Jsoup.parse(result)
            when(SourceApp.getSourceAppFromName(mCollectionItem!!.sourceApp)){
                SourceApp.ZhiHu -> fetchZhiHuCoverUrlAndTitle(document)
                SourceApp.HuPu -> fetchHuPuCoverUrlAndTitle(document)
                SourceApp.DouBan -> fetchDouBanCoverUrlAndTitle(document)
                SourceApp.WeiBo -> fetchWeiBoCoverUrlAndTitle(document)
                SourceApp.WeiXin -> fetchWeiXinCoverUrlAndTitle(document)
                SourceApp.UNKNOWN -> {
                    showToast("未知来源")
                    failToCollect()
                }
            }
        }
    }

    /**
     * 以下函数获得不同App文章的封面图片链接（使用文章中出现的第一张图片），以及获取标题，还有富文本内容以及作者信息
     */
    // 知乎
    private fun fetchZhiHuCoverUrlAndTitle(document: Document) {
        val isZhuanLan = mCollectionItem!!.url.contains("zhuanlan") // 是专栏还是回答
        // 作者信息
        fetchAvatarUrl(document, if(isZhuanLan) ".Post-Author .Avatar" else ".ContentItem-meta .Avatar")
        fetchAuthorName(document, if(isZhuanLan) ".Post-Author .AuthorInfo-name" else ".ContentItem-meta .AuthorInfo-name")
        // 富文本
        mCollectionItem!!.richContent = document.selectFirst(if(isZhuanLan) ".Post-RichText" else ".RichContent-inner")?.outerHtml()
            ?: "未抓取到知乎内容"
        if(needToFetchTitle()){ // 抓取标题
            val titleElm = document.selectFirst(if(isZhuanLan) ".Post-Title" else ".QuestionHeader-title")
            if(titleElm != null){
                mCollectionItem!!.title = titleElm.text()
            }else{
                mCollectionItem!!.title = if(isZhuanLan) "来自知乎的专栏文章" else "来自知乎的回答"
            }
        }
        if(isZhuanLan) {  // 专栏文章
            document.selectFirst(".TitleImage")?.let {
                if(it.hasAttr("src")){ // 是img标签
                    mCollectionItem!!.coverUrl = it.attr("src")
                    saveCollectionItem()
                    return
                }else{ // 是div标签，网址在style的url()中
                    Pattern.compile("(.*)url\\((.*)\\)").matcher(it.attr("style")).apply {
                        if(find()){
                            mCollectionItem!!.coverUrl = group(2)
                            saveCollectionItem()
                            return
                        }
                    }
                }
            }
        }else{ // 知乎回答
            fetchCoverUrlAndSaveItem(document, ".RichContent-inner")
        }
    }

    // 虎扑
    private fun fetchHuPuCoverUrlAndTitle(document: Document) {
        // 作者信息
        fetchAuthorName(document, ".detail-author .author-name")
        fetchAvatarUrl(document, ".detail-author img")
        mCollectionItem!!.richContent = document.selectFirst(".bbs-thread-content")?.outerHtml() ?: "未抓取到虎扑内容"
        if(needToFetchTitle()){
            mCollectionItem!!.title = document.selectFirst(".bbs-user-title")?.text() ?: "来自虎扑的文章"
        }
        fetchCoverUrlAndSaveItem(document, ".bbs-thread-content")
    }

    // 豆瓣
    private fun fetchDouBanCoverUrlAndTitle(document: Document){
        if(mCollectionItem!!.url.contains("note")){
            // 日记
            // 作者信息
            fetchAuthorName(document, ".note-author")
            fetchAvatarUrl(document, ".note_author_avatar")
            // 内容
            mCollectionItem!!.richContent = document.selectFirst("#link-report")?.outerHtml() ?: "未抓取到日记内容"
            if(needToFetchTitle()){
                mCollectionItem!!.title = "[日记]${document.selectFirst(".note-header h1")?.text() ?: "豆瓣日记"}"
            }
            fetchCoverUrlAndSaveItem(document, "#link-report")
        }else if(mCollectionItem!!.url.contains("status")){
            // 广播
            // 作者信息
            fetchAuthorName(document, ".hd .lnk-people")
            fetchAvatarUrl(document, ".hd .usr-pic img")
            // 内容
            val clsName = ".status-saying"
            if(document.selectFirst(clsName) != null){
                mCollectionItem!!.richContent = document.selectFirst(".status-saying")?.outerHtml() ?: "未抓取到广播内容"
                if(needToFetchTitle()){ // 标题
                    mCollectionItem!!.title = "「${document.selectFirst(".lnk-people").text()}」的广播"
                }
                fetchCoverUrlAndSaveItem(document, clsName)
            }else{
                logAllContent("CollectionActivity", document.outerHtml())

                val html = document.outerHtml()
                val idx1 = html.indexOf("application/ld+json")
                val idx2 = html.indexOf("<link rel=\"shortcut icon\"")
                val content = html.substring(idx1 + 21, idx2).replace("</script>", "").trim()
                printLog("豆瓣内容：$content")
                try {
                    val jsonObject = JSONObject(content)
                    mCollectionItem!!.richContent = jsonObject.getString("headline")
                    if(needToFetchTitle()){
                        mCollectionItem!!.title = "「${jsonObject.getJSONObject("author").getString("name")}」的广播"
                        if(jsonObject.getJSONObject("sharedContent").has("thumbnailUrl")){
                            mCollectionItem!!.coverUrl = jsonObject.getJSONObject("sharedContent").getString("thumbnailUrl")
                        }
                    }
                }catch (e: Exception) {
                    showToast("抓取失败")
                    failToCollect()
                }
            }
        }else{
            // 其他，电影，书籍之类的分享
            // 作者信息
            mCollectionItem!!.authorName = "豆瓣"
            mCollectionItem!!.richContent = document.selectFirst(".subjectwrap .subject")?.outerHtml() ?: "未抓取到其他内容"
            val intro = document.selectFirst("#link-report")?.apply { // 删除无关元素
                select(".short").remove()
                select("style").remove()
                select("script").remove()
                select(".report").remove()
            }?.outerHtml() ?: ""
            mCollectionItem!!.richContent += "<h3>简介</h3>$intro"
            if(needToFetchTitle()){
                val name = document.selectFirst("span[property=\"v:itemreviewed\"]").text()
                val catalog = ShareContentParser.getDouBanCatalog(mCollectionItem!!.url)
                mCollectionItem!!.title = catalog + name
            }
            fetchCoverUrlAndSaveItem(document, ".subjectwrap")
        }
    }

    // 微博
    private fun fetchWeiBoCoverUrlAndTitle(document: Document) {
        val html = document.outerHtml()
        val idx1 = html.indexOf("render_data")
        val idx2 = html.lastIndexOf("[0] || {}")
        val renderData = html.substring(idx1 + 14, idx2)
        try {
            val jsonArray = JSONArray(renderData)
            // 作者信息
            mCollectionItem!!.authorName = jsonArray.getJSONObject(0).getJSONObject("status")
                .getJSONObject("user").getString("screen_name")
            mCollectionItem!!.avatarUrl = jsonArray.getJSONObject(0).getJSONObject("status")
                .getJSONObject("user").getString("profile_image_url")
            // 内容
            var text = jsonArray.getJSONObject(0).getJSONObject("status").getString("text").let {
                "<div class=\"weibo-text\">$it</div>"
            }
            val pics = jsonArray.getJSONObject(0).getJSONObject("status").getJSONArray("pics")
            val picStr = StringBuilder()
            for (i in 0 until pics.length()) {
                picStr.append("<img src=\"${pics.getJSONObject(i).getJSONObject("large").getString("url")}\" />")
            }
            text += picStr.toString()
            printLog("微博内容：$text")
            mCollectionItem!!.richContent = text
            if(needToFetchTitle()){ // 标题
                mCollectionItem!!.title = jsonArray.getJSONObject(0).getJSONObject("status").getString("status_title")
            }
            mCollectionItem!!.coverUrl = jsonArray.getJSONObject(0).getJSONObject("status").getJSONArray("pics")
                .getJSONObject(0).getString("url")
            saveCollectionItem()
        }catch (e: JSONException) {
            printLog("微博：空的JsonArray")
            if(needToFetchTitle()){
                mCollectionItem!!.title = "微博正文"
            }
            saveCollectionItem()
        }
    }

    // 微信
    private fun fetchWeiXinCoverUrlAndTitle(document: Document) {
        // 作者信息
        fetchAuthorName(document, "#js_name")

        mCollectionItem!!.richContent = document.selectFirst(".rich_media_content")?.html() ?: "未抓取到微信内容"
        if(needToFetchTitle()){
            mCollectionItem!!.title = document.selectFirst(".rich_media_title")?.text() ?: "来自微信的文章"
        }
        fetchCoverUrlAndSaveItem(document, ".rich_media_content")
    }

    // 分析来自剪贴板的内容
    private fun fetchClipboardText(content: String) {
        val cont = content.trim()
        if(isUrl(cont, true)){
            // 来源是网址
            mCollectionItem = ShareContentParser.formCollectionItem(cont)
            // 抓取标题
            if(mCollectionItem != null){
                mCollectionItem!!.user = currentUser
                CollectionUrlTask().execute(cont)
            }else{
                showToast("无法识别的网址")
                failToCollect()
            }
        }else{
            showToast("不是网址")
            failToCollect()
        }
    }

    // 是否需要抓取标题。如果内容只是网址，则需要。通过判断收藏项的标题是否为空来决定
    private fun needToFetchTitle() = mCollectionItem!!.title.isEmpty()

    /**
     * 获取作者名
     * @param document 文档
     * @param authorNameSelector 包含作者名的元素的类名
     * @return 作者名
     */
    private fun fetchAuthorName(document: Document, authorNameSelector: String){
        val authorName = document.selectFirst(authorNameSelector)
        mCollectionItem!!.authorName = if(authorName != null){
            printLog("showUserInfo: username: ${authorName.outerHtml()}")
            authorName.text()
        } else{ "佚名" }
    }

    /**
     * 获取头像链接
     * @param document 文档
     * @param avatarSelector 包含头像的img元素的类名
     * @return 链接名
     */
    private fun fetchAvatarUrl(document: Document, avatarSelector: String) {
        val avatar = document.selectFirst(avatarSelector)
        mCollectionItem!!.avatarUrl = if(avatar != null){
            printLog("showUserInfo: avatar: ${avatar.outerHtml()}")
            avatar.attr("src")
        }else { "" }
    }

    /**
     * 抓取图片链接的统一函数
     * @param document 网页文档
     * @param clsName 包围img的父元素的类名
     */
    private fun fetchCoverUrlAndSaveItem(document: Document, clsName: String) {
        document.selectFirst(clsName).apply {
            if(this != null){
                run loop@ {
                    select("img").forEach {
                        var coverUrl = it.attr("src")
                        if(mCollectionItem!!.sourceApp == SourceApp.WeiXin._name){
                            // 微信比较特殊
                            coverUrl = it.attr("data-src")
                        }
                        // printLog("图片地址：$coverUrl, ${isUrl(coverUrl, true)}")
                        if(isUrl(coverUrl, true)){
                            // printLog("选中图片：$coverUrl")
                            mCollectionItem!!.coverUrl = coverUrl
                            return@loop // 相当于break
                        }
                    }
                }
                // 保存
                saveCollectionItem()
            }else{
                printLog("抓取图片失败：空Div")
                failToCollect()
            }
        }
    }

    // 将收藏项保存到云端
    private fun saveCollectionItem() {
        mCollectionItem?.saveCollection(this, { successToCollect() }, { failToCollect() })
    }

    /**
     * 处理重定向的问题，获取正确的连接
     * @param theUrl 网址
     * @return 连接
     * @throws Exception 异常
     */
    @Throws(Exception::class)
    private fun getAvailableConnection(theUrl: String): HttpURLConnection {
        val connection = URL(theUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect() // 连接
        val requestCode = connection.responseCode
        if (requestCode == HttpURLConnection.HTTP_MOVED_TEMP
            || requestCode == HttpURLConnection.HTTP_MOVED_PERM
        ) {
            printLog("getAvailableConnection: location: " + connection.getHeaderField("Location"))
            return getAvailableConnection(connection.getHeaderField("Location"))
        }
        return connection
    }

    /**
     * 收藏成功后，播放动画并结束活动
     */
    private fun successToCollect() {
        collection_progress.clearAnimation()
        val animDrawable = AnimatedVectorDrawableCompat.create(this, R.drawable.avd_collection_progress)
        collection_progress.setImageDrawable(animDrawable)
        collection_tv.text = "收藏成功"
        if(animDrawable != null){
            animDrawable.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    super.onAnimationEnd(drawable)
                    Handler().postDelayed(Runnable{
                        finish()
                    }, 800)
                }
            })
            animDrawable.start()
        }else{
            finish()
        }
    }

    /**
     * 显示失败信息并结束活动
     */
    private fun failToCollect() {
        collection_progress.clearAnimation()
        val animDrawable = AnimatedVectorDrawableCompat.create(this, R.drawable.avd_collection_fail)
        collection_progress.setImageDrawable(animDrawable)
        collection_tv.text = "收藏失败"
        if(animDrawable != null){
            animDrawable.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    super.onAnimationEnd(drawable)
                    Handler().postDelayed(Runnable{
                        finish()
                    }, 800)
                }
            })
            animDrawable.start()
        }else{
            finish()
        }
    }
}