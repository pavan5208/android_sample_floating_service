package com.appz.screen.android_sample_floating_chat_head

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import androidx.core.app.NotificationCompat
import com.appz.screen.android_sample_floating_chat_head.MainActivity.Companion.ACTION_STOP_FOREGROUND

class FloatingControlService :Service() {

    private var windowManager: WindowManager? = null
    private var floatingControlView: ViewGroup? = null
    var iconHeight = 0
    var iconWidth = 0
    private var screenHeight = 0
    private var screenWidth = 0
    private var hideHandler: Handler? = null
    private var hideRunnable: Runnable? = null
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(windowManager == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        }
        if(floatingControlView == null ){
            val li = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            floatingControlView = li.inflate(R.layout.layout_floating_control_view, null) as ViewGroup?
        }
        if (intent?.action != null && intent.action.equals(
                ACTION_STOP_FOREGROUND, ignoreCase = true)) {
            removeFloatingContro()
            stopForeground(true)
            stopSelf()
        }else {
            generateForegroundNotification()
            addFloatingMenu()
        }
        return START_STICKY

        //Normal Service To test sample service comment the above    generateForegroundNotification() && return START_STICKY
        // Uncomment below return statement And run the app.
//        return START_NOT_STICKY
    }

    private fun removeFloatingContro() {
        if(floatingControlView?.parent !=null) {
            windowManager?.removeView(floatingControlView)
        }
    }

    private fun addFloatingMenu() {
        if (floatingControlView?.parent == null) {
            //Set layout params to display the controls over any screen.
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_PHONE else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            params.height = dpToPx(50)
            params.width = dpToPx(50)
            iconWidth = params.width
            iconHeight = params.height
            screenHeight = windowManager?.defaultDisplay?.height ?: 0
            screenWidth = windowManager?.defaultDisplay?.width ?: 0
            //Initial position of the floating controls
            params.gravity = Gravity.TOP or Gravity.START
            params.x = 0
            params.y = 100

            //Add the view to window manager
            windowManager?.addView(floatingControlView, params)
            try {
                addOnTouchListener(params)
            } catch (e: Exception) {
                // TODO: handle exception
            }

        }
    }

    private fun addOnTouchListener(params: WindowManager.LayoutParams) {
        //Add touch listerner to floating controls view to move/close/expand the controls
            floatingControlView?.setOnTouchListener(object : View.OnTouchListener {
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var initialX = 0
                private var initialY = 0

                override fun onTouch(view: View?, motionevent: MotionEvent): Boolean {
                    val flag3: Boolean
                    flag3 = true
                    var flag = false
                    when (motionevent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            params.alpha = 1.0f
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = motionevent.rawX
                            initialTouchY = motionevent.rawY
                            Log.d(
                                "OnTouchListener",
                                java.lang.StringBuilder("POS: x = ")
                                    .append(initialX).append(" y = ")
                                    .append(initialY).append(" tx = ")
                                    .append(initialTouchX)
                                    .append(" ty = ")
                                    .append(initialTouchY).toString()
                            )
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            flag = flag3
                            if (Math.abs(initialTouchX - motionevent.rawX) >= 25f) {
                                return flag
                            } else {
                                flag = flag3
                                if (Math.abs(
                                        initialTouchY
                                                - motionevent.rawY
                                    ) >= 25f
                                ) {
                                    return flag
                                } else {
                                    return true
                                }
                            }
                        }
                        MotionEvent.ACTION_MOVE -> {
                            initialX = params.x
                            initialY = params.y
                            if ((motionevent.rawX < (initialX - iconWidth / 2).toFloat()) || (motionevent.rawY < (initialY - iconHeight / 2).toFloat()) || (motionevent.rawX
                                    .toDouble() > initialX.toDouble() + iconWidth.toDouble() * 1.2)
                            ) {
                            }
                            params.x = (motionevent.rawX - (iconWidth / 2).toFloat()).toInt()
                            params.y = (motionevent.rawY - iconHeight.toFloat()).toInt()
                            try {
                                windowManager?.updateViewLayout(floatingControlView, params)
                            } // Misplaced declaration of an exception
                            // variable
                            catch (e: java.lang.Exception) {
                                e.printStackTrace()
//                                ExceptionHandling(e)
                                return true
                            }
                            return true
                        }
                        else -> {
                        }
                    }
                    return flag
                }
            })


    }



    //Notififcation for ON-going
    private var iconNotification: Bitmap? = null
    private var notification: Notification? = null
    var mNotificationManager: NotificationManager? = null
    private val mNotificationId = 123

    private fun generateForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intentMainLanding = Intent(this, MainActivity::class.java)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intentMainLanding, 0)
            iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            if (mNotificationManager == null) {
                mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assert(mNotificationManager != null)
                mNotificationManager?.createNotificationChannelGroup(
                    NotificationChannelGroup("chats_group", "Chats")
                )
                val notificationChannel =
                    NotificationChannel("service_channel", "Service Notifications",
                        NotificationManager.IMPORTANCE_MIN)
                notificationChannel.enableLights(false)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
                mNotificationManager?.createNotificationChannel(notificationChannel)
            }
            val builder = NotificationCompat.Builder(this, "service_channel")

            builder.setContentTitle(StringBuilder(resources.getString(R.string.app_name)).append(" service is running").toString())
                .setTicker(StringBuilder(resources.getString(R.string.app_name)).append("service is running").toString())
                .setContentText("Touch to open") //                    , swipe down for more options.
                .setSmallIcon(R.drawable.ic_alaram)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setWhen(0)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
            if (iconNotification != null) {
                builder.setLargeIcon(Bitmap.createScaledBitmap(iconNotification!!, 128, 128, false))
            }
            builder.color = resources.getColor(R.color.purple_200)
            notification = builder.build()
            startForeground(mNotificationId, notification)
        }

    }

    //Method to convert dp to px
    private fun dpToPx(dp: Int): Int {
        val displayMetrics = this.resources.displayMetrics
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

}