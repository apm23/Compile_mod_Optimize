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
    void lowerShellUsesZeroOverlapDecorationGeometry() throws IOException {
        String lower = compact(resource("/assets/linkedshulker/models/block/linked_shulker_lower.json"));
        assertTrue(lower.contains("\"from\":[0,2,0],\"to\":[16,8,2]"), "front wall drifted");
        assertTrue(lower.contains("\"from\":[0,2,14],\"to\":[16,8,16]"), "rear wall drifted");
        assertTrue(lower.contains("\"from\":[0,2,2],\"to\":[2,8,14]"), "left wall drifted");
        assertTrue(lower.contains("\"from\":[14,2,2],\"to\":[16,8,14]"), "right wall drifted");
        assertTrue(lower.contains("\"from\":[2.05,2.02,2.05],\"to\":[13.95,2.5,13.95]"), "inner floor must stay clear of side walls");

        // Decorative plates must sit outside the solid body, never coplanar/inset into it.
        assertTrue(lower.contains("\"from\":[2,7.35,-0.04],\"to\":[14,7.85,-0.01]"), "north purple rim lost depth separation");
        assertTrue(lower.contains("\"from\":[-0.04,7.35,2],\"to\":[-0.01,7.85,14]"), "west purple rim lost depth separation");
        assertTrue(lower.contains("\"from\":[-0.05,0,0],\"to\":[-0.01,8,1.1]"), "gold corner plate moved back into body");
        assertTrue(lower.contains("\"from\":[0,0,-0.05],\"to\":[1.1,8,-0.01]"), "gold corner return plate moved back into body");
        assertFalse(lower.contains("\"from\":[0,0,0],\"to\":[1.1,8,1.1]"), "old overlapping gold cuboid returned");
        assertFalse(lower.contains("\"from\":[2,7.35,0],\"to\":[14,7.85,0.7]"), "old overlapping purple rim returned");
    }

    @Test
    void stableClosedAndOpenLidsAreDepthSeparated() throws IOException {
        assertStableLid(0, 8.0, 16.0, 7.94, 7.99, 16.01, 16.12, 16.55);
        assertStableLid(7, 13.0, 21.0, 12.94, 12.99, 21.01, 21.12, 21.55);
    }

    private static void assertStableLid(int frame, double bodyY, double bodyTop, double innerBottom, double innerTop,
                                        double topGoldBottom, double crystalBottom, double crystalTop) throws IOException {
        String lid = compact(resource("/assets/linkedshulker/models/block/linked_shulker_lid_" + frame + ".json"));
        assertTrue(lid.contains("\"from\":[0," + number(bodyY) + ",0],\"to\":[16," + number(bodyTop) + ",16]"), "lid body drifted at frame " + frame);
        assertTrue(lid.contains("\"from\":[1," + number(innerBottom) + ",1],\"to\":[15," + number(innerTop) + ",15]"), "underside overlaps lid body at frame " + frame);
        assertTrue(lid.contains("\"from\":[2," + number(bodyY + 0.2) + ",-0.04],\"to\":[14," + number(bodyY + 0.7) + ",-0.01]"), "purple rim is not outside body at frame " + frame);
        assertTrue(lid.contains("\"from\":[-0.05," + number(bodyY - 0.15) + ",0],\"to\":[-0.01," + number(bodyTop + 0.15) + ",1.1]"), "gold plate is not outside body at frame " + frame);
        assertTrue(lid.contains("\"from\":[6.2," + number(topGoldBottom) + ",6.2],\"to\":[9.8," + number(bodyTop + 0.1) + ",9.8]"), "top gold plate overlaps body at frame " + frame);
        assertTrue(lid.contains("\"from\":[6.8," + number(crystalBottom) + ",6.8],\"to\":[9.2," + number(crystalTop) + ",9.2]"), "top crystal overlap/drift at frame " + frame);
        assertFalse(lid.contains("\"from\":[0," + number(bodyY - 0.15) + ",0],\"to\":[1.1," + number(bodyTop + 0.15) + ",1.1]"), "old overlapping gold cuboid returned at frame " + frame);
    }

    @Test
    void premiumTexturePaletteIsCustomAndComplete() throws IOException {
        String textures = compact(resource("/assets/linkedshulker/models/block/linked_shulker_textures.json"));
        assertTrue(textures.contains("\"body\":\"linkedshulker:block/premium_body\""));
        assertTrue(textures.contains("\"gold\":\"minecraft:block/raw_gold_block\""));
        assertTrue(textures.contains("\"crystal\":\"linkedshulker:block/premium_crystal\""));
        assertTrue(textures.contains("\"crystal2\":\"linkedshulker:block/premium_crystal\""));
        assertTrue(textures.contains("\"inner\":\"linkedshulker:block/premium_inner\""));
        assertNotNull(LinkedShulkerRegressionTest.class.getResourceAsStream("/assets/linkedshulker/textures/block/premium_body.png"));
        assertNotNull(LinkedShulkerRegressionTest.class.getResourceAsStream("/assets/linkedshulker/textures/block/premium_crystal.png"));
        assertNotNull(LinkedShulkerRegressionTest.class.getResourceAsStream("/assets/linkedshulker/textures/block/premium_inner.png"));
    }

    @Test
    void recipeAndHeldScaleStayExactlyLocked() throws IOException {
        String recipe = compact(resource("/data/linkedshulker/recipe/linked_shulker_box.json"));
        assertTrue(recipe.contains("\"pattern\":[\"ECE\"]"));
        assertTrue(recipe.contains("\"E\":\"minecraft:ender_pearl\""));
        assertTrue(recipe.contains("\"C\":\"minecraft:chest\""));

        String item = compact(resource("/assets/linkedshulker/models/item/linked_shulker_box.json"));
        assertTrue(item.contains("\"scale\":[0.58,0.58,0.58]"));
        assertTrue(item.contains("\"scale\":[0.36,0.36,0.36]"));
        assertTrue(item.contains("\"scale\":[0.34,0.34,0.34]"));
    }

    private static String number(double value) {
        double rounded = Math.round(value * 100.0) / 100.0;
        return Double.toString(rounded);
    }

    private static String compact(String text) {
        return text.replace(" ", "").replace("\n", "").replace("\r", "").replace("\t", "");
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = LinkedShulkerRegressionTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
