package aztech.modern_industrialization.client.machines.multiblocks;

import aztech.modern_industrialization.client.machines.MachineRenderState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * Fleet port (26.1): wrench / hatch highlights of a multiblock, computed in extractRenderState (client thread, with the
 * level) and drawn in submit. Positions are relative to the controller.
 */
public class MultiblockMachineRenderState extends MachineRenderState {
    public final List<BlockPos> hatchOverlays = new ArrayList<>();
    public final List<Highlight> highlights = new ArrayList<>();

    /**
     * @param preview the member block to place there, or {@code null} for a red cube (a wrong block is in the way)
     */
    public record Highlight(BlockPos offset, @Nullable MovingBlockRenderState preview) {}
}
