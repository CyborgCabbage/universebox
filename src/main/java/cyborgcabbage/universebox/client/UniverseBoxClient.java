package cyborgcabbage.universebox.client;

import cyborgcabbage.universebox.UniverseBox;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import qouteall.imm_ptl.core.render.PortalEntityRenderer;

public class UniverseBoxClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(
                UniverseBox.DEPENDENT_PORTAL,
                (EntityRendererFactory) PortalEntityRenderer::new
        );
    }
}
