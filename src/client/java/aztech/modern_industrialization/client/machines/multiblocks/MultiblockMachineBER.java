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

import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.multiplayer.ClientLevel;
import aztech.modern_industrialization.client.util.RenderHelper;
import aztech.modern_industrialization.client.machines.MachineRenderState;
import aztech.modern_industrialization.MITags;
import aztech.modern_industrialization.client.machines.MachineBlockEntityRenderer;
import aztech.modern_industrialization.config.MIClientConfig;
import aztech.modern_industrialization.machines.MachineBlock;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.HatchFlags;
import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.multiblocks.MultiblockMachineBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.ShapeMatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class MultiblockMachineBER extends MachineBlockEntityRenderer<MultiblockMachineBlockEntity> {
    public MultiblockMachineBER(BlockEntityRendererProvider.Context ctx) {
        super(ctx);
    }

    // Fleet port (26.1): BERs are split into extractRenderState + submit; the old render() override (and the separate
    // MultiblockErrorHighlight pass drawn after the level) is rebuilt on top of the machine render state.
    @Override
    public MachineRenderState createRenderState() {
        return new MultiblockMachineRenderState();
    }

    @Override
    public void extractRenderState(MultiblockMachineBlockEntity be, MachineRenderState machineState, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(be, machineState, partialTicks, cameraPosition, breakProgress);
        var state = (MultiblockMachineRenderState) machineState;
        state.hatchOverlays.clear();
        state.highlights.clear();

        int maxHighlightDistance = MIClientConfig.INSTANCE.maxMultiblockHighlightOverlayRange.getAsInt();
        if (maxHighlightDistance == 0 || !(be.getLevel() instanceof ClientLevel level)) {
            return;
        }

        // Only render if holding a wrench AND if the shape is not valid.
        boolean drawHighlights = isHoldingWrench() && !be.isShapeValid();
        HatchType hatchType = getHeldHatchType();
        if (!drawHighlights && hatchType == null) {
            return;
        }
        ShapeMatcher matcher = be.createShapeMatcher();
        var player = Minecraft.getInstance().player;
        BlockPos origin = be.getBlockPos();

        for (BlockPos pos : matcher.getPositions()) {
            if (player != null && player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > (double) maxHighlightDistance * maxHighlightDistance) {
                // Skip blocks that are far from the player to mitigate FPS drops.
                continue;
            }
            BlockPos offset = pos.subtract(origin);

            HatchFlags hatchFlag = matcher.getHatchFlags(pos);
            if (hatchType != null && MIClientConfig.INSTANCE.hatchPlacementOverlay.getAsBoolean() && hatchFlag != null
                    && hatchFlag.allows(hatchType)) {
                // Highlight placeable hatches in green
                state.hatchOverlays.add(offset);
            }
            if (drawHighlights && !matcher.matches(pos, level)) {
                var existingState = level.getBlockState(pos);
                if (existingState.isAir() || /* approximate check for e.g. grass and snow */ existingState.canBeReplaced()) {
                    var preview = new MovingBlockRenderState();
                    preview.blockPos = pos;
                    preview.randomSeedPos = pos;
                    preview.blockState = matcher.getSimpleMember(pos).getPreviewState();
                    preview.biome = level.getBiome(pos);
                    preview.cardinalLighting = level.cardinalLighting();
                    preview.lightEngine = level.getLightEngine();
                    state.highlights.add(new MultiblockMachineRenderState.Highlight(offset, preview));
                } else {
                    state.highlights.add(new MultiblockMachineRenderState.Highlight(offset, null));
                }
            }
        }
    }

    @Override
    public void submit(MachineRenderState machineState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(machineState, poseStack, collector, camera);
        var state = (MultiblockMachineRenderState) machineState;

        for (BlockPos offset : state.hatchOverlays) {
            poseStack.pushPose();
            poseStack.translate(offset.getX() - 0.005, offset.getY() - 0.005, offset.getZ() - 0.005);
            poseStack.scale(1.01f, 1.01f, 1.01f);
            RenderHelper.drawOverlay(poseStack, collector);
            poseStack.popPose();
        }
        for (var highlight : state.highlights) {
            poseStack.pushPose();
            BlockPos offset = highlight.offset();
            poseStack.translate(offset.getX() + 0.25, offset.getY() + 0.25, offset.getZ() + 0.25);
            poseStack.scale(0.5f, 0.5f, 0.5f);
            if (highlight.preview() == null) {
                RenderHelper.drawCube(poseStack, collector, 0xFFFF3232);
            } else {
                collector.submitMovingBlock(poseStack, highlight.preview());
            }
            poseStack.popPose();
        }
    }

    private static boolean isHoldingWrench() {
        Player player = Minecraft.getInstance().player;
        return player != null && (player.getMainHandItem().is(MITags.WRENCHES) || player.getOffhandItem().is(MITags.WRENCHES));
    }

    @Nullable
    private static HatchType getHeldHatchType() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        HatchType mainHand = getHatchType(player.getMainHandItem());
        HatchType offHand = getHatchType(player.getOffhandItem());
        return mainHand == null ? offHand : mainHand;
    }

    @Nullable
    private static HatchType getHatchType(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BlockItem) {
            BlockItem blockItem = (BlockItem) item;
            if (blockItem.getBlock() instanceof MachineBlock) {
                MachineBlock block = (MachineBlock) blockItem.getBlock();
                BlockEntity be = block.newBlockEntity(new BlockPos(0, 0, 0), block.defaultBlockState());
                if (be instanceof HatchBlockEntity hatch) {
                    return hatch.getHatchType();
                }
            }
        }
        return null;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(MultiblockMachineBlockEntity blockEntity) {
        return AABB.INFINITE;
    }
}
