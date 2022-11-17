package com.hiennv.flutter_callkit_incoming

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle

class TransparentActivity : Activity() {

    companion object {

        fun getIntentAccept(context: Context, data: Bundle?): Intent {
            val intent = Intent(context, TransparentActivity::class.java)
            intent.putExtra("data", data)
            intent.putExtra("type", "ACCEPT")
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            return intent
        }

        fun getIntentCallback(context: Context, data: Bundle?): Intent {
            val intent = Intent(context, TransparentActivity::class.java)
            intent.putExtra("data", data)
            intent.putExtra("type", "CALLBACK")
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            return intent
        }

    }


    override fun onStart() {
        super.onStart()
        setVisible(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = intent.getStringExtra("type")
        when (type) {
            "ACCEPT" -> {
                AppUtils.logger("ACTION_CALL_ACCEPT onCreate - sendBroadcast(ACCEPT)")
                val data = intent.getBundleExtra("data")
                val acceptIntent = CallkitIncomingBroadcastReceiver.getIntentAccept(this@TransparentActivity, data)
                sendBroadcast(acceptIntent)
            }
            "CALLBACK" -> {
                val data = intent.getBundleExtra("data")
                val acceptIntent = CallkitIncomingBroadcastReceiver.getIntentCallback(this@TransparentActivity, data)
                sendBroadcast(acceptIntent)
            }
            else -> { // Note the block
                AppUtils.logger("ACTION_CALL_ACCEPT onCreate - default - sendBroadcast($type)")
                val data = intent.getBundleExtra("data")
                val acceptIntent = CallkitIncomingBroadcastReceiver.getIntentAccept(this@TransparentActivity, data)
                sendBroadcast(acceptIntent)
            }
        }
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppUtils.logger("ACTION_CALL_ACCEPT onConfigurationChanged - newConfig: $newConfig")
    }
}