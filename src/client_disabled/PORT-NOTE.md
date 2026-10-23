
## FTB Quests "built multiblock" task (upstream 1.21.x #1391, backported 2026-10-08)

`java/aztech/modern_industrialization/compat/ftbquests/task/BuiltMultiblockQuestTask.java` and the full upstream
`FTBQuestsFacadeImpl` (`FTBQuestsFacadeImpl.upstream.java.txt`) are parked here: the task registration
(`TaskTypes.register`, `Icon`) uses FTB Library/Quests API typed on `ResourceLocation`, and the only FTB Quests on the
classpath is the 1.21.1 build (2100.1.3). The multiblock advancement trigger (`advancement/multiblock`) and the item/fluid
quest progress caching from #1391/#1396/the fluid commit are active. Restore when FTB Quests ships for 26.1.

## Parked in the 2026-10-15 upstream sync (1.21.x up to a081a9a81)
- #1404 "Display block information when looking at a block or hatch overlay in the multiblock hologram":
  `MultiblockMemberTooltip`, `MultiblockMemberTooltipData` — built on upstream's MultiblockHighlight queue, which the
  port replaced by MultiblockMachineRenderState/BER; needs a render-state based hover check.
- #1391 FTB Quests "built_multiblock" task: `compat/ftbquests/task/*` — still no FTB Quests build for 26.1.
Applied in that sync: #1400 (Jade combined fluids), #1403 (full-inventory fast path, ResourceHandler), #1407 (biome tag
name), #1401/#1405 (GuideME), #1398, and the rest merged cleanly; #1385/#1386/#1394 were already in the port.

## 2026-10-23 upstream sync (1.21.x a081a9a81..2a5cce73f, #1409-#1421)
Applied: #1416 (Chemical Reactor grasslike recipes, ported into VanillaCompatRecipeProvider), #1417 (Create quarry
recipe JSON), #1418 (matches_multiple_recipes advancement), #1419 (attribute descriptions), #1420 (gametest property:
neoforge.enabledGameTestNamespaces), #1421 docs, #1411 translations. #1409 (whitelisted pipes self-insertion) and
#1403/#1400 were already in the port's ResourceHandler code. #1413 (dev runtime JEI vs Create) not applicable (EMI only).
Parked: #1414 (MultiblockMemberTooltip text wrapping, same parked feature as #1404); #1412 KubeJS ShapeTemplateHelper
(no KubeJS integration in the port; the SimpleMember additions are merged).
