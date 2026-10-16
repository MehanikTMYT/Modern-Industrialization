# Upstream 1.21.x compat recipe providers (#1374 Create, #1380 AE2 cable dyeing) — parked

Backported from upstream 1.21.x but not compiled yet: the 26.1 port's `CompatRecipeProvider` (single provider,
new datagen API) already has its AE2 part disabled (`TODO 26.1 restore AE2 compat recipes`) because the referenced
AE2/Create items are not registered during datagen (`BuiltInRegistries.ITEM.getOrThrow` fails).

To enable: run `runData` with AE2 26.1 and the Create 26.1 port on the datagen runtime classpath, port these
classes to `MachineRecipeBuilder(items, fluids, type, eu, duration)` / `CompatRecipeProvider`, then regenerate.
