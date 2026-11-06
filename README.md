# Create: Decals

Decals for Create vehicles, trains and airships, baked into copycat models: crisp on any copied material, on
contraptions and Sable vehicles, under shaders, at no frame cost. Prototype results: `docs/decals-prototype.md`; plan
for v1.0 (milestones and status): `docs/decals-mvp-plan.md` (both in the workspace).

Needs JDK 21 for Gradle (`JAVA_HOME="/c/Program Files/Microsoft/jdk-21.0.10.7-hotspot"`). Compiles on Create 6.0.0,
runs dev on 6.0.10 with Sable 2.0.6 (no code refers to Sable).
`-Prun_profile=floor` runs NeoForge 21.1.125 and Create 6.0.0 in `run-floor/`.

## Code (`com.goldskinmc.createdecals`)

- `block/`: `DecalPanelBlock` (Create's `CopycatPanelBlock`, a decal on its front), `DecalBlock` (a full-cube
  `CopycatBlock`, a decal on any face; hides neighbours' faces against a solid material, as copycats otherwise let them
  through), `DecalCopycatBlockEntity` (one type for both: a `CopycatBlockEntity` with a decal per face, saved, synced,
  kept in schematics, and turned with the block when a contraption puts it back turned).
- `decal/`: `DecalState` (decal id, colour, quarter turns, flip, tile of a larger decal), `DecalFrame` (which way is up
  and right on each face), `Decals` (ids; the built-in list, read from our jar so the server can suggest them).
- `client/`: `DecalLibrary` (every `assets/<ns>/decals/<path>.json` in the loaded resource packs: size in tiles and
  category; sprite `<ns>:decal/<path>` on the block atlas, stitched from `textures/decal/` by our
  `assets/minecraft/atlases/blocks.json`), `DecalQuads` (one quad per decal, coplanar with the material's face, cutout
  layer, colour in the tint index, cached), `DecalPanelModel` and `DecalBlockModel` (Create's copycat models plus decal
  quads), `DecalsClient` (wraps the models, tints, loads the library).
- `registry/`: Registrate entries, lang, recipes, creative tab. `command/DecalsCommand`: `/decals decal <pos> <face>
  <decal|none> [colour] [rotation] [flip]` and `/decals fill <from> <to> <face> <decal|none> [colour]` (debug and
  admins; players get the Decal Stencil at M2).

## Build and check

- Blockstates, item models, lang, recipes, loot and tags: `./gradlew runData`, then commit `src/generated/`.
- `./gradlew build`: unit tests, `ArchitectureTest` (no Sable, Aeronautics, Sodium or Iris classes; common code off the
  client) and `ContentCompletenessTest` (every block has a name, tooltip, recipe, loot and item model; every decal a
  sprite and a definition).
- `./gradlew runGameTestServer` (and with `-Prun_profile=floor`): game tests in `gametest/` (saving, schematics,
  contraption turns, dye), ending "All N required tests passed".
- `-Pdev_perf=true` adds Sodium 0.8.13, Iris 1.8.14-beta.1 and spark (`-Pdev_iris=false`: Sodium alone).
  `./gradlew installShaderpack` puts Complementary Reimagined into `run/shaderpacks/` (personal-use licence: never
  bundle it); shaders on/off is `enableShaders` in `run/config/iris.properties`.
