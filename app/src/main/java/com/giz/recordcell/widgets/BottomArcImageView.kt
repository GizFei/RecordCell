package com.giz.recordcell.widgets

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import com.giz.android.toolkit.dp2px
import com.giz.recordcell.R
import kotlin.math.sqrt

class BottomArcImageView(context: Context,
                         attrs: AttributeSet?,
                         defStyleAttr: Int) : ImageView(context, attrs, defStyleAttr) {

    constructor(context: Context): this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    // 自定义属性
    private val arcPercent: Int // 默认16

    // 过程变量
    private val rectRect = RectF()
    private val arcRect = RectF()
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private var isShaderSet = false

    private val mShadePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private var shadeLength = 16f
    private var shadeColor = Color.LTGRAY

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BottomArcImageView, defStyleAttr, 0)
        arcPercent = typedArray.getInteger(R.styleable.BottomArcImageView_arcPercent, 16)
        shadeLength = typedArray.getDimension(R.styleable.BottomArcImageView_arcShadeLength, dp2px(context, 8f))
        shadeColor = typedArray.getColor(R.styleable.BottomArcImageView_arcShadeColor, Color.LTGRAY)

        typedArray.recycle()
    }
    init {
        with(mShadePaint) {
            color = shadeColor
            maskFilter = BlurMaskFilter(shadeLength, BlurMaskFilter.Blur.OUTER)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if(arcPercent in 0..100) {
            val h = arcPercent * 0.01f * height
            val w = width.toFloat()
            val b = w / (2f * sqrt(3f)) + h // 椭圆y轴半径
            val a = sqrt(3f * b * b * w * w / (12f * b * b - w * w)) // 椭圆x轴半径
            val arcLeft = left - (a - w / 2f)
            val arcRight = right + (a - w / 2f)
            val arcTop = bottom - shadeLength - 2f * b
            rectRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom - h - shadeLength)
            arcRect.set(arcLeft, arcTop, arcRight, bottom - shadeLength)
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawable ?: return
        if(drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0){
            return
        }
        if(imageMatrix == null && paddingTop == 0 && paddingLeft == 0){
            drawable.draw(canvas)
        }else {
            val saveCount = canvas.saveCount
            canvas.save()

            if(cropToPadding) {
                canvas.clipRect(scrollX + paddingLeft, scrollY + paddingTop,
                    scrollX + right - left - paddingRight,
                    scrollY + bottom - top - paddingBottom);
            }

            canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())

            if(!isShaderSet) {
                val bitmap = drawable2Bitmap(drawable)
                mPaint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                isShaderSet = true
            }
            canvas.drawArc(arcRect, 30f, 120f, false, mShadePaint)
            canvas.drawRect(rectRect, mPaint)
            canvas.drawArc(arcRect, 30f, 120f, false, mPaint)

            canvas.restoreToCount(saveCount)
        }
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        isShaderSet = false
    }

    private fun drawable2Bitmap(drawable: Drawable?): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if(drawable == null){
            return bitmap
        }

        val canvas = Canvas(bitmap)
        imageMatrix.let { canvas.concat(it) }
        drawable.draw(canvas)
        return bitmap
    }


}