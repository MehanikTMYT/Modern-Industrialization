/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package aztech.modern_industrialization.client.items;

import aztech.modern_industrialization.client.util.RenderHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import aztech.modern_industrialization.items.SteamDrillHooks;
import aztech.modern_industrialization.items.SteamDrillItem;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;

public class SteamDrillHighlight {
    // Fleet port (26.1): the shape of the whole mining area is computed while the outline is extracted (the renderer
    // may not touch the level) and drawn instead of the single-block outline.
    public static void onBlockHighlight(ExtractBlockOutlineRenderStateEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SteamDrillItem drillItem)) {
            return;
        }

        var area = drillItem.getArea(player.level(), player, stack, true);
        if (area == null) {
            return;
        }

        var origin = area.center().immutable(); // captured by the renderer below; Area hands out a mutable pos
        MutableObject<VoxelShape> fullShape = new MutableObject<>(Shapes.empty());

        SteamDrillItem.forEachMineableBlock(player.level(), area, player, (blockPos, state) -> {
            // Use raw method to skip our BlockStateBase mixin...
            float destroyProgress = SteamDrillHooks.getDestroyProgressRaw(state, player, player.level(), blockPos);
            if (destroyProgress <= 1e-9) {
                return;
            }

            var blockShape = state.getShape(player.level(), blockPos, CollisionContext.of(player));
            blockShape = blockShape.move(blockPos.getX() - origin.getX(), blockPos.getY() - origin.getY(), blockPos.getZ() - origin.getZ());

            fullShape.setValue(Shapes.joinUnoptimized(fullShape.getValue(), blockShape, BooleanOp.OR));
        });

        VoxelShape shape = fullShape.getValue();
        if (shape == Shapes.empty()) {
            return;
        }
        event.addCustomRenderer((outline, buffers, poseStack, translucentPass, levelState) -> {
            Vec3 cam = levelState.cameraRenderState.pos;
            RenderHelper.renderVoxelShape(poseStack, buffers.getBuffer(RenderTypes.lines()), shape,
                    origin.getX() - cam.x, origin.getY() - cam.y, origin.getZ() - cam.z, 0, 0, 0, 0.4f);
            return true;
        });
    }
}
