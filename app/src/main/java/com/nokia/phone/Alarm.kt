package com.nokia.phone

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import java.util.Calendar

data class Alarm(val h: Int, val m: Int, val on: Boolean)

/** Lưu danh sách báo thức (SharedPreferences) và đặt lịch bằng AlarmManager. */
object AlarmStore {
    private const val PREF = "alarms"
    private const val MAX = 10

    fun load(ctx: Context): List<Alarm> {
        val s = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("list", "") ?: ""
        return s.split(",").mapNotNull { p ->
            val x = p.split(":")
            val h = x.getOrNull(0)?.toIntOrNull()
            val m = x.getOrNull(1)?.toIntOrNull()
            if (x.size == 3 && h != null && m != null) Alarm(h, m, x[2] == "1") else null
        }.take(MAX)
    }

    fun save(ctx: Context, list: List<Alarm>) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("list", list.joinToString(",") { "${it.h}:${it.m}:${if (it.on) 1 else 0}" }).apply()
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Nhạc báo thức: chưa chọn = mặc định của máy, chuỗi rỗng = im lặng. */
    fun alarmSound(ctx: Context): Uri? {
        val s = prefs(ctx).getString("sound", null)
        return when {
            s == null -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            s.isEmpty() -> null
            else -> Uri.parse(s)
        }
    }
    fun setAlarmSound(ctx: Context, uri: Uri?) { prefs(ctx).edit().putString("sound", uri?.toString() ?: "").apply() }
    fun resetAlarmSound(ctx: Context) { prefs(ctx).edit().remove("sound").apply() }

    private fun pi(ctx: Context, i: Int): PendingIntent = PendingIntent.getBroadcast(
        ctx, 100 + i, Intent(ctx, AlarmReceiver::class.java).putExtra("i", i),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Huỷ lịch cũ rồi đặt lại cho lần reo kế tiếp của từng báo thức đang bật (lặp mỗi ngày). */
    fun schedule(ctx: Context, list: List<Alarm> = load(ctx)) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until MAX) am.cancel(pi(ctx, i))
        val show = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        list.take(MAX).forEachIndexed { i, a ->
            if (!a.on) return@forEachIndexed
            val c = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, a.h); set(Calendar.MINUTE, a.m)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            try {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(c.timeInMillis, show), pi(ctx, i))
            } catch (_: SecurityException) {}
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val i = intent.getIntExtra("i", 0)
        val a = AlarmStore.load(ctx).getOrNull(i)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sound = AlarmStore.alarmSound(ctx)
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel("nokia_alarm", "Báo thức", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(sound, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
            }
            nm.deleteNotificationChannel("nokia_alarm")   // tạo lại để áp dụng nhạc báo thức mới chọn
            nm.createNotificationChannel(ch)
        }
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        @Suppress("DEPRECATION")
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(ctx, "nokia_alarm")
        else Notification.Builder(ctx).setSound(sound).setPriority(Notification.PRIORITY_HIGH)
        val n = b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Báo thức")
            .setContentText(a?.let { "%02d:%02d".format(it.h, it.m) } ?: "")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT   // lặp tiếng chuông đến khi tắt thông báo
        try { nm.notify(2000 + i, n) } catch (_: SecurityException) {}
        AlarmStore.schedule(ctx)                            // đặt lại cho ngày mai
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) { AlarmStore.schedule(ctx) }
}
