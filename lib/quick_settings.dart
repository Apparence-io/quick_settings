import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:quick_settings/src/quick_settings_platform.dart';

import 'src/quick_settings_interface.dart';

export 'src/quick_settings_interface.dart' show Tile, TileStatus;

typedef OnTileClicked = Tile? Function(Tile tile);
typedef OnTileAdded = Tile? Function(Tile tile);
typedef OnTileRemoved = void Function();

class QuickSettings {
  static QuickSettingsInterface? _quickSettingsInterface;

  static QuickSettingsInterface get _instance {
    return _quickSettingsInterface ??= QuickSettingsInterface();
  }

  /// Register top-level function to listen to Tile events.
  /// This should be done as soon as possible in your app.
  /// These callbacks will be called even when your app is closed.
  ///
  /// If you don't want to change the Tile, just return null
  static setup({
    OnTileClicked? onTileClicked,
    OnTileAdded? onTileAdded,
    OnTileRemoved? onTileRemoved,
  }) {
    if (!kIsWeb && Platform.isAndroid) {
      QuickSettingsPlatform.instance.registerHandlers(
        onTileClicked: onTileClicked,
        onTileAdded: onTileAdded,
        onTileRemoved: onTileRemoved,
      );
      if (onTileAdded == null &&
          onTileRemoved == null &&
          onTileClicked == null) {
        _instance.disableTile();
      } else {
        _instance.enableTile();
      }
    }
  }

  /// Asks to add this tile to QuickSettings of the user.
  ///
  /// If the user already has it, this method has no effect.
  /// The system might automatically deny if the user has already denied it
  /// several times.
  static Future<void> addTileToQuickSettings({
    required String label,
    required String drawableName,
  }) async {
    if (!kIsWeb && Platform.isAndroid) {
      await _instance.addTileToQuickSettings(label, drawableName);
    }
  }

  /// Enable the service associated with your Tile. Your Tile will be present in
  /// the list of third party tiles and the user will be able to add it to its
  /// Quick Settings panel.
  static Future<void> enableTile() async {
    if (!kIsWeb && Platform.isAndroid) {
      await _instance.enableTile();
    }
  }

  /// Disable the service associated with your Tile. Your tile will be removed
  /// from the list of third party tiles and from the Quick Settings panel.
  static Future<void> disableTile() async {
    if (!kIsWeb && Platform.isAndroid) {
      await _instance.disableTile();
    }
  }
}
