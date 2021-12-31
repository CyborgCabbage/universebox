package cyborgcabbage.universebox.structure;

import com.mojang.serialization.Codec;
import cyborgcabbage.universebox.UniverseBox;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.PostPlacementProcessor;
import net.minecraft.structure.StructureGeneratorFactory;
import net.minecraft.structure.StructurePiecesGenerator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;

import java.util.Optional;

public class PocketStructure extends StructureFeature<StructurePoolFeatureConfig> {

        public PocketStructure(Codec<StructurePoolFeatureConfig> codec) {
            super(codec, (context) -> {
                        // Check if the spot is valid for structure gen. If false, return nothing to signal to the game to skip this spawn attempt.
                        ChunkPos chunkPos = context.chunkPos();
                        int f = 4;
                        if (chunkPos.z % f == 0 && chunkPos.x % f == 0) {
                            return PocketStructure.createPiecesGenerator(context);
                        }
                        return Optional.empty();
                    },
                    PostPlacementProcessor.EMPTY);
        }

        public static Optional<StructurePiecesGenerator<StructurePoolFeatureConfig>> createPiecesGenerator(StructureGeneratorFactory.Context<StructurePoolFeatureConfig> context) {
            BlockPos blockpos = context.chunkPos().getBlockPos(8,64,8);

            StructurePoolFeatureConfig newConfig = new StructurePoolFeatureConfig(
                    () -> context.registryManager().get(Registry.STRUCTURE_POOL_KEY)
                            .get(new Identifier(UniverseBox.MODID, "pocket/start")),
                    10
            );

            StructureGeneratorFactory.Context<StructurePoolFeatureConfig> newContext = new StructureGeneratorFactory.Context<>(
                    context.chunkGenerator(),
                    context.biomeSource(),
                    context.seed(),
                    context.chunkPos(),
                    newConfig,
                    context.world(),
                    context.validBiome(),
                    context.structureManager(),
                    context.registryManager()
            );

            return MyStructurePoolBasedGenerator.generate(
                    newContext, // Used for StructurePoolBasedGenerator to get all the proper behaviors done.
                    PoolStructurePiece::new, // Needed in order to create a list of jigsaw pieces when making the structure's layout.
                    blockpos, // Position of the structure. Y value is ignored if last parameter is set to true.
                    false,  // Special boundary adjustments for villages. It's... hard to explain. Keep this false and make your pieces not be partially intersecting.
                    // Either not intersecting or fully contained will make children pieces spawn just fine. It's easier that way.
                    false // Place at heightmap (top land). Set this to false for structure to be place at the passed in blockpos's Y value instead.
                    // Definitely keep this false when placing structures in the nether as otherwise, heightmap placing will put the structure on the Bedrock roof.
            );
        }
    }
