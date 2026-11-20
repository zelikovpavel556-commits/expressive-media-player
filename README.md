# Material Expressive Media Player

Android media player UI built with **Material 3 Expressive** (Material You).

## What's inside

- `app/build.gradle.kts` — Compose + `androidx.compose.material3:material3:1.5.0`
  (Material 3 Expressive lives inside this library since 1.4.0)
- `app/src/main/java/com/example/mymediaexpressive/MainActivity.kt`

## How expressive design is achieved

- `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` — opt-in to expressive APIs
- `MaterialExpressiveTheme` with expressive motion (`MotionScheme.expressive()`),
  shapes and typography
- Dynamic color (Material You): `dynamicLightColorScheme` / `dynamicDarkColorScheme`
  on Android 12+, fallback to static schemes below
- Expressive components used: `CircularWavyProgressIndicator`, `Slider` with
  `rememberSliderState`, `Switch`, `FilledTonalButton`

## Open in Android Studio

Import this folder as a new Android project and run on a device/emulator.
