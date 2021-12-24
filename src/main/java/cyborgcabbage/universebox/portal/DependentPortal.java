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

    @Override
    public void tick() {
        super.tick();
        if(!world.isClient) {
            if(dataInvalid) {
                /*UniverseBox.LOGGER.warn("Removing Portal (Self)");
                UniverseBox.LOGGER.warn("parentPosX: "+parentPosX);
                UniverseBox.LOGGER.warn("parentPosY: "+parentPosY);
                UniverseBox.LOGGER.warn("parentPosZ: "+parentPosZ);
                UniverseBox.LOGGER.warn("parentDimension: "+parentDimension);
                UniverseBox.LOGGER.warn("Data invalid, portal was removed");*/
                remove(Entity.RemovalReason.KILLED);
            }
            //Remove the portal if there is not a corresponding block-entity that has its UUID
            World parentWorld = world.getServer().getWorld(parentDimension);
            if(parentWorld != null) {
                if (parentWorld.isChunkLoaded(parentPosX >> 4, parentPosZ >> 4)) {
                    Optional<UniverseBoxBlockEntity> optionalBlockEntity = parentWorld.getBlockEntity(new BlockPos(parentPosX, parentPosY, parentPosZ), UniverseBox.UNIVERSE_BOX_BLOCK_ENTITY);
                    if (optionalBlockEntity.isPresent()) {
                        UniverseBoxBlockEntity blockEntity = optionalBlockEntity.get();
                        if (!blockEntity.outerPortalUuid.equals(getUuid()) && !blockEntity.innerPortalUuid.equals(getUuid())) {
                            //UniverseBox.LOGGER.warn("No parent found at " + parentPosX + " " + parentPosY + " " + parentPosZ + " in " + parentDimension + " so the portal was removed");
                            //remove(Entity.RemovalReason.KILLED);
                            /*System.out.println("blockEntity.outerPortalUuid "+blockEntity.outerPortalUuid);
                            System.out.println("blockEntity.innerPortalUuid "+blockEntity.innerPortalUuid);
                            System.out.println("this.uuid                   "+getUuid());
                            System.out.println(getUuid().equals(blockEntity.outerPortalUuid));
                            System.out.println(getUuid().equals(blockEntity.innerPortalUuid));*/
                            dataInvalid = true;
                        }
                    }else{
                        //UniverseBox.LOGGER.warn("No parent found at " + parentPosX + " " + parentPosY + " " + parentPosZ + " in " + parentDimension + " so the portal was removed");
                        //remove(Entity.RemovalReason.KILLED);
                        //System.out.println("No block-entity");
                        dataInvalid = true;
                    }
                }
            }else{
                //UniverseBox.LOGGER.warn("World is null so the portal was removed");
                //remove(Entity.RemovalReason.KILLED);
                //System.out.println("No world");
                dataInvalid = true;
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
        parentPosX = compoundTag.getInt("parentPosX");
        parentPosY = compoundTag.getInt("parentPosY");
        parentPosZ = compoundTag.getInt("parentPosZ");
        parentDimension = DimId.getWorldId(compoundTag, "parentDimension", world.isClient);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound compoundTag) {
        super.writeCustomDataToNbt(compoundTag);
        if(parentDimension == null) return;
        compoundTag.putInt("parentPosX", parentPosX);
        compoundTag.putInt("parentPosY", parentPosY);
        compoundTag.putInt("parentPosZ", parentPosZ);
        DimId.putWorldId(compoundTag, "parentDimension", parentDimension);
    }
}
