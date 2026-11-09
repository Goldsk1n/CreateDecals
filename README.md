# Create: Decals

Decals for Create vehicles, trains and airships, baked into copycat models: crisp on any copied material, on
contraptions and Sable vehicles, under shaders, at no frame cost. Prototype results: `docs/decals-prototype.md`; plan
for v1.0 (milestones and status): `docs/decals-mvp-plan.md`; mod-page draft: `docs/decals-mod-page.md`, its images in
`docs/decals-page-images/` (all in the workspace). Changes per version: `CHANGELOG.md`. Licence: MIT.

Needs JDK 21 for Gradle (`JAVA_HOME="/c/Program Files/Microsoft/jdk-21.0.10.7-hotspot"`). Compiles on Create 6.0.0,
runs dev on 6.0.10 with Sable 2.0.6 (no code refers to Sable).
`-Prun_profile=floor` runs NeoForge 21.1.125 and Create 6.0.0 in `run-floor/`.

## Code (`com.goldskinmc.createdecals`)

- `block/`: `DecalPanelBlock` (Create's `CopycatPanelBlock`, a decal on its front), `DecalBlock` (a full-cube
  `CopycatBlock`, a decal on any face; hides neighbours' faces against a solid material, as copycats otherwise let them
  through), `DecalCopycatBlockEntity` (one type for both: a `CopycatBlockEntity` with two decals per face, back and front, saved, synced,
  kept in schematics, and turned with the block when a contraption puts it back turned).
- `decal/`: `DecalState` (decal id, colour, quarter turns, flip, glow; for decals larger than one block their size in
  tiles and the tile this face shows), `DecalText` (a typed word's characters as letter, digit and sign decals), `DecalPlacement` (where each tile of a flipped and turned large decal lands, from its
  top-left face; a typed word is laid as an N x 1 grid the same way), `DecalFrame` (which way is up and right on each face), `Decals` (ids; the built-in list, read from our
  jar so the server can suggest them). `block/DecalTiles` places a whole decal and changes every tile of one (eraser,
  dye).
- `client/`: `DecalLibrary` (every `assets/<ns>/decals/<path>.json` in the loaded resource packs: size in tiles and
  category; sprite `<ns>:decal/<path>` on the block atlas, stitched from `textures/decal/` by our
  `assets/minecraft/atlases/blocks.json`), `DecalQuads` (one quad per decal, coplanar with the material's face, cutout
  layer, colour in the tint index, cached; the front decal is added after the back one, so it wins the same depth tie the
  back one wins over the material; a glowing decal is unshaded, without ambient occlusion and baked at full light,
  NeoForge's `QuadTransformers.settingMaxEmissivity`), `DecalPanelModel`, `DecalBlockModel` and `DecalStepModel` (Create's copycat models plus decal
  quads), `DecalsClient` (wraps the models, tints, loads the library).
- `block/DecalEdits`: whether a player may change a block's decals, asked for every block a click reaches (large
  decals, words, eraser, dye, glow, Create copycats converting): `mayInteract` (spawn protection, world border),
  `mayUseItemAt` (adventure mode) and NeoForge's `BlockEvent.EntityPlaceEvent`, which claim mods cancel; refused tiles
  are left out like missing ones (game test `claimedBlocksAreLeftAlone`).
- `block/CreateCopycats`: a decal placed on Create's own Copycat Panel (that block exactly, on its front) or Copycat
  Step (any face) turns it into a Decal Panel or Decal Step in place: block-state properties copied, the block entity's
  saved data loaded into ours (Create's `setMaterial` may take a neighbour's material state instead), the old block's
  consumed item emptied first so Create drops nothing. The stencil and its outline treat such a block as ours without a
  decal. Create has no full copycat block, so nothing turns into a Decal Block.
- `block/DecalStepBlock` and `client/DecalStepModel`: Create's copycat step with decals on any face. `client/DecalFaces`
  says where a decal lies on each face of every decal block (depth and the part of the decal shown): a step's face is a
  window onto the decal as it would lie on the block's face in that plane, so a bottom step's front shows its lower half
  and lines up with Decal Blocks beside it (`DecalQuads` bakes windows with texture coordinates for any turn and flip);
  faces on the block's boundary come with their side, so they are culled with it.
- `stencil/`: `DecalStencilItem` (used in the air it opens its screen; on a face it puts its decal there or erases it;
  sneaking it picks the face's decal up), `StencilData` (its data component: decal, colour, turns, flip, eraser),
  `StencilPayload` (the screen's choice, client to server, only onto a stencil in that hand). The screen is
  `client/StencilScreen` (built as Create's schematic screens are: a grey header, a blue two-tone body, sunken slots,
  Create-style input bars and a grey tray with the confirm tab, all our drawing in
  `textures/gui/stencil.png`; categories on Create's own buttons (`AllGuiTextures`) with our 12 px icons, Create's
  `IconButton`s and `AllIcons` to turn, flip, erase and finish), opened by
  `client/StencilClientEvents`; `client/StencilItemModel` puts the chosen decal on the
  stencil's icon; `client/StencilOutline` shows what a held stencil would do: a see-through ghost of each tile it
  would place (the decal's own quad from `DecalQuads`, so steps' windows match, lifted a hair and drawn in
  `Sheets.translucentCullBlockSheet()`), green outlines where tiles land and red where they cannot; with the eraser, the
  decal it takes drawn over in red (only that layer's tiles); a red outline on any other block aimed at. A click on such
  a block falls through to using the stencil in the air, so `StencilClientEvents` opens the screen with a note naming the
  block. `client/StencilKeys`: Ctrl + scroll turns the held stencil (on the client at once, then `StencilPayload`), and
  the undo key (Z, active only while a stencil is held, `IKeyConflictContext`) sends `stencil/UndoPayload`.
  `block/DecalUndo` keeps each player's last 10 stencil clicks in server memory (every tile `DecalTiles` changed, before
  and after, recorded while `DecalStencilItem` runs the click) and puts back only tiles still holding what the click
  left, where the player may still edit (game tests `undoPutsBackTheLastClick`, `undoKeepsTenClicks`). A fake player's
  clicks (a Deployer's) are not kept: Create's Deployer player answers to its owner's UUID, so they would land in the
  owner's undo. `block/DecalEffects` plays each recorded click's sound and dust on the server: vanilla's dye-on-sign sound
  and dust of the decal's colour per tile for a decal put on, Create's Clipboard eraser for one taken off (and for undo),
  its pen for a pick-up.
- Create's Deployer: it calls the stencil's `useOn` and the blocks' `useItemOn` with its fake player, so stamping, dye,
  glow and the eraser work from a Deployer with no code of their own. A moving Deployer pointing down (or up) clicks
  with a level ray, so Create hands over the block's side; `DecalEdits.clickedFace` takes the top (or bottom) from the
  fake player's pitch. Game tests `deployerStampsDecals`, `movingDeployerStampsDecals` (on a Mechanical Piston),
  `deployerDyesDecals`, `fakePlayersLeaveNoUndo`; Ponder scene `decal_stencil_deployer` (under the stencil and Create's
  Deployer). Create's
  Clipboard copies decals too (`ClipboardCloneable` on the block entity).
- `contraption/DecalMovement` (a `MovementBehaviour` for both blocks; does nothing while moving) and
  `client/ContraptionGlow`: on a Create contraption or train, glowing decals are drawn a second time at full light from
  `renderInContraption`, in vanilla's sign-text layer (`RenderType.textPolygonOffset`, so it wins over the mesh's copy
  of the same face without flicker), a front decal over a glowing back one too. Needed for Sodium: Create meshes
  contraptions into a `BufferBuilder`, whose `putBulkData` Sodium replaces with a fast path that keeps the light the
  block renderer worked out and drops the quad's baked full light (its own terrain pipeline keeps it, so placed blocks and
  Sable vehicles glow without this).
- `ponder/`: Ponder scenes (`DecalsScenes`: the Decal Panel, the Decal Block, the Decal Stencil with large decals, and
  stamping with Deployers, also listed under Create's Deployer; tag "Decals"), laid out in `Layouts`, whose schematics `PonderSchematics` writes at datagen (copycat material and decals
  in the block entity data). Ponder's world hands models no block-entity data, so the copycat models fall back to the
  block entity's own (`DecalPanelModel.withBlockEntityData`), and scenes change decals with
  `modifyBlockEntityNBT(..., true)` so the section is redrawn.
- `registry/`: Registrate entries, data components, lang, recipes, creative tab. `command/DecalsCommand`:
  `/decals decal <pos> <face> <decal|none> [colour] [rotation] [flip]` and `/decals fill <from> <to> <face> <decal|none>
  [colour]` (debug and admins).

## Build and check

- Blockstates, item models, lang (Ponder text included), recipes, loot, tags and Ponder schematics: `./gradlew runData`
  (default profile; on `floor` Create 6.0.0's own datagen fails without Curios),
  then commit `src/generated/`.
- `./gradlew build`: unit tests, `ArchitectureTest` (no Sable, Aeronautics, Sodium or Iris classes; common code off the
  client) and `ContentCompletenessTest` (every block has a name, tooltip, recipe, loot, item model and Ponder scene with
  its schematic; every decal a sprite and a definition).
- `./gradlew runGameTestServer` (and with `-Prun_profile=floor`): game tests in `gametest/` (saving, schematics,
  contraption turns, dye, stencil, Clipboard, large decals, the Schematicannon placing both blocks), ending "All N required tests passed".
- `-Pdev_perf=true` adds Sodium 0.8.13, Iris 1.8.14-beta.1 and spark (`-Pdev_iris=false`: Sodium alone).
  `./gradlew installShaderpack` puts Complementary Reimagined into `run/shaderpacks/` (personal-use licence: never
  bundle it); shaders on/off is `enableShaders` in `run/config/iris.properties`.
- Dedicated server: `runServer` uses `run-server/` (superflat, offline mode, EULA accepted by the owner Nov 7, 2026).
- `-Pdev_sable=false` leaves Sable and Veil out of `latest` runs (game folder `run-nosable/`), to tell their effects from ours.
- `-Pdev_tools=true` adds JEI and The One Probe (the same versions as Lightworks') for playing by hand; `latest` only,
  never shipped.
- `-Pdev_copycats=true` adds Copycats+ (pinned by Modrinth version id, as in Lightworks) to dev runs and game tests; the
  build always compiles against it for `compat/copycats`.
- **Copycats+** (optional, All Rights Reserved; only its `foundation` package, which its wiki calls public and stable;
  `ArchitectureTest` keeps every reference in `compat/copycats`). Its copycats are recognised by `ICopycatBlock`
  (`compat/copycats/CopycatsPlusBlocks`; not Create's copycats or ours, not kinetic ones) and keep decals in a NeoForge
  data attachment on their block entity (`block/AttachedDecals`, `AttachedDecalHolder`), saved with it, so contraptions
  (`saveWithFullMetadata`), Sable vehicles and chunks carry them. NeoForge 21.1 does not send block entity attachments,
  so `block/DecalSync` does: on change, with each chunk a player starts to see, and a tick after one is read on the server
  (a contraption putting blocks back). `block/DecalHolder` is what the stencil, dye, undo and outline work through, our
  block entity or an attachment alike. `client/AttachedDecalModel` wraps each of its block states' models (lowest
  priority, after Create's model swapper) and projects each decal onto every quad that faces its side
  (`DecalQuads.faces`/`projected`: same corners, so it wins the depth tie; texture as if square on to that face), so
  slopes need nothing of their own; Create's contraption renderer asks the model for its data too
  (`getModelData(renderWorld, ...)`). `client/CopycatsPlusModels` also registers a colour handler that passes non-decal
  tints to `ICopycatBlock.wrappedColor()`. Game tests `copycatsPlusTakeDecals` and `createCopycatsStillConvert` (they
  pass at once without it). Turning with a contraption and schematics go through Create's own public registries,
  which Create asks before a block entity's `transform` and `writeSafe` (`block/DecalTransforms.registerCopycatsPlus`,
  at common setup): for each of Copycats+'s block entity types, Copycats+'s own method runs, then the decals turn
  (`DecalTransforms.turned`, shared with our block entity) or are added to the schematic's data under NeoForge's
  attachments key (game test `copycatsPlusDecalsTurnAndGoIntoSchematics`). No mixin. The same setup gives every Copycats+
  copycat without a movement behaviour of its own our `DecalMovement` (39 of them), so `ContraptionGlow` redraws their
  glowing decals full-bright on moving contraptions under Sodium (`AttachedDecalModel.glowQuads`, cached per block entity
  while its decals are unchanged). Create's Clipboard asks every behaviour of a `SmartBlockEntity`, which Copycats+'s are:
  `block/DecalClipboard` attaches a `ClipboardCloneable` behaviour with Create's public `attachBehaviourLate` just before
  a Clipboard reaches one (click events at highest priority on both sides, and `client/CopycatsPlusClipboard` while one is
  aimed at, for Create's outline), under the same key our block entity uses, so decals copy between our blocks and
  Copycats+'s both ways (game test `clipboardAndGlowOnCopycatsPlus`).
