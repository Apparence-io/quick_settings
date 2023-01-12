[![Apparence.io](/docs/assets/apparence_banner.png)](https://www.apparence.io/)
![Quick Settings header](/docs/assets/quick_settings_header.png)

# Quick Settings

This Flutter plugin lets you create and react to your own custom tile in the Android Quick Settings panel.

All of the following features are available directly from your Dart code:
- Ask the user to add your custom tile
- Handle clicks on your tile even when your app has been killed
- React to user addition or removal of your tile

Using this plugin below Android 7.0 will have no effect.

Find more about the native Quick Settings API on the [official documentation](https://developer.android.com/develop/ui/views/quicksettings-tiles).

Native to Dart communication in an Android Service has been heavily inspired by [firebase_messaging](https://pub.dev/packages/firebase_messaging).

## Installation

Start by adding this plugin to your `pubspec.yaml`:
```
flutter pub add quick_settings
```

## Usage

A good place to setup `quick_settings` is in your `main()` function:
``` dart
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  QuickSettings.setup(
    onTileClicked: onTileClicked,
    onTileAdded: onTileAdded,
    onTileRemoved: onTileRemoved,
  );

  runApp(const MyApp());
}
```
The callbacks `onTileClicked`, `onTileAdded` and `onTileRemoved` **must** be defined as **top-level functions**.
After being setup, they will be called even if your app is not running.

If none of them is provided, your Tile and its associated service are disabled.

`onTileClicked` is triggered when your tile is clicked:

![onTileClicked example](/docs/assets/on_tile_clicked.gif)

`onTileAdded` is triggered when your tile is added to the active Quick Settings tiles:

![onTileAdded example](/docs/assets/on_tile_added.gif)
It can either be triggered by the user when they interact with the system UI like above, or from a call to `QuickSettings.addTileToQuickSettings()` in your app.

`onTileRemoved` is triggered when your tile is removed from the active Quick Settings tiles:

![onTileRemoved example](/docs/assets/on_tile_removed.gif)

Let's see details on each of these callbacks.

### Tile clicks

Clicks are handled with an `OnTileClicked` callback defined as below:
``` dart
typedef OnTileClicked = Tile? Function(Tile tile);
```
The tile passed as parameter can let you decide what action to do depending on its `TileStatus` for instance.
Here is a concrete implementation:
``` dart
Tile onTileClicked(Tile tile) {
  final oldStatus = tile.tileStatus;
  if (oldStatus == TileStatus.active) {
    // 1.
    tile.label = "Alarm OFF";
    tile.tileStatus = TileStatus.inactive;
    tile.subtitle = "6:30 AM";
    tile.drawableName = "alarm_off";
    AlarmManager.instance.unscheduleAlarm();
  } else {
    // 2.
    tile.label = "Alarm ON";
    tile.tileStatus = TileStatus.active;
    tile.subtitle = "6:30 AM";
    tile.drawableName = "alarm_check";
    AlarmManager.instance.scheduleAlarm();
  }
  // 3.
  return tile;
}
```
The above implementation use the `TileStatus` to make different actions:
1. Tile has been clicked while it was active. Since it's a toggle, we set it to inactive with the appropriate values. In this case, we disable the alarm.
2. Tile has been clicked while it was inactive. We set it to active with the appropriate values and we enable the alarm.
3. Return the update tile. You can also return null if you don't want to update it.

⚠️ `onTileClicked` should be a **top-level function**.

### Tile added

You can react when the user adds your Tile to Quick Settings with the following callback:
``` dart
typedef OnTileAdded = Tile? Function(Tile tile);
```
In our example, we will use it to enable the alarm:
``` dart
Tile onTileAdded(Tile tile) {
  tile.label = "Alarm ON";
  tile.tileStatus = TileStatus.active;
  tile.subtitle = "6:30 AM";
  tile.drawableName = "alarm_check";
  AlarmManager.instance.scheduleAlarm();
  return tile;
}
```
In a more realistic scenario, you would probably fetch the current status of the alarm and update the Tile with either an active status if it is set or an inactive status otherwise.
You could also display the current hour of the alarm.

⚠️ `onTileAdded` should be a **top-level function**.

### Tile removed

You also get an occasion to react to Tile removal. However, you can't return an updated Tile as with the other callbacks since it can't be updated at that moment.
Here is the callback definition:
``` dart
typedef OnTileRemoved = void Function();
```
In our example, we disable the alarm:
``` dart
void onTileRemoved() {
  print("Tile removed");
  AlarmManager.instance.unscheduleAlarm();
}
```
In a more realistic scenario, you could schedule a notification, stop an ongoing chronometer or log the event to an analytics service.

⚠️ `onTileRemoved` should be a **top-level function**.

## Customizing your Tile

In Dart, a Tile has the following properties:
|Name|Android version|Description|
|-|-|-|
|`label`|7+|Label of your tile. It is always displayed to the user.|
|`tileStatus`|7+|Can be one of the following: active, inactive or unavailable. When a tile is unavailable, it can't be clicked.|
|`contentDescription`|7+|Content description for the tile.|
|`stateDescription`|11+|State description for the tile.|
|`drawableName`|7+|Your native Android drawable name. This icon is expected to be white on alpha, and may be tinted by the system to match it's theme.|
|`subtitle`|10+|Subtitle for the tile. It might be visible to the user when the Quick Settings panel is fully open.|

`contentDescription` and `stateDescription` don't have much documentation on the [official documentation](https://developer.android.com/reference/android/service/quicksettings/Tile) but it seems to be related to accessibility.

The `drawableName` should be a native Android drawable name. You can find several of them in the [example project](https://github.com/Apparence-io/quick_settings/example/android/app/src/main/res/drawable).

![Drawable example](/docs/assets/drawable_example.png)

In the example project, you could use "alarm", "alarm_check", "alarm_off" or "quick_settings_base_icon" as your `drawableName` for example.

## Adding your tile to the Quick Settings panel programmatically

Starting from Android 13, you can ask the user to add your Tile to the Quick Settings.
You can do it in Flutter with the following:
``` dart
QuickSettings.addTileToQuickSettings(
    label: "Alarm Toggle",
    drawableName: "alarm",
);
```
It will prompt below dialog:

![Add Tile dialog](/docs/assets/add_to_quick_settings.png)

If the user already has the Tile, this method has no effect.
The system might automatically deny if the user has already denied it several times.
Before Android 13, this has no effect.

`QuickSettings.addTileToQuickSettings()` returns an `AddTileResult` with a `success` parameter, but it is not reliable since the Android implementation's callback is not.
It will be mostly useful to know if your drawable has been correctly parsed by the plugin or not.

## Customizing the default tile in the system UI

Your Tile will appear in the list of Quick Settings tiles with the following default appearance:

![Default Tile appearance](/docs/assets/default_appearance_base.png)

There are 3 elements displayed: a **title**, a **subtitle** and an **icon**.

To customize the title, you must override `quick_settings_base_label` in your Android resources.
If you don't have one, create the file `/android/app/src/main/res/values/strings.xml` and add the `quick_settings_base_label` String:
``` xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Other resources -->
    <string name="quick_settings_base_label">QS Example</string>
</resources>
```

The subtitle is the `android:label` from your application's manifest.
You can find it at `/android/app/src/main/AndroidManifest.xml`.
``` xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="io.apparence.quick_settings_example">
   <application
        android:label="Quick Settings Example (app label)"
        android:name="${applicationName}"
        android:icon="@mipmap/ic_launcher">
        <!-- Other elements -->
    </application>
</manifest>
```

The default icon can also be overridden by providing your own drawable with the name `quick_settings_base_icon.xml` as it is done in the example project.

![Default drawable](/docs/assets/default_icon.png)

Here is the customized appearance:

![Customized appearance](/docs/assets/default_appearance_updated.png)

## Roadmap

If you are using this plugin and would like something added, please [create an issue](https://github.com/Apparence-io/quick_settings/issues/new).

[![Apparence.io](/docs/assets/apparence_banner.png)](https://www.apparence.io/)
