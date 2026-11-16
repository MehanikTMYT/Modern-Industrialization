package com.mehaniktmyt.mi_structure_viewer.emi;

import java.util.ArrayList;
import java.util.List;

import com.mehaniktmyt.mi_structure_viewer.MultiblockCell;
import com.mehaniktmyt.mi_structure_viewer.MultiblockLayer;
import com.mehaniktmyt.mi_structure_viewer.MultiblockStructure;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.api.widget.TextWidget.Alignment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One multiblock structure displayed as an interactive layer viewer:
 * header with machine name, dimensions and casing, prev/next layer buttons,
 * the current layer as a slot grid, and the aggregated block list below.
 *
 * <p>Layer state lives in the recipe instance and is mutated by the buttons —
 * the same pattern EMI itself uses for paged ingredient recipes
 * ({@code EmiIngredientRecipe.PageManager}).
 */
public class MultiblockEmiRecipe extends BasicEmiRecipe {

    private static final int CELL = 18;
    private static final int CELL_INNER = 16;

    private final MultiblockStructure structure;
    private int layerIndex;

    public MultiblockEmiRecipe(MultiblockCategoryEmi category, MultiblockStructure structure) {
        super(category, id(structure), displayWidth(structure), displayHeight(structure));
        this.structure = structure;
    }

    private static Identifier id(MultiblockStructure structure) {
        String path = structure.machine().getPath();
        if (structure.alternative() != null && !structure.alternative().isBlank()) {
            path += "#" + structure.alternative();
        }
        return Identifier.fromNamespaceAndPath("mi_structure_viewer", "/multiblock/" + path);
    }

    private static int displayWidth(MultiblockStructure s) {
        int gridWidth = maxCols(s) * CELL + 2;
        return Math.max(134, gridWidth);
    }

    private static int displayHeight(MultiblockStructure s) {
        int header = 22;           // name + dimensions/casing/variant
        int layerBar = 14;         // buttons + "Layer i/n"
        int grid = maxRows(s) * CELL;
        int blocks = blockRows(s) * CELL + 6;
        int hatches = hatchRow(s);
        return header + layerBar + grid + blocks + hatches;
    }

    private static int maxCols(MultiblockStructure s) {
        return s.layers().stream().mapToInt(MultiblockLayer::cols).max().orElse(3);
    }

    private static int maxRows(MultiblockStructure s) {
        return s.layers().stream().mapToInt(MultiblockLayer::rows).max().orElse(3);
    }

    private static int blockRows(MultiblockStructure s) {
        int perRow = Math.max(1, (displayWidth(s) - 4) / CELL);
        return Math.max(1, (s.blocks().size() + perRow - 1) / perRow);
    }

    private static int hatchRow(MultiblockStructure s) {
        return s.hatchTypes().isEmpty() ? 0 : 10;
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public boolean hideCraftable() {
        return true;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int width = widgets.getWidth();
        int height = widgets.getHeight();
        List<MultiblockLayer> layers = structure.layers();
        if (layers.isEmpty()) {
            widgets.addText(Component.translatable("mi_structure_viewer.empty_shape"), 0, 0, 0xFFFFFF, true);
            return;
        }

        if (layerIndex < 0 || layerIndex >= layers.size()) {
            layerIndex = 0;
        }

        // Header: machine name.
        widgets.addText(structure.name(), 0, 0, 0xFFFFFF, true);
        // Second line: dimensions + casing + variant, with hatch info as tooltip.
        MutableComponent line2 = Component.empty();
        line2 = line2.append(Component.translatable("mi_structure_viewer.dimensions",
                structure.sizeX(), structure.sizeY(), structure.sizeZ()));
        if (structure.casingName() != null) {
            line2 = line2.append(Component.literal(" | "))
                    .append(Component.translatable("mi_structure_viewer.casing", structure.casingName()));
        }
        if (structure.variantName() != null) {
            line2 = line2.append(Component.literal(" | "))
                    .append(Component.translatable("mi_structure_viewer.variant", structure.variantName()));
        }
        widgets.addText(line2, 0, 11, 0xA0A0A0, true);
        if (!structure.hatchTypes().isEmpty()) {
            widgets.addTooltipText(hatchTooltip(), 0, 11, width, 10);
        }

        // Layer navigation bar.
        int barY = 22;
        MultiblockLayer layer = layers.get(layerIndex);
        widgets.addButton(0, barY, 12, 12, 0, 0, () -> true, (mx, my, b) -> {
            if (layerIndex > 0) {
                layerIndex--;
            }
        });
        widgets.addButton(width - 12, barY, 12, 12, 12, 0, () -> true, (mx, my, b) -> {
            if (layerIndex < layers.size() - 1) {
                layerIndex++;
            }
        });
        Component layerText = Component.translatable("mi_structure_viewer.layer",
                layerIndex + 1, layers.size(), layer.yOffset());
        widgets.addText(layerText.getVisualOrderText(), width / 2, barY + 2, 0xFFFFFF, true)
                .horizontalAlign(Alignment.CENTER);
        widgets.addTooltipText(List.of(
                Component.translatable("mi_structure_viewer.prev_layer"),
                Component.translatable("mi_structure_viewer.next_layer")), 0, barY, width, 12);

        // Layer grid.
        int gridWidth = layer.cols() * CELL;
        int gx = (width - gridWidth) / 2;
        int gy = barY + 14;
        for (int row = 0; row < layer.rows(); row++) {
            for (int col = 0; col < layer.cols(); col++) {
                int x = gx + col * CELL;
                int y = gy + row * CELL;
                MultiblockCell cell = layer.cell(row, col);
                if (cell == null) {
                    continue;
                }
                addCell(widgets, cell, x, y);
            }
        }

        // Aggregated block requirements.
        int by = gy + layer.rows() * CELL + 4;
        if (!structure.blocks().isEmpty()) {
            int perRow = Math.max(1, (width - 4) / CELL);
            for (int i = 0; i < structure.blocks().size(); i++) {
                var req = structure.blocks().get(i);
                int bx = 2 + (i % perRow) * CELL;
                int byy = by + (i / perRow) * CELL;
                Item item = req.previewState().getBlock().asItem();
                if (item != Items.AIR) {
                    widgets.addSlot(EmiStack.of(item, req.count()), bx, byy)
                            .appendTooltip(Component.translatable("mi_structure_viewer.required_blocks").append(
                                    Component.literal(" " + req.count() + "x ").append(
                                            req.previewState().getBlock().getName())));
                } else {
                    widgets.addDrawable(bx, byy, CELL, CELL,
                            (draw, mx, my, delta) -> draw.fill(1, 1, CELL_INNER, CELL_INNER, 0xFF555555));
                    widgets.addTooltipText(List.of(Component.translatable("mi_structure_viewer.unknown_block",
                            BuiltInRegistries.BLOCK.getKey(req.previewState().getBlock()))), bx, byy, CELL, CELL);
                }
            }
        }
    }

    private void addCell(WidgetHolder widgets, MultiblockCell cell, int x, int y) {
        if (cell.kind() == MultiblockCell.Kind.HATCH) {
            widgets.addDrawable(x, y, CELL, CELL,
                    (draw, mx, my, delta) -> {
                        draw.fill(1, 1, CELL_INNER, CELL_INNER, 0xFF2E5F8A);
                        draw.fill(3, 3, CELL_INNER - 2, CELL_INNER - 2, 0xFF4C88C2);
                    });
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("mi_structure_viewer.hatch_cell"));
            tip.add(Component.translatable("mi_structure_viewer.hatch_tooltip",
                    String.join(", ", cell.hatchDescriptions())));
            widgets.addTooltipText(tip, x, y, CELL, CELL);
            return;
        }
        BlockState state = cell.previewState();
        if (state == null) {
            return;
        }
        Item item = state.getBlock().asItem();
        if (item != Items.AIR) {
            widgets.addSlot(EmiStack.of(item), x, y);
        } else {
            widgets.addDrawable(x, y, CELL, CELL,
                    (draw, mx, my, delta) -> draw.fill(1, 1, CELL_INNER, CELL_INNER, 0xFF555555));
            widgets.addTooltipText(List.of(Component.translatable("mi_structure_viewer.unknown_block",
                    BuiltInRegistries.BLOCK.getKey(state.getBlock()))), x, y, CELL, CELL);
        }
    }

    private List<Component> hatchTooltip() {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.translatable("mi_structure_viewer.hatches_allowed"));
        for (Component c : structure.hatchTypes()) {
            tip.add(Component.literal("  ").append(c));
        }
        return tip;
    }
}
