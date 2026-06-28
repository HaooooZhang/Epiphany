# AGENTS.md — Epiphany (顿悟)

NeoForge 1.21.1 library mod providing a modular skill-tree system. Data-driven, API-first.

## Build & Run

```bash
./gradlew build                    # Full build (compile + test + jar)
./gradlew runClient                # Launch Minecraft client
./gradlew runServer                # Launch dedicated server
./gradlew runData                  # Run data generators
```

- **Java 21**, **Gradle 9.2.1**, **Parchment mappings** (2024.11.17)
- modid: `epiphany` | package: `ink.myumoon.epiphany`
- Generated resources output to `src/generated/resources/`
- `generateModMetadata` task auto-runs via `neoForge.ideSyncTask` — do not run manually

## Architecture

### Four core datapack registries (all Codec-based, registered via data pack JSON)

| Registry | Purpose | Has Condition? | Has Reward? |
|----------|---------|:---:|:---:|
| `Module` | Independent skill-tree unit; contains Insights | Yes | on_select + on_complete |
| `Insight` | Incremental upgrade node inside a Module | No | Yes (quantitative) |
| `Epiphany` | Global qualitative ability; chosen after Module complete | Yes | Yes (qualitative) |
| `Path` | Optional grouping label for Epiphanies | No | No |

### Three extension registries (for mod compatibility)

- `InsightRewardType` — reward types usable by Insights
- `EpiphanyRewardType` — reward types usable by Epiphanies
- `ConditionType` — unlock conditions for Module / Epiphany

### Player data: NeoForge AttachmentType

All player state persisted via NBT on an Attachment attached to `ServerPlayer`:
- `aptitude` (long), `insightPoints` (int), `totalInsightPointsSpent` (int)
- Per-Module: `unlockedInsights` set, `selected`, `completed`, `unlocked` booleans
- Per-Insight: `selected`, `moduleSelected` booleans
- Per-Epiphany: `selected`, `unlocked` booleans
- `epiphanySlots` (int), `usedEpiphanySlots` (int)

**Must configure Attachment sync** for client-side UI access. Use NeoForge's `AttachmentType.Builder#serialize` + `copyOnDeath` + sync where needed.

### Event bus conventions

- **Pre**-prefixed events → cancellable (block the action)
- **Post**-prefixed or no-prefix events → notification only
- All events fire on the Forge event bus (`NeoForge.EVENT_BUS`)

### API: four static manager classes

`AptitudeManager` / `ModuleManager` / `InsightManager` / `EpiphanyManager` — all public static methods. Query/set/unlock/reset per entity.

### Commands

Root: `/epiphany` with sub-command groups: `aptitude`, `insight`, `module`, `epiphany`, `path`, `reset`. Admin-only for mutations.

## Confirmed Design Decisions

- **Epiphany → Path direction**: Epiphany has optional `"path"` field; Path does NOT hold an epiphany list. One Epiphany belongs to at most one Path.
- **Insight tree via depth**: Module JSON defines `"insights"` as an array of `{"id": "...", "depth": N}`. Same-depth Insights are AND-related — all at depth 0 must be unlocked before depth 1, etc. Parent of a depth-N Insight is the nearest preceding depth-(N-1) entry in the array.
- **Insight unlock requirements**: Module must be selected + all ancestor Insights (all lower depths) must be unlocked.
- **Module select cost**: Uniform across all modules, configured via `moduleSelectCost` in Config (default 1, range 0–100). Not per-module.
- **Module `initial_state` semantics**:
  - `locked` + no condition → can only be unlocked via API/command (developer-triggered flow).
  - `locked` + condition → auto-unlocks when condition is met.
  - `selectable` → visible and selectable immediately (no condition needed).
- **`ModuleCompleteEvent` cancel behavior**: If cancelled (Pre event), no reward is granted, no Epiphany slot is added, and `ModuleCompletedEvent` is NOT fired. Module stays in "all Insights unlocked, not completed" state — completion can be re-triggered later.
- **Aptitude acquisition**: Data pack JSON mapping table (`action_type` + `target_id` → `aptitude_value`) plus API events for mod extensibility. Exact format TBD.
- **Reset semantics**: No single-point undo during gameplay. `/epiphany reset all` full wipe; `/epiphany reset select` clears module/epiphany choices but keeps aptitude/points. All admin-only.
- **Registry entry removal**: If a datapack update removes an unlocked Insight/Module/Epiphany, refund the spent resources (points/slots) to prevent progress loss.
- **Condition logic**: AND/OR/NOT combinators using `{"type": "epiphany:and", "conditions": [...]}` pattern.
- **Network sync**: NeoForge `AttachmentType.Builder#serialize` + `copyOnDeath` for syncing player data to client.

## Dependencies

- **LDLib2** (`curse.maven:ldlib-626676:8280846`) — frontend UI framework. Docs at `Docs/ldlib2/`.
- No mixins. No access transformers.

## Current State

The codebase is scaffold from NeoForged MDK. Three Java files exist with example content (example_block, example_item, example_tab). Replace — do not add alongside.
