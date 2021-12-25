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
    public UUID outerPortalUuid;
    public UUID innerPortalUuid;
    public RegistryKey<World> outerDimension;
    public RegistryKey<World> innerDimension;
    public int pocketIndex;

    public UniverseBoxBlockEntity(BlockPos pos, BlockState state) {
        super(UniverseBox.UNIVERSE_BOX_BLOCK_ENTITY, pos, state);
    }

    // Serialize the BlockEntity
    @Override
    public void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        boolean anyNull = false;
        if(outerPortalUuid == null) anyNull = true;
        if(innerPortalUuid == null) anyNull = true;
        if(outerDimension == null) anyNull = true;
        if(innerDimension == null) anyNull = true;
        if(anyNull){
            UniverseBox.LOGGER.warn("Some fields of UniverseBoxBlockEntity were null, so writeNbt was cancelled");
            return;
        }
        // Save the current value of the number to the tag
        tag.putUuid("OuterPortalUUID", outerPortalUuid);
        tag.putUuid("InnerPortalUUID", innerPortalUuid);
        DimId.putWorldId(tag,"OuterDimension", outerDimension);
        DimId.putWorldId(tag,"InnerDimension", innerDimension);
        tag.putInt("PocketIndex", pocketIndex);
    }

    // Deserialize the BlockEntity
    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        outerPortalUuid = tag.getUuid("OuterPortalUUID");
        innerPortalUuid = tag.getUuid("InnerPortalUUID");
        outerDimension = DimId.getWorldId(tag,"OuterDimension", false);
        innerDimension = DimId.getWorldId(tag,"InnerDimension", false);
        pocketIndex = tag.getInt("PocketIndex");
    }
}
