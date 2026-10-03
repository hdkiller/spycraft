package com.hdkiller.spycraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Spy Base Structure & Mission Verification Tests")
class SpyBaseMissionBuilderTest {

    @Test
    @DisplayName("Verify spybase.json.gz asset is packaged, valid gzip, and matches dimensions")
    void testSpyBaseAssetIntegrity() throws Exception {
        InputStream raw = getClass().getResourceAsStream("/data/spycraft/structures/spybase.json.gz");
        assertNotNull(raw, "Structure file /data/spycraft/structures/spybase.json.gz must exist in resources");

        try (GZIPInputStream gz = new GZIPInputStream(raw);
             InputStreamReader reader = new InputStreamReader(gz, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            assertTrue(json.has("width"));
            assertTrue(json.has("height"));
            assertTrue(json.has("length"));
            assertTrue(json.has("palette"));
            assertTrue(json.has("blocks"));

            assertEquals(103, json.get("width").getAsInt(), "Structure width should be 103 blocks");
            assertEquals(96, json.get("height").getAsInt(), "Structure height should be 96 blocks");
            assertEquals(93, json.get("length").getAsInt(), "Structure length should be 93 blocks");

            assertEquals(56, json.getAsJsonArray("palette").size(), "Palette should contain 56 unique block types");
            assertEquals(67477, json.getAsJsonArray("blocks").size(), "Total block count should be exactly 67,477");
        }
    }
}
