# ❄️ ChillRay

**Turn your phone into a freeze ray.** A tiny (~17 KB) Android app that controls **Mundo Clima** and other Midea-based air conditioners with your phone's infrared blaster. No ads, no accounts, no internet, no bloat.

## Download

👉 Grab [`MandoAire.apk`](MandoAire.apk) (or the latest [release](../../releases)).

**Requirements:** Android 4.4+ and a phone with an IR blaster (most Xiaomi, Redmi and POCO phones have one; Samsung and Pixel don't).

## Features

- Power on / off
- Temperature from 17 to 30 °C
- Modes: Cool, Dry, Auto, Heat, Fan
- Remembers your last settings
- Two protocols, switchable in the app:
  - **A — Coolix** (24-bit): Midea RG57 / RG51 / R51 remotes. The most common one, try it first.
  - **B — Midea** (48-bit): RG66 / R09C style remotes.

If your AC doesn't react to one protocol, switch to the other.

## Build

No Gradle and no Android Studio, just `aapt2`, `javac`, `d8` and `apksigner`. The script expects a JDK and the Android SDK one level up (`../jdk-*`, `../sdk` with `platforms;android-34` and `build-tools;34.0.0`):

```sh
./build.sh
```

## Project layout

| File | What it does |
|------|--------------|
| `src/.../IrCodes.java` | IR frame encoders (Coolix + Midea) |
| `src/.../MainActivity.java` | The whole UI, built in code (no XML layouts) |
| `build.sh` | Builds, aligns and signs the APK |

## Credits

The IR protocols are ported from the excellent [IRremoteESP8266](https://github.com/crankyoldgit/IRremoteESP8266) library.

*Not affiliated with Mundo Clima or Midea.*
