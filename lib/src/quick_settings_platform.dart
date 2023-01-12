import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:quick_settings/src/quick_settings_interface.dart';

import '../quick_settings.dart';

@pragma('vm:entry-point')
void _quickSettingsCallbackDispatcher() {
  // Initialize state necessary for MethodChannels.
  WidgetsFlutterBinding.ensureInitialized();

  // Handle native calls into dart
  QuickSettingsBackgroundToDart.setup(QuickSettingsBackgroundToDartImpl());

  // Once we've finished initializing, let the native portion of the plugin
  // know that it can start scheduling alarms.
  QuickSettingsBackgroundInterface().onInitialized();
}

class QuickSettingsBackgroundToDartImpl extends QuickSettingsBackgroundToDart {
  @override
  Tile? onTileClicked(int userCallbackHandle, Tile tile) {
    final CallbackHandle handle =
        CallbackHandle.fromRawHandle(userCallbackHandle);
    // PluginUtilities.getCallbackFromHandle performs a lookup based on the
    // callback handle and returns a tear-off of the original callback.
    final closure =
        PluginUtilities.getCallbackFromHandle(handle)! as OnTileClicked;
    return closure(tile);
  }

  @override
  Tile? onTileAdded(int callbackHandle, Tile tile) {
    final CallbackHandle handle = CallbackHandle.fromRawHandle(callbackHandle);
    // PluginUtilities.getCallbackFromHandle performs a lookup based on the
    // callback handle and returns a tear-off of the original callback.
    final closure =
        PluginUtilities.getCallbackFromHandle(handle)! as OnTileAdded;
    return closure(tile);
  }

  @override
  void onTileRemoved(int callbackHandle) {
    final CallbackHandle handle = CallbackHandle.fromRawHandle(callbackHandle);
    // PluginUtilities.getCallbackFromHandle performs a lookup based on the
    // callback handle and returns a tear-off of the original callback.
    final closure =
        PluginUtilities.getCallbackFromHandle(handle)! as OnTileRemoved;
    return closure();
  }
}

class QuickSettingsPlatform {
  bool _handlersInitialized = false;

  QuickSettingsPlatform._();

  static QuickSettingsPlatform? _instance;

  /// The current [QuickSettingsPlatform] instance.
  static QuickSettingsPlatform get instance {
    return _instance ??= QuickSettingsPlatform._();
  }

  /// Register handler to the native side.
  Future<void> registerHandlers({
    OnTileClicked? onTileClicked,
    OnTileAdded? onTileAdded,
    OnTileRemoved? onTileRemoved,
  }) async {
    if (defaultTargetPlatform != TargetPlatform.android) {
      return;
    }

    if (!_handlersInitialized) {
      _handlersInitialized = true;
      final CallbackHandle bgHandle = PluginUtilities.getCallbackHandle(
        _quickSettingsCallbackDispatcher,
      )!;
      final CallbackHandle? onTileClickedHandle = onTileClicked == null
          ? null
          : PluginUtilities.getCallbackHandle(onTileClicked)!;
      final CallbackHandle? onTileAddedHandle = onTileAdded == null
          ? null
          : PluginUtilities.getCallbackHandle(onTileAdded)!;
      final CallbackHandle? onTileRemovedHandle = onTileRemoved == null
          ? null
          : PluginUtilities.getCallbackHandle(onTileRemoved)!;

      QuickSettingsInterface().startBackgroundIsolate(
        bgHandle.toRawHandle(),
        onTileClickedHandle?.toRawHandle(),
        onTileAddedHandle?.toRawHandle(),
        onTileRemovedHandle?.toRawHandle(),
      );
    }
  }
}
