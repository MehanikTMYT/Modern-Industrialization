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

package aztech.modern_industrialization.client.machines.multiblocks;

import net.minecraft.client.multiplayer.ClientLevel;
import org.jspecify.annotations.Nullable;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import com.mojang.blaze3d.vertex.QuadInstance;
import aztech.modern_industrialization.client.machines.MachineRenderState;
import aztech.modern_industrialization.client.util.RenderHelper;
import aztech.modern_industrialization.machines.blockentities.multiblocks.LargeTankMultiblockBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.MultiblockMachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public class MultiblockTankBER extends MultiblockMachineBER {
    public MultiblockTankBER(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    // Fleet port (26.1): the fluid of a formed large tank, captured in extractRenderState and submitted as tiled quads.
    public static class TankRenderState extends MultiblockMachineRenderState {
        boolean show;
        FluidResource fluid = FluidResource.EMPTY;
        int color;
        int minX, maxX, minZ, maxZ;
        float bottomY, topY;
    }

    // Keep the fluid faces off the inner faces of the glass/casing around the interior (coplanar faces lose the depth test).
    private static final float INSET = 0.002f;

    @Override
    public MachineRenderState createRenderState() {
        return new TankRenderState();
    }

    @Override
    public void extractRenderState(MultiblockMachineBlockEntity be, MachineRenderState machineState, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(be, machineState, partialTicks, cameraPosition, breakProgress);
        var state = (TankRenderState) machineState;
        var tank = (LargeTankMultiblockBlockEntity) be;
        FluidResource fluid = tank.getFluid();
        state.show = tank.isShapeValid() && !fluid.isEmpty() && tank.getFullnessFraction() > 0;
        if (!state.show) {
            return;
        }
        int[] corner = tank.getCornerPosition();
        state.maxX = corner[0];
        state.minX = corner[3];
        state.maxZ = corner[2];
        state.minZ = corner[5];
        int totalMaxY = corner[1], totalMinY = corner[4];
        float height = totalMaxY + 1 - totalMinY;
        float fullness = (float) tank.getFullnessFraction();
        // Render gas from top to bottom
        boolean gas = fluid.getFluid().getFluidType().isLighterThanAir();
        state.bottomY = totalMinY + (gas ? 1 - fullness : 0) * height;
        state.topY = totalMinY + (gas ? 1 : fullness) * height;
        state.fluid = fluid;
        state.color = be.getLevel() instanceof ClientLevel level ? RenderHelper.getFluidColor(fluid, level, be.getBlockPos()) : RenderHelper.getFluidColor(fluid);
    }

    @Override
    public void submit(MachineRenderState machineState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(machineState, poseStack, collector, camera);
        var state = (TankRenderState) machineState;
        if (!state.show) {
            return;
        }
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockItemSheet(), (pose, vc) -> {
            TextureAtlasSprite sprite = RenderHelper.getFluidSprite(state.fluid);
            if (sprite == null) {
                return;
            }
            var quad = new QuadInstance();
            quad.setColor(ARGB.opaque(state.color));
            quad.setLightCoords(RenderHelper.FULL_LIGHT);

            int yFrom = (int) Math.floor(state.bottomY), yTo = (int) Math.ceil(state.topY) - 1;
            for (int y = yFrom; y <= yTo; y++) {
                float bottom = Math.max(state.bottomY - y, 0), top = Math.min(state.topY - y, 1);
                for (int x = state.minX; x <= state.maxX; x++) {
                    RenderHelper.putFluidQuad(pose, vc, sprite, quad, Direction.NORTH, 0, bottom, 1, top, INSET, x, y, state.minZ);
                    RenderHelper.putFluidQuad(pose, vc, sprite, quad, Direction.SOUTH, 0, bottom, 1, top, INSET, x, y, state.maxZ);
                }
                for (int z = state.minZ; z <= state.maxZ; z++) {
                    RenderHelper.putFluidQuad(pose, vc, sprite, quad, Direction.WEST, 0, bottom, 1, top, INSET, state.minX, y, z);
                    RenderHelper.putFluidQuad(pose, vc, sprite, quad, Direction.EAST, 0, bottom, 1, top, INSET, state.maxX, y, z);
                }
            }
            for (int x = state.minX; x <= state.maxX; x++) {
                for (int z = state.minZ; z <= state.maxZ; z++) {
                    RenderHelper.putFluidQuad(pose, vc, sprite, quad, Direction.UP, 0, 0, 1, 1, Math.max(1 - (state.topY - yTo), INSET), x, yTo, z);
                    RenderHelper.putFluidQuad(pose, vc, sprite, quad, Direction.DOWN, 0, 0, 1, 1, Math.max(state.bottomY - yFrom, INSET), x, yFrom, z);
                }
            }
        });
    }
}
