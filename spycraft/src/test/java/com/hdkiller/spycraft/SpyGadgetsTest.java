package com.hdkiller.spycraft;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("5 New Spy Gadgets Verification Tests")
class SpyGadgetsTest {

    @Test
    @DisplayName("Verify gadget textures exist in assets")
    void testGadgetTexturesExist() {
        String[] textures = {
            "/assets/spycraft/textures/item/thermal_goggles.png",
            "/assets/spycraft/textures/item/hologram_projector.png",
            "/assets/spycraft/textures/item/tranquilizer_gun.png",
            "/assets/spycraft/textures/item/smoke_grenade.png",
            "/assets/spycraft/textures/item/climbing_gloves.png",
            "/assets/spycraft/textures/item/parachute_backpack.png",
            "/assets/spycraft/textures/item/binoculars.png"
        };

        for (String tex : textures) {
            InputStream is = getClass().getResourceAsStream(tex);
            assertNotNull(is, "Texture must exist: " + tex);
            try {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(is);
                assertNotNull(img, "Texture must be decodable image: " + tex);
                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        int argb = img.getRGB(x, y);
                        int a = (argb >> 24) & 0xFF;
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;
                        if (a > 0) {
                            assertFalse(r < 20 && g == 255 && b < 50,
                                "Corrupted fluorescent green pixel found at (" + x + "," + y + ") in " + tex);
                        }
                    }
                }
            } catch (Exception e) {
                fail("Failed to read texture: " + tex + " - " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("Verify gadget model definitions exist in assets")
    void testGadgetModelJsonsExist() {
        String[] models = {
            "/assets/spycraft/models/item/thermal_goggles.json",
            "/assets/spycraft/models/item/hologram_projector.json",
            "/assets/spycraft/models/item/tranquilizer_gun.json",
            "/assets/spycraft/models/item/smoke_grenade.json",
            "/assets/spycraft/models/item/climbing_gloves.json",
            "/assets/spycraft/models/item/parachute_backpack.json",
            "/assets/spycraft/models/item/binoculars.json"
        };

        for (String model : models) {
            InputStream is = getClass().getResourceAsStream(model);
            assertNotNull(is, "Model JSON must exist: " + model);
        }
    }

    @Test
    @DisplayName("Verify gadget localization keys exist in en_us.json")
    void testGadgetLocalization() throws Exception {
        InputStream is = getClass().getResourceAsStream("/assets/spycraft/lang/en_us.json");
        assertNotNull(is, "en_us.json must exist");
        String langContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(langContent.contains("item.spycraft.thermal_goggles"), "Thermal goggles translation missing");
        assertTrue(langContent.contains("item.spycraft.hologram_projector"), "Hologram projector translation missing");
        assertTrue(langContent.contains("item.spycraft.tranquilizer_gun"), "Tranquilizer gun translation missing");
        assertTrue(langContent.contains("item.spycraft.smoke_grenade"), "Smoke grenade translation missing");
        assertTrue(langContent.contains("item.spycraft.climbing_gloves"), "Climbing gloves translation missing");
        assertTrue(langContent.contains("item.spycraft.parachute_backpack"), "Parachute backpack translation missing");
        assertTrue(langContent.contains("item.spycraft.binoculars"), "Binoculars translation missing");
    }

    @Test
    @DisplayName("Verify gadget localization keys exist in hu_hu.json")
    void testHungarianLocalization() throws Exception {
        InputStream is = getClass().getResourceAsStream("/assets/spycraft/lang/hu_hu.json");
        assertNotNull(is, "hu_hu.json must exist");
        String langContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(langContent.contains("item.spycraft.thermal_goggles"), "Thermal goggles Hungarian translation missing");
        assertTrue(langContent.contains("item.spycraft.hologram_projector"), "Hologram projector Hungarian translation missing");
        assertTrue(langContent.contains("item.spycraft.tranquilizer_gun"), "Tranquilizer gun Hungarian translation missing");
        assertTrue(langContent.contains("item.spycraft.smoke_grenade"), "Smoke grenade Hungarian translation missing");
        assertTrue(langContent.contains("item.spycraft.climbing_gloves"), "Climbing gloves Hungarian translation missing");
        assertTrue(langContent.contains("item.spycraft.parachute_backpack"), "Parachute backpack Hungarian translation missing");
        assertTrue(langContent.contains("item.spycraft.binoculars"), "Binoculars Hungarian translation missing");
    }
}
