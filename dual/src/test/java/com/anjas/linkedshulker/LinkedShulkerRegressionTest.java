package com.anjas.linkedshulker;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

final class LinkedShulkerRegressionTest {
    @Test
    void channelNamesAreExactAndCaseSensitive() {
        assertEquals("Share", ChannelStorageData.normalize("Share"));
        assertEquals("ShAre", ChannelStorageData.normalize("ShAre"));
        assertNotEquals(ChannelStorageData.normalize("Share"), ChannelStorageData.normalize("ShAre"));
        assertEquals("default", ChannelStorageData.normalize("   "));
    }

    @Test
    void differentlyCapitalizedNamesUseDifferentInventories() {
        ChannelStorageData data = new ChannelStorageData();
        assertNotSame(data.inventory("Share"), data.inventory("ShAre"));
        assertSame(data.inventory("Share"), data.inventory("Share"));
    }

    @Test
    void allEightOpeningFramesExistAndFullOpenHasPremiumInterior() throws IOException {
        for (int frame = 0; frame <= 7; frame++) {
            String path = "/assets/linkedshulker/models/block/linked_shulker_box_open_" + frame + ".json";
            assertFalse(resource(path).isBlank(), "missing frame " + frame);
        }
        String fullOpen = resource("/assets/linkedshulker/models/block/linked_shulker_box_open_7.json");
        assertTrue(fullOpen.contains("\"elements\""));
        assertTrue(count(fullOpen, "\"from\"") >= 8, "full-open model lost 3D interior detail");
        assertFalse(fullOpen.contains("\"rotation\""), "open model unexpectedly contains hinge rotation");
    }

    @Test
    void itemTransformsStayCompactLikeNormalBlocks() throws IOException {
        String item = resource("/assets/linkedshulker/models/item/linked_shulker_box.json").replace(" ", "");
        assertTrue(item.contains("\"scale\":[0.58,0.58,0.58]"), "GUI scale drifted");
        assertTrue(item.contains("\"scale\":[0.36,0.36,0.36]"), "first-person scale drifted");
        assertTrue(item.contains("\"scale\":[0.34,0.34,0.34]"), "third-person scale drifted");
        assertTrue(item.contains("firstperson_righthand"));
        assertTrue(item.contains("thirdperson_righthand"));
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = 0; (i = text.indexOf(needle, i)) >= 0; i += needle.length()) n++;
        return n;
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = LinkedShulkerRegressionTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
