flutter pub run pigeon \
  --input pigeons/quick_settings_interface.dart \
  --dart_out lib/src/quick_settings_interface.dart \
  --experimental_kotlin_out ./android/src/main/kotlin/io/apparence/quick_settings/pigeon/QuickSettingsInterface.kt \
  --experimental_kotlin_package "io.apparence.quick_settings.pigeon"