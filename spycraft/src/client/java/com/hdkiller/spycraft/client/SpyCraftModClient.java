package com.hdkiller.spycraft.client;

import net.fabricmc.api.ClientModInitializer;

public class SpyCraftModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.hdkiller.spycraft.network.BinocularScanPayload.TYPE,
                (payload, context) -> BinocularScanState.update(payload));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> BinocularScanState.clear());
    }
}
