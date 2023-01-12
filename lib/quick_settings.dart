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
    QuickSettingsPlatform.instance.registerHandlers(
      onTileClicked: onTileClicked,
      onTileAdded: onTileAdded,
      onTileRemoved: onTileRemoved,
    );
  }

  /// Asks to add this tile to QuickSettings of the user.
  ///
  /// If the user already has it, this method has no effect.
  /// The system might automatically deny if the user has already denied it
  /// several times.
  static Future<void> addTileToQuickSettings({
    required String label,
    required String drawableName,
  }) {
    return _instance.addTileToQuickSettings(label, drawableName);
  }
}
