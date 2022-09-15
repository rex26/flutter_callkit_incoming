package com.hiennv.flutter_callkit_incoming

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import androidx.annotation.Nullable

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** FlutterCallkitIncomingPlugin */
class FlutterCallkitIncomingPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var instance: FlutterCallkitIncomingPlugin? = null

        fun getInstance(): FlutterCallkitIncomingPlugin?  {
            // 如果用new的方式无法得到里面的真实变量如：activity
            /*if(instance == null){
                instance = FlutterCallkitIncomingPlugin()
            }*/
            return instance
        }

        private val eventHandler = EventCallbackHandler()
        private val specificEventHandler = EventCallbackHandler()

        fun sendEvent(event: String, body: Map<String, Any>) {
            eventHandler.send(event, body)
        }

        fun sendSpecificEvent(event: String, body: Map<String, Any>) {
            specificEventHandler.send(event, body)
        }

        private fun sharePluginWithRegister(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding, @Nullable handler: MethodCallHandler) {
            if(instance == null) {
                instance = FlutterCallkitIncomingPlugin()
            }
            instance!!.context = flutterPluginBinding.applicationContext
            instance!!.callkitNotificationManager = CallkitNotificationManager(flutterPluginBinding.applicationContext)
            instance!!.channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_callkit_incoming")
            instance!!.channel?.setMethodCallHandler(handler)
            instance!!.events =
                EventChannel(flutterPluginBinding.binaryMessenger, "flutter_callkit_incoming_events")
            instance!!.events?.setStreamHandler(eventHandler)
        }

    }

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private var activity: Activity? = null
    private var context: Context? = null
    private var callkitNotificationManager: CallkitNotificationManager? = null
    private var channel: MethodChannel? = null
    private var events: EventChannel? = null

    /***
     * 增加一个channel专门处理特殊的场景(flutter_callkit_specific_events)：
     * 1.flutter app被杀掉后，收到FCM消息，需要监听这个channel来处理挂断事件
     */
    private var specificEvents: EventChannel? = null


    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.context = flutterPluginBinding.applicationContext
        callkitNotificationManager = CallkitNotificationManager(flutterPluginBinding.applicationContext)
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_callkit_incoming")
        channel?.setMethodCallHandler(this)
        events =
            EventChannel(flutterPluginBinding.binaryMessenger, "flutter_callkit_incoming_events")
        events?.setStreamHandler(eventHandler)
        specificEvents =
            EventChannel(flutterPluginBinding.binaryMessenger, "flutter_callkit_specific_events")
        specificEvents?.setStreamHandler(specificEventHandler)
 //       sharePluginWithRegister(flutterPluginBinding, this)
    }

    public fun showIncomingNotification(data: Data) {
        data.from = "notification"
        callkitNotificationManager?.showIncomingNotification(data.toBundle())
        //send BroadcastReceiver
        context?.sendBroadcast(
            CallkitIncomingBroadcastReceiver.getIntentIncoming(
                requireNotNull(context),
                data.toBundle()
            )
        )
    }

    public fun showMissCallNotification(data: Data) {
        callkitNotificationManager?.showIncomingNotification(data.toBundle())
    }

    public fun startCall(data: Data) {
        context?.sendBroadcast(
            CallkitIncomingBroadcastReceiver.getIntentStart(
                requireNotNull(context),
                data.toBundle()
            )
        )
    }

    public fun endCall(data: Data) {
        context?.sendBroadcast(
            CallkitIncomingBroadcastReceiver.getIntentEnded(
                requireNotNull(context),
                data.toBundle()
            )
        )
    }

    public fun endAllCalls() {
        val calls = getDataActiveCalls(context)
        calls.forEach {
            context?.sendBroadcast(
                CallkitIncomingBroadcastReceiver.getIntentEnded(
                    requireNotNull(context),
                    it.toBundle()
                )
            )
        }
        removeAllCalls(context)
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        try {
            when (call.method) {
                "showCallkitIncoming" -> {
                    val data = Data(call.arguments()?: HashMap<String, Any?>())
                    data.from = "notification"
                    //send BroadcastReceiver
                    context?.sendBroadcast(
                        CallkitIncomingBroadcastReceiver.getIntentIncoming(
                            requireNotNull(context),
                            data.toBundle()
                        )
                    )
                    result.success("OK")
                }
                "showMissCallNotification" -> {
                    val data = Data(call.arguments()?: HashMap<String, Any?>())
                    data.from = "notification"
                    callkitNotificationManager?.showMissCallNotification(data.toBundle())
                    result.success("OK")
                }
                "startCall" -> {
                    val data = Data(call.arguments()?: HashMap<String, Any?>())
                    context?.sendBroadcast(
                        CallkitIncomingBroadcastReceiver.getIntentStart(
                            requireNotNull(context),
                            data.toBundle()
                        )
                    )
                    result.success("OK")
                }
                "endCall" -> {
                    val data = Data(call.arguments()?: HashMap<String, Any?>())
                    context?.sendBroadcast(
                        CallkitIncomingBroadcastReceiver.getIntentEnded(
                            requireNotNull(context),
                            data.toBundle()
                        )
                    )
                    result.success("OK")
                }
                "endAllCalls" -> {
                    val calls = getDataActiveCalls(context)
                    calls.forEach {
                        if(it.isAccepted) {
                            context?.sendBroadcast(
                                CallkitIncomingBroadcastReceiver.getIntentEnded(
                                    requireNotNull(context),
                                    it.toBundle()
                                )
                            )
                        }else {
                            context?.sendBroadcast(
                                CallkitIncomingBroadcastReceiver.getIntentDecline(
                                    requireNotNull(context),
                                    it.toBundle()
                                )
                            )
                        }
                    }
                    removeAllCalls(context)
                    result.success("OK")
                }
                "activeCalls" -> {
                    result.success(getDataActiveCallsForFlutter(context))
                }
                "getDevicePushTokenVoIP" -> {
                    result.success("")
                }
            }
        } catch (error: Exception) {
            result.error("error", error.message, "")
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        AppUtils.logger("onDetachedFromEngine()")
        channel?.setMethodCallHandler(null)
    }


    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        instance = this
        AppUtils.logger("onAttachedToActivity()")
        this.activity = binding.activity
        this.context = binding.activity.applicationContext
    }

    override fun onDetachedFromActivityForConfigChanges() {
        AppUtils.logger("onDetachedFromActivityForConfigChanges()")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        instance = this
        AppUtils.logger("onReattachedToActivityForConfigChanges()")
        this.activity = binding.activity
        this.context = binding.activity.applicationContext
    }

    override fun onDetachedFromActivity() {
        AppUtils.logger("onDetachedFromActivity()")
    }

    fun isMainActivityKilled(): Boolean {
        if (activity == null || activity!!.isDestroyed || activity!!.isFinishing) {
            AppUtils.logger(
                "activity == null:" + (activity == null) + ",isDestroyed:" + (activity?.isDestroyed) + ",isFinishing:" + (activity?.isFinishing)
            )
            return true
        }
        AppUtils.logger( "activity ====== is alive ======")
        return false
    }

    class EventCallbackHandler : EventChannel.StreamHandler {

        private var eventSink: EventChannel.EventSink? = null

        override fun onListen(arguments: Any?, sink: EventChannel.EventSink) {
            eventSink = sink
        }

        fun send(event: String, body: Map<String, Any>) {
            val data = mapOf(
                "event" to event,
                "body" to body
            )
            Handler(Looper.getMainLooper()).post {
                eventSink?.success(data)
            }
        }

        override fun onCancel(arguments: Any?) {
            eventSink = null
        }
    }


}
