package com.hdkiller.spycraft.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Save-wide laser definitions, stored by Minecraft's normal autosave mechanism. */
public class LaserNetworkSavedData extends SavedData {
    public static final Factory<LaserNetworkSavedData> FACTORY =
            new Factory<>(LaserNetworkSavedData::new, LaserNetworkSavedData::load, null);
    final Map<UUID, LaserForcefieldManager.ForcefieldNetwork> networks = new ConcurrentHashMap<>();

    public static LaserNetworkSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LaserNetworkSavedData data = new LaserNetworkSavedData();
        ListTag traps = tag.getList("traps", Tag.TAG_COMPOUND);
        for (int i = 0; i < traps.size(); i++) {
            CompoundTag entry = traps.getCompound(i);
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("dimension"));
            if (dimension == null || !entry.hasUUID("owner") || !entry.hasUUID("id")) continue;
            UUID owner = entry.getUUID("owner");
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimension);
            var trap = new LaserForcefieldManager.LaserTrap(entry.getUUID("id"), owner, key);
            trap.active = entry.getBoolean("active");
            for (long pos : entry.getLongArray("pylons")) trap.pylons.add(BlockPos.of(pos));
            if (!trap.pylons.isEmpty()) {
                data.networks.computeIfAbsent(owner, LaserForcefieldManager.ForcefieldNetwork::new).traps.add(trap);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag traps = new ListTag();
        for (var network : networks.values()) {
            for (var trap : network.getAllTraps()) {
                if (trap.dimension == null || trap.pylons.isEmpty()) continue;
                CompoundTag entry = new CompoundTag();
                entry.putUUID("owner", trap.owner);
                entry.putUUID("id", trap.id);
                entry.putString("dimension", trap.dimension.location().toString());
                entry.putBoolean("active", trap.active);
                entry.putLongArray("pylons", trap.pylons.stream().mapToLong(BlockPos::asLong).toArray());
                traps.add(entry);
            }
        }
        tag.put("traps", traps);
        return tag;
    }
}
