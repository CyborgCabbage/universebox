package cyborgcabbage.universebox.block.entity;

import cyborgcabbage.universebox.UniverseBox;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.nbt.NbtElement.STRING_TYPE;

public class UniverseBoxBlockEntity extends BlockEntity {
    public int pocketIndex = -1;
    public static final String TAG = "PocketIndex";
    private Text customName;

    public UniverseBoxBlockEntity(BlockPos pos, BlockState state) {
        super(UniverseBox.UNIVERSE_BOX_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        tag.putInt(TAG, pocketIndex);
        if (this.customName != null) {
            tag.putString("CustomName", Text.Serializer.toJson(this.customName));
        }

    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        pocketIndex = tag.getInt(TAG);
        if (tag.contains("CustomName", STRING_TYPE)) {
            this.customName = Text.Serializer.fromJson(tag.getString("CustomName"));
        }
    }

    public void setCustomName(Text customName) {
        this.customName = customName;
    }

    public Text getCustomName() {
        return this.customName;
    }

    public boolean hasCustomName() {
        return this.getCustomName() != null;
    }
}
