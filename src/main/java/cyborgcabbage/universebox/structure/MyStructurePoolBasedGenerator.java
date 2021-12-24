package cyborgcabbage.universebox.structure;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.block.JigsawBlock;
import net.minecraft.structure.JigsawJunction;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructureGeneratorFactory;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePiecesGenerator;
import net.minecraft.structure.pool.EmptyPoolElement;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.pool.StructurePools;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;
import net.minecraft.world.gen.random.AtomicSimpleRandom;
import net.minecraft.world.gen.random.ChunkRandom;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyStructurePoolBasedGenerator {
    static final Logger LOGGER = LogManager.getLogger();

    public static Optional<StructurePiecesGenerator<StructurePoolFeatureConfig>> generate(StructureGeneratorFactory.Context<StructurePoolFeatureConfig> factoryContext, PieceFactory pieceFactory, BlockPos pos, boolean bl, boolean placeOnSurface) {
        //RNG for this chunk
        ChunkRandom chunkRandom = new ChunkRandom(new AtomicSimpleRandom(0L));
        chunkRandom.setCarverSeed(factoryContext.seed(), factoryContext.chunkPos().x, factoryContext.chunkPos().z);
        //Get things from context
        DynamicRegistryManager dynamicRegistryManager = factoryContext.registryManager();
        StructurePoolFeatureConfig structurePoolFeatureConfig = factoryContext.config();
        ChunkGenerator chunkGenerator = factoryContext.chunkGenerator();
        StructureManager structureManager = factoryContext.structureManager();
        HeightLimitView heightLimitView = factoryContext.world();
        Predicate<Biome> predicate = factoryContext.validBiome();
        //Init StructureFeature (doesn't appear to do anything)
        StructureFeature.init();
        //Get registry of structure pools
        Registry<StructurePool> registry = dynamicRegistryManager.get(Registry.STRUCTURE_POOL_KEY);
        //Get rotation of structure
        BlockRotation blockRotation = BlockRotation.NONE;//BlockRotation.random(chunkRandom);
        //Get pool
        StructurePool structurePool = structurePoolFeatureConfig.getStartPool().get();
        //Get a random structure element from the pool
        StructurePoolElement structurePoolElement = structurePool.getRandomElement(chunkRandom);
        if (structurePoolElement == EmptyPoolElement.INSTANCE) {
            //There is no structure so we zooop
            return Optional.empty();
        }
        //Get the first section of the structure, e.g. the well from the village
        PoolStructurePiece poolStructurePiece = pieceFactory.create(
                structureManager,
                structurePoolElement,
                pos,
                structurePoolElement.getGroundLevelDelta(),
                blockRotation,
                structurePoolElement.getBoundingBox(structureManager, pos, blockRotation)
        );
        //Get the bounding box of the structure
        BlockBox blockBox = poolStructurePiece.getBoundingBox();
        int middleX = (blockBox.getMaxX() + blockBox.getMinX()) / 2;
        int middleZ = (blockBox.getMaxZ() + blockBox.getMinZ()) / 2;
        int relativeY = placeOnSurface ? pos.getY() + chunkGenerator.getHeightOnGround(middleX, middleZ, Heightmap.Type.WORLD_SURFACE_WG, heightLimitView) : pos.getY();
        if (!predicate.test(chunkGenerator.getBiomeForNoiseGen(BiomeCoords.fromBlock(middleX), BiomeCoords.fromBlock(relativeY), BiomeCoords.fromBlock(middleZ)))) {
            return Optional.empty();
        }
        int absoluteY = blockBox.getMinY() + poolStructurePiece.getGroundLevelDelta();
        //Offset
        poolStructurePiece.translate(0, relativeY - absoluteY, 0);
        return Optional.of((structurePiecesCollector, context) -> {
            ArrayList<PoolStructurePiece> list = Lists.newArrayList();
            list.add(poolStructurePiece);
            if (structurePoolFeatureConfig.getSize() <= 0) {
                return;
            }
            int r = 80;
            Box box = new Box(middleX - r, relativeY - r, middleZ - r, middleX + r + 1, relativeY + r + 1, middleZ + r + 1);
            StructurePoolGenerator structurePoolGenerator = new StructurePoolGenerator(
                    registry,
                    structurePoolFeatureConfig.getSize(),
                    pieceFactory,
                    chunkGenerator,
                    structureManager,
                    list,
                    chunkRandom
            );
            structurePoolGenerator.structurePieces.addLast(
                    new ShapedPoolStructurePiece(
                            poolStructurePiece,
                            new MutableObject<VoxelShape>(
                                    VoxelShapes.combineAndSimplify(
                                            VoxelShapes.cuboid(box),
                                            VoxelShapes.cuboid(Box.from(blockBox)),
                                            BooleanBiFunction.ONLY_FIRST
                                    )
                            ),
                            0
                    )
            );
            while (!structurePoolGenerator.structurePieces.isEmpty()) {
                ShapedPoolStructurePiece shapedPoolStructurePiece = structurePoolGenerator.structurePieces.removeFirst();
                structurePoolGenerator.generatePiece(shapedPoolStructurePiece.piece, shapedPoolStructurePiece.pieceShape, shapedPoolStructurePiece.currentSize, bl, heightLimitView);
            }
            list.forEach(structurePiecesCollector::addPiece);
        });
    }

    public static void generate(DynamicRegistryManager registryManager, PoolStructurePiece piece, int maxDepth, PieceFactory pieceFactory, ChunkGenerator chunkGenerator, StructureManager structureManager, List<? super PoolStructurePiece> results, Random random, HeightLimitView world) {
        Registry<StructurePool> registry = registryManager.get(Registry.STRUCTURE_POOL_KEY);
        StructurePoolGenerator structurePoolGenerator = new StructurePoolGenerator(registry, maxDepth, pieceFactory, chunkGenerator, structureManager, results, random);
        structurePoolGenerator.structurePieces.addLast(new ShapedPoolStructurePiece(piece, new MutableObject<VoxelShape>(VoxelShapes.UNBOUNDED), 0));
        while (!structurePoolGenerator.structurePieces.isEmpty()) {
            ShapedPoolStructurePiece shapedPoolStructurePiece = structurePoolGenerator.structurePieces.removeFirst();
            structurePoolGenerator.generatePiece(shapedPoolStructurePiece.piece, shapedPoolStructurePiece.pieceShape, shapedPoolStructurePiece.currentSize, false, world);
        }
    }

    public static interface PieceFactory {
        public PoolStructurePiece create(StructureManager var1, StructurePoolElement var2, BlockPos var3, int var4, BlockRotation var5, BlockBox var6);
    }

    static final class StructurePoolGenerator {
        private final Registry<StructurePool> registry;
        private final int maxSize;
        private final PieceFactory pieceFactory;
        private final ChunkGenerator chunkGenerator;
        private final StructureManager structureManager;
        private final List<? super PoolStructurePiece> children;
        private final Random random;
        final Deque<ShapedPoolStructurePiece> structurePieces = Queues.newArrayDeque();

        StructurePoolGenerator(Registry<StructurePool> registry, int maxSize, PieceFactory pieceFactory, ChunkGenerator chunkGenerator, StructureManager structureManager, List<? super PoolStructurePiece> children, Random random) {
            this.registry = registry;
            this.maxSize = maxSize;
            this.pieceFactory = pieceFactory;
            this.chunkGenerator = chunkGenerator;
            this.structureManager = structureManager;
            this.children = children;
            this.random = random;
        }

        void generatePiece(PoolStructurePiece piece, MutableObject<VoxelShape> pieceShape, int minY, boolean modifyBoundingBox, HeightLimitView world) {
            StructurePoolElement structurePoolElement = piece.getPoolElement();
            BlockPos blockPos = piece.getPos();
            BlockRotation blockRotation = piece.getRotation();
            StructurePool.Projection projection = structurePoolElement.getProjection();
            boolean bl = projection == StructurePool.Projection.RIGID;
            MutableObject<VoxelShape> mutableObject = new MutableObject<VoxelShape>();
            BlockBox blockBox = piece.getBoundingBox();
            int i = blockBox.getMinY();
            block0: for (Structure.StructureBlockInfo structureBlockInfo2 : structurePoolElement.getStructureBlockInfos(this.structureManager, blockPos, blockRotation, this.random)) {
                StructurePoolElement structurePoolElement2;
                MutableObject<VoxelShape> mutableObject2;
                Direction direction = JigsawBlock.getFacing(structureBlockInfo2.state);
                BlockPos blockPos2 = structureBlockInfo2.pos;
                BlockPos blockPos3 = blockPos2.offset(direction);
                int j = blockPos2.getY() - i;
                int k = -1;
                Identifier identifier = new Identifier(structureBlockInfo2.nbt.getString("pool"));
                Optional<StructurePool> optional = this.registry.getOrEmpty(identifier);
                if (!optional.isPresent() || optional.get().getElementCount() == 0 && !Objects.equals(identifier, StructurePools.EMPTY.getValue())) {
                    LOGGER.warn("Empty or non-existent pool: {}", (Object)identifier);
                    continue;
                }
                Identifier identifier2 = optional.get().getTerminatorsId();
                Optional<StructurePool> optional2 = this.registry.getOrEmpty(identifier2);
                if (!optional2.isPresent() || optional2.get().getElementCount() == 0 && !Objects.equals(identifier2, StructurePools.EMPTY.getValue())) {
                    LOGGER.warn("Empty or non-existent fallback pool: {}", (Object)identifier2);
                    continue;
                }
                boolean bl2 = blockBox.contains(blockPos3);
                if (bl2) {
                    mutableObject2 = mutableObject;
                    if (mutableObject.getValue() == null) {
                        mutableObject.setValue(VoxelShapes.cuboid(Box.from(blockBox)));
                    }
                } else {
                    mutableObject2 = pieceShape;
                }
                ArrayList<StructurePoolElement> list = Lists.newArrayList();
                if (minY != this.maxSize) {
                    list.addAll(optional.get().getElementIndicesInRandomOrder(this.random));
                }
                list.addAll(optional2.get().getElementIndicesInRandomOrder(this.random));
                Iterator iterator = list.iterator();
                while (iterator.hasNext() && (structurePoolElement2 = (StructurePoolElement)iterator.next()) != EmptyPoolElement.INSTANCE) {
                    for (BlockRotation blockRotation2 : BlockRotation.randomRotationOrder(this.random)) {
                        List<Structure.StructureBlockInfo> list2 = structurePoolElement2.getStructureBlockInfos(this.structureManager, BlockPos.ORIGIN, blockRotation2, this.random);
                        BlockBox blockBox2 = structurePoolElement2.getBoundingBox(this.structureManager, BlockPos.ORIGIN, blockRotation2);
                        int l = !modifyBoundingBox || blockBox2.getBlockCountY() > 16 ? 0 : list2.stream().mapToInt(structureBlockInfo -> {
                            if (!blockBox2.contains(structureBlockInfo.pos.offset(JigsawBlock.getFacing(structureBlockInfo.state)))) {
                                return 0;
                            }
                            Identifier identifier9 = new Identifier(structureBlockInfo.nbt.getString("pool"));
                            Optional<StructurePool> optional9 = this.registry.getOrEmpty(identifier9);
                            Optional<StructurePool> optional29 = optional9.flatMap(pool -> this.registry.getOrEmpty(pool.getTerminatorsId()));
                            int i9 = optional9.map(pool -> pool.getHighestY(this.structureManager)).orElse(0);
                            int j9 = optional29.map(pool -> pool.getHighestY(this.structureManager)).orElse(0);
                            return Math.max(i9, j9);
                        }).max().orElse(0);
                        for (Structure.StructureBlockInfo structureBlockInfo22 : list2) {
                            int t;
                            int r;
                            int p;
                            if (!JigsawBlock.attachmentMatches(structureBlockInfo2, structureBlockInfo22)) continue;
                            BlockPos blockPos4 = structureBlockInfo22.pos;
                            BlockPos blockPos5 = blockPos3.subtract(blockPos4);
                            BlockBox blockBox3 = structurePoolElement2.getBoundingBox(this.structureManager, blockPos5, blockRotation2);
                            int m = blockBox3.getMinY();
                            StructurePool.Projection projection2 = structurePoolElement2.getProjection();
                            boolean bl3 = projection2 == StructurePool.Projection.RIGID;
                            int n = blockPos4.getY();
                            int o = j - n + JigsawBlock.getFacing(structureBlockInfo2.state).getOffsetY();
                            if (bl && bl3) {
                                p = i + o;
                            } else {
                                if (k == -1) {
                                    k = this.chunkGenerator.getHeightOnGround(blockPos2.getX(), blockPos2.getZ(), Heightmap.Type.WORLD_SURFACE_WG, world);
                                }
                                p = k - n;
                            }
                            int q = p - m;
                            BlockBox blockBox4 = blockBox3.offset(0, q, 0);
                            BlockPos blockPos6 = blockPos5.add(0, q, 0);
                            if (l > 0) {
                                r = Math.max(l + 1, blockBox4.getMaxY() - blockBox4.getMinY());
                                blockBox4.encompass(new BlockPos(blockBox4.getMinX(), blockBox4.getMinY() + r, blockBox4.getMinZ()));
                            }
                            if (VoxelShapes.matchesAnywhere((VoxelShape)mutableObject2.getValue(), VoxelShapes.cuboid(Box.from(blockBox4).contract(0.25)), BooleanBiFunction.ONLY_SECOND)) continue;
                            mutableObject2.setValue(VoxelShapes.combine((VoxelShape)mutableObject2.getValue(), VoxelShapes.cuboid(Box.from(blockBox4)), BooleanBiFunction.ONLY_FIRST));
                            r = piece.getGroundLevelDelta();
                            int s = bl3 ? r - o : structurePoolElement2.getGroundLevelDelta();
                            PoolStructurePiece poolStructurePiece = this.pieceFactory.create(this.structureManager, structurePoolElement2, blockPos6, s, blockRotation2, blockBox4);
                            if (bl) {
                                t = i + j;
                            } else if (bl3) {
                                t = p + n;
                            } else {
                                if (k == -1) {
                                    k = this.chunkGenerator.getHeightOnGround(blockPos2.getX(), blockPos2.getZ(), Heightmap.Type.WORLD_SURFACE_WG, world);
                                }
                                t = k + o / 2;
                            }
                            piece.addJunction(new JigsawJunction(blockPos3.getX(), t - j + r, blockPos3.getZ(), o, projection2));
                            poolStructurePiece.addJunction(new JigsawJunction(blockPos2.getX(), t - n + s, blockPos2.getZ(), -o, projection));
                            this.children.add(poolStructurePiece);
                            if (minY + 1 > this.maxSize) continue block0;
                            this.structurePieces.addLast(new ShapedPoolStructurePiece(poolStructurePiece, mutableObject2, minY + 1));
                            continue block0;
                        }
                    }
                }
            }
        }
    }

    static final class ShapedPoolStructurePiece {
        final PoolStructurePiece piece;
        final MutableObject<VoxelShape> pieceShape;
        final int currentSize;

        ShapedPoolStructurePiece(PoolStructurePiece piece, MutableObject<VoxelShape> pieceShape, int currentSize) {
            this.piece = piece;
            this.pieceShape = pieceShape;
            this.currentSize = currentSize;
        }
    }
}


