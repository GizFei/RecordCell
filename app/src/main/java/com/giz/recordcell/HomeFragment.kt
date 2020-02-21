package com.giz.recordcell

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.util.set
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import cn.bmob.v3.BmobUser
import com.android.volley.Response
import com.giz.recordcell.activities.*
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.bmob.WebImageUtils
import com.giz.recordcell.data.RecordCategory
import com.giz.recordcell.pagers.*
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_home.*
import kotlin.math.max

class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
        const val REQUEST_CODE_TODO = 10

        private fun printLog(msg: String) = Log.d(TAG, msg)

        fun newInstance() = HomeFragment()
    }

    private lateinit var mainActivity: MainActivity
    private val currentUser: RecordUser by lazy { BmobUser.getCurrentUser(RecordUser::class.java) }

    // 用于滑动隐藏底部工具栏的滚动监听器
    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if(dy > 0 && !mainActivity.isBottomBarScrolling) {
                // 向上滚动
                mainActivity.bottomBarSlideDown()
            } else if (dy < 0 && !mainActivity.isBottomBarScrolling) {
                // 向下滚动
                mainActivity.bottomBarSlideUp()
            }
        }
    }
    private val onScrollChangeListener = {_: View?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
        val dy = scrollY - oldScrollY
        if(dy > 0 && !mainActivity.isBottomBarScrolling){
            // 向下滚动
            mainActivity.bottomBarSlideDown()
        }else if(dy < 0 && !mainActivity.isBottomBarScrolling) {
            // 向上滚动
            mainActivity.bottomBarSlideUp()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        // 设置功能列表
        RecordCategory.functionDescList = currentUser.functionListOrder
//        currentUser = BmobUser.getCurrentUser(RecordUser::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 应用工具栏滑动时透明度变化
        home_app_bar.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            override fun onOffsetChanged(p0: AppBarLayout?, p1: Int) {
                val alpha = -p1 / home_app_bar.totalScrollRange.toFloat()
                home_collapse_toolbar.alpha = 1f - alpha
            }
        })
        // 以下代码隐藏AppbarLayout里的头像和背景图
        if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("record_home_show_header", true)){
            home_header_container.layoutParams = home_header_container.layoutParams.apply { height = 0 }
//         或者：home_header_container.visibility = View.GONE
        }

        // 设置ViewPager
        home_view_pager.adapter = HomePagerAdapter()
        home_tab_layout.setupWithViewPager(home_view_pager) // 与TabLayout关联滑动
        home_tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {}

            override fun onTabUnselected(p0: TabLayout.Tab?) {
                val slidingIndicators = home_tab_layout[0] as ViewGroup
                p0?.position?.takeIf { it < slidingIndicators.childCount }?.let {
                    val tabView = slidingIndicators.getChildAt(it) as ViewGroup
                    printLog("${tabView::class.java}")
                    if(tabView.childCount > 1){
                        // printLog("放大字体")
                        tabView[1].takeIf { t -> t is TextView }?.animate()?.scaleX(1.0f)?.scaleY(1.0f)
                    }
                }
            }

            override fun onTabSelected(p0: TabLayout.Tab?) {
                // printLog("tab位置：${p0?.position}, ${home_tab_layout.childCount}")
                val slidingIndicators = home_tab_layout[0] as ViewGroup
                p0?.position?.takeIf { it < slidingIndicators.childCount }?.let {
                    val tabView = slidingIndicators.getChildAt(it) as ViewGroup
                    if(tabView.childCount > 1){
                        // printLog("放大字体")
                        tabView[1].takeIf { t -> t is TextView }?.animate()?.scaleX(1.4f)?.scaleY(1.4f)
                    }
                }
            }
        })
        home_view_pager.post {
            val slidingIndicators = home_tab_layout[0] as ViewGroup
            0.takeIf { it < slidingIndicators.childCount }?.let {
                val tabView = slidingIndicators.getChildAt(it) as ViewGroup
                if(tabView.childCount > 1){
                    // printLog("放大字体")
                    tabView[1].takeIf { t -> t is TextView }?.animate()?.scaleX(1.4f)?.scaleY(1.4f)
                }
            }
        }

        home_avatar.post { fetchAvatarImg() }
    }

    override fun onResume() {
        super.onResume()
        if(mainActivity.hasWindowFocus()){
            val rect = Rect()
            mainActivity.window.decorView.getWindowVisibleDisplayFrame(rect)
            setToolbarHeight(rect.top)
        }
        printLog("HomeFragment: onResume:恢复运行")
        todoPagerItemView?.updatePagerRecyclerView()
        taskBoxPagerItemView?.updatePagerRecyclerView()
        littleNotePagerItemView?.updatePagerRecyclerView()
        dailyItemPagerItemView?.updatePagerRecyclerView()
        collectionItemPagerItemView?.updatePagerRecyclerView()
    }

    // ViewPager内包含的各个视图
    private var todoPagerItemView: TodoPagerItemView? = null
    var taskBoxPagerItemView: TaskBoxPagerItemView? = null
    private var littleNotePagerItemView: LittleNotePagerItemView? = null
    private var dailyItemPagerItemView: DailyItemPagerItemView? = null
    private var collectionItemPagerItemView: CollectionItemPagerItemView? = null

    private inner class HomePagerAdapter : PagerAdapter() {
        private val titles = RecordCategory.getAvailableCategoryDescList()
        private val cacheViews = SparseArray<View>()

        override fun getCount(): Int = titles.size

        override fun isViewFromObject(view: View, `object`: Any) = view == `object`

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            if(cacheViews[position] != null){
                return cacheViews[position]!!
            }
            val view: View = when(RecordCategory.getCategoryFromDesc(titles[position])) {
                RecordCategory.TODO -> {
                    todoPagerItemView = TodoPagerItemView(mainActivity, currentUser).apply {
                        addRecyclerViewOnScrollListener(onScrollListener)
                        setOnTodoItemClickListener{
                            startActivity(NewTodoActivity.newIntent(mainActivity, NewTodoActivity.TODO_MODE_SEE, it))
                        }
                        updatePagerRecyclerView()
                    }
                    todoPagerItemView!!.getPagerItemView()
                }
                RecordCategory.TASKBOX -> {
                    taskBoxPagerItemView = TaskBoxPagerItemView(mainActivity, currentUser).apply {
                        addRecyclerViewOnScrollListener(onScrollListener)
                        setOnTaskBoxItemClickListener {
                            startActivity(TaskBoxDetailActivity.newIntent(mainActivity, it))
                        }
                        setonTaskBoxDeleteListener {
                            todoPagerItemView?.updatePagerRecyclerView()
                        }
                        updatePagerRecyclerView()
                    }
                    taskBoxPagerItemView!!.getPagerItemView()
                }
                RecordCategory.LITTLE_NOTE -> {
                    littleNotePagerItemView = LittleNotePagerItemView(mainActivity, currentUser).apply {
                        addRecyclerViewOnScrollListener(onScrollListener)
                        setOnNoteItemClickListener {
                            startActivity(NewLittleNoteActivity.newIntent(mainActivity,
                                NewLittleNoteActivity.NOTE_MODE_SEE, it))
                        }
                        updatePagerRecyclerView()
                    }
                    littleNotePagerItemView!!.getPagerItemView()
                }
                RecordCategory.DAILY -> {
                    dailyItemPagerItemView = DailyItemPagerItemView(mainActivity, currentUser).apply {
                        addRecyclerViewOnScrollListener(onScrollListener)
                        setOnDailyItemClickListener {
                            startActivity(DailyFinishCaseActivity.newIntent(mainActivity, it))
                        }
                        updatePagerRecyclerView()
                    }
                    dailyItemPagerItemView!!.getPagerItemView()
                }
                RecordCategory.FAVORITE -> {
                    collectionItemPagerItemView = CollectionItemPagerItemView(mainActivity, currentUser).apply {
                        addRecyclerViewOnScrollListener(onScrollListener)
                        updatePagerRecyclerView()
                    }
                    collectionItemPagerItemView!!.getPagerItemView()
                }
                else -> View(mainActivity)
            }
            cacheViews[position] = view
            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            // container.removeView(`object` as View) 不销毁视图
        }

        override fun getPageTitle(position: Int) = titles[position]
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        printLog("返回活动：${requestCode}, $resultCode")
//        when(requestCode){
//            REQUEST_CODE_TODO -> {
//                if(resultCode == NewTodoActivity.RESULT_CODE_OK){
//                    todoPagerItemView?.updatePagerRecyclerView()
//                }
//            }
//        }
//    }

    // 获取头像图片
    private fun fetchAvatarImg() {
        printLog("头像网址：${currentUser.avatar}。${home_avatar.width}")
        WebImageUtils.downloadImage(mainActivity, currentUser.avatar, object : Response.Listener<Bitmap> {
            override fun onResponse(response: Bitmap?) {
                if(response != null){
                    printLog("获得图片：${response.width}, ${response.height}")
                    home_avatar.setImageBitmap(response)
                }
            }
        }, home_avatar.width, home_avatar.height)
    }

    private var hasSetStatusBarHeight = false
    fun setToolbarHeight(height: Int) {
        if(!hasSetStatusBarHeight){
            printLog("状态栏高度：${height}")
            home_toolbar.layoutParams = home_toolbar.layoutParams.apply { this.height =
                max(height, home_toolbar.height) }
            hasSetStatusBarHeight = true
        }
    }
}