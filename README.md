# Space Ship Duke

A fast-paced, top-down 2D space shooter developed in pure Java 25. Built from scratch without external game engines or physics libraries, focusing entirely on raw performance, low memory footprint, and deterministic mechanics.

## How to Play

You pilot Duke, a lone spaceship tasked with defending a vital Star Core from relentless waves of hostile ships. The game ends if Duke's hull is destroyed or if the Star Core's health drops to zero.

### Mechanics & Progression
* **The Threat:** Enemies become progressively stronger and more resilient with each passing wave. You must manage two distinct enemy behaviors:
    * *Hunters:* Aggressively track, chase, and shoot at Duke.
    * *Kamikazes:* Ignore the player entirely and dive-bomb straight into the Star Core to inflict damage.
* **Survival & Mining:** Between waves, you have a brief window to recover. Switch to your Drill weapon and mine the stray space rocks drifting towards the core to restore Duke's health.
* **Upgrades:** Surviving a wave grants you access to the Upgrade Menu. You must choose one stat enhancement (Max Health, Damage, Bullet Size, or Fire Rate) per round to keep up with the scaling enemy threat.

### Controls
* **`W` `A` `S` `D`** - Move the spaceship
* **`Mouse`** - Aim your weapons
* **`Left Click`** - Fire Laser / Use Drill
* **`1`** - Equip Laser (Combat Mode)
* **`2`** - Equip Drill (Mining / Healing Mode)
* **`ESC`** - Pause the game

## Architecture Decisions

* **Fixed Time Step Game Loop:** The core engine uses an accumulator-based game loop. This decouples the physics engine (running at a strict 60 Ticks Per Second) from the rendering pipeline, ensuring deterministic collisions and preventing the "tunneling" effect, regardless of the host machine's frame rate.
* **Asynchronous Input Polling:** A custom `GameInput` class captures AWT events (keyboard and mouse) asynchronously and maps them to primitive boolean arrays. The game loop reads these states safely during the physics update, eliminating OS-level key-repeat delays and input lag.
* **Integer-Based State Machine:** Instead of heavy object-oriented state patterns or Java `enum` classes (which add metadata overhead), the game flow (`START_MENU`, `GAME_PLAYING`, `PAUSE`, `GAME_OVER`) is controlled by lightweight integer constants to navigate the logic gates instantly.
* **Hybrid Rendering:** The rendering pipeline utilizes `Graphics2D` with sub-pixel rendering (`RenderingHints`) enabled for smooth geometric scaling.

## Optimization Strategies (Data-Oriented Design)

Due to strict constraints and a focus on performance, traditional Object-Oriented Programming (OOP) was intentionally bypassed in favor of **Data-Oriented Design (DOD)**:

* **Primitive Parallel Arrays:** Entities like Bullets, Rocks, and Enemies are not instantiated as objects. Their properties (X, Y, health, active state, rotation) are stored in contiguous primitive arrays (`float[]`, `boolean[]`, `int[]`).
* **Zero-Allocation Object Pooling:** The `new` keyword is strictly avoided during gameplay. All arrays are pre-allocated at startup up to their maximum capacity (e.g., `BULLETS_POOL_CAPACITY = 50`). Entities are "spawned" and "destroyed" by simply toggling their respective boolean flag in the `active` array, keeping the Java Garbage Collector completely dormant to prevent micro-stuttering.
* **Math Optimizations:** Heavy mathematical operations were reduced. Instead of using `Math.sqrt()` for circular collision detection, the engine calculates the squared distances (`sqrDistance`).
* **Single Texture Atlas:** All visual assets are packed into a single `SpriteSheet.png`. This reduces I/O disk operations at startup to exactly one file read and prevents the GPU from context-switching between different image buffers during the render loop.

## Size Breakdown

By avoiding external dependencies and using low-level Java APIs, the project maintains an incredibly small footprint:

By avoiding external dependencies and strictly utilizing low-level Java APIs alongside highly optimized assets, the final compiled executable achieves an incredibly minimal footprint of approximately **~31.6 KiB**.

Here is the exact breakdown of the compiled classes and packed resources:

* **Core Logic (`.class` files): ~24.2 KiB**
    * `Game.class` (Main Engine & State Machine): 21.8 KiB
    * `GameInput.class` (Asynchronous Input Handler): 2.0 KiB
    * `Main.class` (Entry Point): 369 Bytes

* **Visual Assets: 1.5 KiB**
    * `SpriteSheet.png`: 1.5 KiB

* **Audio Assets: 5.9 KiB**
    * `powerUp.wav`: 4.9 KiB
    * `laserShoot.wav`: 1.0 KiB

## Future Improvements

This project was built under extreme time constraints during a Game Jam. As a result, the primary goal was delivering a Minimum Viable Product (MVP) and a fun experience. Known areas for future refactoring include:

* **Code Refactoring:** The `physicsUpdate()` method handles multiple responsibilities. It needs to be decoupled into smaller, single-purpose methods (e.g., `updateMovement()`, `checkCollisions()`).
* **Magic Numbers:** Several layout coordinates, UI limits, and spawn distances are currently hardcoded. These should be extracted into centralized constants.
* **Advanced Collision Filtering:** Implementing a bitmask-based broad-phase collision system to further optimize the nested loops in the collision detection pipeline.