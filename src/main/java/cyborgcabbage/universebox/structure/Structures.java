package cyborgcabbage.universebox.structure;

import cyborgcabbage.universebox.UniverseBox;
import net.fabricmc.fabric.api.structure.v1.FabricStructureBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.chunk.StructureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;

public class Structures {

    public static StructureFeature<StructurePoolFeatureConfig> POCKET_STRUCTURE = new PocketStructure(StructurePoolFeatureConfig.CODEC);

    public static void setupAndRegisterStructureFeatures() {
        FabricStructureBuilder.create(new Identifier(UniverseBox.MODID, "pocket_structure"), POCKET_STRUCTURE)
                .step(GenerationStep.Feature.UNDERGROUND_STRUCTURES)

                .defaultConfig(new StructureConfig(
                        1, /* average distance apart in chunks between spawn attempts */
                        0, /* minimum distance apart in chunks between spawn attempts. MUST BE LESS THAN ABOVE VALUE */
                        399117345 ))
                .register();
    }
}
