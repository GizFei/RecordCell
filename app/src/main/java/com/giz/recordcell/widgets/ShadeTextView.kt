package com.giz.recordcell.widgets

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.giz.recordcell.R
import com.giz.recordcell.helpers.OnPressScaleChangeTouchListener
import kotlin.math.min


class ShadeTextView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    TextView(context, attrs, defStyleAttr) {

    private var cornerRadius = 0f // 圆角半径
    private var shadeLength = 16f
    private var shadeColor = Color.LTGRAY
    private var mSolidColor = Color.WHITE
    private var yOffset = 0f
    private var fullRoundCorner = false // true,则圆角半径为高度的一半

    private val mSolidPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val mShadePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val mMaskPaint = Paint().also { it.color = Color.argb(48,0, 0, 0) }

    private var extraShadeLength = dp2px(1f)

    var showMask = false
        set(value) {
            field = value
            invalidate()
        }

    constructor(context: Context) : this(context, null) {}
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {}

    init {
        val array: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.ShadeTextView, defStyleAttr, 0)

        cornerRadius = array.getDimension(R.styleable.ShadeTextView_stv_radius, dp2px(8f))
        shadeLength = array.getDimension(R.styleable.ShadeTextView_stv_shadeLength, dp2px(8f))
        shadeColor = array.getColor(R.styleable.ShadeTextView_stv_shadeColor, Color.LTGRAY)
        mSolidColor = array.getColor(R.styleable.ShadeTextView_stv_solidColor, Color.WHITE)
        yOffset = array.getDimension(R.styleable.ShadeTextView_stv_yOffset, 0f)
        fullRoundCorner = array.getBoolean(R.styleable.ShadeTextView_fullRoundCorner, false)

        array.recycle()

        val paddingHorizontal = (shadeLength + dp2px(14f)).toInt()
        val paddingVertical = (shadeLength + dp2px(8f)).toInt()
        setPadding(paddingHorizontal + paddingStart, paddingVertical + paddingTop,
            paddingHorizontal + paddingRight, paddingVertical + paddingBottom)
        extraShadeLength += yOffset / 2f
    }

    init {
        with(mShadePaint){
            color = shadeColor
            if(yOffset > 0f){
                maskFilter = BlurMaskFilter(shadeLength, BlurMaskFilter.Blur.NORMAL)
            }else{
                maskFilter = BlurMaskFilter(shadeLength, BlurMaskFilter.Blur.OUTER)
            }
        }
        mSolidPaint.color = mSolidColor
    }

    private fun dp2px(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value,
            resources.displayMetrics
        )
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
        cornerRadius = if(fullRoundCorner){ radius }else{ min(cornerRadius, radius) }
    }

    override fun onDraw(canvas: Canvas) {
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
        super.onDraw(canvas)
    }

    /**
     * 设置圆角半径
     * @param radius 半径值，单位dp
     */
    fun setCornerRadius(radius: Float) {
        cornerRadius = dp2px(radius)
        invalidate()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if(enabled) {
            mShadePaint.color = shadeColor
            mSolidPaint.color = mSolidColor
        }else{
            mSolidPaint.color = Color.GRAY
            mShadePaint.color = Color.LTGRAY
        }
        invalidate()
    }

    fun enableOnPressScaleTouchListener(minScale: Float = 0.88f, duration: Long = 180L,
                                        onClickListener: OnClickListener? = null){
        setOnTouchListener(object : OnPressScaleChangeTouchListener(minScale, duration){
            override fun onActionDown() { showMask = true }
            override fun onActionMoveOut() { showMask = false }
            override fun onActionUpOrCancel() { showMask = false }
        })
        if(onClickListener == null){
            setOnClickListener{}
        }else{
            setOnClickListener(onClickListener)
        }
    }
    fun enableOnPressScaleTouchListener(minScale: Float = 0.88f, duration: Long = 180L,
                                        onClickListener: (View) -> Unit){
        setOnTouchListener(object : OnPressScaleChangeTouchListener(minScale, duration){
            override fun onActionDown() { showMask = true }
            override fun onActionMoveOut() { showMask = false }
            override fun onActionUpOrCancel() { showMask = false }
        })
        setOnClickListener{
            onClickListener(it)
        }
    }
}
