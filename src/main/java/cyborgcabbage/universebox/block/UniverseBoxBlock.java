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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world instanceof ServerWorld serverWorld) {
            UniverseBoxBlockEntity blockEntity = (UniverseBoxBlockEntity)world.getBlockEntity(pos);
            //Get pocket dimension id
            if(blockEntity.pocketIndex == -1) {
                PocketState pocketState = serverWorld.getPersistentStateManager().getOrCreate(PocketState::fromNbt, PocketState::new, "pocket_state");
                blockEntity.pocketIndex = pocketState.getAndIncrement();
                UniverseBox.LOGGER.info("ID of new Pocket Dimension is " + blockEntity.pocketIndex);

                blockEntity.markDirty();
            }
            //Create link to pocket dimension
            World innerWorld = world.getServer().getWorld(UniverseBox.POCKET_DIMENSION);
            World outerWorld = world;

            RegistryKey<World> innerDimension = UniverseBox.POCKET_DIMENSION;
            RegistryKey<World> outerDimension = world.getRegistryKey();

            int ipX = blockEntity.pocketIndex*16+8;
            int ipY = 64;
            int ipZ = 8;

            Vec3d outerPortalPos = new Vec3d(pos.getX()+0.5, pos.getY()+PORTAL_VERTICAL_OFFSET, pos.getZ()+0.5);
            Vec3d innerPortalPos = new Vec3d(ipX+0.5, ipY+PORTAL_VERTICAL_OFFSET, ipZ+0.5);

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
            outerPortal.setPocketIndex(blockEntity.pocketIndex);
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
            innerPortal.setPocketIndex(blockEntity.pocketIndex);
            innerPortal.world.spawnEntity(innerPortal);
        }
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new UniverseBoxBlockEntity(pos, state);
    }
}
