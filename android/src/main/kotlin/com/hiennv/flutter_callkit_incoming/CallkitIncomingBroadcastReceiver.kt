package com.hiennv.flutter_callkit_incoming

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.telecom.TelecomManager
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class CallkitIncomingBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallkitIncomingReceiver"
        var silenceEvents = false

        fun getIntent(context: Context, action: String, data: Bundle?) =
            Intent().apply {
                setClassName(context.packageName, "com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver")
                this.action = "${context.packageName}.${action}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
                `package` = context.packageName
            }

        fun getIntentIncoming(context: Context, data: Bundle?) =
            Intent().apply {
                setClassName(context.packageName, "com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver")
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_INCOMING}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
                `package` = context.packageName
            }

        fun getIntentStart(context: Context, data: Bundle?) =
            Intent().apply {
                setClassName(context.packageName, "com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver")
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_START}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
                `package` = context.packageName
            }

        fun getIntentAccept(context: Context, data: Bundle?) =
            Intent().apply {
                setClassName(context.packageName, "com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver")
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_ACCEPT}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
                `package` = context.packageName
            }

        fun getIntentDecline(context: Context, data: Bundle?) =
            Intent().apply {
                setClassName(context.packageName, "com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver")
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_DECLINE}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
                `package` = context.packageName
            }

        fun getIntentEnded(context: Context, data: Bundle?) =
            Intent().apply {
                setClassName(context.packageName, "com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver")
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_ENDED}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
                `package` = context.packageName
            }

        fun getIntentTimeout(context: Context, data: Bundle?) =
            Intent().apply {
                setClassName(context.packageName, "com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver")
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_TIMEOUT}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
                `package` = context.packageName
            }

        fun getIntentCallback(context: Context, data: Bundle?) =
            Intent().apply {
                setClassName(context.packageName, "com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver")
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_CALLBACK}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
                `package` = context.packageName
            }

        fun getIntentHeldByCell(context: Context, data: Bundle?) =
            Intent().apply {
                setClassName(context.packageName, "com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver")
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_HELD}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
                `package` = context.packageName
            }

        fun getIntentUnHeldByCell(context: Context, data: Bundle?) =
            Intent().apply {
                setClassName(context.packageName, "com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver")
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_UNHELD}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
                `package` = context.packageName
            }

        fun getIntentConnected(context: Context, data: Bundle?) =
            Intent().apply {
                setClassName(context.packageName, "com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver")
                action = "${context.packageName}.${CallkitConstants.ACTION_CALL_CONNECTED}"
                putExtra(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA, data)
                `package` = context.packageName
            }
    }

    // Get notification manager dynamically to handle plugin lifecycle properly
    private fun getCallkitNotificationManager(): CallkitNotificationManager? {
        return FlutterCallkitIncomingPlugin.getInstance()?.getCallkitNotificationManager()
    }

    @SuppressLint("MissingPermission")
    private fun registerTelecomIncomingCall(context: Context, data: Bundle) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val parsed = try {
            Data.fromBundle(data)
        } catch (e: Exception) {
            null
        } ?: return
        if (parsed.id.isEmpty()) return
        if (CallkitConnection.find(parsed.id) != null) {
            Log.d(TAG, "Telecom call already registered id=${parsed.id} — skip")
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.MANAGE_OWN_CALLS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "MANAGE_OWN_CALLS not granted — Telecom incoming skipped")
            return
        }
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return
        val manager = InAppCallManager(context.applicationContext)
        val handle = manager.getPhoneAccountHandle()
        val extras = Bundle().apply {
            putBundle(CallkitConnection.EXTRA_CALL_BUNDLE, data)
            putInt(
                TelecomManager.EXTRA_INCOMING_VIDEO_STATE,
                android.telecom.VideoProfile.STATE_AUDIO_ONLY,
            )
        }
        try {
            telecom.addNewIncomingCall(handle, extras)
            Log.d(TAG, "Telecom addNewIncomingCall id=${parsed.id}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Telecom addNewIncomingCall rejected: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Telecom addNewIncomingCall error: ${e.message}")
        }
    }

    private fun driveTelecomConnection(context: Context, data: Bundle, action: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val parsed = try {
            Data.fromBundle(data)
        } catch (e: Exception) {
            null
        } ?: return
        val conn = CallkitConnection.find(parsed.id) ?: return
        when (action) {
            CallkitConstants.ACTION_CALL_ACCEPT -> conn.markAccepted()
            CallkitConstants.ACTION_CALL_DECLINE -> conn.markDeclined(context)
            CallkitConstants.ACTION_CALL_ENDED -> conn.markEnded()
            CallkitConstants.ACTION_CALL_TIMEOUT -> conn.markMissed()
        }
    }


    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val data = intent.extras?.getBundle(CallkitConstants.EXTRA_CALLKIT_INCOMING_DATA) ?: return

        Log.d(TAG, action)

        when (action) {
            "${context.packageName}.${CallkitConstants.ACTION_CALL_INCOMING}" -> {
                try {
                    registerTelecomIncomingCall(context, data)
                    val incomingData = Data.fromBundle(data)
                    if (incomingData.isFullScreen && isScreenLockedOrOff(context)) {
                        val intent = CallkitIncomingActivityWimi.getIntent(context, data)
                        context.startActivity(intent)
                        sendEventFlutter(CallkitConstants.ACTION_CALL_INCOMING, data)
                        addCall(context, incomingData)
                    } else {
                        // Screen is on — show a discrete heads-up notification WITHOUT
                        // fullScreenIntent so the app is never forced to the foreground.
                        showDiscreteIncomingNotification(context, data)
                        sendEventFlutter(CallkitConstants.ACTION_CALL_INCOMING, data)
                        addCall(context, incomingData)
                    }
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_START}" -> {
                try {
                    // start service and show ongoing call when call is accepted
                    CallkitNotificationService.startServiceWithAction(
                        context,
                        CallkitConstants.ACTION_CALL_START,
                        data
                    )
                    sendEventFlutter(CallkitConstants.ACTION_CALL_START, data)
                    addCall(context, Data.fromBundle(data), true)
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_ACCEPT}" -> {
                try {
                    driveTelecomConnection(context, data, CallkitConstants.ACTION_CALL_ACCEPT)
                    FlutterCallkitIncomingPlugin.notifyEventCallbacks(CallkitEventCallback.CallEvent.ACCEPT, data)
                    // start service and show ongoing call when call is accepted
                    CallkitNotificationService.startServiceWithAction(
                        context,
                        CallkitConstants.ACTION_CALL_ACCEPT,
                        data
                    )
                    sendEventFlutter(CallkitConstants.ACTION_CALL_ACCEPT, data)
                    addCall(context, Data.fromBundle(data), true)
                    FlutterCallkitIncomingPlugin.acceptCallHandleCallback(data)
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_DECLINE}" -> {
                try {
                    driveTelecomConnection(context, data, CallkitConstants.ACTION_CALL_DECLINE)
                    FlutterCallkitIncomingPlugin.notifyEventCallbacks(CallkitEventCallback.CallEvent.DECLINE, data)
                    // clear notification
                    getCallkitNotificationManager()?.clearIncomingNotification(data, false)
                    sendEventFlutter(CallkitConstants.ACTION_CALL_DECLINE, data)
                    removeCall(context, Data.fromBundle(data))
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_ENDED}" -> {
                try {
                    driveTelecomConnection(context, data, CallkitConstants.ACTION_CALL_ENDED)
                    FlutterCallkitIncomingPlugin.notifyEventCallbacks(CallkitEventCallback.CallEvent.END, data)
                    // clear notification and stop service
                    getCallkitNotificationManager()?.clearIncomingNotification(data, false)
                    CallkitNotificationService.stopService(context)
                    sendEventFlutter(CallkitConstants.ACTION_CALL_ENDED, data)
                    removeCall(context, Data.fromBundle(data))
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_TIMEOUT}" -> {
                try {
                    driveTelecomConnection(context, data, CallkitConstants.ACTION_CALL_TIMEOUT)
                    // clear notification and show miss notification
                    val notificationManager = getCallkitNotificationManager()
                    notificationManager?.clearIncomingNotification(data, false)
                    notificationManager?.showMissCallNotification(data)
                    sendEventFlutter(CallkitConstants.ACTION_CALL_TIMEOUT, data)
                    removeCall(context, Data.fromBundle(data))
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_CONNECTED}" -> {
                try {
                    // update notification on going connected
                    getCallkitNotificationManager()?.showOngoingCallNotification(data, true)
                    sendEventFlutter(CallkitConstants.ACTION_CALL_CONNECTED, data)
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }

            "${context.packageName}.${CallkitConstants.ACTION_CALL_CALLBACK}" -> {
                try {
                    getCallkitNotificationManager()?.clearMissCallNotification(data)
                    sendEventFlutter(CallkitConstants.ACTION_CALL_CALLBACK, data)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        val closeNotificationPanel = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                        context.sendBroadcast(closeNotificationPanel)
                    }
                } catch (error: Exception) {
                    Log.e(TAG, null, error)
                }
            }
        }
    }

    /**
     * Show a discrete heads-up notification for an incoming call when the screen is on.
     * Unlike [CallkitNotificationManager.showIncomingNotification], this method does NOT set a
     * fullScreenIntent or a contentIntent that opens an activity, so the app is never forced to the
     * foreground when the notification appears or when the user taps Decline.
     */
    @SuppressLint("MissingPermission")
    private fun showDiscreteIncomingNotification(context: Context, data: Bundle) {
        // Play the ringtone / vibration (mirrors CallkitNotificationManager.showIncomingNotification)
        FlutterCallkitIncomingPlugin.getInstance()?.getCallkitSoundPlayerManager()?.play(data)

        val notificationId =
            data.getString(CallkitConstants.EXTRA_CALLKIT_ID, "callkit_incoming").hashCode()
        val duration = data.getLong(CallkitConstants.EXTRA_CALLKIT_DURATION, 0L)
        val caller = data.getString(CallkitConstants.EXTRA_CALLKIT_NAME_CALLER, "") ?: ""
        val typeCall = data.getInt(CallkitConstants.EXTRA_CALLKIT_TYPE, -1)
        val actionColor = data.getString(CallkitConstants.EXTRA_CALLKIT_ACTION_COLOR, "#4CAF50") ?: "#4CAF50"
        val textDecline = data.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_DECLINE, "") ?: ""
        val textAccept = data.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_ACCEPT, "") ?: ""

        // Ensure the notification channel exists
        getCallkitNotificationManager()?.createNotificationChanel(data)

        // Decline → broadcast — does NOT bring the app to the foreground
        val declinePendingIntent = run {
            val intent = getIntentDecline(context, data)
            PendingIntent.getBroadcast(context, notificationId, intent, getPendingIntentFlags())
        }
        // Accept → TransparentActivity — intentionally brings the app to the foreground
        val acceptPendingIntent = run {
            val intent = TransparentActivity.getIntent(context, CallkitConstants.ACTION_CALL_ACCEPT, data)
            PendingIntent.getActivity(context, notificationId, intent, getPendingIntentFlags())
        }
        // Timeout — fired when the notification auto-dismisses after [duration] ms
        val timeoutPendingIntent = run {
            val intent = getIntentTimeout(context, data)
            PendingIntent.getBroadcast(context, notificationId, intent, getPendingIntentFlags())
        }

        val smallIcon = if (typeCall > 0) R.drawable.ic_video else R.drawable.ic_accept
        val declineLabel = if (TextUtils.isEmpty(textDecline)) context.getString(R.string.text_decline) else textDecline
        val acceptLabel  = if (TextUtils.isEmpty(textAccept))  context.getString(R.string.text_accept)  else textAccept

        val builder = NotificationCompat.Builder(context, CallkitNotificationManager.NOTIFICATION_CHANNEL_ID_INCOMING)
            .setSmallIcon(smallIcon)
            .setContentTitle(caller)
            .setContentText(data.getString(CallkitConstants.EXTRA_CALLKIT_HANDLE, ""))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSound(null)
            .setTimeoutAfter(duration)
            .setDeleteIntent(timeoutPendingIntent)
            // No setContentIntent  → tapping the notification body does nothing (no app launch)
            // No setFullScreenIntent → the notification stays discrete, never forces the app to
            //                          the foreground (avoids launching CallkitIncomingActivity)
            .addAction(R.drawable.ic_decline, declineLabel, declinePendingIntent)
            .addAction(R.drawable.ic_accept,  acceptLabel,  acceptPendingIntent)

        try { builder.color = Color.parseColor(actionColor) } catch (_: Exception) {}

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun getPendingIntentFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else
            PendingIntent.FLAG_UPDATE_CURRENT

    private fun isScreenLockedOrOff(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val screenInteractive = pm?.isInteractive == true
        val keyguardLocked = km?.isKeyguardLocked == true
        return !screenInteractive || keyguardLocked
    }

    private fun sendEventFlutter(event: String, data: Bundle) {
        if (silenceEvents) return

        val android = mapOf(
            "isCustomNotification" to data.getBoolean(
                CallkitConstants.EXTRA_CALLKIT_IS_CUSTOM_NOTIFICATION,
                false
            ),
            "isCustomSmallExNotification" to data.getBoolean(
                CallkitConstants.EXTRA_CALLKIT_IS_CUSTOM_SMALL_EX_NOTIFICATION,
                false
            ),
            "ringtonePath" to data.getString(CallkitConstants.EXTRA_CALLKIT_RINGTONE_PATH, ""),
            "backgroundColor" to data.getString(
                CallkitConstants.EXTRA_CALLKIT_BACKGROUND_COLOR,
                ""
            ),
            "backgroundUrl" to data.getString(CallkitConstants.EXTRA_CALLKIT_BACKGROUND_URL, ""),
            "actionColor" to data.getString(CallkitConstants.EXTRA_CALLKIT_ACTION_COLOR, ""),
            "textColor" to data.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_COLOR, ""),
            "incomingCallNotificationChannelName" to data.getString(
                CallkitConstants.EXTRA_CALLKIT_INCOMING_CALL_NOTIFICATION_CHANNEL_NAME,
                ""
            ),
            "missedCallNotificationChannelName" to data.getString(
                CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_NOTIFICATION_CHANNEL_NAME,
                ""
            ),
            "isImportant" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_IMPORTANT, true),
            "isBot" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_IS_BOT, false),
        )
        val missedCallNotification = mapOf(
            "id" to data.getInt(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_ID),
            "showNotification" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_SHOW),
            "count" to data.getInt(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_COUNT),
            "subtitle" to data.getString(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_SUBTITLE),
            "callbackText" to data.getString(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_CALLBACK_TEXT),
            "isShowCallback" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_MISSED_CALL_CALLBACK_SHOW),
        )
        val callingNotification = mapOf(
            "id" to data.getString(CallkitConstants.EXTRA_CALLKIT_CALLING_ID),
            "showNotification" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_CALLING_SHOW),
            "subtitle" to data.getString(CallkitConstants.EXTRA_CALLKIT_CALLING_SUBTITLE),
            "callbackText" to data.getString(CallkitConstants.EXTRA_CALLKIT_CALLING_HANG_UP_TEXT),
            "isShowCallback" to data.getBoolean(CallkitConstants.EXTRA_CALLKIT_CALLING_HANG_UP_SHOW),
        )
        val forwardData = mapOf(
            "id" to data.getString(CallkitConstants.EXTRA_CALLKIT_ID, ""),
            "nameCaller" to data.getString(CallkitConstants.EXTRA_CALLKIT_NAME_CALLER, ""),
            "avatar" to data.getString(CallkitConstants.EXTRA_CALLKIT_AVATAR, ""),
            "number" to data.getString(CallkitConstants.EXTRA_CALLKIT_HANDLE, ""),
            "type" to data.getInt(CallkitConstants.EXTRA_CALLKIT_TYPE, 0),
            "duration" to data.getLong(CallkitConstants.EXTRA_CALLKIT_DURATION, 0L),
            "textAccept" to data.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_ACCEPT, ""),
            "textDecline" to data.getString(CallkitConstants.EXTRA_CALLKIT_TEXT_DECLINE, ""),
            "acceptColor" to data.getString(CallkitConstants.EXTRA_CALLKIT_ACCEPT_COLOR, ""),
            "declineColor" to data.getString(CallkitConstants.EXTRA_CALLKIT_DECLINE_COLOR, ""),
            "extra" to data.getSerializable(CallkitConstants.EXTRA_CALLKIT_EXTRA),
            "missedCallNotification" to missedCallNotification,
            "callingNotification" to callingNotification,
            "android" to android
        )
        FlutterCallkitIncomingPlugin.sendEvent(event, forwardData)
    }
}
