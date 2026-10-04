package com.hdkiller.spycraft;

import com.hdkiller.spycraft.network.BinocularScanPayload;
import com.hdkiller.spycraft.network.DroneActionPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkPayloadTest {
    @Test
    void droneActionRoundTripsAllFourModes() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            for (int mode = 0; mode < 4; mode++) {
                var payload = new DroneActionPayload(mode);
                DroneActionPayload.CODEC.encode(buffer, payload);
                assertEquals(payload, DroneActionPayload.CODEC.decode(buffer));
            }
        } finally {
            buffer.release();
        }
    }

    @Test
    void scanPayloadRoundTripsProgressLockAndReset() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            for (var payload : new BinocularScanPayload[] {
                    new BinocularScanPayload(123, 12, false),
                    new BinocularScanPayload(123, 24, true),
                    new BinocularScanPayload(-1, 0, false)}) {
                BinocularScanPayload.CODEC.encode(buffer, payload);
                assertEquals(payload, BinocularScanPayload.CODEC.decode(buffer));
            }
        } finally {
            buffer.release();
        }
    }
}
