package com.jarvan.tobias

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.alipay.sdk.app.AuthTask
import com.alipay.sdk.app.EnvUtils
import com.alipay.sdk.app.PayTask
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import kotlinx.coroutines.*


class TobiasPlugin() : FlutterPlugin, MethodCallHandler, ActivityAware {
    private var activity: Activity? = null

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar): Unit {
            val channel = MethodChannel(registrar.messenger(), "com.jarvanmo/tobias")
            channel.setMethodCallHandler(TobiasPlugin().apply {
                activity = registrar.activity()
            })
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "version" -> version(result)
            "pay" -> pay(call, result)
            "auth" -> auth(call, result)
            "isAliPayInstalled" -> isAliPayInstalled(result)
            else -> result.notImplemented()
        }

    }

    private fun pay(call: MethodCall, result: Result) {
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            if (call.argument<Int>("payEnv") == 1) {
                EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX)
            } else {
                EnvUtils.setEnv(EnvUtils.EnvEnum.ONLINE)
            }
            val payResult = doPayTask(call.argument("order") ?: "")
            result.success(payResult.plus("platform" to "android"))
        }
    }

    private suspend fun doPayTask(orderInfo: String): Map<String, String> {

        return GlobalScope.async(Dispatchers.Default, CoroutineStart.DEFAULT) {
            val alipay = PayTask(activity)
            alipay.payV2(orderInfo, true) ?: mapOf<String, String>()
        }.await()
    }


    private fun auth(call: MethodCall, result: Result) {
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            val authResult = doAuthTask(call.arguments as String)
            result.success(authResult.plus("platform" to "android"))
        }
    }

    private suspend fun doAuthTask(authInfo: String): Map<String, String> {

        return GlobalScope.async(Dispatchers.Default, CoroutineStart.DEFAULT) {
            val alipay = AuthTask(activity)
            alipay.authV2(authInfo, true) ?: mapOf<String, String>()
        }.await()
    }


    private fun version(result: Result) {
        GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
            val version = doGetVersionTask()
            result.success(version)
        }
    }

    private fun isAliPayInstalled(result: Result) {
        val manager = activity?.packageManager
        if (manager != null) {
            val action = Intent(Intent.ACTION_VIEW)
            action.data = Uri.parse("alipays://")
            val list = manager.queryIntentActivities(action, PackageManager.GET_RESOLVED_FILTER)
            result.success(list != null && list.size > 0)
        } else {
            result.error("-1", "can't find packageManager", null)
        }
    }

    private suspend fun doGetVersionTask(): String {

        return GlobalScope.async(Dispatchers.Default, CoroutineStart.DEFAULT) {
            val alipay = PayTask(activity)
            alipay.version ?: ""
        }.await()
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(binding.binaryMessenger, "com.jarvanmo/tobias")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    }

    override fun onDetachedFromActivity() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }
}
