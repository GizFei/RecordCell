package com.giz.recordcell.bmob

import cn.bmob.v3.BmobUser
import com.giz.recordcell.data.RecordCategory

data class RecordUser(var introduction: String = "添加个人简介",
                      var avatar: String = WebImageUtils.DEFAULT_AVATAR_URL,
                      var avatarHash: String = WebImageUtils.DEFAULT_AVATAR_HASH,
                      var headerBg: String = WebImageUtils.DEFAULT_HEADER_BG_URL,
                      var headerBgHash: String = WebImageUtils.DEFAULT_HEADER_BG_HASH,
                      var functionListOrder: MutableList<String> = RecordCategory.getAvailableCategoryDescList()) : BmobUser()