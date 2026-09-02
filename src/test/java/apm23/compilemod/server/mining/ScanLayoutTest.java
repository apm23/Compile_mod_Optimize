package com.apm23.custompickaxe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import org.junit.jupiter.api.Test;

final class ScanLayoutTest {
    @Test
    void coversEveryPositionExactlyOnceForAllLevels() {
        verifySide(8); verifySide(16); verifySide(64); verifySide(128);
    }

    private static void verifySide(int side) {
        int total = ScanLayout.totalPositions(side);
        int half = side / 2;
        BitSet seen = new BitSet(total);
        for (int index = 0; index < total; index++) {
            int x = ScanLayout.offsetX(index, side);
            int y = ScanLayout.offsetY(index, side);
            int z = ScanLayout.offsetZ(index, side);
            assertTrue(x >= -half && x <= half - 1);
            assertTrue(y >= -half && y <= half - 1);
            assertTrue(z >= -half && z <= half - 1);
            int normalized = (x + half) + (z + half) * side + (y + half) * side * side;
            assertTrue(!seen.get(normalized));
            seen.set(normalized);
        }
        assertEquals(total, seen.cardinality());
        assertEquals(-half, ScanLayout.offsetX(0, side));
        assertEquals(-half, ScanLayout.offsetY(0, side));
        assertEquals(-half, ScanLayout.offsetZ(0, side));
        assertEquals(half - 1, ScanLayout.offsetX(total - 1, side));
        assertEquals(half - 1, ScanLayout.offsetY(total - 1, side));
        assertEquals(half - 1, ScanLayout.offsetZ(total - 1, side));
    }
}
