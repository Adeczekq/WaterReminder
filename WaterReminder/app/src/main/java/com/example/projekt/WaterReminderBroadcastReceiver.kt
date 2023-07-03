package com.example.projekt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class WaterReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ACTION_ZERO_WATER") {
            val mainActivityIntent = Intent(context, MainActivity::class.java)
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mainActivityIntent.putExtra("ACTION_ZERO_WATER", true)
            context.startActivity(mainActivityIntent)
        }
        else {
            MainActivity.showNotification(context)
        }
    }
}
