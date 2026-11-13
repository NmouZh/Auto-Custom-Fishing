# Auto Custom Fishing for CustomFishing

## Notice

This project's code is **100% AI-generated**.

This means:

- The code works, but the implementation may not be elegant
- The project structure and details may be messy
- This repository is more like a "runnable proof of concept + a foundation for further improvement"

If you are reading the code, treat it as a reference for the approach rather than expecting hand-polished production code.

## Current Known Issue

The biggest problem with older versions:

**It could not reel in the rod instantly and reliably at the exact moment the correct Title/Subtitle message was received.**

The old implementation split "detecting the right moment" and "clicking" across ticks: decoding happened on packet arrival, but the actual click waited for the next client tick (up to 50ms) — enough time for the pointer to leave the success zone on fast bars.

The current architecture fixes this:

- Click decision and execution happen in the same `Hud.setSubtitle` call stack; no tick queueing
- Speed prediction and the advance curve are still kept as compensation for high-difficulty bars
- Real-server behavior still needs in-game verification; if fish are still missed, tune the curve rather than adding delay

The repository still includes an **advance curve editor**, specifically for adjusting the early-click amount per bar and per speed. The default parameters can catch most fish, but not 100%. Issue / PR feedback from real gameplay is welcome.

## Introduction

This is a client-side auto-fishing mod written for the [Custom-Fishing](https://github.com/Xiao-MoMi/Custom-Fishing) server plugin, running on `Minecraft 26.2 / 1.21.11 + Fabric`.

The goal is straightforward: for Custom-Fishing's "Stardew Valley-like" fishing minigame, automate the whole repetitive loop of casting, waiting, detecting the minigame, judging the timing, and reeling in.

Currently only the river fishing mode is automated; sea fishing mode is not supported.

## What This Project Does

Custom-Fishing's river fishing is not vanilla Minecraft's "reel in when the bobber sinks" mechanic. Instead, after a fish bites, a minigame with a pointer and a success zone appears. This mod:

- Automatically casts the rod
- Listens for a fish bite and automatically reels in to enter the minigame
- Parses Title / Subtitle UI data sent by the server
- Reverse-engineers the pointer position and movement speed
- Clicks automatically at the optimal moment to complete the minigame
- Automatically recasts after completion and starts the next round

Essentially, it is a client-side automation tool that performs custom parsing of the Custom-Fishing river fishing UI protocol.

## Supported Scope

- Supported: river fishing `accurate_click`-style minigame
- Not supported: sea fishing / tension-style gameplay
- Not supported: generic server auto-fishing
- Requirement: the client must be able to receive the title component data sent by Custom-Fishing

## Core Logic

This project does not use image recognition or screen reading. It directly analyzes the UI component data the client receives.

The overall flow:

1. `FishingHookMixin` listens to the local player's fishing hook `biting` state and reels in immediately on a bite.
2. `HudMixin` intercepts `Hud#setTitle`, `setSubtitle`, `setOverlayMessage` and hands components to `ComponentScanner`.
3. `ComponentScanner` splits components into "text + effective font" parts; `UiDecoder` extracts the Custom-Fishing bar glyphs and offset characters.
4. The pointer progress is reverse-engineered from the offsets and mapped to logical grid positions on the current bar.
5. `PointerTracker` records recent position changes and estimates the average speed.
6. `ClickPredictor` decides the click timing from the success zone, speed and the configured advance curve; the click runs in the same packet-handling call stack, with no tick queueing.
7. `FishingController` drives the whole flow with a state machine: cast, wait for bite, enter minigame, click, reel in, recast.

In short:

`Bite detection + Title parsing + position speed measurement + advance prediction + state machine control`

## Main Features

- Full river fishing automation
- Automatic recast
- Timeout auto-reel and recast
- Per-bar-type advance amount configuration
- Global advance fine-tuning
- Built-in curve editor to adjust advance by speed
- Config UI integrated with Cloth Config / Mod Menu
- Logs of the last 20 clicks for tuning

## Usage

### Requirements

- Minecraft: `26.2`
- Java: `25`
- Fabric Loader: `0.19.3+`
- Fabric API: `0.157.0+26.2`

The same sources also build for `1.21.11`:

- Minecraft: `1.21.11`
- Java: `21`
- Fabric Loader: `0.20.1+`
- Fabric API: `0.141.6+1.21.11`
- Cloth Config: `21.11.153+fabric`
- Mod Menu: `17.0.1`

### Installation

1. Build the project, or use a pre-built jar.
2. Put the mod into the client `mods` folder.
3. Join a server with Custom-Fishing enabled.
4. Hold a fishing rod.
5. Press the hotkey to enable auto fishing.

### Default Hotkeys

- `R`: toggle auto fishing
- `O`: open the config screen

If Mod Menu is installed, the config screen can also be opened directly from Mod Menu.

## Configuration

Config file location:

`config/autofishing.json`

Main options:

- `modEnabled`: master switch
- `globalAdvance`: global advance fine-tune, applies to all bars
- `autoRecastMinutes`: timeout before auto-reeling and recasting after casting, in minutes, `0` to disable
- `castIntervalSeconds`: base waiting time between reeling in and recasting (in seconds)
- `castIntervalRandomSeconds`: random jitter range around the base cast interval (in seconds); each actual interval is randomized within `base ± range` to simulate human behavior, `0` to disable
- `barCurves`: advance curve per bar, configured as "speed(ms/section) -> advance amount"

Default philosophy:

- Low-difficulty bars use smaller advance amounts
- Mid/high-difficulty bars use more aggressive advance
- The faster the speed, the earlier the click usually needs to be

## Project Structure

```text
src/main/java/cafe/yamds/
  AutoFishingMod.java                Common mod entry point

src/client/java/cafe/yamds/
  AutoFishingClient.java             Client entry point (wiring only)
  detection/
    ComponentScanner.java            Component → text parts
    UiDecoder.java                   Custom-Fishing protocol decoder (pure)
    BarTable.java                    Bar metadata and success tables (single source of truth)
    PointerTracker.java              Pointer speed estimation
    FishingUiDetector.java           Latest frame and activity tracking
  fishing/
    FishingController.java           State machine (pure)
    ClickPredictor.java              Click timing prediction (pure)
    FishingEnvironment.java          Minecraft action interface
    FishingLog.java                  Click log
  config/
    AutoFishConfig.java              Config data and validation
    ConfigStore.java                 Load/save and curve cache
    AdvanceCurve.java / CurvePoint.java
  ui/
    AutoFishConfigScreen.java        Cloth Config screen
    BarVisualizer.java               Bar visualization
    ModMenuIntegration.java          Mod Menu integration
  mixin/
    FishingHookMixin.java            Bite detection

src/versions/26.2/java/cafe/yamds/   26.2-only glue
  ModRuntime.java                    Runtime assembly
  fishing/MinecraftFishingEnvironment.java
  ui/KeyBindings.java, CurveEditorScreen.java, CurveEditorWidget.java, OpenCurveEditorEntry.java
  mixin/HudMixin.java                Hud class

src/versions/1.21.11/java/cafe/yamds/ 1.21.11-only glue (same 7 files)

src/test/java/                       Pure JVM unit tests (no Minecraft)
```

## Development and Testing

```bash
./gradlew build      # build for 26.2
./gradlew test       # run unit tests

# build for 1.21.11 (requires JDK 21)
./gradlew build -Pminecraft_version=1.21.11 -Pjava_version=21 \
  -Ploader_version=0.20.1 -Pfabric_version=0.141.6+1.21.11 \
  -Pcloth_config_version=21.11.153+fabric -Pmodmenu_version=17.0.1
```

Architecture rules, state diagram and extension guide: [AGENTS.md](AGENTS.md).

## Who Should Read This

If you want to understand the code quickly, the most valuable classes to start with are:

- `FishingController` (state machine)
- `FishingUiDetector` / `UiDecoder` (protocol parsing and speed)
- `ClickPredictor` (click timing)
- `BarTable` (bar data)
- `HudMixin` / `FishingHookMixin` (event entry points)

These files basically cover the core behavior.

## Building

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

Build output is located at:

`build/libs/`

## Known Limitations

- Only river fishing is automated so far
- The logic heavily depends on Custom-Fishing's current title component structure and custom font offset rules
- Clicks now execute on packet arrival, but network/server processing still adds inherent latency; high-difficulty bars still rely on speed prediction + advance compensation
- If the server plugin changes its UI encoding in the future, this mod may need to be updated accordingly
- This is a highly customized project; compatibility with other fishing plugins is not guaranteed

## License

The repository uses the existing `LICENSE` file in the project.
