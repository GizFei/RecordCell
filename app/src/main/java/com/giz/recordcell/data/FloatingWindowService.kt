package com.giz.recordcell.data

import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import cn.bmob.v3.BmobUser
import cn.bmob.v3.datatype.BmobDate
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import com.giz.android.toolkit.getScreenHeight
import com.giz.android.toolkit.getScreenWidth
import com.giz.recordcell.R
import com.giz.recordcell.bmob.LittleNote
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.helpers.toggleVisibility
import java.util.*
import kotlin.math.max
import kotlin.math.min

class FloatingWindowService : Service() {

    companion object {
        var isStarted = false
    }

    private lateinit var windowManager: WindowManager
    private val layoutParams = WindowManager.LayoutParams()

    private val screenWidth by lazy { getScreenWidth(this) }
    private val screenHeight by lazy { getScreenHeight(this) }

    private var viewWidth = 0
    private var viewHeight = 0

    override fun onCreate() {
        super.onCreate()
        isStarted = true

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        layoutParams.apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            }else{
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.RGBA_8888
            gravity = Gravity.START or Gravity.TOP
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            x = 0
            y = 300
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showFloatingWindow()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showFloatingWindow() {
        if(Settings.canDrawOverlays(this)){
            LayoutInflater.from(this).inflate(R.layout.floating_editor, null)
                .apply {
                    val container = findViewById<CardView>(R.id.floating_editor_editor_layout)
                    val openBtn = findViewById<ImageView>(R.id.floating_editor_open_btn)
                    openBtn.setOnTouchListener(FloatingOnTouchListener(this){
                        if(container.visibility == View.GONE){
                            // 显示，可以弹出软键盘
                            openBtn.animate().rotation(45f)
                            this@FloatingWindowService.layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            windowManager.updateViewLayout(this, this@FloatingWindowService.layoutParams)
                        }else{
                            // 隐藏
                            openBtn.animate().rotation(0f)
                            this@FloatingWindowService.layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            windowManager.updateViewLayout(this, this@FloatingWindowService.layoutParams)
                        }
                        container.toggleVisibility(this@FloatingWindowService) { container.visibility == View.GONE }
                    })
                    findViewById<ImageView>(R.id.floating_editor_close_btn).setOnClickListener{
                        // Log.d("FloatingWindow", "关闭悬浮窗")
                        isStarted = false
                        windowManager.removeView(this)
                        stopSelf()
                    }
                    val editText = findViewById<EditText>(R.id.floating_editor_edittext)
                    findViewById<TextView>(R.id.floating_editor_save_btn).setOnClickListener {
                        if(editText.text.isEmpty()){
                            showToast("便签内容不能为空")
                        }else{
                            it.isEnabled = false
                            // 保存为便签
                            LittleNote(
                                BmobUser.getCurrentUser(RecordUser::class.java),
                                editText.text.toString(),
                                BmobDate(Date()),
                                null,
                                mutableListOf()
                            ).save(object : SaveListener<String>(){
                                override fun done(p0: String?, p1: BmobException?) {
                                    if(p1 == null){
                                        showToast(R.string.bmob_save_success_text)
                                        isStarted = false
                                        windowManager.removeView(this@apply)
                                        stopSelf()
                                    }else{
                                        context.showToast(R.string.bmob_save_failure_text)
                                        Log.d("Note", "保存失败：$p1")
                                    }
                                }
                            })
                        }
                    }
                    addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                        viewWidth = this.width
                        viewHeight = this.height
                    }
                    // setOnTouchListener(FloatingOnTouchListener())
                }.also { windowManager.addView(it, layoutParams) }
        }
    }


    private inner class FloatingOnTouchListener(val targetView: View, val onClick: () -> Unit): View.OnTouchListener {
        private var x: Int = 0;
        private var y: Int = 0;
        private var moved = false
        private var currentScale = 1.0f
        private val minScale = 0.8f

        override fun onTouch(v: View, event: MotionEvent?): Boolean {
            event ?: return false
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = event.rawX.toInt()
                    y = event.rawY.toInt()
                    moved = false
                    startAnimation(v, currentScale, minScale)
                }
                MotionEvent.ACTION_MOVE -> {
                    // Log.d("FloatingWindow", "高度：$viewHeight")
                    moved = true
                    val moveX = event.rawX - x
                    val moveY = event.rawY - y
                    x = event.rawX.toInt()
                    y = event.rawY.toInt()
                    val topX = max(0, moveX.toInt() + layoutParams.x)
                    layoutParams.x = min(topX, screenWidth - viewWidth)
                    val topY = max(0, moveY.toInt() + layoutParams.y)
                    layoutParams.y = min(topY, screenHeight - viewHeight)
                    windowManager.updateViewLayout(targetView, layoutParams)
                }
                MotionEvent.ACTION_UP -> {
                    startAnimation(v, currentScale, 1.0f)
                    if(!moved){
                        onClick()
                    }
                }
            }
            return true
        }

        private fun startAnimation(v: View, fromScale: Float, toScale: Float) {
            val animator = ValueAnimator.ofFloat(fromScale, toScale)
            animator.duration = 200
            animator.interpolator = FastOutSlowInInterpolator()
            animator.addUpdateListener {
                currentScale = it.animatedValue as Float
                v.scaleX = currentScale
                v.scaleY = currentScale
            }
            animator.start()
        }
    }
}