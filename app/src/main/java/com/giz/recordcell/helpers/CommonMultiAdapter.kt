package com.giz.recordcell.helpers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

abstract class CommonMultiAdapter<H, T>(context: Context,
                                        private val mData: MutableList<MultiData>,
                                        @LayoutRes private val headerLayout: Int,
                                        @LayoutRes private val itemLayout: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class ViewType {
        HEADER,
        ITEM
    }

    private val mInflater = LayoutInflater.from(context)

    data class MultiData(val data: Any, val type: ViewType)

    override fun getItemCount(): Int = mData.size

    override fun getItemViewType(position: Int): Int = mData[position].type.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if(viewType == ViewType.HEADER.ordinal){
            CommonMultiHeadHolder(mInflater.inflate(headerLayout, parent, false))
        }else{
            CommonMultiViewHolder(mInflater.inflate(itemLayout, parent, false))
        }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is CommonMultiHeadHolder){
            bindHeaderData(holder, mData[position].data as H, position)
        }else{
            bindItemData(holder as CommonMultiViewHolder, mData[position].data as T, position)
        }
    }

    abstract fun bindHeaderData(holder: CommonMultiHeadHolder, data: H, pos: Int)

    abstract fun bindItemData(holder: CommonMultiViewHolder, data: T, pos: Int)

    fun updateData(data: MutableList<MultiData>){
        mData.clear()
        mData.addAll(data)
        notifyDataSetChanged()
    }


    class CommonMultiViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class CommonMultiHeadHolder(view: View) : RecyclerView.ViewHolder(view)
}