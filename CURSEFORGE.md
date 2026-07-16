# Footblock Ultimate ⚽🔥

**Footblock Ultimate** is the next-generation sequel to the original *Footblock* mod. Re-engineered for Minecraft **1.21.1**, it brings physics-based football, tactile controls, animated kicks, match equipment, and multiplayer stadium gameplay into your blocky world.

---

## 🏆 World Cup 2026 Update — 0.0.12-ALPHA

Turn your stadium into a tournament venue with a complete, original match-day kit inspired by the three-host spirit of 2026. Every new visual asset is generated programmatically, and every inventory item uses real 3D voxel geometry rather than a flat pixel-art sprite.

*   **World Cup 2026 Ball**: A new tri-color tournament ball that preserves the full dribbling, passing, shooting, bouncing, rolling, multiplayer sync, and pickup systems of the classic football.
*   **Swerve Shots & Telemetry**: Turn your aim across your body while releasing a shot with the World Cup ball to add airborne curve. An action-bar readout reports shot power and curve direction.
*   **Framed Directional Goal Lines**: A ball must cross in the detector arrow's direction. Solid posts, crossbars, slabs, or other collision blocks above the detector mask those cells, automatically cutting the valid sensor area to the square or rectangular opening of your goal. Rolling and airborne shots remain supported up to four blocks above the strip.
*   **Referee Whistle**: Use the fully 3D whistle to stop and detach every football within a true 48-block radius. Designated referees can sneak while using it to reset the World Cup score to 0–0 and begin a new match.
*   **Score Manager Console**: Open its dedicated GUI to select a team, add or remove a point, set an exact score, reset both teams, or end the score and remove the sidebar from every player's HUD.
*   **Global Localization**: Every item, tooltip, match message, scoreboard label, and console control is translated into 12 major languages.
*   **World Cup Trophy**: Place the original voxel trophy on a podium, then interact with it to launch a server-wide trophy-lift celebration, protected by a per-player 10-second cooldown.
*   **Survival & Creative Support**: Every registered mod item has a crafting recipe, and every block drops itself. All Footblock content now lives in its own **Footblock Ultimate** Creative tab instead of a vanilla tab.

> This is an unofficial, fan-made update with original tournament-inspired designs. It does not reproduce official emblems, team crests, or the real-world trophy model.

---

## ✨ Core Football Features

*   **Programmatic 3D Footballs**: Both balls use custom seven-part voxel-sphere models that look at home in Minecraft while visibly rolling through the world.
*   **Tactile Dribbling**: Walk into a loose ball to take control. It follows your feet dynamically, and each player can control only one ball at a time.
*   **Charged Power Shots**: Hold **Left-Click (Attack)** while dribbling and release to shoot. The gold HUD bar displays the current charge; sprinting adds speed and lift.
*   **Charged Passing**: Hold **Right-Click (Use)** and release to pass. The cyan HUD bar displays charge. Players using vanilla scoreboard teams will target only teammates; unteamed players retain open casual targeting.
*   **Dynamic Kick Animations**: Programmatic leg kicks and balancing arm swings select a physically appropriate leg and synchronize across multiplayer clients.
*   **Physics & Bounces**: Footballs react to gravity, friction, walls, floors, ceilings, and kinetic force with a `0.6` bounce coefficient.
*   **Competitive Steals**: Run into an opponent's controlled ball to take possession. Short anti-oscillation cooldowns prevent rapid ownership flicker.
*   **Positional Audio**: Randomized kick variations change pitch with shot power, while the referee whistle has its own generated sound.
*   **Portable Equipment**: Place a football from its item and interact with the entity to return the correct ball variant to your inventory.

---

## 🎮 Controls & Match Setup

1.  **Get the Kit**: Craft every item in Survival, open the dedicated **Footblock Ultimate** Creative tab, or use `/give`.
2.  **Build the Goals**: Place **Red Goal Line** or **Blue Goal Line** detectors beneath the goal mouth and point every arrow into the net. Put solid blocks over detector cells for the posts and crossbar: those blocks mask the sensor, leaving only the interior square or rectangle able to score.
3.  **Designate a Referee**: Operators can reset immediately. Servers can also grant reset access with `/tag <player> add footblock_referee` or by putting officials on a vanilla scoreboard team named `referee`.
4.  **Manage the Score**: Interact with the placeable **Score Manager Console** to adjust either team, reset both teams, or end the score and clear the HUD without stopping nearby balls.
5.  **Spawn a Ball**: Use either football item on the ground.
6.  **Dribble**: Walk into the ball. Crouch while controlling it to release it without kicking.
7.  **Shoot**: Hold **Left-Click**, aim, and release. Sprint for a stronger shot. With the World Cup ball, turn your aim relative to your body before release to bend the flight.
8.  **Pass**: Hold **Right-Click**, aim toward a player, and release. If no eligible target is in the aim cone, the ball travels as a low ground pass.
9.  **Stop Play**: Any player can use the whistle normally to detach and halt every football within 48 blocks without changing the score.
10. **Retrieve the Ball**: Interact with a football entity to pick it up. A scored ball is automatically dropped at the goal line for the next kickoff.
11. **Celebrate**: Place the **World Cup Trophy** on a podium and interact with it after the final whistle.

The team score is stored in the `footblock_score` vanilla scoreboard objective. Individual scorer totals are recorded in `footblock_goals`, making them available to commands, maps, and server-side tournament systems. If players use vanilla teams named `red` and `blue`, own goals are announced and excluded from the scorer's personal total.

---

Create a pitch, invite your friends, stage a tournament, and lift the trophy in **Footblock Ultimate: World Cup 2026 Update**!
