package cyborgcabbage.universebox.portal;

import cyborgcabbage.universebox.UniverseBox;
import cyborgcabbage.universebox.block.entity.UniverseBoxBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.dimension_sync.DimId;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.Optional;

public class DependentPortal extends Portal {
    private int parentPosX;
    private int parentPosY;
    private int parentPosZ;
    private RegistryKey<World> parentDimension;
    private int pocketIndex;

    private boolean dataInvalid = false;

    public DependentPortal(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    public void setParentDimension(RegistryKey<World> dimension){
        parentDimension = dimension;
    }

    public void setParentPos(BlockPos pos){
        parentPosX = pos.getX();
        parentPosY = pos.getY();
        parentPosZ = pos.getZ();
    }

    public void setPocketIndex(int index){
        pocketIndex = index;
    }

    @Override
    public void tick() {
        super.tick();
        if(!world.isClient) {
            //Remove the portal if there is not a corresponding block-entity that has the right pocketIndex
            World parentWorld = world.getServer().getWorld(parentDimension);
            if(parentWorld != null) {
                if (parentWorld.isChunkLoaded(parentPosX >> 4, parentPosZ >> 4)) {
                    Optional<UniverseBoxBlockEntity> optionalBlockEntity = parentWorld.getBlockEntity(new BlockPos(parentPosX, parentPosY, parentPosZ), UniverseBox.UNIVERSE_BOX_BLOCK_ENTITY);
                    if (optionalBlockEntity.isPresent()) {
                        UniverseBoxBlockEntity blockEntity = optionalBlockEntity.get();
                        if (pocketIndex != blockEntity.pocketIndex) {
                            remove(Entity.RemovalReason.KILLED);
                        }
                    }else{
                        remove(Entity.RemovalReason.KILLED);
                    }
                }
            }else{
                remove(Entity.RemovalReason.KILLED);
            }
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound compoundTag) {
        super.readCustomDataFromNbt(compoundTag);
        if(!compoundTag.contains("parentPosX") ) dataInvalid = true;
        if(!compoundTag.contains("parentPosY") ) dataInvalid = true;
        if(!compoundTag.contains("parentPosZ") ) dataInvalid = true;
        if(!compoundTag.contains("parentDimension") ) dataInvalid = true;
        if(!compoundTag.contains("pocketIndex") ) dataInvalid = true;
        parentPosX = compoundTag.getInt("parentPosX");
        parentPosY = compoundTag.getInt("parentPosY");
        parentPosZ = compoundTag.getInt("parentPosZ");
        parentDimension = DimId.getWorldId(compoundTag, "parentDimension", world.isClient);
        pocketIndex = compoundTag.getInt("pocketIndex");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound compoundTag) {
        super.writeCustomDataToNbt(compoundTag);
        if(parentDimension == null) return;
        compoundTag.putInt("parentPosX", parentPosX);
        compoundTag.putInt("parentPosY", parentPosY);
        compoundTag.putInt("parentPosZ", parentPosZ);
        DimId.putWorldId(compoundTag, "parentDimension", parentDimension);
        compoundTag.putInt("pocketIndex", pocketIndex);
    }
}
