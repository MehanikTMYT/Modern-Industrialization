
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
