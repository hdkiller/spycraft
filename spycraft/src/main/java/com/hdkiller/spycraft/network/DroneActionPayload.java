package com.hdkiller.spycraft.network;

import com.hdkiller.spycraft.SpyCraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DroneActionPayload(int mode) implements CustomPacketPayload {
    public static final Type<DroneActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SpyCraftMod.MOD_ID, "drone_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DroneActionPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.mode()),
            buffer -> new DroneActionPayload(buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
