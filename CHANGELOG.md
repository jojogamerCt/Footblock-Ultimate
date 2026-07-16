# Changelog

All notable changes to the **Footblock Ultimate** mod are documented in this file.

---

## [0.0.12-ALPHA] - 2026-07-15

### World Cup 2026 Update

The **World Cup 2026 Update** adds an original tournament-inspired match kit, automatic scoring, and new ball physics. Every new visual asset is generated programmatically and every new inventory model is fully 3D.

### 🚀 Added Features

*   **World Cup 2026 Ball**:
    *   Added a second playable football variant with an original white, red, green, blue, and gold design inspired by the three-host spirit of 2026.
    *   Ball variant now synchronizes to clients, persists in NBT, uses the correct entity texture, and returns the matching item when retrieved or after a goal.
    *   Added airborne swerve physics. Turning the player's aim relative to their body during a World Cup ball shot applies decaying curve spin.
    *   Added action-bar shot telemetry for power percentage and left, right, or straight flight.
*   **Automatic Tournament Scoring**:
    *   Added non-colliding, illuminated **Red Goal Line** and **Blue Goal Line** blocks with visible direction arrows.
    *   A ball must cross in the arrow direction. Solid posts, crossbars, slabs, and other collision blocks now mask detector columns, cutting the valid sensor area to the square or rectangular goal opening.
    *   Valid goals announce the scorer and score, trigger celebration particles and audio, update the persistent `footblock_score` objective, and display it in the sidebar.
    *   Individual scorer totals are recorded in the persistent `footblock_goals` objective. Players on vanilla teams named `red` or `blue` receive explicit own-goal handling without inflating their personal totals.
    *   Scored balls become their matching item at the goal line, creating a clean kickoff reset and preventing duplicate goals.
*   **Score Manager Console**:
    *   Added a placeable, fully 3D console with a dedicated GUI for managing Red and Blue scores without stopping active footballs.
    *   Select either team to add or remove one point, set an exact score, or reset both teams to 0-0.
    *   The **End Score** action removes the active score objective and immediately clears the sidebar from every player's HUD.
    *   Replaced the blurred vanilla screen and gray controls with a sharp, custom dark-cyan interface and themed team/action buttons.
    *   Added complete generated translations for 12 major languages: English, Italian, Spanish, French, German, Brazilian Portuguese, Russian, Simplified Chinese, Japanese, Korean, Arabic, and Hindi.
*   **Referee Whistle**:
    *   Added a functional, programmatically modeled 3D whistle with a generated positional sound.
    *   Normal use stops and detaches all footballs within a true 48-block radius.
    *   Sneak-use stops nearby footballs and resets Red and Blue to 0–0 for operators, players tagged `footblock_referee`, or players on a vanilla team named `referee`.
*   **World Cup Trophy**:
    *   Added a placeable, original voxel championship trophy with a matching collision outline and 3D inventory model.
    *   Interacting with the trophy triggers a multiplayer trophy-lift announcement, fireworks, particles, and celebration audio, with a 10-second per-player cooldown.
*   **Survival & Creative Integration**:
    *   Added and audited recipes for every registered item: both footballs, the whistle, both goal lines, the trophy, and the score manager console.
    *   Added self-drop loot tables for every registered Footblock block.
    *   Added a dedicated **Footblock Ultimate** Creative tab and moved every mod item out of the vanilla tab.
*   **Programmatic Asset Pipeline**:
    *   Added a deterministic generator for tournament textures, 3D JSON models (including the score manager console), blockstates, recipes, loot tables, and the mono whistle sound.

### 🔧 Fixes & Adjustments

*   **Fabric Creative Tab Population**: Built all seven entries directly through the tab's display-item generator so the dedicated tab appears and its items can be taken on Fabric as well as NeoForge.
*   **Team-Aware Passes**: Players assigned to a vanilla scoreboard team now target only teammates. Unteamed casual play keeps the previous open targeting behavior.
*   **Network Validation**: Shot and pass power packets now reject non-finite values and clamp input to the valid `0.0–1.0` range.
*   **Animation Broadcast Safety**: Kick animation broadcasts now allocate a separate packet buffer per recipient instead of reusing one mutable buffer.
*   **Reproducible Builds**: Replaced Gradle plugin snapshots with the published Loom `1.14.476` and Architectury `3.4.164` releases, plus explicit Loom plugin resolution.
*   **Documentation Accuracy**: Corrected Creative, crafting, teammate-targeting, and latency claims and repaired corrupted UTF-8 emoji in the public descriptions.

## [0.0.11-ALPHA] - 2026-06-04

This release adds a cooperative/competitive passing mechanic, limits dribbling quantities, and introduces programmatic, dynamic player animations to enhance match realism.

### 🚀 Added Features

*   **Charged Passing System**: Hold **Right-Click (Use)** while dribbling to charge a pass.
    *   **Player Homing**: Scans for eligible players within 30 blocks and a 45-degree aim cone, then projects the pass toward the best target's feet.
    *   **Ground Passes**: If no eligible player is in sight, performs a soft ground pass in the look direction.
    *   **Cyan Progress HUD**: Shows a cyan progress bar beneath the crosshair while charging a pass.
    *   **Pass Audio**: Plays a lighter, higher-pitched kick sound for passes.
*   **Dynamic Kicking Animations**:
    *   **Walking Cycle Alignment**: Detects the walking stride and uses the leg currently positioned behind the player.
    *   **Sideways Kicking Alignment**: Crosses the appropriate leg when aiming sideways relative to the torso.
    *   **Balance Arm Swings**: Moves the opposite arm forward and the same-side arm backward for athletic balance.
    *   **Multiplayer Animation Sync**: Broadcasts kick animations so nearby players see the action.
    *   **Client Prediction**: Starts the local animation immediately while the server verifies the action.

### 🔧 Fixes & Adjustments

*   **Dribble Limit**: Limited possession to one attached football per player.
*   **Interaction Safety**: Suppressed default right-click interactions while charging a pass.

## [0.0.10-ALPHA] - 2026-06-03

This is the initial alpha release of **Footblock Ultimate**, the next-generation sequel to the original Footblock mod.

### 🚀 Added Features

*   **Voxel-Sphere Ball**: Added a programmatically constructed 3D football designed for Minecraft's visual style.
*   **Tactile Dribbling System**: Walk into the ball to take possession and roll it ahead of the player.
*   **Charged Power Shots**: Hold **Left-Click (Attack)** while dribbling to charge a kick from 0% to 100%.
*   **Power Shot HUD**: Added a gold charge bar below the crosshair.
*   **Voxel Physics**: Added gravity, friction, and collision responses for walls, ceilings, floors, and blocks.
*   **Competitive Steals**: Allowed players to steal possession with a 10-tick anti-oscillation cooldown.
*   **Portable Footballs**: Added item placement and entity retrieval.
*   **Custom Kick Sounds**: Added three randomized kick sounds with power-based pitch shifting.
*   **Visual Assets**: Added a programmatically generated vector and raster mod icon.
*   **Information Hub**: Added the public listing description in [CURSEFORGE.md](CURSEFORGE.md).

### 🔧 Fixes & Adjustments

*   **Shot Re-attachment Lock**: Added a 15-tick cooldown after kicks to prevent immediate possession reattachment.
*   **Mod Menu Icons**: Moved `icon.png` to the root resource path for correct Fabric and NeoForge packaging.
