package com.giz.recordcell.widgets

import android.app.Dialog
import android.content.Context
import com.giz.recordcell.R

class WaitProgressDialog(context: Context) : Dialog(context, R.style.CustomDialog) {
    init {
        setContentView(R.layout.dialog_wait_progress)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }
}