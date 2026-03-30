# Xye Game Engine -- Technical Specification for Android Port

Extracted from Xye 0.12.2 C++ source code (SourceForge, zlib/libpng license).
Author: Victor Hugo Soliz Kuncar (vexorian), 2005--2013.

---

## 1. Game Constants

```
GRID_WIDTH  = 30   (XYE_HORZ)
GRID_HEIGHT = 20   (XYE_VERT)
FPS         = 20   (XYE_FPS)
FAST_FORWARD_SPEED = 3  (frames per loop iteration in FF mode)
OBJECT_COLORS = 5  (Yellow=0, Red=1, Blue=2, Green=3, Purple=4)
```

The grid wraps toroidally: coordinates exceeding bounds wrap to the opposite edge.
```
FixHorz(x) = if x >= 30 then 0; if x < 0 then 29
FixVert(y) = if y >= 20 then 0; if y < 0 then 19
```

Y-axis: Y=0 is bottom, Y=19 is top. Iteration in loop_gameplay scans j from 19 down to 0 (top to bottom).

## 2. Directions

```kotlin
enum class Dir { UP, DOWN, LEFT, RIGHT }

// UP    => dy = +1
// DOWN  => dy = -1
// RIGHT => dx = +1
// LEFT  => dx = -1
```

Helper functions:
- `Opposite(dir)`: UP<->DOWN, LEFT<->RIGHT
- `Clock(dir)`: clockwise rotation (UP->RIGHT->DOWN->LEFT->UP)
- `AClock(dir)`: counter-clockwise rotation
- `RandomDir()`: uniform random from 4 directions

## 3. Grid Architecture

Each grid cell (`square`) holds:

| Field     | Type       | Description                                          |
|-----------|------------|------------------------------------------------------|
| `object`  | `obj?`     | At most one "normal" object (block, gem, beast, etc) |
| `gobject` | `gobj?`    | At most one "ground" object (trick door, portal, etc)|
| `ex`      | `explosion?`| Visual explosion effect                             |
| `gs`      | `groundskin`| Floor appearance variant (GROUND_1 or GROUND_2)    |
| `R,G,B`   | `Uint8`    | Floor color                                          |

Key constraint: a cell can contain at most ONE normal object AND at most ONE ground object simultaneously.

## 4. Entity Type Hierarchy

### 4.1 Normal Objects (class `obj`)

All normal objects implement:
- `trypush(dir, pusher) -> bool` -- Can this object be pushed by pusher in direction dir?
- `Loop(died) -> bool` -- Per-tick update; returns whether the object moved; sets `died` if it was destroyed
- `HasRoundCorner(corner) -> bool` -- Does this object have the specified round corner?
- `HasBlockColor(bc) -> bool` -- Does this object match the given block color?
- `Kill()` / `Kill(byBlackHole)` -- Destroy this object
- `DoMagnetism(horz, vert, Moved) -> bool` -- Check and apply magnetic forces

### 4.2 Ground Objects (class `gobj`)

Ground objects implement:
- `OnEnter(entering)` -- Called when a normal object enters this cell
- `OnLeave(entering)` -- Called when a normal object leaves this cell
- `CanEnter(entering, dir) -> bool` -- Can the normal object enter?
- `CanLeave(entering, dir) -> bool` -- Can the normal object leave?
- `Loop()` -- Per-tick update (no return value; ground objects don't "move")

## 5. Complete Entity Catalog

### 5.1 Normal Objects

| # | otype           | Class          | XML Tag         | .kye Char | Description |
|---|-----------------|----------------|-----------------|-----------|-------------|
| 1 | OT_XYE          | `xye`          | `<xye>`         | `K`       | Player (green circle). Has lives. Collects gems. |
| 2 | OT_ROBOXYE      | `roboxye`      | `<bot>`          | `?`       | AI companion that mirrors Xye's moves toward Xye. |
| 3 | OT_WALL         | `wall`         | `<wall>`, `<roundwall>` | `1`-`9`, `M` | Immovable obstacle. 6 type variations. Round corners via `round="1379"`. Some types resist fire (kind > 3). |
| 4 | OT_BLOCK        | `block`        | `<block>`        | `b`, `B`  | Pushable colored block (square or round). |
| 5 | OT_LARGEBLOCK   | `largeblock`   | `<largeblockpart>` | --      | Multi-cell block. Connected via `sharededges="URDL"`. Pushed as a group. |
| 6 | OT_METALBLOCK   | `metalblock`   | `<metalblock>`   | --        | Fire-resistant pushable block. |
| 7 | OT_SCROLLBLOCK  | `scrollblock`  | `<scroll>`       | --        | One-way pushable block: can only be pushed in its facing direction. Turners change its direction. |
| 8 | OT_WILDCARD     | `wildcard`     | `<wild>`         | --        | Matches ANY block color for marked areas. |
| 9 | OT_GEMBLOCK     | `gemblock`     | `<gemblock>`     | `@`       | Block that also activates when all gems of matching color OR all stars are collected. Fire-resistant. |
|10 | OT_GEM          | `gem`          | `<gem>`          | `*`, `$`  | Collectible. Only Xye can pick up. When all gems collected, level is won (`TerminateGame(true)`). 4 types: Diamond(blue), Ruby(red), Topaz(yellow), Emerald(green). |
|11 | OT_STAR         | `star`         | `<star>`         | --        | Optional collectible. Tracked separately from gems. Does not trigger level completion. |
|12 | OT_EARTH        | `earth`        | `<earth>`        | `e`       | Consumed when Xye/bot walks over it. Blocks all other objects. |
|13 | OT_KEY          | `key`          | `<key>`          | --        | Xye picks it up (inventory). Used to open matching-color locks. |
|14 | OT_LOCK         | `lock`         | `<lock>`         | --        | Opened (consumed) when Xye pushes into it while holding matching key. |
|15 | OT_TELEPORT     | `teleport`     | `<teleport>`     | `(`, `)`, `_`, `'` | Directional teleport pair. Entry requires moving in opposite of teleport's facing. Scans along its axis for matching exit. |
|16 | OT_BLACKHOLE    | `dangerous`    | `<blacky>`       | `H`       | Kills anything that enters. Absorbs objects visually. |
|17 | OT_MINE         | `dangerous`    | `<mine>`         | `o`       | Explodes on contact. Kills Xye and non-fire-resistant objects. Fire-resistant objects destroy the mine instead. |
|18 | OT_FIREBALL     | `dangerous`    | (created by firepad) | --    | Moving projectile in one direction. Kills on contact. Fire-resistant itself. |
|19 | OT_PUSHER       | `impacter`     | `<pusher>`       | `L`,`R`,`U`,`D` | Self-moving block that bounces. Yellow: normal speed (every 10 ticks). Purple: fast (every 5). Red/purple continue after pushing. |
|20 | OT_ARROW        | `arrow`        | `<arrow>`        | `l`,`r`,`u`,`d`, `<`,`>`,`^`,`v` | Self-moving block. Speed by color: Red=every tick, Yellow=2, Green=3, Blue=4. Turners change direction. |
|21 | OT_AUTO         | `autoarrow`    | `<auto>`         | `A`, `F`  | "Rocky" -- like arrow but with configurable initial direction. Activates once pushed. |
|22 | OT_TURNER       | `turner`       | `<clocker>`, `<aclocker>` | `a`, `c` | Clockwise or counter-clockwise. Changes direction of arrows/scrollblocks that collide. |
|23 | OT_LOWDENSITY   | `lowdensity`   | `<lblock>`       | `.`, `,`  | "Dot block" -- becomes an arrow in the pushed direction after pushed. Speed same as arrow by color. Deactivates on failed move. |
|24 | OT_SURPRISE     | `surprise`     | `<surprise>`     | `:`, `;`, `!` | Transforms when pushed. Yellow->pusher, Blue->wall, Green->changes to blue surprise, Red->explodes, Purple->teleports Xye to its location. |
|25 | OT_TOGGLE       | `toggle`       | `<toggle>`       | `-`, `+`  | Exists in solid/passthrough states. All toggles of same color flip together. When solid, behaves as pushable block; when passthrough, is transparent. |
|26 | OT_NUMBER       | `number`       | `<timer>`        | `}`,`\|`,`{`,`z`,`y`,`x`,`w` | Timer block (0-9). Red: countdown on push, explodes at 0 (fire effect). Blue: auto-countdown every 300 ticks. Yellow: auto-countdown every 60 ticks. Green/Purple: chain-reaction activation near Xye or other active green timers. |
|27 | OT_MAGNETIC     | `magnetic`     | `<magnet>`       | `s`,`S`,`P`,`p`,`Q`,`q` | Three kinds: Magnet (T_MAGNET, attracts), Anti-magnet (T_ANTIMAGNET, repels), Sticky (T_STICKY, holds adjacent objects). Horizontal or vertical orientation. |
|28 | OT_FACTORY      | `factory`      | `<factory>`      | --        | Creates objects when its switch side is pushed. Produces blocks, arrows, mines, pushers, beasts, gems, rattler food, or rattlers. Configurable limit. |
|29 | OT_FILLER       | `filler`       | `<filler>`       | --        | Periodically spawns arrows in its facing direction (every 14 ticks if space is free). |
|30 | OT_SNIPER       | `sniper`       | `<sniper>`       | --        | Aims at Xye and fires arrows toward Xye when aligned (every 7 ticks). |
|31 | OT_BEAST        | `beast`        | `<beast>`        | `E`,`C`,`~`,`[`,`T`,`/` | Enemy. 14 subtypes with different AI. Kills Xye on adjacency (via deathqueue). See Section 7. |
|32 | OT_RATTLER      | `rattler`      | `<rattler>`      | --        | Snake head. Moves with body nodes. Can grow by eating rattler food. Floats over pits if it has body nodes or grow potential. |
|33 | OT_RATTLERNODE  | `rnode`        | `<body>` (child) | --        | Snake body segment. Linked list. Follows head. |
|34 | OT_RATTLERFOOD  | `rfood`        | `<rfood>`        | --        | Food for rattler. Pushable. Round. |
|35 | OT_WINDOW       | `windowblock`  | `<window>`       | --        | "Sensor" block. Activates when adjacent to a same-color block. When all window blocks of a color are active, they become pushable. |
|36 | OT_EXIT         | --             | --               | --        | (Enum exists but not instantiated in source; level completion is via collecting all gems.) |

### 5.2 Ground Objects

| # | otype           | Class        | XML Tag        | .kye Char | Description |
|---|-----------------|--------------|----------------|-----------|-------------|
| 1 | OT_TRICKDOOR    | `tdoor`      | `<oneway>`, `<hiddenway>`, `<force>` | `f`,`g`,`h`,`i` | One-way door. `oneway`: Xye/bot only, one direction. `hiddenway`: custom enter directions (via `ent="2468"`). `force`: any object, acts as force arrow. |
| 2 | OT_BLOCKDOOR    | `blockdoor`  | `<blockdoor>`, `<blocktrap>` | `%`, `=` | Opens/closes based on marked area state. `blockdoor`: opens when ALL marked areas of its color are active. `blocktrap`: opens when AT LEAST ONE is active. |
| 3 | OT_MARKEDAREA   | `marked`     | `<marked>`     | `#`       | Sensor floor tile. Becomes "active" when a same-color block sits on it. Drives blockdoors. |
| 4 | OT_HINT         | `hint`       | `<hint>`       | --        | Shows text when Xye stands on it. |
| 5 | OT_WARNING      | `hint`(warn) | `<warning>`    | --        | Same as hint but with exclamation icon. |
| 6 | OT_PORTAL       | `portal`     | `<portal>`     | --        | Teleports Xye to `(targetx, targety)` on entry. Custom color. One-directional (ground-based, not paired like teleport objects). |
| 7 | OT_FIREPAD      | `firepad`    | `<firepad>`    | --        | Converts any red-colored block that enters into a fireball. |
| 8 | OT_PIT          | `pit`        | `<pit>`        | `O`       | Swallows objects that enter (except fireballs, floating beasts, rattler nodes, and multi-cell large blocks). Object and pit both destroyed. |

## 6. Core Game Loop

### 6.1 Per-Frame Execution Order

```
game::loop()
  if (FinishedLevel):
    incCounters()              // animation only
  else:
    do:
      loop_gameplay()
    while (undo replay || fast-forward)

game::loop_gameplay():
  1. surprise::TransformAll()  // Process all pending surprise transformations
  2. incCounters()             // Advance tick counters (counter, counter2..counter9, beastcounter)
  3. MoveXye()                 // Process player input, attempt Xye movement
  4. for j = 19 downto 0:      // TOP to BOTTOM scan
       for i = 0 to 29:        // LEFT to RIGHT scan
         loop_Sub(i, j)        // Process each cell
  5. deathqueue::KillNow()     // Execute all deferred kills
```

### 6.2 Per-Cell Processing (`loop_Sub`)

```
loop_Sub(i, j):
  sq = grid[i][j]
  if sq.gobject != null:
    sq.gobject.Loop()          // Ground object tick
  if sq.object != null AND sq.object.tic != counter AND sq.object.Loop(died) AND !died:
    sq.object.tic = counter    // Mark as processed this tick (prevents double-processing)
```

The `tic` field prevents an object from being processed twice in the same frame (e.g., if it moved into a cell that hasn't been scanned yet).

### 6.3 Counter System

Multiple modular counters drive timing:

| Counter    | Modulus | Used by |
|------------|---------|---------|
| counter    | --      | Global tick count |
| counter2   | 2       | Beast timing, static beast, toggle |
| counter3   | 3       | (unused in gameplay) |
| counter4   | 4       | Gem sparkle, portal animation |
| counter5   | 5       | Spinner/ward beasts, magnet timing, window block anim |
| counter7   | 7       | Beast animation |
| counter8   | 8       | Trick door animation |
| counter9   | 9       | (unused in gameplay) |
| beastcounter| --     | Beast AI randomization seed |

Beast movement timing (in `beast::Loop`):
- Patience, Tiger, Dard, Ranger: every tick (`OnTime = true`)
- Ward, Spinner, ASpinner: every 5 ticks (`!counter5`)
- Static: every 2 ticks (`counter2 == 0`)
- Default (Gnasher, Blob, Virus, Spike, Twister): every 10 ticks (`!counter2 && !counter5`)

## 7. Beast AI Reference

| # | btype       | AI Function             | OnFail Function     | Behavior |
|---|-------------|-------------------------|---------------------|----------|
| 0 | BT_GNASHER  | BeastAI_Default         | DoNothing           | BFS range 3. Sometimes ignores shortest path, sometimes random. Idle 5% of ticks. |
| 1 | BT_BLOB     | BeastAI_Blob            | DoNothing           | BFS range 5 (or 150 with BlobBoss). Uses teleports/stickies when BlobBoss exists. |
| 2 | BT_VIRUS    | BeastAI_Virus           | DoNothing           | BFS range 3. More aggressive (idle 10%). |
| 3 | BT_SPIKE    | BeastAI_Spike           | DoNothing           | BFS range 4. Almost never idle (1%). Highly directional. |
| 4 | BT_TWISTER  | BeastAI_Twister         | DoNothing           | BFS range 10. Idle 50% of ticks. |
| 5 | BT_DARD     | BeastAI_Dard            | DoNothing           | Only moves when aligned (same row/column) with Xye. Continues in direction for 10 ticks after losing alignment. Floats over pits. |
| 6 | BT_WARD     | BeastAI_ReallyDumb      | WardFails (reverse) | Moves in facing direction. On fail, reverses. |
| 7 | BT_SPINNER  | BeastAI_ReallyDumb      | SpinnerFails (CW)   | Moves in facing direction. On fail, rotates clockwise. |
| 8 | BT_ASPINNER | BeastAI_ReallyDumb      | AspinnerFails (CCW) | Moves in facing direction. On fail, rotates counter-clockwise. |
| 9 | BT_PATIENCE | BeastAI_Patience        | DoNothing           | Full BFS (range 600) using teleports and stickies. Only moves when Xye moved. Also pathfinds to other Patience beasts. Floats over pits. |
|10 | BT_BLOBBOSS | BeastAI_BlobBoss        | DoNothing           | Smart BFS (range 6-100). Also hunts gems. Uses teleports and stickies. Enhances all BT_BLOB behavior. |
|11 | BT_STATIC   | BeastAI_Static          | StaticFails         | Stationary until pushed. When pushed, moves in push direction until it fails. |
|12 | BT_RANGER   | BeastAI_Ranger          | DoNothing           | Only attacks when aligned with Xye AND facing that direction. BFS range 30. |
|13 | BT_TIGER    | BeastAI_Tiger           | DoNothing           | Full BFS (range 600) using teleports/stickies. Moves every tick when Xye moves. Most dangerous beast. |

Beast kill mechanism: After each beast loop (moved or not), `deathqueue::add(x, y, KT_KILLXYE)` is called. This queues a check: if Xye is adjacent (Manhattan distance 1 in cardinal directions), Xye is killed.

Beasts cannot be pushed by Xye (trypush returns false for OT_XYE pusher). They CAN be pushed by other objects (magnets, pushers).

## 8. Push/Move Resolution

### 8.1 Who Can Push

Only these object types can initiate a push:
```
CanPush(type): OT_XYE, OT_PUSHER, OT_MAGNETIC, OT_ROBOXYE
```

### 8.2 Push Resolution (`trypush_common`)

```
trypush_common(dir, pusher, AsRoundObject, died):
  1. If pusher exists and pusher != self and !CanPush(pusher.type): return false
  2. If AsRoundObject:
       sq = RoundAdvance(self, dir, x, y)    // Try diagonal slide
       if sq != null: move to sq; return true
  3. Compute destination (dx, dy) with wrapping
  4. Check destination object:
     a. OT_TELEPORT: Try teleport. May redirect to new destination.
     b. OT_BLACKHOLE / OT_MINE / OT_FIREBALL:
        - If "busy" (absorbing): return false
        - If mine and self is fire-resistant: mine is destroyed; continue
        - Otherwise: self is killed; return true (died=true)
  5. If destination empty (no object):
     - Check ground object CanEnter
     - If allowed: move; return true
  6. return false
```

### 8.3 Round Corner Sliding (`RoundAdvance`)

When a round object is pushed against another object with a matching round corner:
1. Check if the blocking object has a round corner on the appropriate side
2. If yes, check if the diagonal path (two cells) is clear
3. If both diagonal paths are valid, pick one randomly (50/50)
4. Move the pushed object to the diagonal destination

Round corners are identified by numpad positions:
- RC_1 = bottom-left, RC_3 = bottom-right, RC_7 = top-left, RC_9 = top-right

### 8.4 Xye Movement (`TryMoveXye`)

```
TryMoveXye(dir):
  1. Compute destination cell
  2. If destination has:
     - Teleport: try teleport; may redirect
     - Black hole / Mine / Fireball: kill Xye if not busy
  3. If not resolved, try object.trypush(dir, XYE)
  4. If clear, check ground object CanEnter
  5. If all clear: move Xye, record direction
```

Xye can only move every 2 ticks (`LastXyeMove + 1 < counter`).

### 8.5 Object-Specific Push Rules

| Object | Push behavior |
|--------|---------------|
| Block  | Standard push (round if round variant) |
| ScrollBlock | Only pushable in its facing direction (except by magnets) |
| Arrow  | Standard push; round only when pushed in arrow's facing direction |
| LowDensity | Standard push; activates in push direction |
| Surprise | Push triggers transformation; Red explodes without moving |
| Gem    | Only Xye can pick up (removes gem, decrements count) |
| Star   | Only Xye can pick up (increments acquired count) |
| Earth  | Only Xye/Bot can consume (removes earth) |
| Key    | Xye picks up (adds to inventory); others push normally |
| Lock   | Only Xye with matching key can open (consumes both) |
| Beast  | Cannot be pushed by Xye; pushed by others with standard push |
| Rattler/RNode | Cannot be pushed |
| Toggle | Pushable when in solid state; toggles state for all same-color |

## 9. Magnetic System

Three types of magnetic entities:
1. **Magnet (T_MAGNET)**: Pulls adjacent objects toward itself every tick
2. **Anti-Magnet (T_ANTIMAGNET)**: Pushes adjacent objects away
3. **Sticky (T_STICKY)**: Holds adjacent objects in place; prevents them from moving away. Directional -- only affects objects approached from specific sides.

Orientation: horizontal magnets affect objects to their left/right; vertical magnets affect objects above/below.

Objects NOT affected by magnetism:
- Xye, RoboXye, Wall, Earth, AutoArrow, BlackHole, Gem, Filler, Sniper, Rattler, RattlerNode

Window blocks are only affected when all window blocks of their color are active.

## 10. Surprise Block Transformation Table

| Color  | Transformation when pushed |
|--------|----------------------------|
| Yellow | Becomes a pusher (impacter) facing the push direction |
| Blue   | Becomes a wall (with intelligent corner detection) |
| Green  | Changes to a blue surprise (delays transformation by one more push) |
| Red    | Explodes (fire effect in 5 cells -- center + 4 cardinal) |
| Purple | Teleports Xye to the surprise block's former location |

Transformation happens at the START of the next `loop_gameplay()` via `surprise::TransformAll()`.

## 11. Toggle Block Mechanics

All toggle blocks of the same color share state. When any toggle block is pushed, the state for that color flips. Solid-state toggles are pushable and block movement. Transparent-state toggles are invisible/passthrough.

Static field `State[5]` (one per color). `ChangeTic` tracks when last change happened.

## 12. Block Door / Marked Area System

- **Marked Area**: Ground tile that becomes "active" when a block of matching color is placed on it. Wildcard blocks match any color.
- **Block Door**: Ground tile that opens/closes based on marked area state.
  - `blockdoor` (mode=false by default): Opens when ALL marked areas of its color are active
  - `blocktrap` (mode=true): Opens when AT LEAST ONE marked area of its color is active
- When open, any object can pass through. When closed, blocks all objects.
- Gems, keys, and earth can be seen through closed block doors (`InsideKind`).

## 13. Teleport System

Teleports are directional normal objects. They work in pairs:
- Entry teleport has a facing direction (e.g., D_UP)
- Object must approach from the OPPOSITE direction (e.g., D_DOWN for D_UP entry)
- Exit teleport is found by scanning in the entry's facing direction, looking for a teleport with the OPPOSITE facing
- The exit point is one cell beyond the exit teleport in the object's movement direction
- If exit is blocked, tries to push the blocking object
- If exit contains a dangerous (black hole/mine), it may kill the teleported object
- Scan wraps around the grid (toroidal)

## 14. Death/Kill System

### 14.1 Deferred Kill Queue (`deathqueue`)

Kills are not applied immediately during loop processing. Instead, they are queued and executed AFTER all objects have been processed in that frame.

Kill types:
- `KT_KILLXYE`: Checks if Xye is adjacent (cardinal, distance 1) to the kill zone. If yes, kills Xye.
- `KT_KILLORGANICS`: (Defined but rarely used directly)
- `KT_FIRE`: Triggers `SmallBoom` in 5 cells (center + 4 cardinal neighbors). Destroys non-fire-resistant objects.

### 14.2 Object Recycling

Destroyed objects are added to `recycle::add()` queue instead of being deleted immediately. `recycle::run()` actually frees memory at a safe point.

### 14.3 Fire Resistance

Objects that resist fire (not destroyed by mines/fire):
- MetalBlock, GemBlock, Lock, Fireball, Gem, WindowBlock
- Wall (only if kind > 3, i.e., specific wall variations)

## 15. Level File Formats

### 15.1 .xye Format (XML)

```xml
<?xml version="1.0" encoding="ISO-8859-1"?>
<pack>
  <name>Pack Name</name>
  <author>Author Name</author>
  <description>Description text</description>

  <level>
    <title>Level Title</title>
    <bye>Completion message</bye>
    <hint>Global hint text</hint>
    <solution>encoded-move-string</solution>

    <palette>
      <color id="1" red="192" green="192" blue="192" mode="MULTIPLY"/>
    </palette>

    <default>
      <wall color="1" type="2"/>
      <earth color="1"/>
      <trick color="1"/>
      <force color="1"/>
    </default>

    <floor>
      <area x="0" y="0" x2="29" y2="19" color="1" skin="1"/>
    </floor>

    <objects>
      <xye x="5" y="3" lives="4"/>
      <wall x="0" y="0" x2="29" y2="0" type="0" color="1" round="1379"/>
      <block x="5" y="5" bc="Y" round="1" nocolor="0"/>
      <gem x="10" y="10" bc="B"/>
      <beast x="15" y="15" kind="0" dir="D"/>
      <teleport x="3" y="3" dir="R"/>
      <pusher x="7" y="7" bc="Y" dir="D"/>
      <arrow x="8" y="8" bc="R" dir="U" round="0"/>
      <magnet x="9" y="9" kind="0" horz="1"/>
      <surprise x="10" y="5" bc="B" round="0"/>
      <portal x="2" y="2" targetx="20" targety="15" color="1"/>
      <oneway x="5" y="10" dir="U"/>
      <hiddenway x="5" y="11" ent="268"/>
      <force x="5" y="12" dir="U"/>
      <marked x="5" y="6" bc="Y"/>
      <blockdoor x="5" y="7" bc="Y" open="0"/>
      <blocktrap x="5" y="8" bc="Y"/>
      <hint x="3" y="3">Hint text here</hint>
      <warning x="3" y="4">Warning text</warning>
      <firepad x="3" y="5"/>
      <pit x="3" y="6"/>
      <factory x="3" y="7" kind="0" bc="Y" dir="D" swdir="U" limit="5"/>
      <timer x="3" y="8" bc="R" val="5" round="0"/>
      <toggle x="3" y="9" bc="Y" round="0" off="0"/>
      <auto x="3" y="10" bc="Y" dir="D" round="0"/>
      <clocker x="3" y="11" bc="Y" round="0"/>
      <aclocker x="3" y="12" bc="Y" round="0"/>
      <filler x="3" y="13" bc="Y" dir="D" round="0"/>
      <sniper x="3" y="14" bc="Y" round="0"/>
      <lblock x="3" y="15" bc="Y" round="0"/>
      <key x="3" y="16" bc="Y"/>
      <lock x="3" y="17" bc="Y"/>
      <earth x="3" y="18" round="0" color="1"/>
      <bot x="3" y="19"/>
      <star x="4" y="4"/>
      <mine x="4" y="5"/>
      <blacky x="4" y="6"/>
      <wild x="4" y="7" round="0"/>
      <metalblock x="4" y="8" round="0"/>
      <scroll x="4" y="9" bc="Y" dir="D" round="0"/>
      <window x="4" y="10" bc="Y"/>
      <gemblock x="4" y="11" bc="G"/>
      <rattler x="4" y="12" dir="D" grow="3">
        <body x="4" y="13"/>
        <body x="4" y="14"/>
      </rattler>
      <rfood x="4" y="15"/>
      <largeblockpart x="10" y="10" bc="Y" sharededges="RD"/>
    </objects>
  </level>
</pack>
```

#### Attribute Reference

- `bc` (block color): `Y`(Yellow), `R`(Red), `B`(Blue), `G`(Green), `P`(Purple)
- `dir`: `U`(Up), `D`(Down), `L`(Left), `R`(Right)
- `round`: `0` or `1` (boolean for blocks); for walls, string of corners e.g. `"1379"`
- `color`: palette color ID (integer)
- `kind` (beast): 0=Gnasher, 1=Blob, 2=Virus, 3=Spike, 4=Twister, 5=Dard, 6=Ward, 7=Spinner, 8=ASpinner, 9=Patience, 10=BlobBoss, 11=Static, 12=Ranger, 13=Tiger
- `kind` (factory product): 0=block, 1=arrow, 2=lowdensity, 3=mine, 4=pusher, 5=beast, 6=rattlerfood, 7=rattler, 8=gem
- `kind` (magnet): 0=Magnet, 1=Anti-magnet, 2=Sticky

#### Legacy compatibility

Levels may use `<ground>`, `<normal>`, or `<objects>` tags interchangeably for the object container. All three are loaded identically. The `<kyeformat>` tag can embed .kye-format level data within an XML level.

### 15.2 .kye Format (Plain Text)

```
<number of levels>
<level 1 name>
<level 1 hint>
<level 1 bye message>
<20 lines of 30 characters each, top row first>
<level 2 name>
...
```

Character mapping (30x20 grid, rows top to bottom):

| Char | Object | Char | Object |
|------|--------|------|--------|
| `K`  | Xye (player) | `?` | Bot (roboxye) |
| `1`-`9` | Wall (round corners by digit) | `M` | Wall (type 4) |
| `5`  | Solid wall (no round corners) | ` ` | Empty |
| `b`  | Block (square) | `B` | Block (round) |
| `*`  | Gem (blue/diamond) | `$` | Gem (green/emerald) |
| `e`  | Earth | `H` | Black hole |
| `o`  | Mine | `@` | GemBlock (green) |
| `E`  | Gnasher | `C` | Blob |
| `~`  | Virus | `[` | Spike |
| `T`  | Twister | `/` | Dard |
| `s`  | Magnet (vertical) | `S` | Magnet (horizontal) |
| `P`  | Sticky (horizontal) | `p` | Sticky (vertical) |
| `Q`  | Anti-magnet (horizontal) | `q` | Anti-magnet (vertical) |
| `l`  | Arrow left | `r` | Arrow right |
| `u`  | Arrow up | `d` | Arrow down |
| `<`  | Arrow left (round) | `>` | Arrow right (round) |
| `^`  | Arrow up (round) | `v` | Arrow down (round) |
| `L`  | Pusher (bouncer) left | `R` | Pusher right |
| `U`  | Pusher up | `D` | Pusher down |
| `A`  | Auto-arrow (square) | `F` | Auto-arrow (round) |
| `a`  | Clocker (CW turner) | `c` | AClockr (CCW turner) |
| `f`  | One-way left | `g` | One-way right |
| `h`  | One-way up | `i` | One-way down |
| `(`  | Teleport (entry right) | `)` | Teleport (entry left) |
| `_`  | Teleport (entry up) | `'` | Teleport (entry down) |
| `.`  | Dot block (lowdensity, square) | `,` | Dot block (round) |
| `:`  | Surprise (blue, square) | `;` | Surprise (blue, round) |
| `!`  | Surprise (red/bomb) | `-` | Toggle (off) |
| `+`  | Toggle (on) | `#` | Marked area (yellow) |
| `%`  | Block door (yellow) | `=` | Block trap (yellow) |
| `O`  | Pit | `}` | Timer (3) |
| `\|` | Timer (4) | `{` | Timer (5) |
| `z`  | Timer (6) | `y` | Timer (7) |
| `x`  | Timer (8) | `w` | Timer (9) |

### 15.3 .xsb / .slc Format (Sokoban)

Standard Sokoban level format:

```
@ - Sokoban (player/Xye)
+ - Sokoban on target (player on marked area)
# - Wall
$ - Box (block)
. - Target (marked area)
* - Box on target (block on marked area)
  - Empty space
```

.xsb files are plain text with levels separated by blank lines or metadata lines.
.slc files are XML (`<SokobanLevels><LevelCollection><Level Width="N" Height="N">` with `<L>` tags for rows).

Sokoban levels support undo (the only format that does).

Levels that are taller than wide get rotated 90 degrees to fit the 30x20 grid.

## 16. Edge Cases and Quirks

1. **Double-processing prevention**: Each object has a `tic` field set to the current frame counter after processing. This prevents objects that move "forward" in the scan order from being processed twice.

2. **Surprise Transform timing**: Surprise blocks don't transform immediately on push. They are queued (`Pending` counter) and all transform at the START of the next frame in `TransformAll()`, before any objects are processed.

3. **Teleport directionality**: A teleport facing UP requires objects to enter from BELOW (moving UP). The exit search scans UPWARD from the entry teleport, wrapping around.

4. **Beast kill adjacency**: Beasts don't kill Xye directly. They add kill zones to the `deathqueue`. Kill zones check if Xye is within Manhattan distance 1 (cardinal only). This means kills are deferred to end-of-frame.

5. **Fire pad conversion**: When a red-colored block enters a firepad, it is destroyed and replaced with a fireball. This includes red arrows, red blocks, red pushers, etc.

6. **Pit floating**: Dard and Patience beasts float over pits. Rattlers float if they have body nodes or grow potential. Large blocks only fall into pits if they are actually single-cell.

7. **Factory creation**: Factories have a switch side (`swdir`) and a create side (`dir`). Pushing from the switch side into the factory creates an object on the create side.

8. **Impacter (pusher) bouncing**: When a pusher hits something it can't push, it calls `turn()`. Yellow pushers reverse; Red/purple pushers continue forward after pushing an object, only turning on complete blockage.

9. **Round object randomization**: When a round object is pushed against another round object and both diagonal paths are clear, the direction is chosen with 50% probability using `GetRandom32()`.

10. **RoboXye sync**: The bot only moves when Xye moved in the PREVIOUS frame (`AllowRoboXyeMovement` checks `LastXyeMove + 1 == counter`). This means bot movement is always one frame behind Xye.

11. **Window block activation**: Window blocks check their 4 cardinal neighbors for same-color blocks. When ALL window blocks of a color are activated, they become pushable (and affected by magnetism).

12. **Number (timer) colors**: Red counts DOWN on push; Blue auto-counts every 300 ticks; Yellow auto-counts every 60 ticks; Green/Purple chain-react when Xye is adjacent or another active timer of same color is adjacent.

13. **Toroidal wrapping**: All movement and coordinate calculations wrap around both axes. This applies to player movement, object pushing, teleport scanning, beast pathfinding, and magnetic checks.

14. **Object fire resistance check**: Mine explosion kills the mine but not fire-resistant objects pushed into it. The fire-resistant object survives and the mine is destroyed.

15. **Level completion**: The level is won when the last gem is collected (`gem::count[XYE_OBJECT_COLORS] == 0`). Stars are optional collectibles that don't affect completion.

16. **Xye lives**: Xye starts with 4 lives by default (configurable per level). On death, Xye respawns at the last checkpoint. When lives reach 0, game over.

17. **GemBlock activation**: GemBlock trypush checks if all gems of its color are collected. If so, the gemblock becomes collectible (removed). Purple gemblocks activate when all stars are collected.

## 17. Palette System

Colors can be defined in a palette and referenced by integer ID:
```xml
<palette>
  <color id="1" red="128" green="64" blue="32" mode="MULTIPLY"/>
</palette>
```

Mode `MULTIPLY` multiplies with the base sprite color. Mode `RECOLOR` replaces the color entirely. Palette is per-level and cleared between levels.

## 18. Recording/Replay System

Moves are recorded as a sequence of directions with a special "no move" marker. The replay system feeds moves back into the input system. Undo replays the recording in reverse from a full restart, stopping at the target frame.

Solutions can be embedded in level files via `<solution>` tags.

---

## Sources

- SourceForge project: https://sourceforge.net/projects/xye/
- Official website: https://xye.sourceforge.net/
- Source tarball: xye-0.12.2.tar.gz (SHA1: bdfc3d88fbc852fa728c9981f83c9dd048d159ae)
- License: zlib/libpng
