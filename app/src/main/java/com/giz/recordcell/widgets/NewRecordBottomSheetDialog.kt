package com.giz.recordcell.widgets

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.giz.recordcell.R
import com.giz.recordcell.data.RecordCategory
import com.giz.recordcell.helpers.CommonAdapter
import com.giz.recordcell.helpers.OnPressScaleChangeTouchListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.dialog_new_record_bsd.*

class NewRecordBottomSheetDialog(context: Context) : BottomSheetDialog(context, R.style.BottomSheetDialog) {

    private val itemIcons = mapOf(
        RecordCategory.LITTLE_NOTE to R.drawable.ic_notebook,
        RecordCategory.TODO to R.drawable.ic_check,
        RecordCategory.TASKBOX to R.drawable.ic_box,
//        RecordCategory.SCHEDULE to R.drawable.ic_calendar,
        RecordCategory.DAILY to R.drawable.ic_brightness,
//        RecordCategory.ARTICLE to R.drawable.ic_paper,
        RecordCategory.FAVORITE to R.drawable.ic_folders
    )

    var onItemClickListener: OnRecordCategoryItemClickListener? = null

    interface OnRecordCategoryItemClickListener {
        fun onItemClick(rc: RecordCategory?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.dialog_new_record_bsd)
        new_record_recycler_view.adapter = object : CommonAdapter<String>(context, RecordCategory.getAvailableCategoryDescList(),
            R.layout.item_new_record_bsd) {

            override fun bindData(holder: CommonViewHolder, data: String, position: Int) {
                holder.itemView.findViewById<ImageView>(R.id.item_new_record_icon).setImageResource(
                    itemIcons[RecordCategory.getCategoryFromDesc(data)] ?: R.drawable.ic_robot
                )
                holder.itemView.findViewById<TextView>(R.id.item_new_record_text).text = data
                holder.itemView.setOnTouchListener(OnPressScaleChangeTouchListener())
                holder.itemView.setOnClickListener{
                    dismiss()
                    onItemClickListener?.onItemClick(RecordCategory.getCategoryFromDesc(data))
                }
            }
        }
    }
}