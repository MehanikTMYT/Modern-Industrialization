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

package aztech.modern_industrialization.client.machines;

import net.minecraft.world.phys.BlockHitResult;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import aztech.modern_industrialization.MITags;
import aztech.modern_industrialization.client.util.RenderHelper;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.MachineOverlay;
import aztech.modern_industrialization.util.GeometryHelper;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class MachineOverlayClient {
    // Fleet port (26.1): RenderHighlightEvent is gone; outlines are extracted, then drawn by CustomBlockOutlineRenderers.
    public static void onBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
        BlockPos pos = event.getBlockPos();
        var player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().is(MITags.WRENCHES)) {
            return;
        }
        if (!(event.getLevel().getBlockEntity(pos) instanceof MachineBlockEntity machine)
                || !(machine.orientation.params.hasFacing || machine.orientation.params.hasOutput)) {
            return;
        }
        BlockHitResult hit = event.getHitResult();
        Direction face = hit.getDirection();
        Vec3 posOnFace = GeometryHelper.toFaceCoords(MachineOverlay.getPosInBlock(hit), face);

        event.addCustomRenderer((outline, buffers, poseStack, translucentPass, levelState) -> {
            Vec3 cam = levelState.cameraRenderState.pos;
            poseStack.pushPose();
            poseStack.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
            Matrix4f model = poseStack.last().pose();

            // Colored face overlay: the zone under the cursor is green, the others blue.
            VertexConsumer quads = buffers.getBuffer(RenderTypes.debugQuads());
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 3; ++j) {
                    double minX = MachineOverlay.ZONES[i], maxX = MachineOverlay.ZONES[i + 1];
                    double minY = MachineOverlay.ZONES[j], maxY = MachineOverlay.ZONES[j + 1];
                    boolean insideQuad = minX <= posOnFace.x && posOnFace.x <= maxX && minY <= posOnFace.y && posOnFace.y <= maxY;
                    float g = insideQuad ? 1 : 0;
                    float b = insideQuad ? 0 : 1;
                    faceVertex(model, quads, face, minX, minY, g, b);
                    faceVertex(model, quads, face, maxX, minY, g, b);
                    faceVertex(model, quads, face, maxX, maxY, g, b);
                    faceVertex(model, quads, face, minX, maxY, g, b);
                }
            }

            // Extra lines
            VertexConsumer lines = buffers.getBuffer(RenderTypes.lines());
            double[] z = MachineOverlay.ZONES;
            line(poseStack, lines, face, z[1], z[0], z[1], z[3]);
            line(poseStack, lines, face, z[2], z[0], z[2], z[3]);
            line(poseStack, lines, face, z[0], z[1], z[3], z[1]);
            line(poseStack, lines, face, z[0], z[2], z[3], z[2]);
            poseStack.popPose();
            return false; // keep the vanilla outline as well
        });
    }

    private static void faceVertex(Matrix4f model, VertexConsumer quads, Direction face, double faceX, double faceY, float g, float b) {
        Vec3 coord = GeometryHelper.toWorldCoords(new Vec3(faceX, faceY, -3.5e-4), face);
        quads.addVertex(model, (float) coord.x, (float) coord.y, (float) coord.z).setColor(0f, g, b, 0.3f);
    }

    private static void line(PoseStack poseStack, VertexConsumer lines, Direction face, double x0, double y0, double x1, double y1) {
        Vec3 a = GeometryHelper.toWorldCoords(new Vec3(x0, y0, -3.5e-4), face);
        Vec3 b = GeometryHelper.toWorldCoords(new Vec3(x1, y1, -3.5e-4), face);
        Vec3 n = b.subtract(a).normalize();
        var pose = poseStack.last();
        lines.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(0f, 0f, 0f, 0.4f).setNormal(pose, (float) n.x, (float) n.y, (float) n.z).setLineWidth(2.0f);
        lines.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(0f, 0f, 0f, 0.4f).setNormal(pose, (float) n.x, (float) n.y, (float) n.z).setLineWidth(2.0f);
    }
}
