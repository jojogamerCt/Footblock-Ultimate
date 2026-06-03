# Footblock Ultimate

Footblock Ultimate is a modern Minecraft mod built on the **Architectury API**, supporting both **Fabric** and **NeoForge** loaders for Minecraft **1.21.1**.

It is the sequel to the original Footblock mod, featuring revamped 3D assets, custom physics, realistic ball dribbling, and charging/shooting HUD overlays.

## Features
*   **3D Ball Model & Texture**: A vanilla-style blocky sphere soccer ball generated programmatically.
*   **Dribbling / Attachment**: Colliding with the ball attaches it to your feet, following you cleanly as you move.
*   **Stealing Mechanics**: Run into a ball possessed by another player to steal it (with a 0.5-second cooldown).
*   **Click-and-Hold Power Shot**: Hold the left-click button (attack key) while dribbling to charge your kick power, displayed on a sleek, custom HUD overlay below the crosshair.
*   **auditory & Visual Feedback**: Bouncing physics off walls/ground, rotation visual rolling, and dynamic sound pitch corresponding to shoot strength.
*   **Lag-Free Kicking**: Uses client-side prediction to simulate kicks instantly before syncing with the server.
*   **Crouch Drop**: Crouch to cleanly drop the ball at your feet.
*   **Pick Up**: Right-click the ball on the ground to pick it back up into your inventory.

## Development Setup

### Fabric Run
```bash
./gradlew :fabric:runClient
```

### NeoForge Run
```bash
./gradlew :neoforge:runClient
```

### Build
To compile and build the production-ready jars:
```bash
./gradlew build
```
The output jars for each loader will be placed in their respective `build/libs/` folders.
