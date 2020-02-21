package com.giz.recordcell.helpers

import android.content.Context

object SharedPrefUtils {

    private const val PREF_BROADCAST_RC = "BroadcastPref"

    fun getBroadCastRequestCode(context: Context, userId: String): Int{
        getSharedPreferences(context).apply {
            return if(contains(userId)){
                val rc = getInt(userId, 0) + 1
                edit().putInt(userId, rc).apply()
                rc
            }else{
                edit().putInt(userId, 0).apply()
                0
            }
        }
    }

    fun setBroadCastRequestCode(context: Context, userId: String, requestCode: Int) {
        getSharedPreferences(context).apply {
            edit().putInt(userId, requestCode).apply()
        }
    }

    fun containsUserPreferences(context: Context, userId: String) = getSharedPreferences(context)
        .contains(userId)

    private fun getSharedPreferences(context: Context) = context.applicationContext
        .getSharedPreferences(PREF_BROADCAST_RC, Context.MODE_PRIVATE)

}