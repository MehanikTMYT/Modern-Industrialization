package aztech.modern_industrialization.client.blocks.storage.barrel;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class BarrelRenderState extends BlockEntityRenderState {
    // Fleet port (26.1): text drawn directly in BarrelRenderer#submit (was DeferredBarrelTextRenderer)
    public String amountText = "";
    public String itemName = "";
    public boolean locked;
    public final ItemStackRenderState stackRenderState = new ItemStackRenderState();
}
