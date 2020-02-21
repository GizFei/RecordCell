package com.giz.recordcell.pagers

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.giz.recordcell.R

abstract class BaseRecyclerViewPagerItemView(context: Context) {

    private val rootView: View

    protected val mRecyclerView: RecyclerView
    protected val mSwipeRefreshLayout: SwipeRefreshLayout

    private val mOtherViewContainer: FrameLayout

    init {
        rootView = LayoutInflater.from(context).inflate(R.layout.pager_item_recyclerview, null).apply {
            mRecyclerView = findViewById(R.id.pager_item_rv)
            mSwipeRefreshLayout = findViewById(R.id.pager_item_swipe_layout)
            mOtherViewContainer = findViewById(R.id.pager_item_other_view_container)
        }
        mSwipeRefreshLayout.setOnRefreshListener{ updatePagerRecyclerView() }
    }

    // 更新列表
    abstract fun updatePagerRecyclerView()

    protected fun addHeaderView(view: View) {
        mOtherViewContainer.addView(view)
    }

    fun addRecyclerViewOnScrollListener(onScrollListener: RecyclerView.OnScrollListener){
        mRecyclerView.addOnScrollListener(onScrollListener)
    }

    fun setSwipeRefreshLayoutRefreshListener(refreshListener: SwipeRefreshLayout.OnRefreshListener){
        mSwipeRefreshLayout.setOnRefreshListener(refreshListener)
    }
    fun setSwipeRefreshLayoutRefreshListener(listener: () -> Unit){
        mSwipeRefreshLayout.setOnRefreshListener{ listener() }
    }

    fun getPagerItemView() = rootView

    protected fun printLog(msg: String) = Log.d("BaseRecyclerViewPager", msg)
}