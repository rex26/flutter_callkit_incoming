package com.hiennv

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hiennv.flutter_callkit_incoming.CallkitIncomingBroadcastReceiver
import com.hiennv.flutter_callkit_incoming.CallkitSoundPlayerService

class SoundPlayerWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams)  {
    private var data: String? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun doWork(): Result {
        data = inputData.getString(CallkitIncomingBroadcastReceiver.EXTRA_CALLKIT_RINGTONE_PATH)
        playSound()
        return Result.success()
    }

    override fun onStopped() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        super.onStopped()
    }

    private fun playSound() {
        Log.v("call kit","playSound")
        val sound = data
        Log.v("call kit",sound.toString())

        var uri = sound?.let { getRingtoneUri(it) }
        if (uri == null) {
            uri = RingtoneManager.getActualDefaultRingtoneUri(
                applicationContext,
                RingtoneManager.TYPE_RINGTONE
            )
        }
        Log.v("call kit",uri.toString())
        mediaPlayer = MediaPlayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attribution = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setLegacyStreamType(AudioManager.STREAM_RING)
                .build()
            mediaPlayer?.setAudioAttributes(attribution)
        } else {
            mediaPlayer?.setAudioStreamType(AudioManager.STREAM_RING)
        }
        try {
            mediaPlayer?.setDataSource(applicationContext, uri!!)
            mediaPlayer?.prepare()
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        while (true){

        }
    }


    private fun getRingtoneUri(fileName: String) = try {
        if (TextUtils.isEmpty(fileName)) {
            RingtoneManager.getActualDefaultRingtoneUri(
                applicationContext,
                RingtoneManager.TYPE_RINGTONE
            )
        }
        val resId = applicationContext.resources.getIdentifier(fileName, "raw", applicationContext.packageName)
        if (resId != 0) {
            Uri.parse("android.resource://${applicationContext.packageName}/$resId")
        } else {
            if (fileName.equals("system_ringtone_default", true)) {
                RingtoneManager.getActualDefaultRingtoneUri(
                    applicationContext,
                    RingtoneManager.TYPE_RINGTONE
                )
            } else {
                RingtoneManager.getActualDefaultRingtoneUri(
                    applicationContext,
                    RingtoneManager.TYPE_RINGTONE
                )
            }
        }
    } catch (e: Exception) {
        if (fileName.equals("system_ringtone_default", true)) {
            RingtoneManager.getActualDefaultRingtoneUri(
                applicationContext,
                RingtoneManager.TYPE_RINGTONE
            )
        } else {
            RingtoneManager.getActualDefaultRingtoneUri(
                applicationContext,
                RingtoneManager.TYPE_RINGTONE
            )
        }
    }
}