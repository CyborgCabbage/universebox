package cyborgcabbage.universebox.block.entity;

import cyborgcabbage.universebox.UniverseBox;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.dimension_sync.DimId;

import java.util.UUID;

public class UniverseBoxBlockEntity extends BlockEntity {
    public int pocketIndex = -1;
    public static final String TAG = "PocketIndex";

    public UniverseBoxBlockEntity(BlockPos pos, BlockState state) {
        super(UniverseBox.UNIVERSE_BOX_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        tag.putInt(TAG, pocketIndex);
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        pocketIndex = tag.getInt(TAG);
    }
}
