import 'package:flutter/material.dart';
import 'package:quick_settings/quick_settings.dart';
import 'package:quick_settings_example/alarm_manager.dart';

// ignore_for_file: avoid_print

Tile onTileClicked(Tile tile) {
  final oldStatus = tile.tileStatus;
  if (oldStatus == TileStatus.active) {
    // Tile has been clicked while it was active
    // Set it to inactive and change its values accordingly
    // Here: Disable the alarm
    tile.label = "Alarm OFF";
    tile.tileStatus = TileStatus.inactive;
    tile.subtitle = "6:30 AM";
    tile.drawableName = "alarm_off";
    AlarmManager.instance.unscheduleAlarm();
  } else {
    // Tile has been clicked while it was inactive
    // Set it to active and change its values accordingly
    // Here: Enable the alarm
    tile.label = "Alarm ON";
    tile.tileStatus = TileStatus.active;
    tile.subtitle = "6:30 AM";
    tile.drawableName = "alarm_check";
    AlarmManager.instance.scheduleAlarm();
  }
  // Return the updated tile, or null if you don't want to update the tile
  return tile;
}

Tile onTileAdded(Tile tile) {
  tile.label = "Alarm ON";
  tile.tileStatus = TileStatus.active;
  tile.subtitle = "6:30 AM";
  tile.drawableName = "alarm_check";
  AlarmManager.instance.scheduleAlarm();
  return tile;
}

void onTileRemoved() {
  print("Tile removed");
  AlarmManager.instance.unscheduleAlarm();
}

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  QuickSettings.setup(
    onTileClicked: onTileClicked,
    onTileAdded: onTileAdded,
    onTileRemoved: onTileRemoved,
  );

  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(children: [
            ElevatedButton(
              child: const Text("Enable Tile"),
              onPressed: () {
                QuickSettings.enableTile();
              },
            ),
            const SizedBox(height: 20),
            OutlinedButton(
              child: const Text("Disable Tile"),
              onPressed: () {
                QuickSettings.disableTile();
              },
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              child: const Text("Add Tile to QuickSettings (Android 13)"),
              onPressed: () {
                QuickSettings.addTileToQuickSettings(
                  label: "Alarm Toggle",
                  drawableName: "alarm",
                );
              },
            ),
          ]),
        ),
      ),
    );
  }
}
