package com.giz.recordcell.widgets

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.giz.android.toolkit.dp2px
import com.giz.recordcell.R
import kotlin.math.min

// 带阴影的方形按钮，可以添加图标
class ShadeView(context: Context,
                  attrs: AttributeSet?,
                  defStyleAttr: Int) : View(context, attrs, defStyleAttr) {

    companion object {
        private val TAG = MainShadeAddButton::class.java.name
    }

    constructor(context: Context): this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    // 实心和阴影笔刷
    private val mSolidPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val mShadePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val mMaskPaint = Paint().also { it.color = Color.argb(48,0, 0, 0) }
    // 自定义样式
    private var shadeColor: Int = Color.GRAY
    private var _solidColor: Int = Color.BLACK
    private var shadeLength: Float = 0f
    private var cornerRadius: Float = 0f
    private var yOffset: Float = 0f
    // 给阴影长度添加额外的空间，增加美观度
    private var extraShadeLength = dp2px(context, 1f)

    var showMask = false
        set(value) {
            field = value
            invalidate()
        }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShadeView, defStyleAttr, 0)

        shadeColor = typedArray.getColor(R.styleable.ShadeView_shadeColor, Color.GRAY)
        _solidColor = typedArray.getColor(R.styleable.ShadeView_solidColor, Color.BLACK)
        shadeLength = typedArray.getDimension(R.styleable.ShadeView_shadeLength, dp2px(context, 8f))
        cornerRadius = typedArray.getDimension(R.styleable.ShadeView_cornerRadius, dp2px(context, 16f))
        yOffset = typedArray.getDimension(R.styleable.ShadeView_yOffset, 0f)

        typedArray.recycle()

        extraShadeLength += yOffset / 2
    }
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        clipToOutline = false

        mSolidPaint.color = _solidColor

        with(mShadePaint) {
            if(yOffset > 0f){
                maskFilter = BlurMaskFilter(shadeLength, BlurMaskFilter.Blur.NORMAL)
            }else{
                maskFilter = BlurMaskFilter(shadeLength, BlurMaskFilter.Blur.OUTER)
            }
            color = shadeColor
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // 判断阴影长度
        val maxL = min(height, width)
        if((shadeLength + extraShadeLength) * 2 > maxL) {
            shadeLength = maxL / 4f
        }
        // 保证圆角最大为实心高度的一半
        val radius = height / 2f - shadeLength - extraShadeLength
        cornerRadius = min(cornerRadius, radius)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sl = shadeLength + extraShadeLength
        // 绘制阴影
        if(yOffset > 0){
            canvas.drawRoundRect(sl, sl + yOffset, width - sl, height - sl + yOffset,
                cornerRadius, cornerRadius, mShadePaint)
        }else{
            canvas.drawRoundRect(sl, sl, width - sl, height - sl,
                cornerRadius, cornerRadius, mShadePaint)
        }
        // 绘制实心部分
        val sll = sl - 2f
        canvas.drawRoundRect(sll, sll, width - sll, height - sll,
            cornerRadius, cornerRadius, mSolidPaint)
        // 绘制遮罩
        if(showMask) {
            canvas.drawRoundRect(sll, sll, width - sll, height - sll,
                cornerRadius, cornerRadius, mMaskPaint)
        }
    }

//    fun showMask(show: Boolean) {
//        showMask = show
//        invalidate()
//    }

}