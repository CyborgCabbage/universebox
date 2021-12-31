package cyborgcabbage.universebox.structure;

import com.mojang.serialization.Codec;
import cyborgcabbage.universebox.UniverseBox;
import net.minecraft.entity.EntityType;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.PostPlacementProcessor;
import net.minecraft.structure.StructureGeneratorFactory;
import net.minecraft.structure.StructurePiecesGenerator;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;
import org.apache.logging.log4j.Level;

import java.util.Optional;

public class PocketStructure extends StructureFeature<StructurePoolFeatureConfig> {

        public PocketStructure(Codec<StructurePoolFeatureConfig> codec) {
            super(codec, (context) -> {
                        // Check if the spot is valid for structure gen. If false, return nothing to signal to the game to skip this spawn attempt.
                        if (!PocketStructure.canGenerate(context)) {
                            return Optional.empty();
                        }
                        // Create the pieces' layout of the structure and give it to
                        else {
                            return PocketStructure.createPiecesGenerator(context);
                        }
                    },
                    PostPlacementProcessor.EMPTY);
        }

        /**
         * These fields + NoiseChunkGeneratorMixin allows us to have mobs that spawn naturally over time in our structure.
         * No other mobs will spawn in the structure of the same entity classification.
         * The reason you want to match the classifications is so that your structure's mob
         * will contribute to that classification's cap. Otherwise, it may cause a runaway
         * spawning of the mob that will never stop.
         */
        public static final Pool<SpawnSettings.SpawnEntry> STRUCTURE_MONSTERS = Pool.of(
                new SpawnSettings.SpawnEntry(EntityType.ILLUSIONER, 100, 4, 9),
                new SpawnSettings.SpawnEntry(EntityType.VINDICATOR, 100, 4, 9)
        );

        public static final Pool<SpawnSettings.SpawnEntry> STRUCTURE_CREATURES = Pool.of(
                new SpawnSettings.SpawnEntry(EntityType.SHEEP, 30, 10, 15),
                new SpawnSettings.SpawnEntry(EntityType.RABBIT, 100, 1, 2)
        );

        /*
         * This is where extra checks can be done to determine if the structure can spawn here.
         * This only needs to be overridden if you're adding additional spawn conditions.
         *
         * Fun fact, if you set your structure separation/spacing to be 0/1, you can use
         * canGenerate to return true only if certain chunk coordinates are passed in
         * which allows you to spawn structures only at certain coordinates in the world.
         *
         * Basically, this method is used for determining if the land is at a suitable height,
         * if certain other structures are too close or not, or some other restrictive condition.
         *
         * For example, Pillager Outposts added a check to make sure it cannot spawn within 10 chunk of a Village.
         * (Bedrock Edition seems to not have the same check)
         *
         *
         * Also, please for the love of god, do not do dimension checking here.
         * If you do and another mod's dimension is trying to spawn your structure,
         * the locate command will make minecraft hang forever and break the game.
         *
         * Instead, use the removeStructureSpawningFromSelectedDimension method in
         * StructureTutorialMain class. If you check for the dimension there and do not add your
         * structure's spacing into the chunk generator, the structure will not spawn in that dimension!
         */
        private static boolean canGenerate(StructureGeneratorFactory.Context<StructurePoolFeatureConfig> context) {
            ChunkPos chunkPos = context.chunkPos();
            int f = 4;
            if(chunkPos.x % f == 0){
                if(chunkPos.z % f == 0){
                    return true;
                }
            }
            return false;
        }

        public static Optional<StructurePiecesGenerator<StructurePoolFeatureConfig>> createPiecesGenerator(StructureGeneratorFactory.Context<StructurePoolFeatureConfig> context) {
            // Turns the chunk coordinates into actual coordinates we can use. (Gets center of that chunk)
            BlockPos blockpos = context.chunkPos().getBlockPos(8,64,8);

            /*
             * If you are doing Nether structures, you'll probably want to spawn your structure on top of ledges.
             * Best way to do that is to use getColumnSample to grab a column of blocks at the structure's x/z position.
             * Then loop through it and look for land with air above it and set blockpos's Y value to it.
             * Make sure to set the final boolean in StructurePoolBasedGenerator.generate to false so
             * that the structure spawns at blockpos's y value instead of placing the structure on the Bedrock roof!
             */
            //VerticalBlockSample blockView = context.chunkGenerator().getColumnSample(blockpos.getX(), blockpos.getZ(), context.world());

            /*
             * The only reason we are using StructurePoolFeatureConfig here is because further down, we are using
             * StructurePoolBasedGenerator.generate which requires StructurePoolFeatureConfig. However, if you create your own
             * StructurePoolBasedGenerator.generate, you could reduce the amount of workarounds like above that you need
             * and give yourself more opportunities and control over your structures.
             *
             * An example of a custom StructurePoolBasedGenerator.generate in action can be found here (warning, it is using Mojmap mappings):
             * https://github.com/TelepathicGrunt/RepurposedStructures-Fabric/blob/1.18/src/main/java/com/telepathicgrunt/repurposedstructures/world/structures/pieces/PieceLimitedJigsawManager.java
             */
            StructurePoolFeatureConfig newConfig = new StructurePoolFeatureConfig(
                    // The path to the starting Template Pool JSON file to read.
                    //
                    // Note, this is "structure_tutorial:run_down_house/start_pool" which means
                    // the game will automatically look into the following path for the template pool:
                    // "resources/data/structure_tutorial/worldgen/template_pool/run_down_house/start_pool.json"
                    // This is why your pool files must be in "data/<modid>/worldgen/template_pool/<the path to the pool here>"
                    // because the game automatically will check in worldgen/template_pool for the pools.
                    () -> context.registryManager().get(Registry.STRUCTURE_POOL_KEY)
                            .get(new Identifier(UniverseBox.MODID, "pocket/start")),

                    // How many pieces outward from center can a recursive jigsaw structure spawn.
                    // Our structure is only 1 piece outward and isn't recursive so any value of 1 or more doesn't change anything.
                    // However, I recommend you keep this a decent value like 7 so people can use datapacks to add additional pieces to your structure easily.
                    // But don't make it too large for recursive structures like villages or you'll crash server due to hundreds of pieces attempting to generate!
                    10
            );

            // Create a new context with the new config that has our json pool. We will pass this into JigsawPlacement.addPieces
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

            Optional<StructurePiecesGenerator<StructurePoolFeatureConfig>> structurePiecesGenerator =
                    MyStructurePoolBasedGenerator.generate(
                            newContext, // Used for StructurePoolBasedGenerator to get all the proper behaviors done.
                            PoolStructurePiece::new, // Needed in order to create a list of jigsaw pieces when making the structure's layout.
                            blockpos, // Position of the structure. Y value is ignored if last parameter is set to true.
                            false,  // Special boundary adjustments for villages. It's... hard to explain. Keep this false and make your pieces not be partially intersecting.
                            // Either not intersecting or fully contained will make children pieces spawn just fine. It's easier that way.
                            false // Place at heightmap (top land). Set this to false for structure to be place at the passed in blockpos's Y value instead.
                            // Definitely keep this false when placing structures in the nether as otherwise, heightmap placing will put the structure on the Bedrock roof.
                    );

            /*
             * Note, you are always free to make your own StructurePoolBasedGenerator class and implementation of how the structure
             * should generate. It is tricky but extremely powerful if you are doing something that vanilla's jigsaw system cannot do.
             * Such as for example, forcing 3 pieces to always spawn every time, limiting how often a piece spawns, or remove the intersection limitation of pieces.
             *
             * An example of a custom StructurePoolBasedGenerator.generate in action can be found here (warning, it is using Mojmap mappings):
             * https://github.com/TelepathicGrunt/RepurposedStructures-Fabric/blob/1.18/src/main/java/com/telepathicgrunt/repurposedstructures/world/structures/pieces/PieceLimitedJigsawManager.java
             */

            if(structurePiecesGenerator.isPresent()) {
                // I use to debug and quickly find out if the structure is spawning or not and where it is.
                // This is returning the coordinates of the center starting piece.
                UniverseBox.LOGGER.log(Level.DEBUG, "Rundown House at " + blockpos);
            }

            // Return the pieces generator that is now set up so that the game runs it when it needs to create the layout of structure pieces.
            return structurePiecesGenerator;
        }
    }
