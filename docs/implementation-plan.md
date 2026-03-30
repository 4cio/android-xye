# Xye Android Clone: Implementation Plan

**Status:** Draft
**Date:** 2026-03-30
**Target:** Android 14+ (API 34), Kotlin 2.3, Jetpack Compose with Material 3

This document is an executable architecture spec. A Claude session should be able to
read this file and scaffold the entire project, then implement each module
incrementally. It covers project structure, Gradle setup, core data models, game loop
architecture, content parsers, persistence schema, and CI.

---

## Table of Contents

1. [Game Overview](#1-game-overview)
2. [Project Structure](#2-project-structure)
3. [Gradle Multi-Module Setup](#3-gradle-multi-module-setup)
4. [Core Data Models (`:engine`)](#4-core-data-models-engine)
5. [Game Loop Architecture](#5-game-loop-architecture)
6. [Content Parser Interface Design (`:content`)](#6-content-parser-interface-design-content)
7. [Persistence Schema (`:persistence`)](#7-persistence-schema-persistence)
8. [UI Architecture (`:app`)](#8-ui-architecture-app)
9. [Benchmark Module (`:benchmark`)](#9-benchmark-module-benchmark)
10. [GitHub Actions CI](#10-github-actions-ci)
11. [Implementation Phases](#11-implementation-phases)

---

## 1. Game Overview

Xye is a real-time grid-based puzzle game derived from Kye (Colin Garbutt, 1992). The
player (a green circle) navigates a grid to collect all gems while avoiding monsters,
manipulating pushable/pullable blocks, triggering doors, and surviving hazards.

### Core Mechanics

- **Orthogonal movement only** (up/down/left/right, no diagonals)
- **Push blocks:** Player pushes square blocks; round blocks slide on curves
- **Pull blocks:** Magnets attract objects within range along their axis
- **Autonomous entities:** Sliders, rockies, monsters move independently each tick
- **Doors/keys:** Colored keys open matching doors
- **Teleporters:** Paired portals that transport the player or objects
- **Timers:** Blocks that vanish after N ticks
- **Shooters:** Create new sliders/rockies periodically
- **Black holes:** Destroy any non-wall object that enters
- **Gems + star gems:** Collect all gems to win; stars must be collected before the last gem
- **Undo:** Unlimited undo via ring buffer of immutable states
- **Win condition:** All gems collected (and all stars collected before last gem)
- **Loss condition:** Player destroyed by monster, hazard, or black hole

---

## 2. Project Structure

```
xye/
├── settings.gradle.kts
├── build.gradle.kts                    # Root: common config, no source
├── gradle/
│   └── libs.versions.toml              # Version catalog
├── gradle.properties
├── .github/
│   └── workflows/
│       └── ci.yml
│
├── engine/                             # :engine — pure Kotlin, zero Android deps
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/dev/fourco/xye/engine/
│       │   ├── model/
│       │   │   ├── GameState.kt        # Immutable state snapshot
│       │   │   ├── Board.kt            # Grid cells
│       │   │   ├── Entity.kt           # EntityKind, EntityState, EntityId
│       │   │   ├── Position.kt         # Vec2 (col, row), Direction
│       │   │   ├── Inventory.kt        # Collected keys, star gems
│       │   │   ├── Goals.kt            # Gem/star tracking
│       │   │   └── InputIntent.kt      # Player input sealed class
│       │   ├── rules/
│       │   │   ├── GameEngine.kt       # Tick loop orchestrator
│       │   │   ├── MovementRule.kt     # Player + push resolution
│       │   │   ├── MagnetRule.kt       # Pull/attract logic
│       │   │   ├── TriggerRule.kt      # Door/key/switch logic
│       │   │   ├── TeleportRule.kt     # Teleporter pairing + transport
│       │   │   ├── AutonomousRule.kt   # Slider, rocky, monster AI
│       │   │   ├── ShooterRule.kt      # Periodic entity spawning
│       │   │   ├── TimerRule.kt        # Countdown + vanish
│       │   │   ├── BlackHoleRule.kt    # Destruction on entry
│       │   │   ├── CollisionRule.kt    # Entity-entity interactions
│       │   │   ├── CollectionRule.kt   # Gem/star pickup
│       │   │   └── WinLossRule.kt      # Terminal state detection
│       │   ├── undo/
│       │   │   └── UndoRing.kt         # Fixed-capacity ring buffer
│       │   └── replay/
│       │       ├── ReplayRecorder.kt   # Records InputIntent per tick
│       │       └── ReplayPlayer.kt     # Replays recorded inputs
│       └── test/kotlin/dev/fourco/xye/engine/
│           ├── model/
│           │   └── GameStateTest.kt
│           ├── rules/
│           │   ├── GameEngineTest.kt
│           │   ├── MovementRuleTest.kt
│           │   ├── MagnetRuleTest.kt
│           │   └── ...
│           └── undo/
│               └── UndoRingTest.kt
│
├── content/                            # :content — level parsers, pure Kotlin
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/dev/fourco/xye/content/
│       │   ├── LevelPack.kt           # Pack + level metadata
│       │   ├── RuntimeLevel.kt        # Parsed level ready for engine
│       │   ├── LevelParser.kt         # Common parser interface
│       │   ├── XyeParser.kt           # .xye XML format
│       │   ├── KyeParser.kt           # .kye legacy text format
│       │   ├── XsbParser.kt           # .xsb Sokoban format
│       │   └── PackLoader.kt          # Discovers + loads packs from directory/zip
│       ├── main/resources/
│       │   └── packs/                  # Bundled level packs
│       │       └── tutorial/
│       │           └── tutorial.xye
│       └── test/kotlin/dev/fourco/xye/content/
│           ├── XyeParserTest.kt
│           ├── KyeParserTest.kt
│           ├── XsbParserTest.kt
│           └── testdata/              # Sample level files for tests
│               ├── minimal.xye
│               ├── classic.kye
│               └── microban.xsb
│
├── persistence/                        # :persistence — Room + DataStore
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/dev/fourco/xye/persistence/
│       │   ├── XyeDatabase.kt         # Room database definition
│       │   ├── dao/
│       │   │   ├── SaveDao.kt
│       │   │   ├── ProgressDao.kt
│       │   │   └── ReplayDao.kt
│       │   ├── entity/
│       │   │   ├── SaveEntity.kt       # Serialized GameState + undo buffer
│       │   │   ├── ProgressEntity.kt   # Per-level completion status
│       │   │   └── ReplayEntity.kt     # Recorded input sequence
│       │   ├── converter/
│       │   │   └── Converters.kt       # Room type converters for serialization
│       │   └── repository/
│       │       ├── SaveRepository.kt
│       │       ├── ProgressRepository.kt
│       │       └── ReplayRepository.kt
│       └── main/AndroidManifest.xml
│
├── app/                                # :app — Android UI shell
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── kotlin/dev/fourco/xye/app/
│           │   ├── XyeApplication.kt
│           │   ├── MainActivity.kt
│           │   ├── navigation/
│           │   │   └── XyeNavGraph.kt  # Compose Navigation routes
│           │   ├── viewmodel/
│           │   │   ├── HomeViewModel.kt
│           │   │   ├── PackViewModel.kt
│           │   │   ├── GameViewModel.kt    # Owns GameEngine, emits StateFlow
│           │   │   └── WinViewModel.kt
│           │   ├── ui/
│           │   │   ├── theme/
│           │   │   │   ├── Theme.kt
│           │   │   │   ├── Color.kt
│           │   │   │   └── Type.kt
│           │   │   ├── screen/
│           │   │   │   ├── HomeScreen.kt
│           │   │   │   ├── PackListScreen.kt
│           │   │   │   ├── LevelSelectScreen.kt
│           │   │   │   ├── GameScreen.kt
│           │   │   │   └── WinScreen.kt
│           │   │   ├── component/
│           │   │   │   ├── BoardCanvas.kt      # Custom Canvas composable
│           │   │   │   ├── HudOverlay.kt       # Gem count, undo button, timer
│           │   │   │   ├── DPad.kt             # On-screen directional controls
│           │   │   │   └── LevelCard.kt        # Level list item
│           │   │   └── input/
│           │   │       ├── SwipeDetector.kt    # Swipe-to-move gesture
│           │   │       └── KeyboardHandler.kt  # Hardware keyboard/gamepad
│           │   └── di/
│           │       └── AppModule.kt            # Manual DI or Hilt module
│           ├── res/
│           │   ├── values/
│           │   │   ├── strings.xml
│           │   │   └── themes.xml
│           │   └── drawable/               # Vector drawables for entities
│           └── AndroidManifest.xml
│
├── benchmark/                          # :benchmark — deterministic replay + perf
│   ├── build.gradle.kts
│   └── src/
│       └── main/kotlin/dev/fourco/xye/benchmark/
│           ├── DeterminismTest.kt      # Same inputs → same final state
│           ├── PerformanceTest.kt      # Tick throughput measurement
│           └── ReplayVerifier.kt       # Replay file → expected end state
│
└── editor/                             # :editor — Phase 2, optional
    ├── build.gradle.kts
    └── src/
        └── main/kotlin/dev/fourco/xye/editor/
            └── .gitkeep
```

---

## 3. Gradle Multi-Module Setup

### `gradle/libs.versions.toml`

```toml
[versions]
kotlin = "2.3.0"
agp = "9.0.0"
compose-bom = "2026.03.00"
compose-compiler = "2.3.0"             # Matches Kotlin version (merged in K2)
material3 = "1.4.0"
activity-compose = "1.12.3"
navigation-compose = "2.9.0"
lifecycle = "2.9.0"
coroutines = "1.10.2"
room = "2.8.4"
datastore = "1.2.0"
serialization = "1.8.0"
ksp = "2.3.0-1.0.30"
junit5 = "5.11.4"
turbine = "1.2.0"
xmlutil = "0.90.3"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }

# AndroidX
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
lifecycle-runtime = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# DataStore
datastore-prefs = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Coroutines
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

# Serialization
serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }

# XML parsing (pure Kotlin multiplatform)
xmlutil-core = { group = "io.github.pdvrieze.xmlutil", name = "core", version.ref = "xmlutil" }
xmlutil-serialization = { group = "io.github.pdvrieze.xmlutil", name = "serialization", version.ref = "xmlutil" }

# Testing
junit5 = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit5" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
compose-ui-test = { group = "androidx.compose.ui", name = "ui-test-junit4" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "xye"
include(":app")
include(":engine")
include(":content")
include(":persistence")
include(":benchmark")
// include(":editor")  // Phase 2
```

### Module dependency graph

```
:app  ──→  :engine
  │   ──→  :content
  │   ──→  :persistence
  │
:content  ──→  :engine      (RuntimeLevel uses engine model types)
:persistence  ──→  :engine   (serializes GameState)
:benchmark  ──→  :engine
            ──→  :content
```

### Module build script patterns

**`:engine/build.gradle.kts`** — Pure Kotlin JVM library, no Android:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)

    testImplementation(libs.junit5)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

**`:content/build.gradle.kts`** — Pure Kotlin JVM library:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":engine"))
    implementation(libs.serialization.json)
    implementation(libs.xmlutil.core)
    implementation(libs.xmlutil.serialization)

    testImplementation(libs.junit5)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

**`:persistence/build.gradle.kts`** — Android library:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.fourco.xye.persistence"
    compileSdk = 35
    defaultConfig { minSdk = 34 }
}

dependencies {
    api(project(":engine"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.prefs)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)
}
```

**`:app/build.gradle.kts`** — Android application:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "dev.fourco.xye.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.fourco.xye"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":content"))
    implementation(project(":persistence"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.coroutines.android)
}
```

---

## 4. Core Data Models (`:engine`)

### `Position.kt`

```kotlin
package dev.fourco.xye.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class Position(val col: Int, val row: Int) {
    fun move(dir: Direction): Position = when (dir) {
        Direction.UP    -> copy(row = row - 1)
        Direction.DOWN  -> copy(row = row + 1)
        Direction.LEFT  -> copy(col = col - 1)
        Direction.RIGHT -> copy(col = col + 1)
    }
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT;

    val opposite: Direction get() = when (this) {
        UP -> DOWN; DOWN -> UP; LEFT -> RIGHT; RIGHT -> LEFT
    }
}
```

### `Entity.kt`

```kotlin
package dev.fourco.xye.engine.model

import kotlinx.serialization.Serializable

/** Stable identity for an entity across ticks. */
@JvmInline
@Serializable
value class EntityId(val value: Int)

/**
 * Every object on the board is an Entity. The kind determines which rules
 * apply. Properties encode per-instance configuration (color, direction,
 * timer countdown, teleporter pair ID, etc.).
 */
@Serializable
data class Entity(
    val id: EntityId,
    val kind: EntityKind,
    val pos: Position,
    val props: EntityProps = EntityProps(),
)

@Serializable
enum class EntityKind {
    // Player
    Player,

    // Terrain
    Wall,
    SoftBlock,          // Consumed on walk-through
    BlackHole,          // Destroys non-wall objects

    // Collectibles
    Gem,
    StarGem,

    // Pushable / pullable
    PushBlock,          // Square, stops on contact
    RoundBlock,         // Round, slides on curves
    PullBlock,          // Can be pulled by player when adjacent

    // Magnets
    MagnetH,            // Horizontal axis attractor (range 2)
    MagnetV,            // Vertical axis attractor (range 2)

    // Doors / keys
    Door,
    Key,

    // Teleporters
    Teleporter,

    // Triggers / switches
    Trigger,            // Pressure plate or switch, linked to a door

    // Autonomous movers
    SliderUp, SliderDown, SliderLeft, SliderRight,
    RockyUp, RockyDown, RockyLeft, RockyRight,

    // Spawners
    Shooter,            // Creates sliders/rockies periodically

    // Timers
    TimerBlock,         // Vanishes after N ticks

    // Monsters
    Monster,            // Chases player, kills on adjacency

    // Hazards
    Hazard,             // Static, kills player on contact

    // Conveyors
    ConveyorUp, ConveyorDown, ConveyorLeft, ConveyorRight,
}

/**
 * Per-entity properties bag. Fields are nullable; only relevant fields
 * are populated based on EntityKind.
 */
@Serializable
data class EntityProps(
    val color: Int? = null,              // For Door/Key matching
    val direction: Direction? = null,    // For shooters, initial direction
    val pairId: Int? = null,             // Teleporter pair ID
    val triggerId: Int? = null,          // Trigger → Door link
    val timerTicks: Int? = null,         // TimerBlock countdown
    val shootInterval: Int? = null,      // Shooter spawn interval
    val shootKind: EntityKind? = null,   // What the shooter spawns
    val isRound: Boolean = false,        // Affects push-slide physics
)
```

### `Board.kt`

```kotlin
package dev.fourco.xye.engine.model

import kotlinx.serialization.Serializable

/**
 * Immutable grid. Cells contain entity IDs. Multiple entities can
 * occupy the same cell (e.g., player standing on a conveyor).
 */
@Serializable
data class Board(
    val width: Int,
    val height: Int,
    private val cells: Map<Position, List<EntityId>> = emptyMap(),
) {
    fun entitiesAt(pos: Position): List<EntityId> = cells[pos] ?: emptyList()

    fun isInBounds(pos: Position): Boolean =
        pos.col in 0 until width && pos.row in 0 until height

    /** Returns a new Board with the entity added at the given position. */
    fun place(id: EntityId, pos: Position): Board {
        val current = entitiesAt(pos)
        return copy(cells = cells + (pos to current + id))
    }

    /** Returns a new Board with the entity removed from the given position. */
    fun remove(id: EntityId, pos: Position): Board {
        val current = entitiesAt(pos)
        val updated = current - id
        return if (updated.isEmpty()) copy(cells = cells - pos)
        else copy(cells = cells + (pos to updated))
    }
}
```

### `Inventory.kt` and `Goals.kt`

```kotlin
package dev.fourco.xye.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class Inventory(
    val keys: Map<Int, Int> = emptyMap(),   // color → count
    val starGems: Int = 0,
)

@Serializable
data class Goals(
    val totalGems: Int,
    val collectedGems: Int = 0,
    val totalStars: Int = 0,
    val collectedStars: Int = 0,
) {
    val allGemsCollected: Boolean get() = collectedGems >= totalGems
    val allStarsCollected: Boolean get() = collectedStars >= totalStars
    val starsRequired: Boolean get() = totalStars > 0
}
```

### `InputIntent.kt`

```kotlin
package dev.fourco.xye.engine.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface InputIntent {
    @Serializable data object MoveUp : InputIntent
    @Serializable data object MoveDown : InputIntent
    @Serializable data object MoveLeft : InputIntent
    @Serializable data object MoveRight : InputIntent
    @Serializable data object Undo : InputIntent
    @Serializable data object Wait : InputIntent           // No-op, world still ticks
}
```

### `GameState.kt`

```kotlin
package dev.fourco.xye.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val levelId: String,
    val tick: Long,
    val board: Board,
    val entities: Map<EntityId, Entity>,
    val inventory: Inventory,
    val goals: Goals,
    val status: GameStatus,
    val nextEntityId: Int,                  // Monotonic ID generator
) {
    /** Look up an entity by ID, or null if destroyed. */
    fun entity(id: EntityId): Entity? = entities[id]

    /** Find the player entity. */
    fun player(): Entity? = entities.values.find { it.kind == EntityKind.Player }

    /** Produce a new state with an updated entity. */
    fun updateEntity(entity: Entity): GameState = copy(
        entities = entities + (entity.id to entity),
    )

    /** Remove an entity (destroyed, collected, etc.). */
    fun removeEntity(id: EntityId): GameState {
        val entity = entities[id] ?: return this
        return copy(
            entities = entities - id,
            board = board.remove(id, entity.pos),
        )
    }

    /** Move an entity to a new position. */
    fun moveEntity(id: EntityId, to: Position): GameState {
        val entity = entities[id] ?: return this
        val newBoard = board.remove(id, entity.pos).place(id, to)
        val newEntity = entity.copy(pos = to)
        return copy(
            entities = entities + (id to newEntity),
            board = newBoard,
        )
    }
}

@Serializable
enum class GameStatus {
    Playing,
    Won,
    Lost,
    Paused,
}
```

### `UndoRing.kt`

```kotlin
package dev.fourco.xye.engine.undo

import dev.fourco.xye.engine.model.GameState

/**
 * Fixed-capacity ring buffer of immutable GameState snapshots.
 * When full, the oldest snapshot is silently dropped.
 *
 * This class is mutable for performance; it is NOT part of GameState.
 * The GameEngine owns a single instance.
 */
class UndoRing(private val capacity: Int = 512) {
    private val buffer = arrayOfNulls<GameState>(capacity)
    private var head = 0    // Next write position
    private var size = 0

    fun push(state: GameState) {
        buffer[head] = state
        head = (head + 1) % capacity
        if (size < capacity) size++
    }

    /** Pop the most recent state, or null if empty. */
    fun pop(): GameState? {
        if (size == 0) return null
        head = (head - 1 + capacity) % capacity
        size--
        val state = buffer[head]
        buffer[head] = null     // Allow GC
        return state
    }

    fun clear() {
        buffer.fill(null)
        head = 0
        size = 0
    }

    fun isEmpty(): Boolean = size == 0
    fun currentSize(): Int = size

    /** Serialize the entire buffer for save/resume. Returns newest-first. */
    fun toList(): List<GameState> {
        val result = mutableListOf<GameState>()
        var idx = (head - 1 + capacity) % capacity
        repeat(size) {
            buffer[idx]?.let { result.add(it) }
            idx = (idx - 1 + capacity) % capacity
        }
        return result
    }

    /** Restore from a newest-first list. */
    fun fromList(states: List<GameState>) {
        clear()
        // Push in reverse so oldest goes in first
        states.asReversed().forEach { push(it) }
    }
}
```

---

## 5. Game Loop Architecture

### Simulation Phases

The game uses a **deterministic fixed-step simulation**. Each tick processes
exactly these phases in order. Every phase takes a `GameState` and returns a
new `GameState`. No phase mutates anything.

```
┌─────────────────────────────────────────────────────────┐
│                     TICK N                               │
│                                                          │
│  1. INPUT           Read InputIntent from queue          │
│         │                                                │
│  2. UNDO CHECK      If Undo → pop UndoRing, skip rest   │
│         │                                                │
│  3. SNAPSHOT         Push current state to UndoRing      │
│         │                                                │
│  4. PLAYER MOVE     Resolve player movement + push       │
│         │           (PushBlock, RoundBlock slide logic)   │
│         │                                                │
│  5. MAGNET          MagnetH/MagnetV attract entities     │
│         │           within range along axis              │
│         │                                                │
│  6. TRIGGERS        Evaluate trigger/switch → door       │
│         │           state changes                        │
│         │                                                │
│  7. TELEPORT        Transport entities on teleporters    │
│         │           to paired destination                │
│         │                                                │
│  8. AUTONOMOUS      Sliders, rockies, conveyors move     │
│         │           Monsters chase toward player         │
│         │           Shooters spawn if interval elapsed   │
│         │           TimerBlocks decrement, vanish at 0   │
│         │                                                │
│  9. COLLISION       Resolve entity-entity overlaps       │
│         │           Player + Monster/Hazard → Loss       │
│         │           Object + BlackHole → destroy object  │
│         │                                                │
│ 10. COLLECTION      Player on Gem → collect              │
│         │           Player on StarGem → collect star     │
│         │           Player on Key → add to inventory     │
│         │                                                │
│ 11. WIN/LOSS        All gems collected? → Won            │
│         │           Stars required but not done? → block │
│         │           Player destroyed? → Lost             │
│         │                                                │
│ 12. COMMIT          tick++, emit new GameState           │
└─────────────────────────────────────────────────────────┘
```

### `GameEngine.kt` — Interface

```kotlin
package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.GameState
import dev.fourco.xye.engine.model.GameStatus
import dev.fourco.xye.engine.model.InputIntent
import dev.fourco.xye.engine.undo.UndoRing
import dev.fourco.xye.engine.replay.ReplayRecorder

/**
 * Pure, deterministic game engine. No coroutines, no Android deps.
 * The UI layer drives ticks by calling [tick] at a fixed interval.
 */
class GameEngine(
    initialState: GameState,
    private val undoCapacity: Int = 512,
) {
    private var _state: GameState = initialState
    val state: GameState get() = _state

    private val undoRing = UndoRing(undoCapacity)
    private val recorder = ReplayRecorder()

    // Ordered rule chain. Each rule is a pure function: GameState → GameState.
    private val rules: List<GameRule> = listOf(
        MovementRule(),
        MagnetRule(),
        TriggerRule(),
        TeleportRule(),
        AutonomousRule(),
        ShooterRule(),
        TimerRule(),
        CollisionRule(),
        CollectionRule(),
        WinLossRule(),
    )

    /**
     * Advance one tick. If input is Undo, restores previous state instead.
     * Returns the new state. Callers should check state.status for terminal.
     */
    fun tick(input: InputIntent): GameState {
        if (_state.status != GameStatus.Playing) return _state

        recorder.record(_state.tick, input)

        if (input is InputIntent.Undo) {
            val previous = undoRing.pop()
            if (previous != null) _state = previous
            return _state
        }

        // Snapshot for undo before mutation
        undoRing.push(_state)

        // Apply each rule in sequence
        var next = _state.applyInput(input)
        for (rule in rules) {
            next = rule.apply(next)
            if (next.status != GameStatus.Playing) break
        }

        _state = next.copy(tick = next.tick + 1)
        return _state
    }

    /** Restore from a save (state + undo history). */
    fun restore(state: GameState, undoHistory: List<GameState>) {
        _state = state
        undoRing.fromList(undoHistory)
    }

    fun undoHistory(): List<GameState> = undoRing.toList()
    fun replayLog(): List<Pair<Long, InputIntent>> = recorder.entries()
}

/**
 * A single rule in the simulation pipeline.
 * Must be a pure function: same input state → same output state.
 */
interface GameRule {
    fun apply(state: GameState): GameState
}
```

### Game loop driver (in `:app`, `GameViewModel.kt`)

```kotlin
package dev.fourco.xye.app.viewmodel

// Sketch — not full implementation

class GameViewModel(
    private val engine: GameEngine,
    private val saveRepository: SaveRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(engine.state)
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val inputChannel = Channel<InputIntent>(Channel.CONFLATED)

    // Fixed-step tick interval: 150ms = ~6.67 ticks/sec
    // Tunable: faster for arcade levels, slower for puzzle levels
    companion object {
        const val TICK_INTERVAL_MS = 150L
    }

    init {
        // Game loop coroutine
        viewModelScope.launch {
            while (isActive) {
                val input = inputChannel.tryReceive().getOrNull() ?: InputIntent.Wait
                val newState = engine.tick(input)
                _state.value = newState
                if (newState.status != GameStatus.Playing) break
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    fun onInput(intent: InputIntent) {
        inputChannel.trySend(intent)
    }

    fun save() {
        viewModelScope.launch {
            saveRepository.save(engine.state, engine.undoHistory())
        }
    }
}
```

### Key design invariant

**Determinism guarantee:** The engine has zero randomness, zero time-dependence,
and zero threading. Given the same `initialState` and the same sequence of
`InputIntent` values, the engine MUST produce identical output on every
platform. This enables replay verification and benchmark regression testing.

---

## 6. Content Parser Interface Design (`:content`)

### Level metadata and pack model

```kotlin
package dev.fourco.xye.content

import kotlinx.serialization.Serializable

@Serializable
data class LevelPack(
    val id: String,                     // e.g. "tutorial", "classic-kye"
    val name: String,
    val author: String = "",
    val description: String = "",
    val format: LevelFormat,
    val levels: List<LevelMeta>,
)

@Serializable
enum class LevelFormat { XYE, KYE, XSB }

@Serializable
data class LevelMeta(
    val index: Int,                     // Order within pack
    val id: String,                     // Unique within pack
    val name: String,
    val hint: String = "",
    val width: Int,
    val height: Int,
)
```

### Parsed level (ready for engine)

```kotlin
package dev.fourco.xye.content

import dev.fourco.xye.engine.model.*

/**
 * A fully parsed level, ready to be converted into an initial GameState.
 * This is the output of every parser, regardless of source format.
 */
data class RuntimeLevel(
    val meta: LevelMeta,
    val width: Int,
    val height: Int,
    val entities: List<Entity>,         // All entities with positions
    val playerStart: Position,
    val totalGems: Int,
    val totalStars: Int,
) {
    /** Convert to initial GameState for the engine. */
    fun toInitialState(): GameState {
        val entityMap = entities.associateBy { it.id }
        var board = Board(width, height)
        for (entity in entities) {
            board = board.place(entity.id, entity.pos)
        }
        return GameState(
            levelId = meta.id,
            tick = 0,
            board = board,
            entities = entityMap,
            inventory = Inventory(),
            goals = Goals(
                totalGems = totalGems,
                totalStars = totalStars,
            ),
            status = GameStatus.Playing,
            nextEntityId = entities.size,
        )
    }
}
```

### Parser interface

```kotlin
package dev.fourco.xye.content

import java.io.InputStream

/**
 * Common interface for all level format parsers.
 * Implementations must be stateless and thread-safe.
 */
interface LevelParser {
    /** The format this parser handles. */
    val format: LevelFormat

    /** Parse a complete pack (may contain multiple levels). */
    fun parsePack(name: String, input: InputStream): LevelPack

    /** Parse a single level by index from a pack source. */
    fun parseLevel(input: InputStream, index: Int): RuntimeLevel
}
```

### Format-specific parser notes

#### `.xye` (XML)

- XML-based, parsed with `xmlutil` (kotlinx.serialization XML)
- Root element contains metadata + `<level>` elements
- Each `<level>` has `<row>` elements with character/element codes
- Rich entity set: supports all EntityKind values

```xml
<!-- Example .xye structure (for reference, not a real file) -->
<xye version="1">
  <meta name="Tutorial Pack" author="..." />
  <level name="First Steps" width="20" height="15">
    <row>####################</row>
    <row>#@.................#</row>
    <row>#......d....D......#</row>
    <row>#..................#</row>
    <row>####################</row>
  </level>
</xye>
```

#### `.kye` (legacy text)

- Plain text, no XML
- Header line: level name
- Grid: fixed 20x15, one char per cell
- Character map (from original Kye):

| Char | Entity           | Char | Entity            |
|------|------------------|------|-------------------|
| `K`  | Player (Kye)     | `d`  | Gem (diamond)     |
| `#`  | Wall             | `*`  | Star gem          |
| `.`  | Empty            | `B`  | Push block        |
| `b`  | Soft block       | `R`  | Round block       |
| `1`  | Slider right     | `2`  | Slider down       |
| `3`  | Slider left      | `4`  | Slider up         |
| `5`  | Rocky right      | `6`  | Rocky down        |
| `7`  | Rocky left       | `8`  | Rocky up          |
| `H`  | Black hole        | `T`  | Timer block       |
| `S`  | Shooter          | `M`  | Monster           |
| `~`  | Hazard           | `h`  | Magnet horizontal |
| `v`  | Magnet vertical  |

#### `.xsb` (Sokoban)

- Subset mapping: Sokoban has only walls, floors, boxes, goals, and player
- Character map per the Sokoban standard:

| Char | Meaning          | Maps to         |
|------|------------------|-----------------|
| `#`  | Wall             | Wall            |
| ` `  | Floor            | (empty)         |
| `@`  | Player           | Player          |
| `+`  | Player on goal   | Player + Gem    |
| `$`  | Box              | PushBlock       |
| `.`  | Goal             | Gem             |
| `*`  | Box on goal      | PushBlock + Gem |

- Supports RLE variant: `3#` = `###`
- Row separator: `|` (alternative to newline)
- Comment lines start with `;`
- Only orthogonal push is valid (natural fit)

### PackLoader

```kotlin
package dev.fourco.xye.content

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Discovers and loads level packs from:
 * - Bundled resources (classpath)
 * - Filesystem directory
 * - Zip archives
 */
class PackLoader(
    private val parsers: Map<LevelFormat, LevelParser> = mapOf(
        LevelFormat.XYE to XyeParser(),
        LevelFormat.KYE to KyeParser(),
        LevelFormat.XSB to XsbParser(),
    ),
) {
    fun loadFromResource(path: String): LevelPack { /* ... */ }
    fun loadFromFile(file: File): LevelPack { /* ... */ }
    fun loadFromZip(input: InputStream): List<LevelPack> { /* ... */ }

    /** Detect format from file extension. */
    fun detectFormat(filename: String): LevelFormat = when {
        filename.endsWith(".xye", ignoreCase = true) -> LevelFormat.XYE
        filename.endsWith(".kye", ignoreCase = true) -> LevelFormat.KYE
        filename.endsWith(".xsb", ignoreCase = true) -> LevelFormat.XSB
        filename.endsWith(".sok", ignoreCase = true) -> LevelFormat.XSB
        else -> throw IllegalArgumentException("Unknown level format: $filename")
    }
}
```

---

## 7. Persistence Schema (`:persistence`)

### Room entities

```kotlin
package dev.fourco.xye.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Save slot: serialized GameState + undo ring for resume. */
@Entity(tableName = "saves")
data class SaveEntity(
    @PrimaryKey val slotId: String,     // e.g., "auto" or "manual-1"
    val levelId: String,
    val packId: String,
    val tick: Long,
    val stateJson: String,              // kotlinx.serialization JSON of GameState
    val undoJson: String,               // JSON array of GameState snapshots
    val savedAt: Long,                  // System.currentTimeMillis()
)

/** Per-level progress tracking. */
@Entity(tableName = "progress", primaryKeys = ["packId", "levelId"])
data class ProgressEntity(
    val packId: String,
    val levelId: String,
    val status: String,                 // "locked", "unlocked", "completed", "perfected"
    val bestTicks: Long? = null,        // Fewest ticks to complete
    val completedAt: Long? = null,
)

/** Replay recording for a completed level. */
@Entity(tableName = "replays")
data class ReplayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: String,
    val levelId: String,
    val inputsJson: String,             // JSON array of (tick, InputIntent) pairs
    val totalTicks: Long,
    val recordedAt: Long,
)
```

### DAOs

```kotlin
package dev.fourco.xye.persistence.dao

import androidx.room.*
import dev.fourco.xye.persistence.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveDao {
    @Query("SELECT * FROM saves WHERE slotId = :slotId")
    suspend fun get(slotId: String): SaveEntity?

    @Upsert
    suspend fun upsert(save: SaveEntity)

    @Query("DELETE FROM saves WHERE slotId = :slotId")
    suspend fun delete(slotId: String)

    @Query("SELECT * FROM saves ORDER BY savedAt DESC")
    fun allSaves(): Flow<List<SaveEntity>>
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE packId = :packId ORDER BY levelId")
    fun forPack(packId: String): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE packId = :packId AND levelId = :levelId")
    suspend fun get(packId: String, levelId: String): ProgressEntity?

    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Query("SELECT COUNT(*) FROM progress WHERE packId = :packId AND status IN ('completed', 'perfected')")
    suspend fun completedCount(packId: String): Int
}

@Dao
interface ReplayDao {
    @Insert
    suspend fun insert(replay: ReplayEntity): Long

    @Query("SELECT * FROM replays WHERE packId = :packId AND levelId = :levelId ORDER BY totalTicks ASC LIMIT 1")
    suspend fun bestReplay(packId: String, levelId: String): ReplayEntity?

    @Query("DELETE FROM replays WHERE id = :id")
    suspend fun delete(id: Long)
}
```

### Database

```kotlin
package dev.fourco.xye.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.fourco.xye.persistence.dao.*
import dev.fourco.xye.persistence.entity.*

@Database(
    entities = [SaveEntity::class, ProgressEntity::class, ReplayEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class XyeDatabase : RoomDatabase() {
    abstract fun saveDao(): SaveDao
    abstract fun progressDao(): ProgressDao
    abstract fun replayDao(): ReplayDao
}
```

### Repository pattern

Each repository wraps a DAO, handles serialization between engine model types
and Room entity types, and exposes `Flow` for reactive UI updates.

```kotlin
package dev.fourco.xye.persistence.repository

import dev.fourco.xye.engine.model.GameState
import dev.fourco.xye.engine.model.InputIntent
import dev.fourco.xye.persistence.dao.SaveDao
import dev.fourco.xye.persistence.entity.SaveEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SaveRepository(private val dao: SaveDao) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(
        slotId: String,
        packId: String,
        state: GameState,
        undoHistory: List<GameState>,
    ) {
        dao.upsert(
            SaveEntity(
                slotId = slotId,
                levelId = state.levelId,
                packId = packId,
                tick = state.tick,
                stateJson = json.encodeToString(state),
                undoJson = json.encodeToString(undoHistory),
                savedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun load(slotId: String): Pair<GameState, List<GameState>>? {
        val entity = dao.get(slotId) ?: return null
        val state = json.decodeFromString<GameState>(entity.stateJson)
        val undo = json.decodeFromString<List<GameState>>(entity.undoJson)
        return state to undo
    }
}
```

### Level progression model

Nonlinear progression: completing a level unlocks adjacent levels in the pack.
The unlock graph is defined per-pack in the level metadata. If no graph is
specified, levels unlock sequentially (completing level N unlocks N+1). The
`ProgressRepository` manages this:

```kotlin
suspend fun completeLevel(packId: String, levelId: String, ticks: Long, pack: LevelPack) {
    // Update current level
    val existing = dao.get(packId, levelId)
    dao.upsert(ProgressEntity(
        packId = packId,
        levelId = levelId,
        status = "completed",
        bestTicks = minOf(ticks, existing?.bestTicks ?: Long.MAX_VALUE),
        completedAt = System.currentTimeMillis(),
    ))
    // Unlock next levels according to pack graph
    for (nextId in pack.unlockTargets(levelId)) {
        val next = dao.get(packId, nextId)
        if (next == null || next.status == "locked") {
            dao.upsert(ProgressEntity(packId = packId, levelId = nextId, status = "unlocked"))
        }
    }
}
```

---

## 8. UI Architecture (`:app`)

### Navigation graph

```
HomeScreen
    │
    ├──→ PackListScreen (all installed packs)
    │        │
    │        └──→ LevelSelectScreen (levels in pack, shows lock/complete status)
    │                 │
    │                 └──→ GameScreen (board + HUD + controls)
    │                          │
    │                          └──→ WinScreen (stats, next level, replay)
    │
    └──→ SettingsScreen (tick speed, controls, theme)
```

### Route definitions

```kotlin
sealed class XyeRoute(val route: String) {
    data object Home : XyeRoute("home")
    data object PackList : XyeRoute("packs")
    data class LevelSelect(val packId: String) : XyeRoute("packs/{packId}/levels")
    data class Game(val packId: String, val levelId: String) : XyeRoute("play/{packId}/{levelId}")
    data class Win(val packId: String, val levelId: String) : XyeRoute("win/{packId}/{levelId}")
}
```

### GameScreen layout

```
┌─────────────────────────────────────┐
│  ← Pause          Level Name   ↺   │  ← HUD bar (compact)
├─────────────────────────────────────┤
│                                     │
│                                     │
│           ┌───────────┐             │
│           │           │             │
│           │   Board   │             │  ← Centered, square cells
│           │  Canvas   │             │     Auto-scaled to fit
│           │           │             │
│           └───────────┘             │
│                                     │
│         💎 12/20    ⭐ 3/5          │  ← Gem/star counter
│                                     │
│              ┌───┐                  │
│              │ ▲ │                  │
│          ┌───┼───┼───┐             │  ← D-Pad (or swipe anywhere)
│          │ ◄ │   │ ► │             │
│          └───┼───┼───┘             │
│              │ ▼ │                  │
│              └───┘                  │
│         [Undo]                      │
└─────────────────────────────────────┘
```

### BoardCanvas composable

The board renders on a Compose `Canvas`. Each cell is a fixed pixel size
(calculated from `min(availableWidth / boardWidth, availableHeight / boardHeight)`).
Entities are drawn as vector shapes or loaded `ImageVector` drawables.

```kotlin
@Composable
fun BoardCanvas(
    state: GameState,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cellSize = minOf(
            size.width / state.board.width,
            size.height / state.board.height,
        )
        val offsetX = (size.width - cellSize * state.board.width) / 2
        val offsetY = (size.height - cellSize * state.board.height) / 2

        // Draw grid background
        // Draw each entity by kind → shape/color mapping
        // Highlight player position
    }
}
```

### Input handling

Two parallel input mechanisms:

1. **Swipe gestures** — `pointerInput` modifier detects directional swipes
   anywhere on the board area. Minimum drag distance threshold to avoid
   accidental input.

2. **D-Pad buttons** — Four directional `IconButton` composables below the
   board. Includes an undo button.

3. **Hardware keyboard** (optional) — Arrow keys and `Z` for undo, handled via
   `onKeyEvent` modifier on the GameScreen.

All three converge on `GameViewModel.onInput(InputIntent)`.

---

## 9. Benchmark Module (`:benchmark`)

### Determinism test

```kotlin
/**
 * Verify that the engine is fully deterministic:
 * given the same initial state and input sequence,
 * the final state is byte-identical.
 */
class DeterminismTest {
    fun verify(level: RuntimeLevel, inputs: List<InputIntent>): Boolean {
        val state1 = runSimulation(level, inputs)
        val state2 = runSimulation(level, inputs)
        return state1 == state2  // data class structural equality
    }

    private fun runSimulation(level: RuntimeLevel, inputs: List<InputIntent>): GameState {
        val engine = GameEngine(level.toInitialState())
        for (input in inputs) {
            engine.tick(input)
        }
        return engine.state
    }
}
```

### Performance test

```kotlin
/**
 * Measure tick throughput: how many ticks/sec can the engine process
 * on a given level with random inputs.
 */
class PerformanceTest {
    fun measure(level: RuntimeLevel, tickCount: Int = 10_000): PerformanceResult {
        val engine = GameEngine(level.toInitialState())
        val inputs = generateInputSequence(tickCount)
        val startNs = System.nanoTime()
        for (input in inputs) {
            engine.tick(input)
        }
        val elapsedNs = System.nanoTime() - startNs
        return PerformanceResult(
            tickCount = tickCount,
            elapsedMs = elapsedNs / 1_000_000.0,
            ticksPerSec = tickCount / (elapsedNs / 1_000_000_000.0),
        )
    }
}
```

### Replay verifier

```kotlin
/**
 * Load a replay file (inputs + expected end state hash),
 * run the engine, verify the final state matches.
 * Used for regression testing after rule changes.
 */
class ReplayVerifier(private val packLoader: PackLoader) {
    fun verify(replayFile: ReplayFile): VerifyResult {
        val level = packLoader.loadLevel(replayFile.packId, replayFile.levelId)
        val engine = GameEngine(level.toInitialState())
        for ((_, input) in replayFile.inputs) {
            engine.tick(input)
        }
        val actualHash = engine.state.hashCode()
        return if (actualHash == replayFile.expectedHash) {
            VerifyResult.Pass
        } else {
            VerifyResult.Fail(expected = replayFile.expectedHash, actual = actualHash)
        }
    }
}
```

---

## 10. GitHub Actions CI

### `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 15

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Cache Gradle packages
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ runner.os }}-${{ hashFiles('**/*.gradle.kts', '**/libs.versions.toml') }}

      - name: Build all modules
        run: ./gradlew build --no-daemon

      - name: Run engine tests
        run: ./gradlew :engine:test --no-daemon

      - name: Run content tests
        run: ./gradlew :content:test --no-daemon

      - name: Run benchmark determinism checks
        run: ./gradlew :benchmark:test --no-daemon

      - name: Lint
        run: ./gradlew lint --no-daemon
        continue-on-error: true

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: |
            **/build/reports/tests/
            **/build/test-results/
```

---

## 11. Implementation Phases

### Phase 1: Foundation (engine + content + minimal UI)

1. **Scaffold project** — Create all directories, `settings.gradle.kts`,
   `libs.versions.toml`, per-module `build.gradle.kts`. Verify `./gradlew build`
   compiles with empty modules.

2. **Engine data models** — Implement all files in `engine/model/`: `Position`,
   `Direction`, `Entity`, `EntityKind`, `EntityProps`, `Board`, `Inventory`,
   `Goals`, `InputIntent`, `GameState`, `GameStatus`. Write unit tests for
   `Board.place/remove`, `Position.move`, `GameState.moveEntity/removeEntity`.

3. **Undo ring** — Implement `UndoRing` with push/pop/toList/fromList. Test
   capacity wraparound, empty pop, serialization round-trip.

4. **Game engine skeleton** — Implement `GameEngine` with the `GameRule`
   interface. Start with only `MovementRule` (player moves, push blocks) and
   `WinLossRule` (gem collection check). Other rules return state unchanged.

5. **XSB parser** — Implement `XsbParser` first (simplest format). Parse
   Microban level 1 as test fixture. Convert to `RuntimeLevel`, feed to engine,
   verify player can push blocks and collect gems.

6. **Minimal GameScreen** — `BoardCanvas` drawing walls/player/gems as colored
   rectangles. Swipe input. `GameViewModel` with fixed-step loop. No save, no
   packs, hardcoded level.

### Phase 2: Complete rules + parsers

7. **Remaining rules** — Implement `MagnetRule`, `TriggerRule`, `TeleportRule`,
   `AutonomousRule` (sliders, rockies, monsters), `ShooterRule`, `TimerRule`,
   `BlackHoleRule`, `CollisionRule`, `CollectionRule`. Each rule gets its own
   test file with focused test cases.

8. **KYE parser** — Parse `.kye` format (20x15 grid, character map). Test with
   classic Kye level sets.

9. **XYE parser** — Parse `.xye` XML format using `xmlutil`. Test with
   bundled tutorial pack.

10. **Pack loader** — Discover packs from bundled resources and filesystem.
    Zip support for downloaded packs.

### Phase 3: Persistence + polish

11. **Room database** — Implement `XyeDatabase`, DAOs, entities, repositories.
    Auto-save on pause. Manual save slots.

12. **Progress tracking** — Nonlinear unlock graph. Pack completion percentage.
    Best-ticks tracking.

13. **Replay** — `ReplayRecorder` saves inputs per tick. `ReplayPlayer`
    replays at adjustable speed. Persist to Room.

14. **Full UI** — Home screen, pack list, level select with lock/complete
    badges, win screen with stats, settings.

15. **Entity sprites** — Replace colored rectangles with proper vector
    drawables or Canvas-drawn shapes per entity kind.

16. **CI** — GitHub Actions workflow. Determinism test in benchmark module.

### Phase 4: Extras (optional)

17. **Level editor** — `:editor` module. Grid painter, entity palette, test-play,
    export to `.xye`.

18. **Accessibility** — TalkBack descriptions for board state, haptic feedback
    on collisions.

19. **Themes** — Dark/light, retro pixel theme, high-contrast.

---

## Appendix: Entity Rendering Reference

| EntityKind     | Shape          | Color/Style           |
|----------------|----------------|-----------------------|
| Player         | Circle         | Green, filled         |
| Wall           | Square         | Dark gray, filled     |
| SoftBlock      | Square         | Light gray, dashed    |
| Gem            | Diamond        | Cyan, filled          |
| StarGem        | 5-point star   | Gold, filled          |
| PushBlock      | Square         | Brown, filled         |
| RoundBlock     | Circle         | Brown, filled         |
| PullBlock      | Square         | Brown, ring border    |
| MagnetH        | H-bar          | Red                   |
| MagnetV        | V-bar          | Red                   |
| Door           | Square         | Colored, cross-hatch  |
| Key            | Key shape      | Colored, matching door|
| Teleporter     | Diamond        | Purple, pulsing       |
| Trigger        | Square         | Orange, flat          |
| Slider*        | Square+arrow   | Blue                  |
| Rocky*         | Circle+arrow   | Blue                  |
| Shooter        | Square+dot     | Dark red              |
| TimerBlock     | Square+number  | Yellow, countdown     |
| Monster        | Circle+teeth   | Red                   |
| Hazard         | X shape        | Red                   |
| BlackHole      | Circle         | Black, gradient       |
| Conveyor*      | Square+chevron | Gray, animated        |

---

## Appendix: Test Fixtures Needed

These test level files should be created during implementation:

1. **`minimal.xsb`** — 5x5 Sokoban: 1 player, 1 box, 1 goal
2. **`push-chain.xsb`** — Player pushes box into box (should not chain-push)
3. **`classic.kye`** — Single level from Kye with diamonds, walls, sliders
4. **`magnets.kye`** — Level with horizontal and vertical magnets
5. **`teleport.xye`** — Two linked teleporters, player warps
6. **`monster-chase.xye`** — Monster AI: chases player, kills on adjacency
7. **`timer-door.xye`** — Timer block vanishes, opens path; door linked to trigger
8. **`star-before-gem.xye`** — Stars must be collected before the last gem
9. **`win-loss.xye`** — Both win and loss conditions testable
10. **`replay-reference.json`** — Input sequence + expected final state hash

---

## References

- [Xye game homepage](https://xye.sourceforge.net/)
- [Xye introduction and gameplay](https://xye.sourceforge.net/wxye.php)
- [Kye (video game) - Wikipedia](https://en.wikipedia.org/wiki/Kye_(video_game))
- [Kye tribute page on Xye site](https://xye.sourceforge.net/kye.php)
- [Xye - Libregamewiki](https://libregamewiki.org/Xye)
- [Kye Python port (benley/kye)](https://github.com/benley/kye)
- [Sokoban XSB file format](https://sokosolve.sourceforge.net/FileFormatXSB.html)
- [Sokoban level format - Sokoban Wiki](http://sokobano.de/wiki/index.php?title=Level_format)
- [Kye level building](https://www.kye.me.uk/editors.html)
- [Play Kye 3.1](https://playkye.com/)
