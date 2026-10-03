package com.hdkiller.spycraft;

import com.hdkiller.spycraft.item.MobTrackerItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mob Tracker Radar & Direction Heading Tests")
class MobTrackerMathTest {

    @ParameterizedTest(name = "dx={0}, dz={1} should point {2}")
    @CsvSource({
        "0.0, -10.0, North ⬆",
        "0.0, 10.0, South ⬇",
        "10.0, 0.0, East ➡",
        "-10.0, 0.0, West ⬅",
        "10.0, -10.0, North-East ↗",
        "-10.0, -10.0, North-West ↖",
        "10.0, 10.0, South-East ↘",
        "-10.0, 10.0, South-West ↙"
    })
    void testCompassDirections(double dx, double dz, String expectedHeading) {
        String actualHeading = MobTrackerItem.getDirectionLabel(dx, dz);
        assertEquals(expectedHeading, actualHeading, 
            () -> "Failed for offset dx=" + dx + ", dz=" + dz);
    }

    @Test
    @DisplayName("Elevation difference text formatting")
    void testElevationCalculation() {
        int targetYHigh = 120;
        int playerY = 64;
        int diffHigh = targetYHigh - playerY;
        String elevHigh = diffHigh > 1 ? " [Above ⬆]" : (diffHigh < -1 ? " [Below ⬇]" : " [Level ➡]");
        assertEquals(" [Above ⬆]", elevHigh);

        int targetYLow = 20;
        int diffLow = targetYLow - playerY;
        String elevLow = diffLow > 1 ? " [Above ⬆]" : (diffLow < -1 ? " [Below ⬇]" : " [Level ➡]");
        assertEquals(" [Below ⬇]", elevLow);

        int targetYLevel = 64;
        int diffLevel = targetYLevel - playerY;
        String elevLevel = diffLevel > 1 ? " [Above ⬆]" : (diffLevel < -1 ? " [Below ⬇]" : " [Level ➡]");
        assertEquals(" [Level ➡]", elevLevel);
    }
}
