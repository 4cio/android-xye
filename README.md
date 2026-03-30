# Xye for Android

Kotlin/Compose port of [Xye](https://xye.sourceforge.net/), a grid-based puzzle/arcade hybrid derived from [Kye](https://en.wikipedia.org/wiki/Kye_(video_game)).

## What is Xye?

A puzzle game where you control a green circle, collecting all gems while navigating pushable blocks, magnets, teleporters, monsters, and other hazards on a 30x20 toroidal grid. Orthogonal movement only. Unlimited undo. 36 object types. 14 monster AI variants.

## Status

**Planning** — architecture and source analysis complete, implementation not started.

See `docs/` for:
- `implementation-plan.md` — Full build spec (modules, data models, game loop, parsers, persistence, UI, CI)
- `xye-source-analysis.md` — Technical spec extracted from Xye 0.12.2 C++ source code

## Architecture

| Module | Purpose |
|--------|---------|
| `:engine` | Pure Kotlin game rules, simulation, undo (no Android deps) |
| `:content` | Level parsers (.xye XML, .kye, .xsb Sokoban) |
| `:persistence` | Room DB — saves, progress, replays |
| `:app` | Compose UI shell |
| `:benchmark` | Determinism + perf tests |
| `:editor` | Level editor (Phase 2) |

## License

Upstream Xye is [zlib/libpng](https://opensource.org/licenses/Zlib) licensed.
This port: TBD (will be compatible with upstream).
