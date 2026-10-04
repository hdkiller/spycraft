package com.hdkiller.spycraft.network;

import com.hdkiller.spycraft.SpyCraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BinocularScanPayload(int targetId, int dwellTicks, boolean locked) implements CustomPacketPayload {
    public static final Type<BinocularScanPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SpyCraftMod.MOD_ID, "binocular_scan"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BinocularScanPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.targetId());
                buffer.writeVarInt(payload.dwellTicks());
                buffer.writeBoolean(payload.locked());
            },
            buffer -> new BinocularScanPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
