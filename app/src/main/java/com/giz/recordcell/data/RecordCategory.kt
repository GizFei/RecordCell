package com.giz.recordcell.data

/**
 * 记录类型枚举
 * @param desc 中文描述文字
 */
enum class RecordCategory(val desc: String) {

    LITTLE_NOTE(CategoryDesc.DESC_LITTLE_NOTE),     // 小记（便签）
    TODO(CategoryDesc.DESC_TODO),                   // 待办
    TASKBOX(CategoryDesc.DESC_TASKBOX),
    SCHEDULE(CategoryDesc.DESC_SCHEDULE),
    DAILY(CategoryDesc.DESC_DAILY),
    ARTICLE(CategoryDesc.DESC_ARTICLE),
    FAVORITE(CategoryDesc.DESC_FAVORITE),

    ;

    companion object {
        var functionDescList: MutableList<String>? = null   // 主页功能排序

        private val categoryMap: HashMap<String, RecordCategory> = hashMapOf(
            CategoryDesc.DESC_LITTLE_NOTE to LITTLE_NOTE,
            CategoryDesc.DESC_TODO to TODO,
            CategoryDesc.DESC_TASKBOX to TASKBOX,
            CategoryDesc.DESC_SCHEDULE to SCHEDULE,
            CategoryDesc.DESC_DAILY to DAILY,
            CategoryDesc.DESC_ARTICLE to ARTICLE,
            CategoryDesc.DESC_FAVORITE to FAVORITE
        )

        fun getCategoryDescList() = arrayListOf(
            LITTLE_NOTE.desc,
            TODO.desc,
            TASKBOX.desc,
            SCHEDULE.desc,
            DAILY.desc,
            ARTICLE.desc,
            FAVORITE.desc
        )
        fun getAvailableCategoryDescList() = functionDescList ?: arrayListOf(
            DAILY.desc,
            LITTLE_NOTE.desc,
            TODO.desc,
            TASKBOX.desc,
            FAVORITE.desc
        )

        fun getCategoryFromDesc(desc: String): RecordCategory? = categoryMap[desc]
    }


    object CategoryDesc {
        const val DESC_LITTLE_NOTE = "便签" // 也即小记
        const val DESC_TODO = "待办"      // 无提醒时间或一个提醒时间点
        const val DESC_TASKBOX = "任务集" // 包含待办和便签
        const val DESC_SCHEDULE = "日程"  // 一个时间段的事件或全天事件
        const val DESC_DAILY = "日常"     // 即习惯，重复事项
        const val DESC_ARTICLE = "文章"   // 长篇文章
        const val DESC_FAVORITE = "收藏"  // 收藏文章（从第三方应用收藏的文章）
    }
}