package com.giz.recordcell.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.children
import androidx.viewpager.widget.ViewPager
import kotlin.math.max

class AutoHeightViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {

    constructor(context: Context): this(context, null)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var maxHeight = 0
        for(child in children) {
            child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            maxHeight = max(maxHeight, child.measuredHeight)
        }
        if(maxHeight > 0) {
            setMeasuredDimension(measuredWidth, maxHeight)
        }
    }
}