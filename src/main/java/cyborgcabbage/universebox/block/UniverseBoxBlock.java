package cyborgcabbage.universebox.block;

import cyborgcabbage.universebox.UniverseBox;
import cyborgcabbage.universebox.block.entity.UniverseBoxBlockEntity;
import cyborgcabbage.universebox.pocket.PocketState;
import cyborgcabbage.universebox.portal.DependentPortal;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.UUID;

public class UniverseBoxBlock extends Block implements BlockEntityProvider {
    private final double PORTAL_VERTICAL_OFFSET = 0.5;

    private static final VoxelShape BLOCK_SHAPE = VoxelShapes.combineAndSimplify(VoxelShapes.fullCube(), Block.createCuboidShape(1.0, 1.0, 1.0, 15.0, 16.0, 15.0), BooleanBiFunction.ONLY_FIRST);
    public UniverseBoxBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return BLOCK_SHAPE;
    }

    @Override
    public int getOpacity(BlockState state, BlockView world, BlockPos pos) {
        return 0;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (world instanceof ServerWorld serverWorld) {
            //Get pocket dimension id
            PocketState pocketState = serverWorld.getPersistentStateManager().getOrCreate(PocketState::fromNbt, PocketState::new, "pocket_state");
            int pocketId = pocketState.getAndIncrement();
            UniverseBox.LOGGER.info("ID of new Pocket Dimension is "+pocketId);
            //Create link to pocket dimension
            World innerWorld = world.getServer().getWorld(UniverseBox.POCKET_DIMENSION);
            World outerWorld = world;

            RegistryKey<World> innerDimension = UniverseBox.POCKET_DIMENSION;
            RegistryKey<World> outerDimension = world.getRegistryKey();

            int ipX = pocketId*16+8;
            int ipY = 64;
            int ipZ = 8;

            Vec3d outerPortalPos = new Vec3d(pos.getX()+0.5, pos.getY()+PORTAL_VERTICAL_OFFSET, pos.getZ()+0.5);
            Vec3d innerPortalPos = new Vec3d(ipX+0.5, ipY+PORTAL_VERTICAL_OFFSET, ipZ+0.5);

            /*System.out.println("INNER PROTAL POSITION");
            System.out.println(new Vec3d(randomX+0.5, 64+PORTAL_VERTICAL_OFFSET, randomZ+0.5));*/

            //Create outer portal
            DependentPortal outerPortal = UniverseBox.DEPENDENT_PORTAL.create(outerWorld);
            outerPortal.setOriginPos(outerPortalPos);
            outerPortal.setDestinationDimension(innerDimension);
            outerPortal.setDestination(innerPortalPos);
            outerPortal.setOrientationAndSize(
                    new Vec3d(0, 0, 1), // axisW
                    new Vec3d(1, 0, 0), // axisH
                    7.0/8.0, // width
                    7.0/8.0 // height
            );
            outerPortal.setParentPos(pos);
            outerPortal.setParentDimension(outerDimension);
            outerPortal.world.spawnEntity(outerPortal);

            //Create inner portal
            DependentPortal innerPortal = UniverseBox.DEPENDENT_PORTAL.create(innerWorld);
            innerPortal.setOriginPos(innerPortalPos);
            innerPortal.setDestinationDimension(outerDimension);
            innerPortal.setDestination(outerPortalPos);
            innerPortal.setOrientationAndSize(
                    new Vec3d(1, 0, 0), // axisW
                    new Vec3d(0, 0, 1), // axisH
                    7.0/8.0, // width
                    7.0/8.0 // height
            );
            innerPortal.setParentPos(pos);
            innerPortal.setParentDimension(outerDimension);
            innerPortal.world.spawnEntity(innerPortal);

            //Store portal UUID
            UniverseBoxBlockEntity blockEntity = (UniverseBoxBlockEntity)world.getBlockEntity(pos);
            blockEntity.outerPortalUuid = outerPortal.getUuid();
            blockEntity.innerPortalUuid = innerPortal.getUuid();
            blockEntity.outerDimension = outerDimension;
            blockEntity.innerDimension = innerDimension;
            blockEntity.markDirty();
        }
    }

    private void removePortal(World world, RegistryKey<World> dimension, UUID portalUuid){
        if(dimension != null && portalUuid != null) {
            UniverseBox.LOGGER.warn("Removing Portal");
            UniverseBox.LOGGER.warn("portalUuid: "+portalUuid);
            UniverseBox.LOGGER.warn("dimension: "+dimension);
            ServerWorld outerWorld = world.getServer().getWorld(dimension);
            Entity entity = outerWorld.getEntity(portalUuid);
            if(entity != null) entity.remove(Entity.RemovalReason.KILLED);
            else UniverseBox.LOGGER.warn("UniverseBoxBlock could not remove portal as the entity could not be found");
        }else{
            UniverseBox.LOGGER.warn("UniverseBoxBlock could not remove portal as NBT was null");
        }
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);
        if(world instanceof ServerWorld serverWorld) {
            //Remove portals
            UniverseBoxBlockEntity blockEntity = (UniverseBoxBlockEntity)world.getBlockEntity(pos);
            //removePortal(world, blockEntity.innerDimension, blockEntity.innerPortalUuid);
            //removePortal(world, blockEntity.outerDimension, blockEntity.outerPortalUuid);
        }
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new UniverseBoxBlockEntity(pos, state);
    }
}
