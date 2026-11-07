package com.evandouard.hxh.marker;

import com.evandouard.hxh.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ce que porte un repère réglable : le PNJ choisi (« npc ») ou le numéro d'un point de parcours (« index »).
 * Ces données sont sauvegardées avec la structure.
 */
public class MarkerBlockEntity extends BlockEntity {
    private String npc = "";
    private int index = 1;

    public MarkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MARKER.get(), pos, state);
    }

    public String npc() {
        return npc;
    }

    public void setNpc(String id) {
        npc = id;
        changed();
    }

    /** Réglage initial, sans prévenir le monde (le bloc n'y est pas encore). */
    void setNpcSilently(String id) {
        npc = id;
    }

    public int index() {
        return index;
    }

    public void setIndex(int value) {
        index = Math.max(1, Math.min(99, value));
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("npc", npc);
        tag.putInt("index", index);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        npc = tag.getString("npc");
        index = tag.contains("index") ? tag.getInt("index") : 1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
