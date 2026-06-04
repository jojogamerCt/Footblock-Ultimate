# Changelog

All notable changes to the **Footblock Ultimate** mod will be documented in this file.

---


## [0.0.12-ALPHA] - 2026-06-04

This release introduces programmatic, dynamic-based player limb animations when shooting or passing.

### 🚀 Added Features

*   **Dynamic Kicking Animations**: Adds custom limb animations for player models during kicks/passes:
    *   **Walking Cycle Alignment**: When kicking straight forward, the mod detects your walking cycle stride (`limbSwing`) and swings the leg that is currently positioned backward.
    *   **Sideways Kicking Alignment**: If looking/kicking to the right, the character will kick with the left leg (crossing over), and vice-versa, avoiding physically impossible poses.
    *   **Balance Arm Swings**: Swings the opposite arm forward and the same-side arm backward to mimic realistic athletic balance during a kick.
    *   **Multiplayer Animation Sync**: Uses server-to-client S2C packet broadcasting (`KICK_ANIM_S2C_PACKET_ID`) so all nearby players can see other player models animate when they kick or pass.
    *   **Low-Latency Client Prediction**: Runs the animation instantly for the local player to guarantee zero visual lag.

## [0.0.11-ALPHA] - 2026-06-04

This release adds a cooperative/competitive passing mechanic and limits dribbling quantities to enhance match realism.

### 🚀 Added Features

*   **Charged Passing System**: Hold down **Right-Click (Use Key)** while dribbling to charge up a pass to a teammate.
    *   **Teammate Homing**: Scans for alive players in a 30-block distance within a 45-degree cone of the kicker's aim. If found, the pass is accurately projected towards their feet with speed proportional to distance and power.
    *   **Ground Passes**: If no teammate is in sight, performs a soft ground pass in the looking direction.
    *   **Cyan Progress HUD**: Shows a distinct cyan-colored progress bar beneath the crosshair while charging a pass.
    *   **Pass Audio**: Plays a lighter, slightly higher-pitched kick sound on passes for audio feedback.

### 🔧 Fixes & Adjustments

*   **Dribble Limit constraint**: Constrained dribbling attachment to at most 1 football per player at a time. If a player is already dribbling a ball, they cannot attach another ball until they crouch-release or kick the current one.
*   **Interaction Safety**: Suppresses default right-click interactions (such as block placement or item usage) while charging a pass to prevent gameplay conflicts.

## [0.0.10-ALPHA] - 2026-06-03

This is the initial alpha release of **Footblock Ultimate**, the next-generation sequel to the original Footblock mod.

### 🚀 Added Features

*   **Voxel Voxel-Sphere Ball**: Spawns a programmatically constructed 3D blocky sphere football that fits perfectly into the Minecraft vanilla aesthetic.
*   **Tactile Dribbling System**: Walk up to the ball to automatically attach it to your feet. The ball tracks your player movements smoothly, rolling visually based on physical velocity.
*   **Charged Power Shots**: Hold down **Left-Click (Attack Key)** while dribbling to charge up a kick from 0% to 100%. Releasing triggers a fast client-side prediction kick followed by server verification.
*   **Sporty Power Shot HUD**: Dynamically renders a sleek power charge bar on the player's screen directly below the crosshair, indicating current kick power.
*   **Realistic Voxel Physics**: The football supports custom physics, including gravity, ground friction, and collision responses. It bounces realistically off walls, ceilings, floors, and surrounding blocks.
*   **Cooperative & Competitive Steals**: Other players can steal the ball by walking into it. Built-in 10-tick (0.5 second) anti-oscillation cooldowns prevent stuttering during steal interactions.
*   **Survival Gameplay**:
    *   **Right-Click** the football item on blocks to spawn the ball entity.
    *   **Right-Click** the ball entity to pick it up and return it to your inventory.
*   **Custom Sound Effects**: Replaced vanilla slime block noises with 3 custom kick audio variations. A random sound is selected on every kick, playing with dynamic pitch shifting corresponding to the shot's charged power.
*   **Visual Assets**:
    *   Designed a premium vector SVG mod icon featuring the voxel ball on a dark green sporty shield with golden dashed rings.
    *   Implemented automated headless Chrome rendering of the SVG logo to produce root-level `icon.png` and `curseforge_icon.png` files.
*   **Information Hubs**: Included a premium CurseForge layout description in [CURSEFORGE.md](file:///c:/Users/giuse/Documents/Projects/Minecraft%20Mods/FootblockUltimate/CURSEFORGE.md).

### 🔧 Fixes & Adjustments

*   **Shot Re-attachment Lock**: Implemented a **15-tick (0.75-second) kick cooldown** where the player's feet collision check is temporarily ignored, preventing the ball from instantly re-attaching when running forward during a kick.
*   **Mod Menu Icons**: Fixed the Fabric and NeoForge in-game mod list logos by relocating the `icon.png` to the root of the resources directory so that they package directly into the JAR root classpath.
