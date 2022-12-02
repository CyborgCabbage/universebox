package cyborgcabbage.universebox.client;

import cyborgcabbage.universebox.UniverseBox;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import qouteall.imm_ptl.core.render.PortalEntityRenderer;

public class UniverseBoxClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(
                UniverseBox.DEPENDENT_PORTAL,
                PortalEntityRenderer::new
        );
    }
}
