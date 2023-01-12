package io.apparence.quick_settings


import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.IconCompat
import io.apparence.quick_settings.pigeon.TileStatus
import io.flutter.embedding.engine.FlutterShellArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


@RequiresApi(Build.VERSION_CODES.N)
class QuickSettingsService : TileService() {
    private val dartTile
        @SuppressLint("RestrictedApi")
        get() =
            io.apparence.quick_settings.pigeon.Tile(
                qsTile.label.toString(),
                when (qsTile.state) {
                    Tile.STATE_ACTIVE -> {
                        TileStatus.ACTIVE
                    }
                    Tile.STATE_INACTIVE -> {
                        TileStatus.INACTIVE
                    }
                    else -> {
                        TileStatus.UNAVAILABLE
                    }
                },
                drawableName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) resources.getResourceEntryName(
                    qsTile.icon.resId
                ) else resources.getResourceEntryName(IconCompat.createFromIcon(qsTile.icon)!!.resId),
                stateDescription = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) qsTile.stateDescription?.toString() else null,
                contentDescription = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) qsTile.contentDescription?.toString() else null,
                subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) qsTile.subtitle?.toString() else null
            )

    override fun onBind(intent: Intent?): IBinder? {
        requestListeningState(
            this, ComponentName(this, QuickSettingsService::class.java)
        )
        return super.onBind(intent)
    }

    /**
     * Called when the user taps the tile.
     */
    override fun onClick() {
        super.onClick()
        handleTileEvent(
            TileClicked(
                dartTile,
                callback = {
                    if (it != null) {
                        updateTile(it)
                    }
                },
            )
        )
    }

    override fun onTileAdded() {
        super.onTileAdded()
        handleTileEvent(
            TileAdded(
                dartTile,
                callback = {
                    if (it != null) {
                        updateTile(it)
                    }
                },
            ),
        )
    }

    override fun onTileRemoved() {
        handleTileEvent(TileRemoved())
        super.onTileRemoved()
    }

    private fun handleTileEvent(tileEvent: TileEvent) {
        setupFlutter()
        CoroutineScope(Dispatchers.Main).launch {
            val newTile = onTileEvent(
                tileEvent
            )
            if (newTile != null) {
                updateTile(newTile)
            }
        }
    }

    private fun updateTile(newTile: io.apparence.quick_settings.pigeon.Tile) {
        val drawableResourceId = QuickSettingsPlugin.drawableResourceIdFromName(
            applicationContext,
            newTile.drawableName
        )

        // Change the UI of the tile.
        val tile = this.qsTile
        tile.label = newTile.label
        if (drawableResourceId != 0) {
            tile.icon = Icon.createWithResource(applicationContext, drawableResourceId)
        }
        tile.state = when (newTile.tileStatus) {
            TileStatus.ACTIVE -> Tile.STATE_ACTIVE
            TileStatus.INACTIVE -> Tile.STATE_INACTIVE
            TileStatus.UNAVAILABLE -> Tile.STATE_UNAVAILABLE
        }
        tile.contentDescription = newTile.contentDescription
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = newTile.subtitle
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = newTile.stateDescription
        }

        // Need to call updateTile for the tile to pick up changes.
        tile.updateTile()
    }

    private fun setupFlutter() {
        if (flutterBackgroundExecutor == null) {
            flutterBackgroundExecutor = QuickSettingsExecutor(this.applicationContext)
        }
        flutterBackgroundExecutor!!.startBackgroundIsolate()
    }

    companion object {
        private const val TAG = "QuickSettingsService"

        private val messagingQueue = Collections.synchronizedList(LinkedList<TileEvent>())

        private var flutterBackgroundExecutor: QuickSettingsExecutor? = null

        /**
         * Starts the background isolate for the [QuickSettingsService].
         *
         *
         * Preconditions:
         *
         *
         *  * The given `callbackHandle` must correspond to a registered Dart callback. If the
         * handle does not resolve to a Dart callback then this method does nothing.
         *
         */
        fun startBackgroundIsolate(
            context: Context, callbackHandle: Long, shellArgs: FlutterShellArgs?
        ) {
            if (flutterBackgroundExecutor != null) {
                Log.w(TAG, "Attempted to start a duplicate background isolate. Returning...")
                return
            }
            flutterBackgroundExecutor = QuickSettingsExecutor(context)
            flutterBackgroundExecutor!!.startBackgroundIsolate(
                callbackHandle, shellArgs
            )
        }


        /**
         * Called once the Dart isolate (`flutterBackgroundExecutor`) has finished initializing.
         *
         *
         * Invoked by [QuickSettingsPlugin] when it receives the `FirebaseMessaging.initialized` message. Processes all messaging events that came in while the
         * isolate was starting.
         */
        /* package */
        fun onInitialized() {
            Log.i(TAG, "QuickSettingsService started!")
            synchronized(messagingQueue) {

                CoroutineScope(Dispatchers.Main).launch {
                    // Handle all the message events received before the Dart isolate was
                    // initialized, then clear the queue.
                    for (intent in messagingQueue) {
                        val newTile = flutterBackgroundExecutor!!.executeDartCallback(intent)

                        if (newTile != null) {
                            intent.callback?.let { it(newTile) }
                        }
                    }
                    messagingQueue.clear()
                }
            }
        }


        /**
         * Sets the Dart callback handle for the Dart method that is responsible for initializing the
         * background Dart isolate, preparing it to receive Dart callback tasks requests.
         */
        fun setCallbackDispatcher(context: Context, callbackHandle: Long) {
            QuickSettingsExecutor.setCallbackDispatcher(context, callbackHandle)
        }


        suspend fun onTileEvent(tileEvent: TileEvent): io.apparence.quick_settings.pigeon.Tile? {
            if (!flutterBackgroundExecutor!!.isDartBackgroundHandlerRegistered()) {
                Log.w(
                    TAG,
                    "A background message could not be handled in Dart as no onBackgroundMessage handler has been registered."
                )
                return null
            }

            // If we're in the middle of processing queued messages, add the incoming
            // intent to the queue and return.
            synchronized(messagingQueue) {
                if (flutterBackgroundExecutor!!.isNotRunning()) {
                    Log.i(TAG, "Service has not yet started, messages will be queued.")
                    messagingQueue.add(tileEvent)
                    return null
                }
            }

            // There were no pre-existing callback requests. Execute the callback
            // specified by the incoming intent.
            return flutterBackgroundExecutor!!.executeDartCallback(tileEvent)
        }

//        fun onHandleWork(status: Boolean) {
//            if (!flutterBackgroundExecutor!!.isDartBackgroundHandlerRegistered()) {
//                Log.w(
//                    TAG,
//                    "A background message could not be handled in Dart as no onBackgroundMessage handler has been registered."
//                )
//                return
//            }
//
//            // If we're in the middle of processing queued messages, add the incoming
//            // intent to the queue and return.
//            synchronized(messagingQueue) {
//                if (flutterBackgroundExecutor!!.isNotRunning()) {
//                    Log.i(TAG, "Service has not yet started, messages will be queued.")
//                    messagingQueue.add(status)
//                    return
//                }
//            }
//
//            // There were no pre-existing callback requests. Execute the callback
//            // specified by the incoming intent.
//            Handler(Looper.getMainLooper()).post {
//                flutterBackgroundExecutor!!.executeDartCallbackInBackgroundIsolate(status)
//            }
//        }
    }
}