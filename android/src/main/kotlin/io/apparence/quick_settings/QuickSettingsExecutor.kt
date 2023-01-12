package io.apparence.quick_settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.apparence.quick_settings.pigeon.QuickSettingsBackgroundInterface
import io.apparence.quick_settings.pigeon.QuickSettingsBackgroundToDart
import io.apparence.quick_settings.pigeon.Tile
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterShellArgs
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.loader.FlutterLoader
import io.flutter.view.FlutterCallbackInformation
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class QuickSettingsExecutor(private val context: Context) : QuickSettingsBackgroundInterface {

    /**
     * Connects the Android side of this plugin with the background Dart isolate
     *  that was created by this plugin.
     */
    private var quickSettingsBackgroundToDart: QuickSettingsBackgroundToDart? = null

    private var backgroundFlutterEngine: FlutterEngine? = null
    private val isCallbackDispatcherReady: AtomicBoolean = AtomicBoolean(false)


    /**
     * Starts running a background Dart isolate within a new [FlutterEngine] using a previously
     * used entrypoint.
     *
     *
     * The isolate is configured as follows:
     *
     *
     *  * Bundle Path: `io.flutter.view.FlutterMain.findAppBundlePath(context)`.
     *  * Entrypoint: The Dart method used the last time this plugin was initialized in the
     * foreground.
     *  * Run args: none.
     *
     *
     *
     * Preconditions:
     *
     *
     *  * The given callback must correspond to a registered Dart callback. If the handle does not
     * resolve to a Dart callback then this method does nothing.
     *
     */
    fun startBackgroundIsolate() {
        if (isNotRunning()) {
            val callbackHandle: Long = getPluginCallbackHandle()
            if (callbackHandle != 0L) {
                startBackgroundIsolate(callbackHandle, null)
            }
        }
    }

    /**
     * Starts running a background Dart isolate within a new [FlutterEngine].
     *
     *
     * The isolate is configured as follows:
     *
     *
     *  * Bundle Path: `io.flutter.view.FlutterMain.findAppBundlePath(context)`.
     *  * Entrypoint: The Dart method represented by `callbackHandle`.
     *  * Run args: none.
     *
     *
     *
     * Preconditions:
     *
     *
     *  * The given `callbackHandle` must correspond to a registered Dart callback. If the
     * handle does not resolve to a Dart callback then this method does nothing.
     *
     */
    fun startBackgroundIsolate(callbackHandle: Long, shellArgs: FlutterShellArgs?) {
        if (backgroundFlutterEngine != null) {
            Log.e(TAG, "Background isolate already started.")
            return
        }
        val loader = FlutterLoader()
        val mainHandler = Handler(Looper.getMainLooper())
        val myRunnable = Runnable {
            loader.startInitialization(context)
            loader.ensureInitializationCompleteAsync(
                context, null, mainHandler
            ) {
                val appBundlePath = loader.findAppBundlePath()
                val assets: AssetManager = context.assets
                if (isNotRunning()) {
                    backgroundFlutterEngine = if (shellArgs != null) {
                        Log.i(
                            TAG,
                            "Creating background FlutterEngine instance, with args: ${shellArgs.toArray()} "
                        )
                        FlutterEngine(
                            context, shellArgs.toArray()
                        )
                    } else {
                        Log.i(TAG, "Creating background FlutterEngine instance.")
                        FlutterEngine(context)
                    }
                    // We need to create an instance of `FlutterEngine` before looking up the
                    // callback. If we don't, the callback cache won't be initialized and the
                    // lookup will fail.
                    val flutterCallback =
                        FlutterCallbackInformation.lookupCallbackInformation(callbackHandle)
                    val executor: DartExecutor = backgroundFlutterEngine!!.dartExecutor
                    quickSettingsBackgroundToDart = QuickSettingsBackgroundToDart(executor)
                    QuickSettingsBackgroundInterface.setUp(executor, this)
                    val dartCallback =
                        DartExecutor.DartCallback(assets, appBundlePath, flutterCallback)
                    executor.executeDartCallback(dartCallback)
                }
            }
        }
        mainHandler.post(myRunnable)
    }

    /**
     * Returns true when the background isolate has started and is ready to handle background
     * messages.
     */
    fun isNotRunning(): Boolean {
        return !isCallbackDispatcherReady.get()
    }

    /**
     * Get the users registered Dart callback handle for onTileClicked. Returns 0 if not set.
     */
    private fun getOnTileClickedCallbackHandle(): Long {
        val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0)
        return prefs.getLong(
            USER_CALLBACK_HANDLE_KEY, 0
        )
    }

    /** Get the registered Dart callback handle for the messaging plugin. Returns 0 if not set.  */
    private fun getPluginCallbackHandle(): Long {
        val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0)
        return prefs.getLong(CALLBACK_HANDLE_KEY, 0)
    }

    /**
     * Get the users registered Dart callback handle for onTileAdded. Returns 0 if not set.
     */
    private fun getOnTileAddedCallbackHandle(): Long {
        val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0)
        return prefs.getLong(
            ON_TILE_ADDED_HANDLE_KEY, 0
        )
    }

    /**
     * Get the users registered Dart callback handle for onTileRemoved. Returns 0 if not set.
     */
    private fun getOnTileRemovedCallbackHandle(): Long {
        val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, 0)
        return prefs.getLong(
            ON_TILE_REMOVED_HANDLE_KEY, 0
        )
    }


    /**
     * Executes the desired Dart callback in a background Dart isolate.
     */
    suspend fun executeDartCallback(tileEvent: TileEvent): Tile? {
        if (backgroundFlutterEngine == null) {
            Log.i(
                TAG,
                "A background message could not be handled in Dart as no onBackgroundMessage handler has been registered."
            )
            return null
        }

        val result = suspendCoroutine { continuation ->
            when (tileEvent) {
                is TileClicked -> {
                    quickSettingsBackgroundToDart!!.onTileClicked(
                        getOnTileClickedCallbackHandle(),
                        tileEvent.tile!!,
                        callback = {
                            continuation.resume(it)
                        })

                }
                is TileAdded -> {
                    quickSettingsBackgroundToDart!!.onTileAdded(
                        getOnTileAddedCallbackHandle(),
                        tileEvent.tile!!
                    ) {
                        continuation.resume(it)
                    }
                }
                is TileRemoved -> {
                    quickSettingsBackgroundToDart!!.onTileRemoved(
                        getOnTileRemovedCallbackHandle(),
                    ) {
                        continuation.resume(null)
                    }
                }
                else -> {
                    continuation.resume(null)
                }
            }
        }
        return result
    }

    fun isDartBackgroundHandlerRegistered(): Boolean {
        return getPluginCallbackHandle() != 0L
    }

    override fun onInitialized() {
        // This message is sent by the background method channel as soon as the background isolate
        // is running. From this point forward, the Android side of this plugin can send
        // callback handles through the background method channel, and the Dart side will execute
        // the Dart methods corresponding to those callback handles.
        isCallbackDispatcherReady.set(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            QuickSettingsService.onInitialized()
        }
    }

    companion object {
        const val TAG = "quick_settings"
        private const val USER_CALLBACK_HANDLE_KEY = "user_callback_handle"
        private const val CALLBACK_HANDLE_KEY = "callback_handle"
        private const val ON_TILE_ADDED_HANDLE_KEY = "on_tile_aded_handle"
        private const val ON_TILE_REMOVED_HANDLE_KEY = "on_tile_removed_handle"
        private const val SHARED_PREFERENCES_KEY = "quick_settings"


        /**
         * Sets the Dart callback handle for the Dart method that is responsible for initializing the
         * background Dart isolate, preparing it to receive Dart callback tasks requests.
         */
        fun setCallbackDispatcher(context: Context, callbackHandle: Long) {
            setPref(context, CALLBACK_HANDLE_KEY, callbackHandle)
        }

        /**
         * Sets the Dart callback handle for the users Dart handler that is responsible for handling
         * messaging events in the background.
         */
        fun setOnStatusChangedHandle(context: Context, callbackHandle: Long) {
            setPref(context, USER_CALLBACK_HANDLE_KEY, callbackHandle)
        }

        /**
         * Sets the Dart callback handle for the onTileAdded event
         */
        fun setOnTileAddedHandle(context: Context, callbackHandle: Long) {
            setPref(context, ON_TILE_ADDED_HANDLE_KEY, callbackHandle)
        }

        /**
         * Sets the Dart callback handle for the onTileAdded event
         */
        fun setOnTileRemovedHandle(context: Context, callbackHandle: Long) {
            setPref(context, ON_TILE_REMOVED_HANDLE_KEY, callbackHandle)
        }


        private fun setPref(context: Context, key: String, value: Long) {
            val prefs = context.getSharedPreferences(
                SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE
            )
            prefs.edit().putLong(key, value).apply()
        }
    }

}