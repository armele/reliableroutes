# Reliable Routes Architecture

This document is a maintainer's guide to the mod. It explains where behavior lives, how the major pieces interact, and what to check when making changes.

## Project Overview

Reliable Routes is a NeoForge mod for Minecraft 1.21.1. It integrates with MineColonies pathfinding and uses Domum Ornamentum for materially textured blocks. The project targets Java 21.

The code is organized by responsibility:

- `ReliableRoutes.java` registers blocks, items, the creative tab, configuration, networking, and the custom MineColonies navigator.
- `block/` and `item/` implement player interaction and routing blocks.
- `navigation/` contains routing-zone data, selection logic, persistence, and citizen pathfinding.
- `network/` synchronizes nearby routing zones to clients for visualization.
- `client/` and `ReliableRoutesClient.java` contain rendering-only code.
- `src/main/resources/` contains recipes, loot tables, tags, translations, blockstates, and models.
- `src/test/` contains unit tests for geometry and route-status logic.

## Runtime Data Flow

The most important flow is:

1. A player uses the Pathfinder Lens to select two Path Pair blocks and two zone corners.
2. `PathfinderLensItem` validates the selection and creates a `RoutingZone`.
3. `WaypointPairSavedData` stores the completed zone in the current dimension.
4. When a MineColonies citizen requests a path, `ReliableRoutesPathNavigate` asks the saved data whether the trip crosses a routing zone.
5. If it does, the navigator divides the trip into stages: entrance, exit, then the original destination.
6. Each stage uses `PathJobMoveToLocationInZones`, which prevents ordinary swimming through protected zones while preserving solid crossings.
7. The result of a crossing attempt is recorded separately for each direction.
8. While a player holds the lens, the client requests nearby snapshots and renders zone boundaries, endpoints, direction arrows, and health information.

Completed routing data is server-authoritative. Do not move route decisions into client code or rely on the client cache for gameplay behavior.

## Registration and Startup

`ReliableRoutes` is the common mod entry point. It uses NeoForge deferred registers for all blocks, items, and the creative tab.

During common setup it registers `ReliableRoutesPathNavigate` with MineColonies' `IPathNavigateRegistry`. Only MineColonies citizens receive this navigator, and only while `enableCustomPathfinding` is enabled. When the option is disabled, citizens fall back to normal MineColonies navigation; blocks and saved zones remain available.

`ReliableRoutesClient` is a client-only entry point. Keep Minecraft client classes and rendering logic on this side so a dedicated server can load the mod safely.

Shared registry names and model identifiers belong in `Constants`. A new player-facing block normally also needs registration in `ReliableRoutes`, a block item, creative-tab placement, a translation, a recipe, a loot table, a blockstate, and block/item models.

## Blocks, Items, and Tags

`RoutingBlock` is the base class for the road and forbidden-ground variants. It implements Domum Ornamentum's materially textured block interfaces and stores selected material data in a block entity. `RoutingBlockItem` exposes these blocks through the Domum Ornamentum material group.

The blocks gain their MineColonies behavior through data tags rather than Java conditionals:

- `data/minecolonies/tags/block/pathblocks.json` identifies preferred road blocks.
- `data/minecolonies/tags/block/dangerousblocks.json` identifies forbidden ground.

`PathPairBlock` extends `RoutingBlock`. Using it with a Pathfinder Lens delegates to `PathfinderLensItem`; removing an endpoint also removes its saved routing zone.

The lens maintains an unfinished `Draft` in the item's custom data. The selection order is two endpoints followed by two opposite corners. Validation requires both endpoints to exist inside the same colony, limits endpoint and zone size, and rejects overlapping zones. Crouch-use clears a draft or removes an existing zone.

## Routing Model and Persistence

`RoutingZone` is the central immutable value object. Its X/Z bounds come from the selected corners. Its Y bounds are derived from endpoint heights with `VERTICAL_PADDING`, so the zone is a rectangular volume rather than an infinitely tall column.

`WaypointPairSavedData` owns all completed zones and direction health for one `ServerLevel`. Minecraft's `SavedData` storage therefore makes the data persistent and dimension-local. Every mutation must call `setDirty()` or it may not be written to disk.

The saved NBT contains:

- normalized zone bounds;
- the two endpoint positions;
- health from first to second; and
- health from second to first.

Health is directional because a crossing can work one way and fail the other. `WaypointPairDirectionHealth` stores its status, failure reason, and validation game time. Unknown enum values in saved data are currently ignored during loading for basic forward compatibility.

Important invariants enforced by `WaypointPairSavedData` are:

- both endpoints must be inside their zone;
- zones cannot overlap in X/Z;
- an endpoint belongs to at most one zone; and
- removing a zone also removes both direction-health entries.

Keep persistence changes backward-compatible. If fields are renamed or their meaning changes, add migration/default behavior in `load` rather than assuming every world was created by the newest version.

## Route Selection and Citizen Navigation

`WaypointPairSavedData.selectRoute` first checks whether the straight start-to-destination segment intersects a zone. `RoutingZoneMath` performs this box intersection using pure geometry, which keeps the delicate math independently testable.

For an intersected zone, route selection checks which side of the endpoint axis contains the start and destination. A route is selected only when the trip crosses from one side to the other. If the citizen starts inside a zone, an escape plan sends it toward the appropriate endpoint. When multiple zones qualify, the earliest intersection along the trip is chosen; after leaving it, navigation can select the next zone.

`ReliableRoutesPathNavigate` is a small state machine with three stages:

- `TO_ENTRANCE`
- `TO_EXIT`
- `LEAVING_ZONE`

It wraps both normal move requests and "move close to" requests. If a waypoint disappears, a path stage fails, the route is cancelled, or custom routing is disabled, it clears its temporary state and resumes the original MineColonies request. Preserve this fallback behavior when changing the state machine: routing assistance must not leave a citizen permanently stuck.

`PathJobMoveToLocationInZones` extends MineColonies' normal location path job. Inside relevant zones it rejects water nodes, except nodes that help a citizen already inside the zone move toward its escape endpoint. Non-water nodes retain MineColonies' normal scoring and validation.

The `findZonesForPath` query deliberately expands the start/destination rectangle by the citizen's search range. If zone lookup changes, account for pathfinder detours rather than checking only the direct line.

## Client Synchronization and Visualization

The lens overlay does not receive every zone continuously. While the lens is held, `ReliableRoutesClient` periodically sends `RequestWaypointPairsPayload` around the player. The server validates that the player is holding the lens, bounds the requested radius, and replies with `ClientboundWaypointPairsPayload`.

The response contains `RoutingZoneSnapshot` values: a zone plus health in both directions. `ClientWaypointPairCache` stores the latest immutable list for rendering. The cache is disposable and must never become a gameplay source of truth.

`ReliableRoutesClient.LensOverlay` also scans loaded nearby blocks for MineColonies road and dangerous-block tags. It renders those blocks, Path Pair endpoints, zone boxes, directional lanes, route status colors, and the small status HUD shown while aiming at an endpoint.

When changing packet fields, update both codec directions together. Also consider whether the protocol version in `ReliableRoutesNetwork` must change. Keep request validation and collection limits in place because payload contents originate across the network boundary.

## Material Models

Routing blocks use Domum Ornamentum's normal materially textured model support. This preserves the selected source block's directional textures, render types, and tinting. Model changes should be checked both as placed blocks and as inventory items because those paths obtain material data differently.

## Resources and Metadata

The template at `src/main/templates/META-INF/neoforge.mods.toml` is expanded from `gradle.properties` by `generateModMetadata`. Change version numbers, dependency versions, author information, and the short mod description in `gradle.properties`, not in generated build output.

Recipes and loot tables use Minecraft's singular 1.21.1 resource directories (`recipe` and `loot_table`). The `routing_block_materials` tag controls which blocks may be selected as appearance materials. Text shown to players belongs in `assets/reliableroutes/lang/en_us.json`.

## Building and Testing

Use the checked-in Gradle wrapper from the repository root:

```powershell
./gradlew test
./gradlew build
./gradlew runClient
```

`test` runs the JUnit tests. `build` compiles the mod, processes resources and metadata, runs tests, and creates the jar under `build/libs`. `runClient` starts a development client with MineColonies and the configured runtime dependencies.

Add unit tests for pure rules and geometry where possible. `RoutingZoneMathTest` demonstrates boundary and intersection cases, while `WaypointPairAggregateStatusTest` covers status combination. Changes involving actual citizens, networking, rendering, persistence, or block interaction also require an in-game check.

## Safe Change Checklist

Before finishing a change, ask:

1. Is gameplay state still owned and validated by the server?
2. Does every saved-data mutation call `setDirty()`?
3. Can old world data still load safely?
4. Does navigation still fall back to the original MineColonies request on failure?
5. Were packet encoders and decoders changed together?
6. Is client-only code isolated from dedicated-server class loading?
7. Were all required resources and translations added for new content?
8. Do unit tests and `./gradlew build` pass?
