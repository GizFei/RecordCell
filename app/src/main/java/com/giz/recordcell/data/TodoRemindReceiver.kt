package com.giz.recordcell.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.giz.recordcell.R
import com.giz.recordcell.activities.TodoRemindActivity
import com.giz.recordcell.bmob.Todo
import com.giz.recordcell.helpers.printLog
import java.util.*

class TodoRemindReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_TAG = "待办事项提醒"
        private const val GROUP_KEY = "TodoRemindGroup"

        // 创建定时提醒
        fun setupAlarm(context: Context, date: Date, todo: Todo, requestCode: Int) {
            context.printLog("创建闹钟，时间：$date, requestCode: $requestCode")
            context.printLog("Todo消息：$todo")

            val intent = Intent(context, TodoRemindReceiver::class.java)
            intent.action = "com.giz.recorddemo.TODO_REMIND"
            intent.putExtra(TodoRemindActivity.EXTRA_TODO, Bundle().apply {
                putSerializable("todo", todo)
            })
            // 创建广播等待意图
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            // 获取闹钟管理器
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // 设置闹钟
            alarm.setExact(AlarmManager.RTC_WAKEUP, date.time, pendingIntent)
        }
        // 取消闹钟
        fun stopAlarm(context: Context, requestCode: Int) {
            context.printLog("停止闹钟，时间：$requestCode")

            val intent = Intent(context, TodoRemindReceiver::class.java)
            intent.action = "com.giz.recorddemo.TODO_REMIND"
            // 创建广播等待意图
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            // 获取闹钟管理器
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // 取消闹钟
            alarm.cancel(pendingIntent)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        // context.showToast("显示闹钟")
        // intent?.extras?.keySet()?.forEach {  context.printLog(it) }

        val todo = intent?.getBundleExtra(TodoRemindActivity.EXTRA_TODO)?.getSerializable("todo") as? Todo
        if(todo == null){
            Log.d(this::class.java.simpleName, "收到消息为空")
            return
        }
        Log.d(this::class.java.simpleName, "收到消息为：$todo")

        val notificationManager = NotificationManagerCompat.from(context)
        // 兼容O以上版本
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 高重要性，可以弹出悬浮通知
            if(notificationManager.getNotificationChannel(context.packageName) == null){
                val channel = NotificationChannel(context.packageName, CHANNEL_TAG, NotificationManager.IMPORTANCE_HIGH)
                channel.setShowBadge(true)
                notificationManager.createNotificationChannel(channel)
            }
        }

        val notification = NotificationCompat.Builder(context, context.packageName)
            .setTicker("待办提醒")
            .setSmallIcon(R.drawable.check)
            .setContentTitle(todo.itemName)
            .setContentText(todo.remark)
            .setGroup(GROUP_KEY)
            .setContentIntent(PendingIntent.getActivity(context, todo.requestCode,
                TodoRemindActivity.newIntent(context, todo), PendingIntent.FLAG_UPDATE_CURRENT))
            .setAutoCancel(true)
            .setNumber(1) // 设置角标的数字
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        // 用于多条归纳通知时的总结信息
        val summaryNotification = NotificationCompat.Builder(context, context.packageName)
            .setContentTitle("待办事项")
            .setContentText("所有待办事项")
            .setSmallIcon(R.drawable.ic_check)
            .setStyle(NotificationCompat.InboxStyle()
                .setSummaryText("所有待办事项")
            )
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .build()
        notificationManager.notify(todo.requestCode, notification)
        notificationManager.notify(-1, summaryNotification)
    }
}


/*
    private fun setAlarm(date: Date){
        printLog("注册闹钟")
        val intent = Intent(this, TodoRemindReceiver::class.java)
        val to do = To do().apply {
            itemName = "程序测试待办"
            remark = "程序测试待办备注文字部分"
        }
        intent.action = "com.giz.recorddemo.TODO_REMIND"
        intent.putExtra(TodoRemindActivity.EXTRA_TODO, Bundle().apply {
            putSerializable("to do", t odo)
        })
        // 创建广播等待意图
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)
        // 获取闹钟管理器
        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setExact(AlarmManager.RTC_WAKEUP, date.time, pendingIntent)
    }

    测试：
    new_taskbox_tmp_remind.setOnClickListener {
            DateTimePickerDialog(this, Date()){
                new_taskbox_tmp_remind.text = formatNoteDateText(it)
                val to do = To do().apply {
                itemName = "程序测试待办"
                remark = "程序测试待办备注文字部分"
            }
                TodoRemindReceiver.setupAlarm(this, it, to do, 0)
            }.show()
        }
     */