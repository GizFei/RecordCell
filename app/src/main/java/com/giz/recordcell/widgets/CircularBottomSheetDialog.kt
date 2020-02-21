package com.giz.recordcell.widgets

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.updateMargins
import com.giz.android.toolkit.dp2pxSize
import com.giz.android.toolkit.getColorStateList
import com.giz.recordcell.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

// 圆角底页
open class CircularBottomSheetDialog(context: Context, style: Int = R.style.BottomSheetDialog) : BottomSheetDialog(context, style){

    // 显示时完全展开
    fun enableExpanded() {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // 添加顶部灰色指示条
    fun setContentViewWithTopIndicator(view: View) {
        val topView = View(context).apply{
            layoutParams = LinearLayout.LayoutParams(dp2pxSize(context, 48f), dp2pxSize(context, 5f)).apply {
                updateMargins(bottom = dp2pxSize(context, 8f))
                gravity = Gravity.CENTER
            }
            setBackgroundResource(R.drawable.bg_round_corner)
            backgroundTintList = getColorStateList(Color.rgb(216, 216, 216))
        }
        val ll = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(topView)
            addView(view)
        }
        setContentView(ll)
    }

}