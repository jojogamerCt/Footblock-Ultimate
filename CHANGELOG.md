# Changelog

All notable changes to the **Footblock Ultimate** mod will be documented in this file.

---

## [0.0.10-ALPHA] - 2026-06-03

This is the initial alpha release of **Footblock Ultimate**, the next-generation sequel to the original Footblock mod. Built on **Minecraft 1.21.1** with the **Architectury API**, supporting both **Fabric** and **NeoForge** loaders.

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
