package com.giz.recordcell.data

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.UpdateListener
import com.giz.recordcell.R
import com.giz.recordcell.bmob.RecordUser
import com.giz.recordcell.helpers.CommonAdapter
import com.giz.recordcell.helpers.printLog
import com.giz.recordcell.helpers.showToast
import com.giz.recordcell.widgets.WaitProgressDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FunctionListOrderDialog(private val _context: Context,
                              private val user: RecordUser) : MaterialAlertDialogBuilder(_context) {

    private val alertDialog: AlertDialog

    init {
        val view = LayoutInflater.from(_context).inflate(R.layout.dialog_function_list_order, null).apply {
            initOrderRv(findViewById(R.id.function_list_order_rv))
            findViewById<TextView>(R.id.function_list_order_save_btn).setOnClickListener {
                updateUserFunctionList()
            }
        }
        setView(view)
        alertDialog = show()
    }

    private fun initOrderRv(rv: RecyclerView) {
        val adapter = object : CommonAdapter<String>(_context, RecordCategory.getAvailableCategoryDescList(), R.layout.item_function_list){
            override fun bindData(holder: CommonViewHolder, data: String, position: Int) {
                holder.setText(R.id.function_name, data)
            }
        }
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {

            private val _functionList = mutableListOf<String>().apply {
                addAll(RecordCategory.getAvailableCategoryDescList())
            }

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val pos1 = viewHolder.adapterPosition
                val pos2 = target.adapterPosition
                _context.printLog("交换：$pos1, $pos2")
                // Collections.swap(functionList, pos1, pos2)
                _functionList[pos1] = _functionList.set(pos2, _functionList[pos1])
                _functionList.forEach {
                    _context.printLog("交换后：$it")
                }
                RecordCategory.functionDescList = _functionList
                adapter.swapItem(pos1, pos2)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        rv.adapter = adapter
        itemTouchHelper.attachToRecyclerView(rv)
    }

    private fun updateUserFunctionList() {
        alertDialog.dismiss()

        val waitProgressDialog = WaitProgressDialog(_context).apply { show() }
        user.functionListOrder = RecordCategory.getAvailableCategoryDescList()
        user.update(object : UpdateListener() {
            override fun done(p0: BmobException?) {
                waitProgressDialog.dismiss()
                if(p0 == null){
                    _context.showToast("更新成功")
                }else{
                    _context.showToast("更新失败")
                    _context.printLog("更新列表失败：$p0")
                }
            }
        })
    }

}