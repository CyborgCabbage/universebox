package cyborgcabbage.universebox.pocket;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

public class PocketState extends PersistentState {
    public int pocketsUsed;

    public PocketState(int pocketsUsed) {
        this.pocketsUsed = pocketsUsed;
    }

    public PocketState() {
        this(0);
    }

    public static PocketState fromNbt(NbtCompound nbt) {
        return new PocketState(nbt.getInt("pocketsUsed"));
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("pocketsUsed", pocketsUsed);
        return nbt;
    }

    public int getAndIncrement(){
        pocketsUsed += 1;
        markDirty();
        return pocketsUsed-1;
    }
}
