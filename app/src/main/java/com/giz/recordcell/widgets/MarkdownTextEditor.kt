package com.giz.recordcell.widgets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.core.view.get
import androidx.core.view.updateMargins
import com.giz.android.toolkit.dp2pxSize
import com.giz.recordcell.R
import com.giz.recordcell.helpers.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import java.util.regex.Matcher
import java.util.regex.Pattern

class MarkdownTextEditor(private val context: Context,
                         private var mode: Int = MARKDOWN_MODE_EDIT,
                         private val text: String = "") {
    
    companion object {
        private fun printLog(msg: String) = Log.d("MarkdownTextEditor", msg)

        const val MARKDOWN_MODE_SEE = 1
        const val MARKDOWN_MODE_EDIT = 2
    }

    private val editorView = LayoutInflater.from(context).inflate(R.layout.rich_text_editor, FrameLayout(context))
    private val actionsTabLayout: TabLayout
    private val actionsScrollView: HorizontalScrollView
    private val actionsRootContainer: FrameLayout
    private val textBox: WebView // 文本编辑区域

    // 标签名也是命令名
    // 文本格式
    private val textStyleTags = arrayOf("bold", "italic", "underline", "strikeThrough") // 加粗，斜体，下划线，删除线
    private val fontSizeArrays = context.resources.getStringArray(R.array.fontSize)
    // 段落格式
    private val paraStyleTags = arrayOf("justifyLeft", "justifyCenter", "justifyRight",
        "indent", "outdent", "blockquote", "p") // 居左，居中，居右，增加缩进，减少缩进，引用，段落
    private val headingTags = arrayOf("p", "h1", "h2", "h3", "h4", "h5")
    // 编辑
    private val editStyleTags = arrayOf("undo", "redo", "selectAll", "removeFormat") // 撤消，重做，全选，清除格式
    // 富文本
    private val richStyleTags = arrayOf("insertOrderedList", "insertUnorderedList",
        "insertHorizontalRule", "createLink", "insertImage") // 有序列表，无序列表，水平线，链接，图片

    private val translateTagMap = hashMapOf(
        "bold" to "加粗", "italic" to "斜体", "underline" to "下划线", "strikeThrough" to "删除线",
        "indent" to "增加缩进", "outdent" to "减少缩进", "blockquote" to "引用块", "p" to "段落",
        "justifyLeft" to "居左", "justifyCenter" to "居中", "justifyRight" to "居右",
        "undo" to "撤消", "redo" to "重做", "selectAll" to "全选", "removeFormat" to "清除格式",
        "insertOrderedList" to "有序列表", "insertUnorderedList" to "无序列表",
        "insertHorizontalRule" to "添加水平线", "createLink" to "添加链接", "insertImage" to "添加图片"
    )
    private var isFocusFromBlur = false
    // int: scrollY - oldScrollY
    private var onEditorScrollChangeListener: (Int) -> Unit = {}

    init {
        actionsTabLayout = editorView.findViewById(R.id.editor_actions_tabLayout)
        actionsRootContainer = editorView.findViewById(R.id.editor_actions_rootContainer)
        actionsScrollView = editorView.findViewById(R.id.editor_horizontal_sv)
        textBox = editorView.findViewById(R.id.editor_webView)

        setupTextBox()
        textBox.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            onEditorScrollChangeListener(scrollY - oldScrollY)
        }
    }

    // 设置四种编辑操作的按钮（动态添加）和下拉菜单（在布局文件中）
    init {
        // 文本格式
        val textStyleIcons = arrayOf(R.drawable.ic_editor_bold, R.drawable.ic_editor_italic,
            R.drawable.ic_editor_underline, R.drawable.ic_editor_strikethrough)
        editorView.findViewById<LinearLayout>(R.id.editor_textStyle_container).apply {
            for(i in 0..3){
                addView(getTemplateImageButton(textStyleTags[i], textStyleIcons[i]))
            }
        }
        // 段落格式
        val paraStyleIcons = arrayOf(R.drawable.ic_editor_justify_left, R.drawable.ic_editor_justify_center,
            R.drawable.ic_editor_justify_right, R.drawable.ic_editor_indent, R.drawable.ic_editor_outdent,
            R.drawable.ic_editor_blockquote, R.drawable.ic_editor_paragraph)
        editorView.findViewById<LinearLayout>(R.id.editor_paraStyle_container).apply {
            for(i in 0..6){
                addView(getTemplateImageButton(paraStyleTags[i], paraStyleIcons[i]))
            }
        }
        // 编辑
        val editStyleIcons = arrayOf(R.drawable.ic_editor_undo, R.drawable.ic_editor_redo,
            R.drawable.ic_editor_select_all, R.drawable.ic_editor_eraser)
        editorView.findViewById<LinearLayout>(R.id.editor_edit_container).apply {
            for(i in 0..3){
                addView(getTemplateImageButton(editStyleTags[i], editStyleIcons[i]))
            }
        }
        // 富文本
        val richStyleIcons = arrayOf(R.drawable.ic_editor_ordered_list, R.drawable.ic_editor_unordered_list,
            R.drawable.ic_editor_horizonal_rule, R.drawable.ic_editor_link, R.drawable.ic_editor_image)
        editorView.findViewById<LinearLayout>(R.id.editor_richText_container).apply {
            for(i in 0..4){
                addView(getTemplateImageButton(richStyleTags[i], richStyleIcons[i]))
            }
        }
        // 设置Spinner
        setSpinnerItemSelectedListener(R.id.editor_fontSize_spinner, fontSizeArrays)
        setSpinnerItemSelectedListener(R.id.editor_heading_spinner, headingTags)
        editorView.findViewById<Spinner>(R.id.editor_fontSize_spinner).setSelection(2) // 默认正常字号
    }

    // 选项卡监听
    init {
        actionsTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showActionCategoryLayout(tab?.position ?: 0)
            }
        })
    }

    fun getEditorView(): View = editorView

    fun setOnEditorScrollChangeListener(listener: (Int) -> Unit){
        onEditorScrollChangeListener = listener
    }

    private inner class SpinnerItemSelectedListener(private val valueArray: Array<String>) : AdapterView.OnItemSelectedListener {
        var firstInit = true

        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if(firstInit){
                firstInit = false
                return
            }
            printLog("Spinner选择：[${valueArray[position]}]项")
            onEditBtnClick(valueArray[position])
        }
    }

    private fun setSpinnerItemSelectedListener(id: Int, valueArray: Array<String>) {
        editorView.findViewById<Spinner>(id).onItemSelectedListener = SpinnerItemSelectedListener(valueArray)
    }

    // 设置图形按钮的相同参数
    private fun getTemplateImageButton(_tag: String,
                                       icon: Int,
                                       onClick: (View) -> Unit = {}) = ImageButton(context, null,
        R.style.Widget_AppCompat_Button_Borderless).apply {
        val pd = dp2pxSize(context, 3f)
        layoutParams = LinearLayout.LayoutParams(dp2pxSize(context, 42f), dp2pxSize(context, 42f)).apply {
            updateMargins(left = pd, right = pd)
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
        setPadding(pd, pd, pd, pd)
        contentDescription = context.getString(R.string.img_desc)
        tag = _tag
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O){
            tooltipText = translateTagMap[_tag]
        }
        setImageResource(icon)
        setOnTouchListener(OnPressScaleChangeTouchListener(minScale = 0.8f))
        setOnClickListener{
            onClick(it)
            onEditBtnClick(_tag)
        }
    }

    fun switchMode(mode: Int) {
        this.mode = mode
        if(mode == MARKDOWN_MODE_SEE) { // 查看模式下隐藏编辑栏
            actionsTabLayout.visibility = View.GONE
            actionsScrollView.visibility = View.GONE
            textBox.evaluateJavascript("javascript:switchMode(false);"){}
        }else{
            actionsTabLayout.toggleVisibility(context) {true}
            actionsScrollView.toggleVisibility(context){true}
            textBox.evaluateJavascript("javascript:switchMode(true);"){}
            textBox.requestFocus()
            context.showSoftInputKeyboard(textBox)
        }
    }

    // 获取内容HTML文本
    fun getHTML(done: (String) -> Unit) {
        textBox.evaluateJavascript("javascript:getDocHtml()"){
            val html = removeUTFCharacters(it.trim('\"')).toString()
            printLog("文本：[${html}]")
            done(html)
        }
    }
    // 把字符串中的"\u003C"去掉
    private fun removeUTFCharacters(data: String): StringBuffer {
        val p: Pattern = Pattern.compile("\\\\u(\\p{XDigit}{4})")
        val m: Matcher = p.matcher(data)
        val buf = StringBuffer(data.length)
        while (m.find()) {
            val ch: String = (m.group(1).toInt(16)).toChar().toString()
            m.appendReplacement(buf, Matcher.quoteReplacement(ch))
        }
        m.appendTail(buf)
        return buf
    }

    private var onPageFinished: () -> Unit = {}
    fun setOnPageFinishedListener(done: () -> Unit) {
        onPageFinished = done
    }

    fun getTextLength(done: (Int) -> Unit) {
        textBox.evaluateJavascript("mdDoc.innerText.replace(/\\s+/g, '').length;"){
            done(it.toInt())
        }
    }

    private var onTextLengthChangeListener: (Int) -> Unit = {}
    fun setOnTextLengthChangeListener(listener: (Int) -> Unit){
        onTextLengthChangeListener = listener
    }

    private var maxLength = -1;
    fun setMaxLength(length: Int){
        maxLength = length
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupTextBox() {
        // 配置webView
        with(textBox) {
            settings.javaScriptEnabled = true
            addJavascriptInterface(AndroidJsMethod(), "AndroidJs") // 添加js调用Android方法的接口类

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if(mode == MARKDOWN_MODE_EDIT){
                        switchMode(MARKDOWN_MODE_EDIT)
                    }else{
                        // 关闭编辑功能，背景为白色
                        textBox.evaluateJavascript("javascript:setContent('$text')"){}
                        switchMode(MARKDOWN_MODE_SEE)
                    }
                    onPageFinished()
                }
            }
            webChromeClient = object : WebChromeClient() {

                override fun onJsPrompt(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    defaultValue: String?,
                    result: JsPromptResult?
                ): Boolean { // 拦截Prompt提示框
                    val linearLayout = FrameLayout(context).apply {
                        setPadding(dp2pxSize(context, 20f), dp2pxSize(context, 8f), dp2pxSize(context, 20f), 0)
                    }
                    val et = EditText(context).apply {
                        setText(defaultValue)
                        requestFocus()
                    }
                    linearLayout.addView(et)
                    MaterialAlertDialogBuilder(context, R.style.CustomDialog)
                        .setTitle(translateTagMap[message] ?: message)
                        .setView(linearLayout)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm(et.text.toString()) }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            result?.cancel()
                            context.hideSoftInputKeyboard(et)
                        }
                        .show()
                    return true
                }
            }
            loadUrl("file:///android_asset/editor.html")
            // loadData(testData, "text/html", "utf-8") // 加载字符串html
        }
    }

    // 显示哪个编辑操作菜单
    private var previousLayout = 0
    private fun showActionCategoryLayout(which: Int){
        // printLog("显示第$which 个操作布局")
        actionsRootContainer[previousLayout].apply {
            val v = this
            animate().alpha(0.4f).setDuration(120).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    v.animate().setListener(null)
                    v.visibility = View.GONE
                    previousLayout = which
                    actionsRootContainer[which].apply {
                        alpha = 0.4f
                        visibility = View.VISIBLE
                        animate().alpha(1f).setDuration(120)
                        actionsScrollView.smoothScrollTo(0, 0)
                    }
                }
            })
        }
    }

    private fun onEditBtnClick(command: String) {
        textBox.requestFocus() // 获得焦点
        context.showSoftInputKeyboard(textBox)
        if(isFocusFromBlur){
            isFocusFromBlur = false
            textBox.evaluateJavascript("javascript:setCaretEnd()"){}
        }
        when(command){
            in paraStyleTags.copyOfRange(0, 5), in richStyleTags.copyOfRange(0, 3), in textStyleTags,
            in editStyleTags -> {
                textBox.evaluateJavascript("javascript:formatDoc('${command}')") {
                    printLog("收到消息：$it") // 返回值
                } // 调用js
            }
            in headingTags, in paraStyleTags.copyOfRange(5, 7) -> {
                textBox.evaluateJavascript("javascript:formatDoc('formatBlock','${command}')") {
                    printLog("收到消息：$it")
                }
            }
            in fontSizeArrays -> {
                val size = fontSizeArrays.indexOf(command)
                textBox.evaluateJavascript("javascript:formatDoc('fontSize','${size + 1}')") {
                    printLog("收到消息：$it")
                }
            }
            in richStyleTags.copyOfRange(3, 5) -> {
                textBox.evaluateJavascript("var sLnk=prompt('$command','http://');" +
                        "if(sLnk&&sLnk!=''&&sLnk!='http://'){formatDoc('$command',sLnk)}") {
                    printLog("收到消息：$it")
                } // 调用js
            }
        }
    }

    private fun onTextLengthExceed() {
        textBox.evaluateJavascript("javascript:formatDoc('undo');"){
            context.showToast("字数不能超过$maxLength")
            getTextLength {
                onTextLengthChangeListener(it)
            }
        }
    }

    inner class AndroidJsMethod {
        @JavascriptInterface
        fun onShowKeyboard(isShown: Boolean) {
            printLog("键盘显示：$isShown")
        }

        @JavascriptInterface
        fun onTextBoxBlur() {
            printLog("文本框失焦")
            isFocusFromBlur = true
        }

        @JavascriptInterface
        fun onTextCounter(count: Int){
            printLog("字数统计：$count")
            onTextLengthChangeListener(count)
            if(maxLength != -1){
                if(count > maxLength) {
                    textBox.post { // 保证webView在同一个线程中执行
                        onTextLengthExceed()
                    }
                }
            }
        }
    }
}