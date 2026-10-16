# Parked upstream 1.21.x client features (fleet backport, 2026-10-04)

Backported from upstream 1.21.x but not yet ported to the 26.1 client APIs; they depend on parts of the 26.1 port
that are still `TODO 26.1` stubs:

- `machines/multiblocks/MultiblockHighlight.java`, `MIShaders.java`, `MIRenderTypes-translucent-highlight.diff`
  — upstream #1276/#1306/#1339: multiblock hatch/member highlight rendered through blocks with a custom shader and
  a configurable range. The port's multiblock highlight renderer (`MultiblockMachineBER`, `MultiblockErrorHighlight`)
  is itself still a TODO; 26.1 replaced `ShaderInstance`/`RenderStateShard` with render pipelines.
- `compat/guideme/**` — upstream #1368: machine / forge hammer recipe types embedded in GuideME pages. Needs the
  GuideME 26.1 layout/tooltip API and the transfer-API resources instead of fabric `ItemVariant`/`FluidVariant`.
