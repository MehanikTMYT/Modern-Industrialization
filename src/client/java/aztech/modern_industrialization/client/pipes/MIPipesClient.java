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

package aztech.modern_industrialization.client.pipes;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.MIItem;
import aztech.modern_industrialization.MIText;
import aztech.modern_industrialization.client.pipes.api.PipeRenderer;
import aztech.modern_industrialization.client.pipes.impl.PipeColorProvider;
import aztech.modern_industrialization.client.pipes.impl.PipeMeshCache;
import aztech.modern_industrialization.client.util.RenderHelper;
import aztech.modern_industrialization.config.MIStartupConfig;
import aztech.modern_industrialization.pipes.MIPipes;
import aztech.modern_industrialization.pipes.api.PipeNetworkType;
import aztech.modern_industrialization.pipes.impl.PipeBlock;
import aztech.modern_industrialization.pipes.impl.PipeBlockEntity;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

public class MIPipesClient {
    public static void setupClient(IEventBus modBus) {
        modBus.addListener(RegisterColorHandlersEvent.BlockTintSources.class, event -> {
            event.register(List.of(new PipeColorProvider()), MIPipes.BLOCK_PIPE.get());
        });
        registerRenderers();

        // Fleet port (26.1): outline only the targeted pipe of a multi-pipe block (RenderHighlightEvent is gone).
        NeoForge.EVENT_BUS.addListener(ExtractBlockOutlineRenderStateEvent.class, event -> {
            var pos = event.getBlockPos();
            if (event.getLevel().getBlockEntity(pos) instanceof PipeBlockEntity pipe && !pipe.hasCamouflage()) {
                var part = PipeBlock.getHitPart(event.getLevel(), pos, event.getHitResult());
                if (part != null) {
                    VoxelShape shape = part.shape;
                    event.addCustomRenderer((outline, buffers, poseStack, translucentPass, levelState) -> {
                        Vec3 cam = levelState.cameraRenderState.pos;
                        RenderHelper.renderVoxelShape(poseStack, buffers.getBuffer(RenderTypes.lines()), shape,
                                pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z, 0, 0, 0, 0.4f);
                        return true;
                    });
                }
            }
        });

        NeoForge.EVENT_BUS.addListener(InputEvent.MouseScrollingEvent.class, event -> {
            var player = Objects.requireNonNull(Minecraft.getInstance().player);

            if (player.isShiftKeyDown() && MIItem.CONFIG_CARD.is(player.getItemInHand(InteractionHand.MAIN_HAND))) {
                // noinspection NonAtomicOperationOnVolatileField
                MIPipes.transparentCamouflage = !MIPipes.transparentCamouflage;
                Minecraft.getInstance().levelRenderer.allChanged();
                var miText = MIPipes.transparentCamouflage ? MIText.TransparentCamouflageEnabled : MIText.TransparentCamouflageDisabled;
                player.sendOverlayMessage(miText.text());
                event.setCanceled(true);
            }
        });
    }

    private static PipeRenderer.Factory makeRenderer(List<String> sprites, boolean innerQuads) {
        return new PipeRenderer.Factory() {
            @Override
            public PipeRenderer create(ModelBaker modelBaker) {
                Material[] ids = sprites.stream()
                        .map(n -> new Material(MI.id("block/pipes/" + n)))
                        .toArray(Material[]::new);
                return new PipeMeshCache(modelBaker.materials(), ids, innerQuads);
            }
        };
    }

    private static final PipeRenderer.Factory ITEM_RENDERER = makeRenderer(Arrays.asList("item", "item_item", "item_in", "item_in_out", "item_out"),
            false);
    private static final PipeRenderer.Factory FLUID_RENDERER = makeRenderer(
            Arrays.asList("fluid", "fluid_item", "fluid_in", "fluid_in_out", "fluid_out"), true);
    private static final PipeRenderer.Factory ELECTRICITY_RENDERER = makeRenderer(Arrays.asList("electricity", "electricity_blocks"), false);

    // Use a set to avoid loading the same renderer multiple times
    public static final Collection<PipeRenderer.Factory> RENDERERS = new LinkedHashSet<>();

    private static void registerRenderers() {
        for (var type : PipeNetworkType.getTypes().values()) {
            if (type.getIdentifier().getPath().endsWith("item_pipe")) {
                PipeRenderer.register(type, ITEM_RENDERER);
            } else if (type.getIdentifier().getPath().endsWith("fluid_pipe")) {
                PipeRenderer.register(type, FLUID_RENDERER);
            } else if (type.getIdentifier().getPath().endsWith("cable")) {
                PipeRenderer.register(type, ELECTRICITY_RENDERER);
            }
        }

        if (MIStartupConfig.INSTANCE.loadAe2Compat()) {
            try {
                Class.forName("aztech.modern_industrialization.client.compat.ae2.MIAEAddonClient")
                        .getMethod("registerPipeRenderers")
                        .invoke(null);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        for (var value : PipeNetworkType.getTypes().values()) {
            RENDERERS.add(PipeRenderer.get(value));
        }
    }
}
