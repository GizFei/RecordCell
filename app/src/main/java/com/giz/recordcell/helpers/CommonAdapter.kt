package com.giz.recordcell.helpers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import java.util.*

abstract class CommonAdapter<T>(context: Context,
                                private val mData: MutableList<T>,
                                @LayoutRes private val mHolderLayout: Int) : RecyclerView.Adapter<CommonAdapter.CommonViewHolder>() {
    /*
    只针对ViewHolder只有一种类型的适配器
     */
    private val mLayoutInflater = LayoutInflater.from(context)

    // 抽象函数
    /**
     * @param holder: 视图托管类
     * @param data: 数据
     * @param position: 数据索引
     */
    abstract fun bindData(holder: CommonViewHolder, data: T, position: Int)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonViewHolder
            = CommonViewHolder(mLayoutInflater.inflate(mHolderLayout, parent, false))

    override fun onBindViewHolder(holder: CommonViewHolder, position: Int) {
        bindData(holder, mData[position], position)
    }

    override fun getItemCount(): Int = mData.size

    // ========================
    // 自定义方法
    // ========================

    // 添加新项
    fun addItem(newItem: T, pos: Int = -1){
        if(pos == -1){
            mData.add(newItem)
            notifyItemInserted(mData.size - 1)
        }else{
            if(pos in 0..mData.size) {  // 检查范围
                mData.add(pos, newItem)
                notifyItemInserted(pos)
            }
        }
    }
    // 移除某项
    fun removeItem(pos: Int){
        if(pos in 0 until mData.size){
            mData.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }
    // 更新全部数据
    fun updateData(newData: MutableList<T>){
        mData.clear()
        mData.addAll(newData)
        notifyDataSetChanged()
    }
    // 更新某项数据
    fun updateItem(pos: Int, newItem: T){
        if(pos in 0 until mData.size){
            mData[pos] = newItem
            notifyItemChanged(pos)
        }
    }
    // 交换两项数据的位置
    fun swapItem(pos1: Int, pos2: Int) {
        Collections.swap(mData, pos1, pos2)
        notifyItemMoved(pos1, pos2)
    }
    // 获取某项数据
    fun getItem(pos: Int): T = mData[pos]

    class CommonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun setText(id: Int, text: String) {
            itemView.findViewById<TextView>(id).text = text
        }
    }
}