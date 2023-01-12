package io.apparence.quick_settings

import android.annotation.SuppressLint
import android.content.Context
import io.apparence.quick_settings.pigeon.QuickSettingsInterface
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding


class QuickSettingsPlugin : FlutterPlugin, ActivityAware {
    private var quickSettingsImpl = QuickSettingsImpl()


    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        QuickSettingsInterface.setUp(flutterPluginBinding.binaryMessenger, quickSettingsImpl)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        quickSettingsImpl.activity = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        quickSettingsImpl.activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        quickSettingsImpl.activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        quickSettingsImpl.activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        quickSettingsImpl.activity = null
    }

    companion object {
        fun drawableResourceIdFromName(context: Context, drawableName: String?): Int {
            if (drawableName == null) return 0
            @SuppressLint("DiscouragedApi")
            var drawableResourceId: Int = context.resources
                .getIdentifier(drawableName, "drawable", context.packageName)
            if (drawableResourceId == 0) {
                // If provided icon was not found, try to get mipmap/ic_launcher
                @SuppressLint("DiscouragedApi")
                drawableResourceId = context.resources
                    .getIdentifier("ic_launcher", "mipmap", context.packageName)
            }
            return drawableResourceId
        }
    }

}
