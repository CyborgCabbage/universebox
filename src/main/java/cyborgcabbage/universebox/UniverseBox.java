package cyborgcabbage.universebox;

import cyborgcabbage.universebox.block.UniverseBoxBlock;
import cyborgcabbage.universebox.block.UniverseBoxOppositeBlock;
import cyborgcabbage.universebox.block.entity.UniverseBoxBlockEntity;
import cyborgcabbage.universebox.portal.DependentPortal;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
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
import net.minecraft.item.ItemGroups;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;

public class UniverseBox implements ModInitializer {
    public static final String MOD_ID = "universebox";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final Block UNIVERSE_BOX_BLOCK = new UniverseBoxBlock(FabricBlockSettings.of(Material.METAL).strength(2.0f).luminance(state -> 10));
    public static BlockEntityType<UniverseBoxBlockEntity> UNIVERSE_BOX_BLOCK_ENTITY;

    public static final RegistryKey<World> POCKET_DIMENSION = RegistryKey.of(RegistryKeys.WORLD, id("pocket_dimension"));

    public static final EntityType<DependentPortal> DEPENDENT_PORTAL = Registry.register(
        Registries.ENTITY_TYPE,
        id( "dependent_portal"),
        FabricEntityTypeBuilder.create(SpawnGroup.MISC, DependentPortal::new)
                .dimensions(new EntityDimensions(1, 1, true))
                .fireImmune()
                .trackRangeBlocks(96)
                .trackedUpdateRate(20)
                .build()
    );

    public static final Block REALITY_WALL_BLOCK = new Block(FabricBlockSettings.of(Material.STONE).strength(-1.0f, 3600000.0f).dropsNothing().allowsSpawning((a, b, c, d) -> false));
    public static final Block UNIVERSE_BOX_OPPOSITE_BLOCK = new UniverseBoxOppositeBlock(FabricBlockSettings.of(Material.METAL).strength(-1.0f, 3600000.0f).dropsNothing().luminance(state -> 10));

    @Override
    public void onInitialize() {
        //Universe Box Block
        Registry.register(Registries.BLOCK, id( "universe_box"), UNIVERSE_BOX_BLOCK);
        BlockItem UNIVERSE_BOX_BLOCK_ITEM = new BlockItem(UNIVERSE_BOX_BLOCK, new FabricItemSettings().maxCount(1));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(UNIVERSE_BOX_BLOCK_ITEM));

        Registry.register(Registries.ITEM, id( "universe_box"), UNIVERSE_BOX_BLOCK_ITEM);
        UNIVERSE_BOX_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, "universebox:universe_box_block_entity", FabricBlockEntityTypeBuilder.create(UniverseBoxBlockEntity::new, UNIVERSE_BOX_BLOCK).build(null));
        //Reality Wall Block
        Registry.register(Registries.BLOCK, id( "reality_wall"), REALITY_WALL_BLOCK);
        Registry.register(Registries.ITEM, id( "reality_wall"), new BlockItem(REALITY_WALL_BLOCK, new FabricItemSettings()));
        //Universe Box Opposite Block
        Registry.register(Registries.BLOCK, id( "universe_box_opposite"), UNIVERSE_BOX_OPPOSITE_BLOCK);
        Registry.register(Registries.ITEM, id( "universe_box_opposite"), new BlockItem(UNIVERSE_BOX_OPPOSITE_BLOCK, new FabricItemSettings()));

        //Loot Tables
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, table, source) -> {
            BiConsumer<Identifier, Integer> addUniverseBoxToPool = (chestId, counterWeight) -> {
                if (chestId.equals(id)) {
                    LootPool.Builder poolBuilder = LootPool.builder()
                            .rolls(ConstantLootNumberProvider.create(1))
                            .with(ItemEntry.builder(UniverseBox.UNIVERSE_BOX_BLOCK).weight(1))
                            .with(ItemEntry.builder(Blocks.AIR).weight(counterWeight));
                    table.pool(poolBuilder);
                }
            };
            addUniverseBoxToPool.accept(LootTables.SIMPLE_DUNGEON_CHEST, 7);
            addUniverseBoxToPool.accept(LootTables.ABANDONED_MINESHAFT_CHEST, 7);
            addUniverseBoxToPool.accept(LootTables.BURIED_TREASURE_CHEST, 7);
            addUniverseBoxToPool.accept(LootTables.END_CITY_TREASURE_CHEST, 8);
            addUniverseBoxToPool.accept(LootTables.STRONGHOLD_CORRIDOR_CHEST, 3);
            addUniverseBoxToPool.accept(LootTables.STRONGHOLD_CROSSING_CHEST, 3);
        });
    }
    
    public static Identifier id(String s) {
        return new Identifier(MOD_ID, s);
    }
}
