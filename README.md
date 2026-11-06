# Create: Decals

Prototype: a Decal Panel, Create's copycat panel with one decal baked into its block model, to learn whether decals
stay crisp on copied materials, under Iris shaders and on moving Create contraptions and Sable vehicles. Results:
`docs/decals-prototype.md`; plan for v1.0: `docs/decals-mvp-plan.md` (both in the workspace).

Needs JDK 21 for Gradle (`JAVA_HOME="/c/Program Files/Microsoft/jdk-21.0.10.7-hotspot"`). Compiles on Create 6.0.0, runs
dev on 6.0.10 with Sable 2.0.6 (no code refers to Sable).

- `content`: `panel/DecalPanelBlock` (extends `CopycatPanelBlock`), `panel/DecalPanelBlockEntity` (extends
  `CopycatBlockEntity`; the decal is saved with the block and handed to the model as model data, and turned with the
  block when a contraption puts it back turned), `client/DecalPanelModel` (extends `CopycatPanelModel`: one extra quad
  in the cutout layer, in the plane of the front face, tinted through its tint index).
- Decals: `decal/Decal` (27 sprites on the block atlas), `decal/DecalState`
  (decal, colour, quarter turns, flip). Pick one with `/decals decal <pos> <decal|none> [colour] [rotation] [flip]` or
  `/decals fill <from> <to> <decal|none> [colour]`; a dye on a panel recolours its decal.
- `-Pdev_perf=true` adds Sodium 0.8.13, Iris 1.8.14-beta.1 and spark (`-Pdev_iris=false`: Sodium alone).
  `./gradlew installShaderpack` puts Complementary Reimagined into `run/shaderpacks/` (personal-use licence: never
  bundle it); shaders on/off is `enableShaders` in `run/config/iris.properties`.
- `./gradlew test`: architecture rules (no Sable, Aeronautics, Sodium or Iris classes; common code off the client).
