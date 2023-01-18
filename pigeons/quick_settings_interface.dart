import 'package:pigeon/pigeon.dart';

/// Object representing a Quick Settings tile with all its possible fields
class Tile {
  /// Main text associated with the Tile
  String label;

  /// Wether this tile is active, inactive or unavailable
  TileStatus tileStatus;

  /// Content description for the Tile
  String? contentDescription;

  /// State description for the Tile
  String? stateDescription;

  /// Native Android drawable name.
  ///
  /// This icon is expected to be white on alpha, and may be tinted by the system to match it's theme.
  String? drawableName;

  /// Subtitle for the tile, null below Android 10.
  ///
  /// It might be visible to the user when the Quick Settings panel is fully open.
  String? subtitle;

  Tile({
    required this.label,
    required this.tileStatus,
    this.contentDescription,
    this.stateDescription,
    this.drawableName,
    this.subtitle,
  });
}

/// Status of a Tile
enum TileStatus {
  /// The Tile is considered to be active.
  ///
  /// Example: the alarm is activated
  active,

  /// The Tile is considered to be inactive.
  ///
  /// Example: the alarm is not activated
  inactive,

  /// Tile is unavailable and can't be clicked.
  ///
  /// Example: the alarm has not been scheduled so it can't be activated.
  unavailable,
}

/// Result when asking to add a tile
class AddTileResult {
  /// [success] will be false when quick_settings can't find your drawable.
  /// Otherwise, it will be true. A limitation from the native side doesn't
  /// let us provide a "denied" state when asking to add the Tile.
  final bool success;

  /// Description of the error. It will used only to explain with drawable has
  /// not been found for now.
  final String? errorDescription;

  AddTileResult({
    required this.success,
    this.errorDescription,
  });
}

@HostApi()
abstract class QuickSettingsInterface {
  @async
  AddTileResult addTileToQuickSettings(String title, String drawableName);

  void enableTile();

  void disableTile();

  void startBackgroundIsolate(
    int pluginCallbackHandle,
    int? onStatusChangedHandle,
    int? onTileAddedHandle,
    int? onTileRemovedHandle,
  );
}

@HostApi()
abstract class QuickSettingsBackgroundInterface {
  void onInitialized();
}

@FlutterApi()
abstract class QuickSettingsBackgroundToDart {
  Tile? onTileClicked(int userCallbackHandle, Tile tile);

  Tile? onTileAdded(int callbackHandle, Tile tile);

  void onTileRemoved(int callbackHandle);
}
