package cyborgcabbage.universebox.block;

import cyborgcabbage.universebox.UniverseBox;
import cyborgcabbage.universebox.block.entity.UniverseBoxBlockEntity;
import cyborgcabbage.universebox.pocket.PocketState;
import cyborgcabbage.universebox.portal.DependentPortal;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.MessageType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UniverseBoxBlock extends Block implements BlockEntityProvider {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    private static final VoxelShape BLOCK_SHAPE = VoxelShapes.combineAndSimplify(VoxelShapes.fullCube(), Block.createCuboidShape(1.0, 1.0, 1.0, 15.0, 16.0, 15.0), BooleanBiFunction.ONLY_FIRST);
    public UniverseBoxBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
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
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerFacing());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world instanceof ServerWorld serverWorld) {
            UniverseBoxBlockEntity blockEntity = (UniverseBoxBlockEntity)world.getBlockEntity(pos);

            //Create link to pocket dimension
            ServerWorld innerWorld = world.getServer().getWorld(UniverseBox.POCKET_DIMENSION);
            ServerWorld outerWorld = serverWorld;

            RegistryKey<World> innerDimension = UniverseBox.POCKET_DIMENSION;
            RegistryKey<World> outerDimension = world.getRegistryKey();

            //Get pocket dimension id
            if(blockEntity.pocketIndex == -1) {
                NbtCompound itemNbt = itemStack.getNbt();
                if(itemNbt != null) {
                    world.getServer().getPlayerManager().broadcast(new LiteralText(itemNbt.toString()), MessageType.CHAT, Util.NIL_UUID);
                    if (itemNbt.contains(UniverseBoxBlockEntity.TAG)) {
                        blockEntity.pocketIndex = itemNbt.getInt(UniverseBoxBlockEntity.TAG);
                    }
                }
            }else{
                world.getServer().getPlayerManager().broadcast(new LiteralText("PocketIndex: "+blockEntity.pocketIndex), MessageType.CHAT, Util.NIL_UUID);
            }
            if(blockEntity.pocketIndex == -1) {
                PocketState pocketState = innerWorld.getPersistentStateManager().getOrCreate(PocketState::fromNbt, PocketState::new, "pocket_state");
                blockEntity.pocketIndex = pocketState.getAndIncrement();
                UniverseBox.LOGGER.info("ID of new Pocket Dimension is " + blockEntity.pocketIndex);

                blockEntity.markDirty();
            }

            int ipX = blockEntity.pocketIndex*64+8;
            int ipY = 63;
            int ipZ = 8;

            Vec3d outerPortalPos = new Vec3d(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5);
            Vec3d innerPortalPos = new Vec3d(ipX+0.5, ipY+0.5, ipZ+0.5);

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
            outerPortal.setRotationTransformation(new Quaternion(Vec3f.POSITIVE_Y,state.get(FACING).asRotation(),true));
            outerPortal.setup(pos, outerDimension, blockEntity.pocketIndex);
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
            innerPortal.setRotationTransformation(new Quaternion(Vec3f.NEGATIVE_Y,state.get(FACING).asRotation(),true));
            innerPortal.setup(pos, outerDimension, blockEntity.pocketIndex);
            innerPortal.setInteractable(false);
            innerPortal.world.spawnEntity(innerPortal);

            if (itemStack.hasCustomName()) {
                blockEntity.setCustomName(itemStack.getName());
            }
        }
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof UniverseBoxBlockEntity universeBoxBlockEntity) {
            if (!world.isClient){
                if(!player.isCreative() || (player.isCreative() && universeBoxBlockEntity.pocketIndex != -1)) {
                    ItemStack itemStack = new ItemStack(UniverseBox.UNIVERSE_BOX_BLOCK);
                    itemStack.setSubNbt("BlockEntityTag", blockEntity.createNbt());
                    if (universeBoxBlockEntity.hasCustomName()) {
                        itemStack.setCustomName(universeBoxBlockEntity.getCustomName());
                    }
                    ItemEntity itemEntity = new ItemEntity(world, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, itemStack);
                    itemEntity.setToDefaultPickupDelay();
                    world.spawnEntity(itemEntity);
                }
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        super.appendTooltip(stack, world, tooltip, options);
        NbtCompound nbtCompound = BlockItem.getBlockEntityNbt(stack);
        if (nbtCompound != null) {
            if (nbtCompound.contains("PocketIndex")) {
                MutableText mutableText = new LiteralText("#").formatted(Formatting.GRAY);
                mutableText.append(String.valueOf(nbtCompound.getInt("PocketIndex")));
                tooltip.add(mutableText);
                return;
            }
        }
        MutableText mutableText = new LiteralText("Unopened").formatted(Formatting.GRAY);
        tooltip.add(mutableText);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        ItemStack itemStack = super.getPickStack(world, pos, state);
        world.getBlockEntity(pos, UniverseBox.UNIVERSE_BOX_BLOCK_ENTITY).ifPresent(blockEntity -> blockEntity.setStackNbt(itemStack));
        return itemStack;
    }

    @Override
    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.BLOCK;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new UniverseBoxBlockEntity(pos, state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        stateManager.add(FACING);
    }
}
