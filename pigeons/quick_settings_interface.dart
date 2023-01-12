import 'package:pigeon/pigeon.dart';

class Tile {
  String label;
  TileStatus tileStatus;
  String? contentDescription;
  String? stateDescription;
  String? drawableName;
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

enum TileStatus { active, inactive, unavailable }

class AddTileResult {
  final bool success;
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
