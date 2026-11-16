package com.mehaniktmyt.mi_structure_viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.multiblocks.SimpleMember;
import aztech.modern_industrialization.machines.multiblocks.ShapeTemplate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Extracts registered multiblock shapes from MI using its public API
 * ({@link ReiMachineRecipes#multiblockShapes}) — no reflection.
 *
 * Everything here is rebuilt from scratch on every snapshot, which makes the
 * class naturally safe for datapack reloads: MI re-runs machine init and this
 * mod re-reads the registry on the next EMI reindex.
 */
public final class StructureIndex {

    private StructureIndex() {}

    /**
     * Reads every registered multiblock shape and converts it to plain MC types.
     * Malformed or empty shapes are skipped with a console warning instead of
     * breaking the whole index.
     */
    public static List<MultiblockStructure> snapshot() {
        List<MultiblockStructure> result = new ArrayList<>();
        for (ReiMachineRecipes.MultiblockShape shape : ReiMachineRecipes.multiblockShapes) {
            if (shape == null || shape.machine() == null || shape.shapeTemplate() == null) {
                continue;
            }
            try {
                MultiblockStructure structure = convert(shape.machine(), shape.shapeTemplate(), shape.alternative());
                // Skip shapes that produce no drawable positions at all.
                if (!structure.isEmpty()) {
                    result.add(structure);
                }
            } catch (Exception e) {
                warn("Failed to read multiblock shape for " + shape.machine() + ": " + e);
            }
        }
        return result;
    }

    private static MultiblockStructure convert(Identifier machine, ShapeTemplate template, String alternative) {
        Map<BlockPos, SimpleMember> members = template.simpleMembers;
        Map<BlockPos, HatchFlags> hatches = template.hatchFlags;
        if (members == null) {
            members = Map.of();
        }
        if (hatches == null) {
            hatches = Map.of();
        }

        Set<BlockPos> positions = new HashSet<>();
        positions.addAll(members.keySet());
        positions.addAll(hatches.keySet());
        if (positions.isEmpty()) {
            // Empty shape: nothing to display.
            return new MultiblockStructure(machine, alternative, machineName(machine), null, null,
                    0, 0, 0, List.of(), List.of(), List.of());
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        // Group block-only positions by preview state to build the requirement list.
        Map<BlockState, Long> blockCounts = new HashMap<>();
        Map<BlockPos, HatchFlags> hatchPositions = new TreeMap<>();
        for (BlockPos pos : positions) {
            HatchFlags flags = hatches.get(pos);
            if (flags != null && !flags.values().isEmpty()) {
                hatchPositions.put(pos, flags);
                continue;
            }
            SimpleMember member = members.get(pos);
            if (member == null) {
                continue;
            }
            try {
                BlockState state = member.getPreviewState();
                if (state != null) {
                    blockCounts.merge(state, 1L, Long::sum);
                }
            } catch (Exception e) {
                warn("Cannot get preview state of member at " + pos + " for " + machine + ": " + e);
            }
        }

        List<MultiblockBlockRequirement> blocks = new ArrayList<>();
        blockCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(e -> blocks.add(new MultiblockBlockRequirement(e.getKey(), e.getValue())));

        // Hatch type descriptions (union across the whole template).
        List<Component> hatchTypes = new ArrayList<>();
        Set<Identifier> seenHatches = new HashSet<>();
        for (HatchFlags flags : hatchPositions.values()) {
            for (HatchType type : flags.values()) {
                if (seenHatches.add(type.id())) {
                    try {
                        hatchTypes.add(type.description());
                    } catch (Exception e) {
                        hatchTypes.add(Component.literal(type.id().toString()).withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }
        Collections.sort(hatchTypes, Comparator.comparing(c -> c.getString().toLowerCase()));

        // Layers, bottom to top.
        List<MultiblockLayer> layers = new ArrayList<>();
        int rows = maxZ - minZ + 1;
        int cols = maxX - minX + 1;
        for (int y = minY; y <= maxY; y++) {
            Map<Integer, MultiblockCell> cells = new HashMap<>();
            boolean any = false;
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!positions.contains(pos)) {
                        continue;
                    }
                    MultiblockCell cell;
                    HatchFlags flags = hatches.get(pos);
                    if (flags != null && !flags.values().isEmpty()) {
                        List<String> descriptions = new ArrayList<>();
                        for (HatchType type : flags.values()) {
                            try {
                                descriptions.add(type.description().getString());
                            } catch (Exception e) {
                                descriptions.add(type.id().toString());
                            }
                        }
                        Collections.sort(descriptions);
                        cell = new MultiblockCell(MultiblockCell.Kind.HATCH, null, descriptions);
                    } else {
                        SimpleMember member = members.get(pos);
                        if (member == null) {
                            continue;
                        }
                        try {
                            cell = new MultiblockCell(MultiblockCell.Kind.BLOCK, member.getPreviewState(), List.of());
                        } catch (Exception e) {
                            warn("Cannot get preview state of member at " + pos + " for " + machine + ": " + e);
                            continue;
                        }
                    }
                    cells.put((z - minZ) * cols + (x - minX), cell);
                    any = true;
                }
            }
            if (any) {
                layers.add(new MultiblockLayer(y - minY, rows, cols, cells));
            }
        }

        Component casingName = null;
        try {
            if (template.hatchCasing != null) {
                casingName = template.hatchCasing.getName();
            }
        } catch (Exception e) {
            // Casing name is purely cosmetic.
        }

        Component variantName = null;
        if (alternative != null && !alternative.isBlank()) {
            try {
                Identifier altId = Identifier.fromNamespaceAndPath(machine.getNamespace(), alternative);
                var block = BuiltInRegistries.BLOCK.getValue(altId);
                if (block != null) {
                    variantName = block.getName();
                }
            } catch (Exception e) {
                // fall back to the raw id below
            }
            if (variantName == null) {
                variantName = Component.literal(alternative).withStyle(ChatFormatting.GRAY);
            }
        }

        return new MultiblockStructure(machine, alternative, machineName(machine), variantName, casingName,
                maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1,
                List.copyOf(layers), List.copyOf(blocks), List.copyOf(hatchTypes));
    }

    /** Machine display name: prefers the machine block translation, falls back to a prettified id. */
    public static Component machineName(Identifier machine) {
        String key = "block." + machine.getNamespace() + "." + machine.getPath();
        return Component.translatableWithFallback(key, prettify(machine.getPath()));
    }

    private static String prettify(String path) {
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.isEmpty() ? path : sb.toString();
    }

    private static void warn(String message) {
        System.out.println("[mi_structure_viewer] " + message);
    }
}
