package com.giz.recordcell.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import kotlin.math.max

class NestedScrollWebView(context: Context, attrs: AttributeSet?, defStyle: Int)
    : WebView(context, attrs, defStyle), NestedScrollingChild {

    private var mLastY: Int = 0
    private val mScrollOffset = IntArray(2)
    private val mScrollConsumed = IntArray(2)
    private var mNestedOffsetY: Int = 0
    private var mChange = false
    private val mChildHelper = NestedScrollingChildHelper(this)

    constructor(context: Context): this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    init {
        isNestedScrollingEnabled = true
    }

    private fun printLog(msg: String){}//Log.d("NestedScrollWebView", msg)

    private var downX = 0f
    private var downY = 0f
    private lateinit var motionEvent: MotionEvent
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        var returnValue = false

        val event = MotionEvent.obtain(e)
        val action = event.actionMasked
        if(action == MotionEvent.ACTION_DOWN){
            mNestedOffsetY = 0
        }
        val eventY = event.y.toInt()
        event.offsetLocation(0f, mNestedOffsetY.toFloat())

        when(action){
            MotionEvent.ACTION_DOWN -> {
                mLastY = eventY
                // Start NestedScroll
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                returnValue = super.onTouchEvent(event)
                mChange = false
                downX = event.x
                downY = event.y
                motionEvent = MotionEvent.obtain(e)
            }
            MotionEvent.ACTION_MOVE -> {
                var deltaY = mLastY - eventY
                printLog("初始deltaY: $deltaY")

                // NestedPreScroll
                if(dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)){
                    printLog("消耗：${mScrollConsumed[1]}")
                    deltaY -= mScrollConsumed[1]
                    event.offsetLocation(0f, -mScrollOffset[1].toFloat())
                    mNestedOffsetY += mScrollOffset[1]
                }
                printLog("Prescroll后的Y: $deltaY")

                val oldY = scrollY
                val newScrollY = max(0, oldY + deltaY)
                val dyConsumed = newScrollY - oldY
                val dyUnconsumed = deltaY - dyConsumed

                // NestedScroll
                printLog("NestedScroll, deltaY: $deltaY")
                if(dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, mScrollOffset)){
                    printLog("${mScrollOffset[1]}")
                    event.offsetLocation(0f, mScrollOffset[1].toFloat())
                    mNestedOffsetY += mScrollOffset[1]
                    mLastY -= mScrollOffset[1]
                }

                if(mScrollConsumed[1] == 0 && mScrollOffset[1] == 0){
                    if(mChange){
                        mChange = false
                        event.action = MotionEvent.ACTION_DOWN
                        super.onTouchEvent(event)
                    }else{
                        returnValue = super.onTouchEvent(event)
                    }
                    event.recycle()
                }else{
                    if(!mChange){
                        mChange = true
                        super.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0))
                    }
                }
//                event.recycle()
//                returnValue = super.onTouchEvent(event)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopNestedScroll()
                returnValue = super.onTouchEvent(event)
            }
        }
        return returnValue
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return mChildHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return mChildHelper.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        mChildHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return mChildHelper.hasNestedScrollingParent()
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean {
        return mChildHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow
        )
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }
}