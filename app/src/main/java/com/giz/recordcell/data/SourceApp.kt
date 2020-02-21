package com.giz.recordcell.data

/**
 * 收藏文章的来源应用
 * @param name 中文名
 */
enum class SourceApp(val _name: String) {
    ZhiHu(AppName.App_ZhiHu),
    HuPu(AppName.App_HuPu),
    DouBan(AppName.App_DouBan),
    WeiBo(AppName.App_WeiBo),
    WeiXin(AppName.App_WeiXin),
    UNKNOWN(AppName.App_Unknown)
    ;

    companion object {
        // 获得应用的包名
        fun getAppPackageName(app: SourceApp) = when(app) {
            ZhiHu -> "com.zhihu.android"
            HuPu -> "com.hupu.games"
            DouBan -> "com.douban.frodo"
            WeiBo -> "com.sina.weibo"
            WeiXin -> "com.tencent.mm"
            UNKNOWN -> "com.giz.recorddemo"
        }
        fun getAppPackageName(source: String) = getAppPackageName(getSourceAppFromName(source))

        // 从名称获取来源App枚举之一
        fun getSourceAppFromName(name: String) = when(name){
            AppName.App_ZhiHu -> ZhiHu
            AppName.App_HuPu -> HuPu
            AppName.App_DouBan -> DouBan
            AppName.App_WeiBo -> WeiBo
            AppName.App_WeiXin -> WeiXin
            else -> UNKNOWN
        }
    }

    object AppName {
        const val App_ZhiHu = "知乎"
        const val App_HuPu = "虎扑"
        const val App_DouBan = "豆瓣"
        const val App_WeiBo = "微博"
        const val App_WeiXin = "微信"
        const val App_Unknown = "方格"
    }
}