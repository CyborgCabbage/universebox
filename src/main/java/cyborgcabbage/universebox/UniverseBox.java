package cyborgcabbage.universebox;

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
import net.fabricmc.fabric.api.loot.v1.FabricLootPoolBuilder;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/*
//Needed
TODO: Add some more structures
//Acceptable losses
TODO: Very rarely spawn entangled boxes, where one box contains another which connects to itself
TODO: Limit render distance in pocket dimension to 2
TODO: Make the pocket dimension structure spawn only when you place the block
TODO: Proper loading animation for the portal
TODO: Always drop when destroyed in an explosion
 */

public class UniverseBox implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("universebox");
    public static final String MODID = "universebox";

    public static final Block UNIVERSE_BOX_BLOCK = new UniverseBoxBlock(FabricBlockSettings.of(Material.METAL).strength(2.0f).luminance(state -> 10));
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
        Registry.register(Registry.ITEM, new Identifier("universebox", "universe_box"), new BlockItem(UNIVERSE_BOX_BLOCK, new FabricItemSettings().maxCount(1).group(ItemGroup.MISC)));
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
        BiomeModifications.addStructure(
                BiomeSelectors.includeByKey(RegistryKey.of(Registry.BIOME_KEY,new Identifier(MODID,"pocket_dimension_biome"))),
                RegistryKey.of(
                        Registry.CONFIGURED_STRUCTURE_FEATURE_KEY,
                        BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE.getId(ConfiguredStructures.CONFIGURED_POCKET_STRUCTURE))
        );
        //Loot Tables
        LootTableLoadingCallback.EVENT.register((resourceManager, lootManager, id, table, setter) -> {
            if (LootTables.SIMPLE_DUNGEON_CHEST.equals(id)) {
                FabricLootPoolBuilder poolBuilder = FabricLootPoolBuilder.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .withEntry(ItemEntry.builder(UniverseBox.UNIVERSE_BOX_BLOCK).weight(1).build())
                        .withEntry(ItemEntry.builder(Blocks.AIR).weight(20).build());
                table.pool(poolBuilder);
            }
            if (LootTables.ABANDONED_MINESHAFT_CHEST.equals(id)) {
                FabricLootPoolBuilder poolBuilder = FabricLootPoolBuilder.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .withEntry(ItemEntry.builder(UniverseBox.UNIVERSE_BOX_BLOCK).weight(1).build())
                        .withEntry(ItemEntry.builder(Blocks.AIR).weight(20).build());
                table.pool(poolBuilder);
            }
            if (LootTables.BURIED_TREASURE_CHEST.equals(id)) {
                FabricLootPoolBuilder poolBuilder = FabricLootPoolBuilder.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .withEntry(ItemEntry.builder(UniverseBox.UNIVERSE_BOX_BLOCK).weight(1).build())
                        .withEntry(ItemEntry.builder(Blocks.AIR).weight(20).build());
                table.pool(poolBuilder);
            }
            if (LootTables.END_CITY_TREASURE_CHEST.equals(id)) {
                FabricLootPoolBuilder poolBuilder = FabricLootPoolBuilder.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .withEntry(ItemEntry.builder(UniverseBox.UNIVERSE_BOX_BLOCK).weight(1).build())
                        .withEntry(ItemEntry.builder(Blocks.AIR).weight(10).build());
                table.pool(poolBuilder);
            }
            if (LootTables.STRONGHOLD_CORRIDOR_CHEST.equals(id)) {
                FabricLootPoolBuilder poolBuilder = FabricLootPoolBuilder.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .withEntry(ItemEntry.builder(UniverseBox.UNIVERSE_BOX_BLOCK).weight(1).build())
                        .withEntry(ItemEntry.builder(Blocks.AIR).weight(10).build());
                table.pool(poolBuilder);
            }
            if (LootTables.STRONGHOLD_CROSSING_CHEST.equals(id)) {
                FabricLootPoolBuilder poolBuilder = FabricLootPoolBuilder.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .withEntry(ItemEntry.builder(UniverseBox.UNIVERSE_BOX_BLOCK).weight(1).build())
                        .withEntry(ItemEntry.builder(Blocks.AIR).weight(10).build());
                table.pool(poolBuilder);
            }
        });
    }
}
