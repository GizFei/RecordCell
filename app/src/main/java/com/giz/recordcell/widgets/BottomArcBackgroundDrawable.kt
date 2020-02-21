package com.giz.recordcell.widgets

import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import androidx.annotation.ColorInt
import kotlin.math.sqrt


class BottomArcBackgroundDrawable private constructor(private val builder: BottomArcBuilder) :
    ShapeDrawable(builder.shape) {

    companion object {
        private const val TAG = "BottomArc"
        fun builder() = BottomArcBuilder()
    }

    /*
    底部为弧线的方形背景
     */
    private val mBgColor: Int // 背景色
    private val mFgColor: Int // 斜切部分的前景色
    private val mArcPercent: Int // 弧线所占矩形的高占侧边的比例，0-100

    init {
        mBgColor = builder.bgColor
        mFgColor = builder.fgColor
        mArcPercent = builder.arcPercent
        initPaint()
    }

    private var mBgPaint: Paint? = null
    private var mFgPaint: Paint? = null

    private fun initPaint() {
        // 放大图片以适应视图的尺寸

        mFgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = mFgColor
            isAntiAlias = true
        }
        mBgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = mBgColor
            isAntiAlias = true
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val bounds = bounds // 获得边界
        canvas.drawRect(bounds, mBgPaint!!)
        //        Log.d("TAG", "draw: " + bounds);
        if (mArcPercent in 0..100) {
            val h = mArcPercent * 0.01f * bounds.height()
            val w = bounds.width().toFloat()
            val b = w / (2.0f * sqrt(3.0)).toFloat() + h // 椭圆y轴半径
            val a = sqrt(3 * b * b * w * w / (12 * b * b - w * w).toDouble()).toFloat() // 椭圆x轴半径
            val arcLeft = bounds.left - (a - w / 2.0f)
            val arcRight = bounds.right + (a - w / 2.0f)
            val arcTop = bounds.bottom - 2 * b
            //            RectF arcRect = new RectF(bounds.left, bounds.bottom - bounds.height() * percent * 2, bounds.right, bounds.bottom);
            val arcRect = RectF(arcLeft, arcTop, arcRight, bounds.bottom.toFloat())
            //            Log.d(TAG, "draw: " + arcRect);
            canvas.drawRect(
                bounds.left.toFloat(),
                bounds.top.toFloat(),
                bounds.right.toFloat(),
                bounds.bottom - h,
                mFgPaint!!
            )
            //            canvas.drawArc(arcRect, 0, 180, false, mFgPaint);
            canvas.drawArc(arcRect, 30f, 120f, false, mFgPaint!!)
        }
    }

    class BottomArcBuilder() {
        val shape: RectShape = RectShape()
        var bgColor: Int = Color.WHITE
        var fgColor: Int = Color.BLACK
        var arcPercent = 16

        fun bgColor(@ColorInt color: Int): BottomArcBuilder {
            bgColor = color
            return this
        }

        fun fgColor(@ColorInt color: Int): BottomArcBuilder {
            fgColor = color
            return this
        }

        fun arcPercent(percent: Int): BottomArcBuilder {
            arcPercent = percent
            return this
        }

        fun build(): BottomArcBackgroundDrawable {
            return BottomArcBackgroundDrawable(this)
        }
    }
}
