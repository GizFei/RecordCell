package com.giz.recordcell.helpers

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.giz.recordcell.R
import java.util.regex.Pattern

// 所有的扩展函数

// 显示Toast通知
fun Activity.showToast(msg: String){
    this.runOnUiThread {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
fun Activity.showToast(resId: Int){
    this.runOnUiThread {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }
}
fun Context.showToast(msg: String){
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
fun Context.showToast(resId: Int){
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}

// 打印调试日志
fun Activity.printLog(msg: String) = Log.d(this::class.java.simpleName, msg)
fun Context.printLog(msg: String) = Log.d(this::class.java.simpleName, msg)

// 绑定返回按钮
fun Activity.bindBackPressedIcon(v: View) = v.setOnClickListener {
    this.onBackPressed()
}

// 弹出，收起软键盘
fun Activity.showSoftInputKeyboard(view: View) {
    val inputMethod = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethod.showSoftInput(view, 0)
}
fun Activity.hideSoftInputKeyboard(view: View){
    val inputMethod = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethod.hideSoftInputFromWindow(view.windowToken, 0)
}
fun Context.showSoftInputKeyboard(view: View) {
    val inputMethod = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethod.showSoftInput(view, 0)
}
fun Context.hideSoftInputKeyboard(view: View){
    val inputMethod = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethod.hideSoftInputFromWindow(view.windowToken, 0)
}

fun String.isEmail(): Boolean =
    Pattern.matches("^([a-z0-9A-Z]+[-|\\\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$",
        this)

fun EditText.clearText() = this.setText("")

fun View.setMargins(width: Int, height: Int, left: Int, top: Int, right: Int, bottom: Int) {
    layoutParams = LinearLayout.LayoutParams(width, height).apply {
        setMargins(left, top, right, bottom)
    }
}
fun View.setMarginsWithMatchParent(left: Int, top: Int, right: Int, bottom: Int) {
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
    ).apply {
        setMargins(left, top, right, bottom)
    }
}
fun View.setMarginsWithWrapContent(left: Int, top: Int, right: Int, bottom: Int) {
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        setMargins(left, top, right, bottom)
    }
}

fun View.toggleVisibility(context: Context, visibleCondition: () -> Boolean) {
    if(visibleCondition()){ // 显示
        if(visibility != View.VISIBLE){
            visibility = View.VISIBLE
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.anim_fade_in))
        }
    }else{
        if(visibility != View.GONE){
            val anim = AnimationUtils.loadAnimation(context, R.anim.anim_fade_out)
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationEnd(animation: Animation?) {
                    visibility = View.GONE
                }

                override fun onAnimationRepeat(animation: Animation?) {

                }

                override fun onAnimationStart(animation: Animation?) {

                }
            })
            startAnimation(anim)
        }
    }
}
fun View.toggleVisibility(visibleCondition: () -> Boolean){
    visibility = if(visibleCondition()) View.VISIBLE else View.GONE
}

inline fun <reified T> T?.doIfNotNull(thing: (T) -> Unit){
    if(this != null){
        thing(this)
    }
}

fun String?.isUrl(loose: Boolean = false) : Boolean {
    val expr = "(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]"
    val looseExpr = "[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]"
    return when (loose) {
        true -> (this?.matches(Regex(expr)) ?: false) || (this?.matches(Regex(looseExpr)) ?: false)
        false -> this?.matches(Regex(expr)) ?: false
    }
}