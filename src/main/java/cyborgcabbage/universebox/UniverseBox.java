package cyborgcabbage.universebox;

import com.mojang.serialization.Codec;
import cyborgcabbage.universebox.block.RealityWallBlock;
import cyborgcabbage.universebox.block.UniverseBoxBlock;
import cyborgcabbage.universebox.block.UniverseBoxOppositeBlock;
import cyborgcabbage.universebox.block.entity.UniverseBoxBlockEntity;
import cyborgcabbage.universebox.portal.DependentPortal;
import cyborgcabbage.universebox.structure.ConfiguredStructures;
import cyborgcabbage.universebox.structure.Structures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.api.DimensionAPI;

public class UniverseBox implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("universebox");
    public static final String MODID = "universebox";

    public static final Block UNIVERSE_BOX_BLOCK = new UniverseBoxBlock(FabricBlockSettings.of(Material.METAL).strength(4.0f).luminance(state -> 10));
    public static BlockEntityType<UniverseBoxBlockEntity> UNIVERSE_BOX_BLOCK_ENTITY;

    public static final RegistryKey<World> POCKET_DIMENSION = RegistryKey.of(Registry.WORLD_KEY, new Identifier("universebox","pocket_dimension"));

    public static final EntityType<DependentPortal> DEPENDENT_PORTAL = Registry.register(
        Registry.ENTITY_TYPE,
        new Identifier("universebox", "dependent_portal"),
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, DependentPortal::new)
                .dimensions(new EntityDimensions(1, 1, true))
                .fireImmune()
                .trackable(96, 20)
                .build()
    );

    public static final Block REALITY_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.STONE).strength(-1.0f, 3600000.0f).dropsNothing().allowsSpawning((a, b, c, d) -> false));
    public static final Block UNIVERSE_BOX_OPPOSITE_BLOCK = new UniverseBoxOppositeBlock(FabricBlockSettings.of(Material.METAL).strength(-1.0f, 3600000.0f).dropsNothing().luminance(state -> 10));
    @Override
    public void onInitialize() {
        //Universe Box Block
        Registry.register(Registry.BLOCK, new Identifier("universebox", "universe_box"), UNIVERSE_BOX_BLOCK);
        Registry.register(Registry.ITEM, new Identifier("universebox", "universe_box"), new BlockItem(UNIVERSE_BOX_BLOCK, new FabricItemSettings().group(ItemGroup.MISC)));
        UNIVERSE_BOX_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, "universebox:universe_box_block_entity", FabricBlockEntityTypeBuilder.create(UniverseBoxBlockEntity::new, UNIVERSE_BOX_BLOCK).build(null));
        //Reality Wall Block
        Registry.register(Registry.BLOCK, new Identifier("universebox", "reality_wall"), REALITY_WALL_BLOCK);
        Registry.register(Registry.ITEM, new Identifier("universebox", "reality_wall"), new BlockItem(REALITY_WALL_BLOCK, new FabricItemSettings().group(ItemGroup.MISC)));
        //Universe Box Opposite Block
        Registry.register(Registry.BLOCK, new Identifier("universebox", "universe_box_opposite"), UNIVERSE_BOX_OPPOSITE_BLOCK);
        Registry.register(Registry.ITEM, new Identifier("universebox", "universe_box_opposite"), new BlockItem(UNIVERSE_BOX_OPPOSITE_BLOCK, new FabricItemSettings().group(ItemGroup.MISC)));
        //Structures
        Structures.setupAndRegisterStructureFeatures();
        ConfiguredStructures.registerConfiguredStructures();
        addStructureSpawningToDimensionsAndBiomes();
        //Dimension
        /*DimensionAPI.serverDimensionsLoadEvent.register((generatorOptions, registryManager) -> {
            SimpleRegistry<DimensionOptions> registry = generatorOptions.getDimensions();
            long seed = generatorOptions.getSeed();

            //Dimension type
            DimensionType dimensionType = registryManager.get(Registry.DIMENSION_TYPE_KEY)
                    .get(new Identifier("universebox:pocket_dimension_type"));
            Validate.notNull(dimensionType);

            //Chunk Generator type
            ChunkGenerator chunkGenerator = registryManager.get();
                    .get(new Identifier("universebox:pocket_dimension_generator"));
            Validate.notNull(chunkGenerator);

            // directly register the dimension
            Identifier dimensionId = new Identifier("universebox:pocket_dimension");
            DimensionAPI.addDimension(
                    seed, registry, dimensionId, () -> dimensionType,

            );
        });*/
    }

    /**
     * used for spawning our structures in biomes.
     * You can move the BiomeModification API anywhere you prefer it to be at.
     * Just make sure you call BiomeModifications.addStructure at mod init.
     */
    public static void addStructureSpawningToDimensionsAndBiomes() {

        /*
         * This is the API you will use to add anything to any biome.
         * This includes spawns, changing the biome's looks, messing with its temperature,
         * adding carvers, spawning new features... etc
         */
        BiomeModifications.addStructure(
                // Add our structure to all biomes that have any of these biome categories. This includes modded biomes.
                // You can filter to certain biomes based on stuff like temperature, scale, precipitation, mod id, etc.
                // See BiomeSelectors's methods for more options or write your own by doing `(context) -> context.whatever() == condition`
                /*BiomeSelectors.categories(
                        Biome.Category.DESERT,
                        Biome.Category.EXTREME_HILLS,
                        Biome.Category.FOREST,
                        Biome.Category.ICY,
                        Biome.Category.JUNGLE,
                        Biome.Category.PLAINS,
                        Biome.Category.SAVANNA,
                        Biome.Category.TAIGA),*/
                //BiomeSelectors.categories(Biome.Category.UNDERGROUND),
                //BiomeSelectors.categories(Biome.Category.PLAINS),
                BiomeSelectors.includeByKey(RegistryKey.of(Registry.BIOME_KEY,new Identifier(MODID,"pocket_dimension_biome"))),
                // The registry key of our ConfiguredStructure so BiomeModification API can grab it
                // later to tell the game which biomes that your structure can spawn within.
                RegistryKey.of(
                        Registry.CONFIGURED_STRUCTURE_FEATURE_KEY,
                        BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE.getId(ConfiguredStructures.CONFIGURED_POCKET_STRUCTURE))
        );

        //////////// DIMENSION BASED STRUCTURE SPAWNING (OPTIONAL) ////////////
//
//        // This is for making sure our ServerWorldEvents.LOAD event always fires after Fabric API's usage of the same event.
//        // This is done so our changes don't get overwritten by Fabric API adding structure spacings to all dimensions.
//        // Requires Fabric API v0.42.0  or newer.
//        Identifier runAfterFabricAPIPhase = new Identifier(StructureTutorialMain.MODID, "run_after_fabric_api");
//        ServerWorldEvents.LOAD.addPhaseOrdering(Event.DEFAULT_PHASE, runAfterFabricAPIPhase);
//
//        ServerWorldEvents.LOAD.register(runAfterFabricAPIPhase, (MinecraftServer minecraftServer, ServerWorld serverWorld) -> {
//            // Skip superflat to prevent issues with it. Plus, users don't want structures clogging up their superflat worlds.
//            if (serverWorld.getChunkManager().getChunkGenerator() instanceof FlatChunkGenerator && serverWorld.getRegistryKey().equals(World.OVERWORLD)) {
//                return;
//            }
//
//            StructuresConfig worldStructureConfig = serverWorld.getChunkManager().getChunkGenerator().getStructuresConfig();
//
//            // Controls the dimension blacklisting and/or whitelisting
//            // If the spacing or our structure is not added for a dimension, the structure doesn't spawn in that dimension.
//            // Note: due to a quirk with how Noise Settings are shared between dimensions, you need this mixin to make a
//            // deep copy of the noise setting per dimension for your dimension whitelisting/blacklisting to work properly:
//            // https://github.com/TelepathicGrunt/RepurposedStructures-Fabric/blob/1.18/src/main/java/com/telepathicgrunt/repurposedstructures/mixin/world/ChunkGeneratorMixin.java
//
//            // Need temp map as some mods use custom chunk generators with immutable maps in themselves.
//            Map<StructureFeature<?>, StructureConfig> tempMap = new HashMap<>(worldStructureConfig.getStructures());
//
//            // Make absolutely sure modded dimension can or cannot spawn our structures.
//            // New dimensions under the minecraft namespace will still get it (datapacks might do this)
//            if(serverWorld.getRegistryKey().equals(World.OVERWORLD)) {
//                tempMap.put(STStructures.RUN_DOWN_HOUSE, FabricStructureImpl.STRUCTURE_TO_CONFIG_MAP.get(STStructures.RUN_DOWN_HOUSE));
//            }
//            else {
//                tempMap.remove(STStructures.RUN_DOWN_HOUSE);
//            }
//
//            // Set the new modified map of structure spacing to the dimension's chunkgenerator.
//            ((StructuresConfigAccessor)worldStructureConfig).setStructures(tempMap);
//
//        });
    }
}
