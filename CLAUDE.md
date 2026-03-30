# CLAUDE.md

## Project Overview

Android port of [Xye](https://xye.sourceforge.net/), a grid-based puzzle/arcade game derived from Kye. Pure Kotlin engine with Jetpack Compose UI. Upstream is C++ with SDL, licensed zlib/libpng.

## Getting Started

Read these docs in order before writing code:

1. **`docs/xye-source-analysis.md`** — Technical spec extracted from Xye 0.12.2 C++ source. Contains: game constants, grid architecture, all 36 normal + 8 ground object types, beast AI variants, push resolution rules, deferred kill system, level format schemas (.xye XML, .kye text, .xsb Sokoban). **This is the authoritative rules reference.**

2. **`docs/implementation-plan.md`** — Full architecture spec: 6 Gradle modules, version catalog, core data models (with Kotlin code), 12-phase simulation pipeline, content parser interfaces, Room persistence schema, Compose UI architecture, CI workflow, and 4 implementation phases with 19 ordered steps. **This is the build plan — follow it.**

## Architecture

```
:engine      — Pure Kotlin, zero Android deps. Game rules, simulation, undo.
:content     — Level format parsers (.xye XML, .kye, .xsb). Depends on :engine.
:persistence — Room DB for saves, progress, replays. Depends on :engine.
:app         — Android UI shell (Compose + Material 3). Depends on all above.
:benchmark   — Determinism + perf tests. Depends on :engine, :content.
:editor      — Phase 2. Level editor. Depends on :engine.
```

## Key Design Decisions

- **Deterministic simulation**: Same inputs must produce identical states. No floating point, no System.currentTimeMillis() in engine.
- **Immutable GameState**: Each tick produces a new state. Old states go to undo ring buffer.
- **12-phase tick pipeline**: input → undo → snapshot → player move → magnets → triggers → teleport → autonomous → collision → collection → win/loss → commit. Order matters — see source analysis.
- **Toroidal 30x20 grid**: Coordinates wrap. Y=0 is bottom, Y=19 is top.
- **Deferred kills**: Beast kills and fire effects are queued during loop processing, executed after all objects process. Prevents evaluation-order bugs.
- **Orthogonal only**: No diagonal movement. This is a core Xye rule, not a simplification.

## Build Requirements

- Java 17+
- Android SDK (compile SDK 36, min SDK 29, target SDK 36)
- Gradle 8.12+ with Kotlin DSL
- No signing config needed initially — debug builds are fine

## Implementation Order

Follow Phase 1 in `docs/implementation-plan.md`:

1. Scaffold Gradle multi-module project
2. Implement core data models in `:engine`
3. Implement GameEngine tick loop with basic entities (Wall, Gem, Player, PushBlock)
4. Add .kye parser in `:content` (simplest format — 30-char lines, character map in source analysis)
5. Build minimal Compose UI: board canvas + d-pad + undo button
6. Wire ViewModel to engine
7. Add more entity types incrementally (see entity catalog in source analysis)
8. Add persistence (saves, progress)
9. Add .xye XML parser
10. Polish: monster AI, sound, animations

## Critical Rules from Source Analysis

- Push resolution is complex: round objects slide diagonally past round-cornered obstacles
- Only 4 types can initiate pushes: Xye (player), Pusher, Magnetic, RoboXye
- 14 beast AI variants with BFS pathfinding (range 3-600), idle probabilities, on-fail behaviors
- Stars must be collected BEFORE the last gem (not after)
- Timer blocks decrement each tick and vanish at 0
- Teleports intercept pushes — pushed objects teleport instead of moving normally
- Black holes destroy non-wall, non-fire-resistant objects

## Testing

- Engine module: JVM unit tests (no Android needed)
- Determinism test: replay recorded inputs, verify state hash matches
- Content module: parse bundled test levels, verify entity counts
- No UI tests needed initially

## CI

GitHub Actions: build all modules, run :engine and :content tests on every push.

## Upstream Reference

- Source: https://sourceforge.net/projects/xye/ (version 0.12.2)
- License: zlib/libpng
- Core files: `src/xye.cpp` (9,347 lines), `src/xye.h` (1,623 lines)
- Official site: https://xye.sourceforge.net/
