package com.hstech.networkspeedmoniter

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Icon
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class InternetSpeedMeter : Service() {

    private var channel = ""
    private lateinit var scheduledExecutorService: ScheduledExecutorService
    private var unit = ""

    override fun onCreate() {
        val sharedPreferences = getSharedPreferences("speedUnit", Context.MODE_PRIVATE)
        unit = sharedPreferences?.getString("1", getString(R.string.Bps))!!
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Thread {
            var notification: Notification.Builder
            var oldReceived: Long = TrafficStats.getTotalRxBytes()
            var oldTransmitted: Long = TrafficStats.getTotalTxBytes()
            var dSpeedForNotification: String
            var uSpeedForNotification: String
            val activityIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            scheduledExecutorService = Executors.newScheduledThreadPool(5)
            scheduledExecutorService.scheduleAtFixedRate({

                val transmittedBytes = TrafficStats.getTotalTxBytes()
                val receivedBytes = TrafficStats.getTotalRxBytes()
                dSpeedForNotification = getSpeedForNotification(receivedBytes, oldReceived)
                uSpeedForNotification = getSpeedForNotification(transmittedBytes, oldTransmitted)
                val str = "Download: $dSpeedForNotification   Upload: $uSpeedForNotification"
                val icon = Icon.createWithBitmap(getSpeedForIcon(dSpeedForNotification))
                notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    channel = createNotificationChannel("111", "Speed Monitor Service")
                    Notification.Builder(this, channel)
                        .setSmallIcon(icon)
                        .setContentText(str)
                        .setShowWhen(false)
                        .setContentIntent(pendingIntent)
                } else {
                    Notification.Builder(this)
                        .setSmallIcon(icon)
                        .setContentText(str)
                        .setContentTitle(getString(R.string.app_name))
                        .setShowWhen(false)
                        .setContentIntent(pendingIntent)
                }
                startForeground(111, notification.build())
                oldReceived = receivedBytes
                oldTransmitted = transmittedBytes
            }, 0, 1, TimeUnit.SECONDS)

        }.start()
        return START_STICKY
    }

    private fun getSpeedForNotification(new: Long, old: Long): String {
        var speed = (new - old) / 1f
        speed /= 1024
        return if (unit == getString(R.string.bps)) {
            speed *= 8
            if (speed < 1000)
                String.format("%.2f", speed) + " Kb/s"
            else
                String.format("%.2f", speed / 1024) + " Mb/s"
        } else {
            if (speed < 1000)
                String.format("%.2f", speed) + " KB/s"
            else
                String.format("%.2f", speed / 1024) + " MB/s"
        }
    }

    private fun getSpeedForIcon(speedStr: String): Bitmap {
        val speed = speedStr.split(" ")[0].toFloat()
        val iconUnit = speedStr.split(" ")[1]
        val iconSpeed: String
        iconSpeed = if (iconUnit[0] == 'K') speed.toInt().toString()
        else if (iconUnit[0] == 'M' && speed < 10) speed.toString().substring(0, 3)
        else speed.toInt().toString()
        return createBitmapFromString(iconSpeed, iconUnit)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_LOW
        )
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }

    private fun createBitmapFromString(speed: String, units: String): Bitmap {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.textSize = 200f
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.WHITE
        paint.typeface = Typeface.createFromAsset(assets, "fonts/Oswald-Bold.ttf")
        val unitsPaint = Paint()
        unitsPaint.isAntiAlias = true
        unitsPaint.textSize = 130f
        unitsPaint.textAlign = Paint.Align.CENTER
        unitsPaint.color = Color.WHITE
        unitsPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val textBounds = Rect()
        paint.getTextBounds(speed, 0, speed.length, textBounds)
        val unitsTextBounds = Rect()
        unitsPaint.getTextBounds(units, 0, units.length, unitsTextBounds)
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawText(speed, 150f, 170f, paint)
        canvas.drawText(units, 150f, 300f, unitsPaint)
        return bitmap
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        scheduledExecutorService.shutdown()
    }
}