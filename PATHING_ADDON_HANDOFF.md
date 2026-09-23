# MineColonies Path-Control Add-on: Implementation Handoff

## Purpose

This document preserves the conclusions and design decisions from an exploratory review of the MineColonies 1.21.1 pathfinding code. It is intended to be copied or referenced when starting implementation in the separate add-on mod project.

The add-on's goal is to give players more control over citizen routes **without replacing or modifying MineColonies pathfinding**. The implementation should use MineColonies' existing block-tag behavior and Domum Ornamentum (DO) materially textured blocks.

Primary example: when a river divides a colony, a player should be able to make a bridge strongly preferred and mark nearby water crossings as forbidden to citizens.

## Scope decision

The first implementation will use only these existing MineColonies block tags:

- `minecolonies:pathblocks`: strongly preferred walking surfaces.
- `minecolonies:dangerousblocks`: impassable terrain for ordinary citizens.

Do not initially implement a custom navigator, path job, mixin, arbitrary cost provider, directional path, waypoint system, or per-colony routing rules.

## Confirmed MineColonies behavior

### Preferred path blocks

MineColonies checks the block **beneath** a prospective citizen node. If it is in `minecolonies:pathblocks`, the normal movement cost is multiplied by `1/6`.

Relevant MineColonies sources:

- `src/main/java/com/minecolonies/api/util/constant/TagConstants.java`
  - Defines the tag ID as `minecolonies:pathblocks` (despite the Java field being named `PATHING_BLOCKS`).
- `src/main/java/com/minecolonies/api/items/ModTags.java`
  - Defines `ModTags.pathingBlocks`.
- `src/main/java/com/minecolonies/core/util/WorkerUtil.java`
  - `isPathBlock(Block)` tests `block.defaultBlockState().is(ModTags.pathingBlocks)`.
- `src/main/java/com/minecolonies/core/entity/pathfinding/pathjobs/AbstractPathJob.java`
  - Detects the block beneath the node as a road and applies `onPathCost`.
- `src/main/java/com/minecolonies/core/entity/pathfinding/PathingOptions.java`
  - Default `onPathCost = 1 / 6d`.
- `src/main/java/com/minecolonies/core/generation/defaults/DefaultBlockTagsProvider.java`
  - Shows existing path blocks, including planks, wooden slabs, carpets, stone bricks, gravel, dirt paths, polished materials, and various DO blocks.

The add-on should append its road block through:

`data/minecolonies/tags/block/pathblocks.json`

```json
{
  "replace": false,
  "values": [
    "<addon_mod_id>:minecolonies_road"
  ]
}
```

### Dangerous blocks

For ordinary citizens, a block in `minecolonies:dangerousblocks` is a hard pathfinding barrier, not merely a high-cost surface. MineColonies rejects both a dangerous block occupying the citizen's movement space and a dangerous supporting block beneath the citizen.

Relevant MineColonies sources:

- `src/main/java/com/minecolonies/core/entity/pathfinding/PathfindingUtils.java`
  - `isDangerous(BlockState)` checks `ModTags.dangerousBlocks` plus built-in hazards.
- `src/main/java/com/minecolonies/core/entity/pathfinding/SurfaceType.java`
  - Returns `NOT_PASSABLE` for dangerous blocks unless the entity's pathing options allow danger.
- `src/main/java/com/minecolonies/core/entity/pathfinding/pathjobs/AbstractPathJob.java`
  - Enforces danger checks when evaluating body/head spaces and supporting blocks.
- `src/main/java/com/minecolonies/core/entity/pathfinding/PathingOptions.java`
  - `canPassDanger` defaults to false.

The add-on should append its forbidden block through:

`data/minecolonies/tags/block/dangerousblocks.json`

```json
{
  "replace": false,
  "values": [
    "<addon_mod_id>:forbidden_ground"
  ]
}
```

MineColonies raiders and some special aquatic entities explicitly enable `canPassDanger`. The forbidden blocks are therefore citizen-routing tools, not defenses against raiders.

### Swimming already has a high cost

Citizens can swim, but stock pathing strongly discourages it:

- Entering water: `+24`.
- Continuing to swim: `+4` per node.
- Diving: another `+4`.

These defaults are in `PathingOptions`, and `AbstractPathJob.computeCost` applies them. A tagged bridge and approach road will already be strongly preferred. Dangerous marker blocks can make nearby water crossings completely unavailable.

## Domum Ornamentum architecture decision

Implement at least two separately registered, materially textured blocks:

1. `<addon_mod_id>:forbidden_ground`
2. `<addon_mod_id>:minecolonies_road`

Each should have its own Architect's Cutter recipe and allow the player to select an eligible visual material.

### Critical constraint: tags apply to block IDs

Minecraft block tags apply to the registered `Block`, not to item NBT, block-entity material data, or an Architect's Cutter recipe.

Therefore, do **not** use one generic routing block with an NBT property such as `mode=road` versus `mode=forbidden`. MineColonies would see both variants as the same block and could not assign different tags.

The two semantic roles require distinct registered block IDs. Their selected DO material is purely visual and does not change their routing tag.

For example, `forbidden_ground` textured with oak planks remains dangerous; it does not inherit the selected oak block's `pathblocks` membership.

### DO implementation requirements

An Architect's Cutter recipe targets an already registered block. The recipe is not itself enough to create a new DO block type.

Each new block will likely need:

- A registered block and block item.
- `IMateriallyTexturedBlock` support.
- One main materially textured component using an appropriate DO valid-skins tag.
- A materially textured block entity, following the DO version used by the add-on.
- Placement logic that transfers `textureData` from the item to the block entity.
- Drops and pick-block behavior that preserve `textureData`.
- A DO materially textured model/model loader configuration.
- Architect's Cutter recipe registration/data.
- Normal delegation of mining tool, break speed, sound, and resistance to the selected main material where appropriate.

Useful DO reference implementations in a matching DO checkout/dependency include:

- `IMateriallyTexturedBlock`
- The standard DO `SlabBlock`
- `StackedSlabBlock`
- `ArchitectsCutterRecipe`

API details may differ between the inspected DO checkout and the add-on's exact NeoForge/DO version. Confirm against the dependency actually used by the new project before copying code.

## Block designs

### Forbidden ground

Initial implementation:

- Full-cube collision and visual model.
- One player-selectable DO material.
- Included in `minecolonies:dangerousblocks`.
- No actual damage behavior is required.
- Players and unrelated mobs may walk over it normally; the danger designation is for MineColonies pathfinding.

Potential later variants:

- Thin forbidden overlay.
- Forbidden slab.
- Waterloggable forbidden-water marker.

For river control, a waterloggable marker would be useful, but it should be treated as a follow-up after the full-cube implementation is verified. Deep water must be tested to ensure citizens cannot route under a surface-only marker. A continuous barrier must cover every usable route, including diagonal approaches and the ends of the restricted section.

### MineColonies road

Initial safe option:

- Full-cube geometry.
- One player-selectable DO material.
- Included in `minecolonies:pathblocks`.

Desired visual option:

- Approximately 80% block height.
- Use Minecraft's normal sixteenth increments, preferably `13/16` (81.25%) or possibly `12/16` (75%), rather than an arbitrary floating-point 80%.
- A 13/16 full-footprint shape should be recognized as walkable by MineColonies, but it increases geometry and transition testing.

Recommended sequence: validate the full-height road first, then introduce the 13/16 shape if the visual distinction is worth it.

The nonstandard-height road must be tested beside:

- Full blocks.
- Slabs and stairs.
- Doors and gates.
- Corners and diagonal transitions.
- Bridge railings and overhead blocks.
- Snow and carpets.
- Two-block-high openings.

## Routing-vision item

Create a client-side inspection item, tentatively called a **Pathfinder's Lens** or **Pathfinder's Goggles**.

When active, it should inspect nearby client-side block states using the actual MineColonies tags:

```java
if (state.is(ModTags.dangerousBlocks))
{
    // Render red: impassable to ordinary citizens.
}
else if (state.is(ModTags.pathingBlocks))
{
    // Render green: preferred path surface.
}
```

Danger should take precedence if a datapack mistakenly places a block in both tags. Optionally render the conflict with an amber/striped diagnostic, but describe it functionally as forbidden.

### Activation

Start with activation while held in either hand. This is simple and has no accessory dependency.

Optional additions:

- Make a goggles item usable in the vanilla helmet slot.
- Add a right-click toggle that persists routing vision without occupying a hand.
- Add compatibility with an accessory-slot mod later.

Do not require an accessory dependency for the first release.

### Rendering recommendation

Use a dedicated client renderer registered through NeoForge's `RenderLevelStageEvent`. Avoid depending on MineColonies internal renderer classes as an API, although they are useful references.

Initial display:

- Green line-box outline for preferred blocks.
- Red line-box outline for forbidden blocks.
- Optional translucent top-face fill.
- Reserve floating icons/text for the block under the crosshair rather than every block.

Useful MineColonies rendering references:

- `core/event/ClientEventHandler.java`
- `core/client/render/worldevent/WorldEventContext.java`
- `core/client/render/worldevent/HighlightManager.java`
- `core/client/render/worldevent/ItemOverlayBoxesRenderer.java`
- `core/client/render/worldevent/ColonyBlueprintRenderer.java`

### Display modes

MineColonies' `pathblocks` tag already contains many common construction blocks. Showing all effective blocks may highlight a large portion of an existing colony.

Recommended modes:

1. **Routing blocks only**: show the add-on's dedicated road and forbidden blocks. This should probably be the default.
2. **All effective MineColonies tags**: show every block MineColonies will treat as preferred or dangerous. This is the authoritative diagnostic mode.
3. **Forbidden only**: useful for finding concealed barriers.

### Performance requirements

Do not scan a large three-dimensional volume every rendered frame.

Suggested initial limits:

- Horizontal radius: 24 blocks.
- Vertical radius: 8 blocks.
- Refresh cached positions every 5-10 client ticks.
- Only inspect loaded chunks.
- Refresh when the player moves a meaningful distance or when the mode changes.
- Cap the number of rendered positions.
- Fade or omit distant outlines.

Depth-tested outlines should be the default. A configurable lower-opacity through-wall survey mode can be added for finding buried or visually disguised routing blocks.

No server packet should be necessary for basic visualization because synchronized block tags and nearby block states are available on the client.

## Suggested first implementation milestone

1. Confirm the add-on's Minecraft, NeoForge, MineColonies, and DO dependency versions.
2. Register `forbidden_ground` and `minecolonies_road` as distinct DO-compatible blocks.
3. Give both blocks full-cube geometry initially.
4. Add Architect's Cutter recipes and verify material selection, placement, drops, and pick-block preservation.
5. Append the block IDs to MineColonies' `dangerousblocks` and `pathblocks` tags.
6. Add the held routing-vision item with cached red/green outlines.
7. Build a river-and-bridge test area and verify citizen route selection.
8. Only after the baseline works, test the 13/16 road and a waterloggable forbidden marker.

## Acceptance tests

### Tag behavior

- A citizen refuses to stand on or cross `forbidden_ground`.
- A citizen selects a reasonable detour around forbidden ground.
- A citizen strongly prefers a continuous `minecolonies_road` route over nearby ordinary terrain.
- A visually identical road and forbidden block retain different routing behavior.
- Changing the selected DO material does not change routing behavior.
- Raiders are documented as potentially ignoring forbidden ground.

### Bridge scenario

- A bridge and both approaches use `minecolonies_road`.
- Forbidden blocks prevent swimming for the intended distance on both sides.
- Citizens cannot cross diagonally between markers.
- Citizens cannot walk around the restricted area's ends unexpectedly.
- Deep-water citizens cannot route beneath the chosen barrier arrangement.
- Placing/removing routing blocks causes subsequent paths to reflect the new layout.

### DO behavior

- All intended eligible materials appear in the Architect's Cutter.
- Placed blocks render the selected material correctly.
- Drops retain their selected material.
- Pick-block retains the selected material.
- Mining sound/tool/speed behavior is sensible.

### Visualization

- Held item activates in either hand.
- Road blocks render green and forbidden blocks red.
- Danger wins when both tags are present.
- “Routing blocks only” and “all effective tags” produce the expected distinction.
- The cache updates after player movement and block placement/removal.
- Performance remains acceptable in a dense colony.

## Explicit non-goals for the initial version

- Multiple levels of path preference.
- Arbitrary per-block movement costs.
- One-way or directional paths.
- Mandatory route waypoints.
- Per-citizen, job-specific, or per-colony routing rules.
- Controller/beacon areas that affect pathing without placed marker blocks.
- Replacing MineColonies' navigator or path jobs.
- Treating forbidden blocks as defenses against raiders.

## Context from inspected repositories

The MineColonies checkout used for the analysis was:

- Minecraft `1.21.1`
- Java `21`
- NeoForge/Forge version property `21.1.80`
- DO version property `1.0.223-snapshot`

MineColonies workspace during analysis:

`C:\Programming\minecolonies`

A DO source checkout was also available at:

`C:\Programming\Domum-Ornamentum`

The new session should treat the add-on project's declared dependencies as authoritative and reconcile API differences before implementation.

