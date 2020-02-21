package com.giz.recordcell.data

import android.util.Log
import cn.bmob.v3.datatype.BmobDate
import com.giz.android.toolkit.isUrl
import com.giz.recordcell.bmob.CollectionItem
import java.util.*
import java.util.regex.Pattern

object ShareContentParser {
    private fun printLog(msg: String) = Log.d("ShareContentParse", msg)

    /**
     * 解析分享内容的来源，只处理第三方App的分享，均包含链接
     * @param shareContent 分享内容文本
     * @return 来源App
     */
    private fun getSourceApp(shareContent: String) = if (shareContent.contains("zhihu.com")) { // 来源为知乎
        SourceApp.ZhiHu
    } else if (shareContent.contains("hupu.com")) { // 来源为虎扑
        SourceApp.HuPu
    } else if (shareContent.contains("douban.com")) { // 豆瓣
        SourceApp.DouBan
    } else if (shareContent.contains("weibo.cn")) { // 微博
        SourceApp.WeiBo
    } else if (shareContent.contains("weixin.qq.com")) { // 微信
        SourceApp.WeiXin
    } else { // 未知
        SourceApp.UNKNOWN
    }

    fun formCollectionItem(shareContent: String): CollectionItem? = when(getSourceApp(shareContent)){
        SourceApp.ZhiHu -> parseZhiHuContent(shareContent)
        SourceApp.HuPu -> parseHuPuContent(shareContent)
        SourceApp.DouBan -> parseDouBanContent(shareContent)
        SourceApp.WeiBo -> parseWeiBoContent(shareContent)
        SourceApp.WeiXin -> parseWeiXinContent(shareContent)
        SourceApp.UNKNOWN -> CollectionItem().apply { sourceApp = SourceApp.UNKNOWN._name }
    }

    // 解析来自知乎的分享内容
    private fun parseZhiHuContent(shareContent: String): CollectionItem? {
        val collectionItem = CollectionItem().apply {
            sourceApp = SourceApp.ZhiHu._name
            collectedTime = BmobDate(Date())
        }

        // 问题问答：https://www.zhihu.com/question/268998966/answer/600981522?utm_source=com.miui.notes&utm_medium=social&utm_oi=732317165998329856
        Pattern.compile("【(.*)】(.*)：(?:\\.{3}\\s|…\\s)(.*)（(.*)）").matcher(shareContent).apply {
            if(find() && groupCount() == 4){
                collectionItem.title = group(1)
                collectionItem.url = group(3)
                return collectionItem
            }
        }
        // 收藏专栏文章：https://zhuanlan.zhihu.com/p/57816934
        Pattern.compile("(.*)（分享自知乎网）(.*)").matcher(shareContent).apply {
            if(find() && groupCount() == 2){
                collectionItem.title = group(1)
                collectionItem.url = group(2)
                return collectionItem
            }
        }
        // 是网址
        if(isUrl(shareContent, true)){
            collectionItem.url = shareContent
            return collectionItem
        }
        return null
    }

    // 解析来自虎扑的分享内容
    private fun parseHuPuContent(shareContent: String): CollectionItem? {
        val collectionItem = CollectionItem().apply {
            sourceApp = SourceApp.HuPu._name
            collectedTime = BmobDate(Date())
        }

        // 只有网址
        if(isUrl(shareContent, true)){
            collectionItem.url = shareContent
            return collectionItem
        }
        // 分享内容
        Pattern.compile("(.*)(http|https)(://.*)").matcher(shareContent).apply {
            if(find() && groupCount() == 3) {
                collectionItem.title = group(1)
                collectionItem.url = group(2) + group(3)
                return collectionItem
            }
        }

        return null
    }

    // 解析来自豆瓣的分享内容
    private fun parseDouBanContent(shareContent: String): CollectionItem? {
        printLog("豆瓣分享内容：[$shareContent]")
        val collectionItem = CollectionItem().apply {
            sourceApp = SourceApp.DouBan._name
            collectedTime = BmobDate(Date())
        }
        if(shareContent.contains("豆瓣日记")){
            // 日记
            // [我要做征友界的泥石流 本人性别女，明年30，母胎单身。 很多人说我长得像周冬雨，
            // 但实际上并不像。 至今没能恋爱的原因是：既  | 豆瓣日记 https://www.douban.com/doubanapp/dispatch?uri=/note/700173726/&dt_platform=other&dt_dapp=1]
            Pattern.compile("(.*)豆瓣日记\\s*(.*)").matcher(shareContent).apply {
                if(find() && groupCount() == 2) {
                    collectionItem.url = group(2)
                    return collectionItem
                }
            }
        }
        if(shareContent.contains("豆瓣评分")){
            // 分享电影/图片等评分项目
            // [《一吻定情》豆瓣评分:5.3(21947人评分)  https://www.douban.com/doubanapp/dispatch/movie/30263995?dt_platform=other&dt_dapp=1]
            Pattern.compile("(《.*》)豆瓣评分(?:.*)\\s+(.*)").matcher(shareContent).apply {
                if(find() && groupCount() == 2) {
                    collectionItem.title = getDouBanCatalog(group(2)) + group(1)
                    collectionItem.url = group(2)
                    return collectionItem
                }
            }
        }
        if(shareContent.split(",").size == 2){
            // 广播
            // [沥青博士 ,  https://www.douban.com/doubanapp/dispatch?uri=/status/2246396150/&dt_platform=other&dt_dapp=1]
            val text = shareContent.split(",")
            collectionItem.title = "「${text[0]}」的广播"
            collectionItem.url = text[1].trim()
            return collectionItem
        }
        // 只是网址
        if(isUrl(shareContent, true)){
            collectionItem.url = shareContent
            return collectionItem
        }

        return null
    }

    private fun parseWeiBoContent(shareContent: String): CollectionItem? {
        // 微博 https://m.weibo.cn/2286908003/4346029334055046
        return CollectionItem().apply {
            sourceApp = SourceApp.WeiBo._name
            collectedTime = BmobDate(Date())
            url = shareContent
        }
    }

    private fun parseWeiXinContent(shareContent: String): CollectionItem? {
        // 微信 https://mp.weixin.qq.com/s/lfyeyvQqQydLZ4IQU6Wf-Q
        return CollectionItem().apply {
            sourceApp = SourceApp.WeiXin._name
            collectedTime = BmobDate(Date())
            url = shareContent
        }
    }

    fun getDouBanCatalog(url: String): String {
        val builder = StringBuilder()
        builder.append("[")
        if (url.contains("game")) {
            builder.append("游戏")
        } else if (url.contains("book")) {
            builder.append("书籍")
        } else if (url.contains("movie")) {
            builder.append("电影|电视|综艺")
        } else if (url.contains("music")) {
            builder.append("音乐")
        } else {
            builder.append("其他")
        }
        builder.append("] ")
        return builder.toString()
    }
}