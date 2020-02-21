package com.giz.recordcell.widgets

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.giz.recordcell.R

// 主界面中带阴影的方形按钮，可以添加图标，用于添加新记录
class MainShadeAddButton(context: Context,
                         attrs: AttributeSet?,
                         defStyleAttr: Int) : ImageView(context, attrs, defStyleAttr) {

    companion object {
        private val TAG = MainShadeAddButton::class.java.name
    }

    constructor(context: Context): this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    private val mShadePaint = Paint()
    private val mSolidPaint = Paint()
    // 阴影部分的长度
    private val mShadeRadius = context.resources.getDimension(R.dimen.shade_button_shade_length)
    // 阴影透明度
    private val mShadeAlpha = 200

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        val solidColor = context.resources.getColor(R.color.main_bottom_bar_add_button, context.theme)
        mSolidPaint.color = solidColor
        with(mShadePaint) {
            maskFilter = BlurMaskFilter(mShadeRadius, BlurMaskFilter.Blur.OUTER)
            color = Color.argb(mShadeAlpha, solidColor.red, solidColor.green, solidColor.blue)
        }
    }

    override fun onDraw(canvas: Canvas) {
        // 绘制阴影
        canvas.drawRect(mShadeRadius, mShadeRadius, width.toFloat(), height.toFloat(), mShadePaint)
        canvas.drawRect(mShadeRadius, mShadeRadius, width.toFloat(), height.toFloat(), mSolidPaint)
        super.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event?.action == MotionEvent.ACTION_DOWN){
            val solidColor = context.resources.getColor(R.color.main_bottom_bar_add_button_dark, context.theme)
            mSolidPaint.color = solidColor
            mShadePaint.color = Color.argb(mShadeAlpha, solidColor.red, solidColor.green, solidColor.blue)
            invalidate()
        }else {
            val solidColor = context.resources.getColor(R.color.main_bottom_bar_add_button, context.theme)
            mSolidPaint.color = solidColor
            mShadePaint.color = Color.argb(mShadeAlpha, solidColor.red, solidColor.green, solidColor.blue)
            invalidate()
        }
        return super.onTouchEvent(event)
    }

}