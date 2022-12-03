package cyborgcabbage.universebox.block;

import cyborgcabbage.universebox.UniverseBox;
import cyborgcabbage.universebox.block.entity.UniverseBoxBlockEntity;
import cyborgcabbage.universebox.pocket.PocketState;
import cyborgcabbage.universebox.portal.DependentPortal;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.MessageType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UniverseBoxBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final BooleanProperty OPEN = Properties.OPEN;
    private static final VoxelShape BLOCK_SHAPE = VoxelShapes.combineAndSimplify(VoxelShapes.fullCube(), Block.createCuboidShape(1.0, 1.0, 1.0, 15.0, 16.0, 15.0), BooleanBiFunction.ONLY_FIRST);
    public UniverseBoxBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if(state.get(OPEN)) {
            return BLOCK_SHAPE;
        }else{
            return VoxelShapes.fullCube();
        }
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(world.isClient) return ActionResult.SUCCESS;
        UniverseBoxBlockEntity blockEntity = (UniverseBoxBlockEntity) world.getBlockEntity(pos);
        if(blockEntity == null) return ActionResult.CONSUME;
        if(state.get(OPEN)){
            BlockPos portalPosition = getPortalPosition(blockEntity.pocketIndex);
            Box box = Box.from(Vec3d.of(portalPosition)).withMinY(0).expand(31,0,31);
            List<PlayerEntity> players = world.getServer().getWorld(UniverseBox.POCKET_DIMENSION).getEntitiesByClass(PlayerEntity.class, box, p -> p.getUuid() == player.getUuid());
            boolean intersectsBlock = player.getBoundingBox().intersects(Box.from(Vec3d.of(pos)));
            if(!players.isEmpty() || intersectsBlock){
                player.sendMessage(new TranslatableText("block.universebox.universe_box.cannot_close"), true);
                world.playSound(null, pos, SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1.f, 1.f);
                return ActionResult.CONSUME;
            }
        }
        state = state.cycle(OPEN);
        boolean open = state.get(OPEN);
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD);
        world.playSound(null, pos, open ? SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN : SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 1.f, 1.f);
        world.emitGameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        if(open){
            openBox(world, pos, state, world, blockEntity);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world instanceof ServerWorld serverWorld) {
            UniverseBoxBlockEntity blockEntity = (UniverseBoxBlockEntity)world.getBlockEntity(pos);
            if(blockEntity == null) return;
            if(state.get(OPEN)) {
                openBox(world, pos, state, serverWorld, blockEntity);
            }

            if (itemStack.hasCustomName()) {
                blockEntity.setCustomName(itemStack.getName());
            }
        }
    }

    private void openBox(World world, BlockPos pos, BlockState state, World serverWorld, UniverseBoxBlockEntity blockEntity) {
        //Generate pocket dimension id
        if (blockEntity.pocketIndex == -1) {
            PocketState pocketState = world.getServer().getWorld(UniverseBox.POCKET_DIMENSION).getPersistentStateManager().getOrCreate(PocketState::fromNbt, PocketState::new, "pocket_state");
            blockEntity.pocketIndex = pocketState.getAndIncrement();
            blockEntity.markDirty();
        }
        createPortals(pos, state.get(FACING), blockEntity.pocketIndex, serverWorld);
    }

    private void createPortals(BlockPos outerPos, Direction rotation, int pocketIndex, World outerWorld) {
        //Create link to pocket dimension
        ServerWorld innerWorld = outerWorld.getServer().getWorld(UniverseBox.POCKET_DIMENSION);

        RegistryKey<World> innerDimension = UniverseBox.POCKET_DIMENSION;
        RegistryKey<World> outerDimension = outerWorld.getRegistryKey();

        BlockPos innerPos = getPortalPosition(pocketIndex);

        //Create portals
        Vec3d outerPortalPos = new Vec3d(outerPos.getX()+0.5, outerPos.getY()+0.5, outerPos.getZ()+0.5);
        Vec3d innerPortalPos = new Vec3d(innerPos.getX()+0.5, innerPos.getY()+0.5, innerPos.getZ()+0.5);

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
        outerPortal.setRotationTransformation(new Quaternion(Vec3f.POSITIVE_Y, rotation.asRotation(),true));
        outerPortal.setup(outerPos, outerDimension, pocketIndex);
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
        innerPortal.setRotationTransformation(new Quaternion(Vec3f.NEGATIVE_Y, rotation.asRotation(),true));
        innerPortal.setup(outerPos, outerDimension, pocketIndex);
        innerPortal.setInteractable(false);
        innerPortal.world.spawnEntity(innerPortal);
    }

    private static BlockPos getPortalPosition(int pocketIndex) {
        int x = 0;
        int z = 0;
        if(pocketIndex != 0) {
            int rIndex = (int) Math.ceil(Math.floor(Math.sqrt(pocketIndex)) / 2.0);
            int rSize = rIndex * 2 + 1;
            int rArea = rIndex * 8;
            int rLess = (rSize - 2) * (rSize - 2);
            int rOffset = pocketIndex - rLess;
            int rSide = rOffset / (rArea/4);
            int rSideOffset = rOffset % (rArea/4);
            switch(rSide){
                case 0 -> {
                    x = rIndex;
                    z = rSideOffset-rIndex;
                }
                case 1 -> {
                    z = rIndex;
                    x = rIndex-rSideOffset;
                }
                case 2 -> {
                    x = -rIndex;
                    z = rIndex-rSideOffset;
                }
                case 3 -> {
                    z = -rIndex;
                    x = rSideOffset-rIndex;
                }
            }
        }

        return new BlockPos(8+x*64, 63, 8+z*64);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof UniverseBoxBlockEntity universeBoxBlockEntity) {
            if (!world.isClient){
                if(!player.isCreative() || (player.isCreative() && universeBoxBlockEntity.pocketIndex != -1)) {
                    ItemStack itemStack = new ItemStack(UniverseBox.UNIVERSE_BOX_BLOCK);
                    if(universeBoxBlockEntity.pocketIndex != -1)
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
        stateManager.add(FACING, OPEN);
    }
}
