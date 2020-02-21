package com.giz.recordcell.widgets

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import com.giz.recordcell.R
import com.giz.recordcell.helpers.OnPressScaleChangeTouchListener
import kotlin.math.min


class ShadeImageButton(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    ImageView(context, attrs, defStyleAttr) {

    companion object {
        /**
         * 带阴影的圆角图片控件
         * 在"attrs.settings"文件中声明样式
        <declare-styleable name="ShadeImageButton">
            <attr name="sib_type" format="enum">
                <enum name="circle" value="1" />
                <enum name="corner" value="2" />
            </attr>
            <attr name="sib_radius" format="dimension" />
            <attr name="sib_shadeColor" format="color" />
            <attr name="sib_solidColor" format="color" />
            <attr name="sib_shadeLength" format="dimension" />
            <attr name="sib_yOffset" format="dimension" />
        </declare-styleable>
         * sib_type：圆角样式：circle-圆形；corner-圆角
         * sib_radius：圆角模式下的圆角半径
         * sib_shadeColor：阴影颜色
         * sib_shadeLength：阴影长度
         * sib_solidColor：实心颜色
         * sib_yOffset：y轴偏移
         */
        // 圆形，圆角，默认
        private const val TYPE_CIRCLE = 1
        private const val TYPE_CORNER = 2
        private const val TYPE_DEFAULT = 0
    }

    private var mType = TYPE_DEFAULT // 类型
    private var mRadius = 0f // 圆角半径
    private var mShadeLength = 16f
    private var mShadeColor = Color.LTGRAY
    private var mSolidColor = Color.WHITE
    private var yOffset = 0f

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val mSolidPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val mShadePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val mMaskPaint = Paint().also { it.color = Color.argb(48,0, 0, 0) }

    var showMask = false
        set(value) {
            field = value
            invalidate()
        }

    constructor(context: Context) : this(context, null) {}
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {}

    init {
        val array: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.ShadeImageButton, defStyleAttr, 0)

        mType = array.getInt(R.styleable.ShadeImageButton_sib_type, TYPE_DEFAULT)
        mRadius = array.getDimension(R.styleable.ShadeImageButton_sib_radius, dp2px(8f))
        mShadeLength = array.getDimension(R.styleable.ShadeImageButton_sib_shadeLength, dp2px(8f))
        mShadeColor = array.getColor(R.styleable.ShadeImageButton_sib_shadeColor, Color.LTGRAY)
        mSolidColor = array.getColor(R.styleable.ShadeImageButton_sib_solidColor, Color.WHITE)
        yOffset = array.getDimension(R.styleable.ShadeImageButton_sib_yOffset, 0f)

        array.recycle()
    }

    init {
        with(mShadePaint){
            color = mShadeColor
            if(yOffset > 0f){
                maskFilter = BlurMaskFilter(mShadeLength, BlurMaskFilter.Blur.NORMAL)
            }else{
                maskFilter = BlurMaskFilter(mShadeLength, BlurMaskFilter.Blur.OUTER)
            }
        }
        mSolidPaint.color = mSolidColor

        setOnTouchListener(object : OnPressScaleChangeTouchListener(duration = 160L){
            override fun onActionDown() { showMask = true }
            override fun onActionMoveOut() { showMask = false }
            override fun onActionUpOrCancel() { showMask = false }
        })
        setOnClickListener {  }
    }

    private fun dp2px(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value,
            resources.displayMetrics
        )
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) { // 当模式为圆形模式的时候，我们强制让宽高一致
        if (mType == TYPE_CIRCLE) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            val result = min(measuredWidth, measuredHeight)
            setMeasuredDimension(result, result)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    private var isShaderSet = false
    override fun onDraw(canvas: Canvas) {
        val drawable = drawable
        val matrix: Matrix? = imageMatrix
        if (drawable == null) {
            return
        }
        if (drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
            return
        }
        if (matrix == null && paddingTop == 0 && paddingLeft == 0) {
            drawable.draw(canvas)
        } else {
            val saveCount: Int = canvas.saveCount
            canvas.save()
            if (cropToPadding) {
                val scrollX = scrollX
                val scrollY = scrollY
                canvas.clipRect(
                    scrollX + paddingLeft, scrollY + paddingTop,
                    scrollX + right - left - paddingRight,
                    scrollY + bottom - top - paddingBottom
                )
            }
            //canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
            if (mType == TYPE_CIRCLE) {
                val bitmap = drawable2Bitmap(drawable)
                if(!isShaderSet){
                    mPaint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    isShaderSet = true
                }
                val sl = mShadeLength + yOffset / 2 + dp2px(1f) // 额外1dp阴影范围
                canvas.drawCircle(width / 2f, height / 2f + yOffset, width / 2f - sl, mShadePaint)
                canvas.drawCircle(width / 2f, height / 2f, width / 2f - sl + 2f, mSolidPaint)
                canvas.drawCircle(width / 2f, height / 2f, width / 2f - sl + 2f, mPaint)
                if(showMask){
                    canvas.drawCircle(width / 2f, height / 2f, width / 2f - sl + 2f, mMaskPaint)
                }
            } else if (mType == TYPE_CORNER) {
                val bitmap = drawable2Bitmap(drawable)
                if(!isShaderSet){
                    mPaint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    isShaderSet = true
                }
                // 阴影
                val sl = mShadeLength + yOffset / 2 + dp2px(1f)
//                canvas.drawRoundRect(
//                    paddingLeft + sl, paddingTop + sl + yOffset,
//                    width - paddingRight - sl, height - paddingBottom - sl + yOffset,
//                    mRadius, mRadius, mShadePaint
//                )
                canvas.drawRoundRect(sl, sl + yOffset, width - sl, height - sl + yOffset,
                    mRadius, mRadius, mShadePaint)
                val sll = sl - 2
                val bgRect = RectF(sll, sll, width - sll, height - sll)
                val imRect = RectF(paddingLeft + sll, paddingTop + sll,
                    width - paddingRight - sll, height - paddingBottom - sll)
                // 背景
                canvas.drawRoundRect(bgRect, mRadius, mRadius, mSolidPaint)
                // 图像
                canvas.drawRoundRect(imRect, mRadius, mRadius, mPaint)
                if(showMask){ // 遮罩
                    canvas.drawRoundRect(bgRect, mRadius, mRadius, mMaskPaint)
                }
            } else {
                if (matrix != null) {
                    canvas.concat(matrix)
                }
                drawable.draw(canvas)
            }
            canvas.restoreToCount(saveCount)
        }
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        isShaderSet = false
    }

    /**
     * drawable转换成bitmap
     */
    private fun drawable2Bitmap(drawable: Drawable?): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (drawable == null) {
            return bitmap
        }
        val canvas = Canvas(bitmap)
        //根据传递的scaletype获取matrix对象，设置给bitmap
        val matrix: Matrix? = imageMatrix
        if (matrix != null) {
            canvas.concat(matrix)
        }
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * 设置圆角半径
     * @param radius 半径值，单位dp
     */
    fun setCornerRadius(radius: Float) {
        mRadius = dp2px(radius)
        invalidate()
    }

    /**
     * 设置样式
     * TYPE_CORNER, TYPE_CIRCLE, TYPE_DEFAULT
     * @param type 类型
     */
    fun setType(type: Int) {
        mType = type
        invalidate()
    }

    fun setSolidColor(color: Int){
        mSolidColor = color
        mSolidPaint.color = color
        invalidate()
    }

    fun setShadeColor(color: Int){
        mShadeColor = color
        mShadePaint.color = color
        invalidate()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if(!enabled) {
            mSolidPaint.color = Color.GRAY
            mShadePaint.color = Color.LTGRAY
        }else{
            mSolidPaint.color = mSolidColor
            mShadePaint.color = mShadeColor
        }
        invalidate()
    }

}
