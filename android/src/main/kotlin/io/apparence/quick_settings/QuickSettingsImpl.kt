package io.apparence.quick_settings

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.graphics.drawable.IconCompat
import io.apparence.quick_settings.pigeon.AddTileResult
import io.apparence.quick_settings.pigeon.QuickSettingsInterface
import io.flutter.embedding.engine.FlutterShellArgs

class QuickSettingsImpl : QuickSettingsInterface {
    var activity: Activity? = null

    override fun addTileToQuickSettings(
        title: String,
        drawableName: String,
        callback: (Result<AddTileResult>) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val componentName = ComponentName(
                activity!!, QuickSettingsService::class.java
            )
            // Enable the service if it has not been enabled yet
            val enableFlag: Int = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            activity!!.packageManager.setComponentEnabledSetting(
                componentName, enableFlag, PackageManager.DONT_KILL_APP
            )

            val drawableResourceId =
                QuickSettingsPlugin.drawableResourceIdFromName(activity!!, drawableName)
            if (drawableResourceId == 0) {
                return callback(
                    Result.success(
                        AddTileResult(
                            false,
                            "Icon $drawableName not found"
                        )
                    )
                );
            }

            val icon = IconCompat.createWithResource(
                activity!!, drawableResourceId
            )

            val statusBarService = activity!!.getSystemService(
                StatusBarManager::class.java
            )
            statusBarService.requestAddTileService(componentName,
                title,
                icon.toIcon(activity),
                {}) { result ->
                Log.d("QS", "requestAddTileService result: $result")
            }
            // TODO Android API is broken, the callback is never called so we can't know for sure if it worked or not
            return callback(Result.success(AddTileResult(true)));
        }
    }

    /**
     * Enable the Quick Settings service and its associated Tile.
     */
    override fun enableTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val componentName = ComponentName(
                activity!!, QuickSettingsService::class.java
            )
            val enableFlag: Int = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            activity!!.packageManager.setComponentEnabledSetting(
                componentName, enableFlag, PackageManager.DONT_KILL_APP
            )
        }
    }

    /**
     * Disable the Quick Settings service and its associated Tile.
     */
    override fun disableTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val componentName = ComponentName(
                activity!!, QuickSettingsService::class.java
            )
            val disableFlag: Int = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            activity!!.packageManager.setComponentEnabledSetting(
                componentName, disableFlag, PackageManager.DONT_KILL_APP
            )
        }
    }

    /**
     * This method starts the background isolate which will be able to handle callbacks from
     * the TileService associated with your QuickSettings Tile.
     */
    override fun startBackgroundIsolate(
        pluginCallbackHandle: Long,
        onStatusChangedHandle: Long?,
        onTileAddedHandle: Long?,
        onTileRemovedHandle: Long?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            var shellArgs: FlutterShellArgs? = null
            if (activity != null) {
                // Supports both Flutter Activity types:
                //    io.flutter.embedding.android.FlutterFragmentActivity
                //    io.flutter.embedding.android.FlutterActivity
                // We could use `getFlutterShellArgs()` but this is only available on `FlutterActivity`.
                shellArgs = FlutterShellArgs.fromIntent(activity!!.intent)
            }
            QuickSettingsService.setCallbackDispatcher(
                activity!!.applicationContext,
                pluginCallbackHandle
            )

            onStatusChangedHandle?.apply {
                QuickSettingsExecutor.setOnStatusChangedHandle(
                    activity!!.applicationContext,
                    this
                )
            }
            onTileAddedHandle?.apply {
                QuickSettingsExecutor.setOnTileAddedHandle(
                    activity!!.applicationContext,
                    this
                )
            }
            onTileRemovedHandle?.apply {
                QuickSettingsExecutor.setOnTileRemovedHandle(
                    activity!!.applicationContext,
                    this
                )
            }
            QuickSettingsService.startBackgroundIsolate(
                activity!!.applicationContext,
                pluginCallbackHandle, shellArgs
            )
        }
    }
}