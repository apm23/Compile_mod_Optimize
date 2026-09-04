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
    void premiumModelGeometryIsLocked() throws IOException {
        String lower = compact(resource("/assets/linkedshulker/models/block/linked_shulker_lower.json"));
        assertTrue(lower.contains("\"from\":[0,2,0],\"to\":[16,8,2]"), "front wall drifted");
        assertTrue(lower.contains("\"from\":[0,2,14],\"to\":[16,8,16]"), "rear wall drifted");
        assertTrue(lower.contains("\"from\":[0,2,2],\"to\":[2,8,14]"), "left wall drifted");
        assertTrue(lower.contains("\"from\":[14,2,2],\"to\":[16,8,14]"), "right wall drifted");
        assertTrue(lower.contains("\"from\":[1.5,2,1.5],\"to\":[14.5,2.6,14.5]"), "hollow End interior drifted");
        assertTrue(lower.contains("\"from\":[0,0,0],\"to\":[1.1,8,1.1]"), "front-left slim gold rail drifted");
        assertTrue(lower.contains("\"from\":[14.9,0,14.9],\"to\":[16,8,16]"), "rear-right slim gold rail drifted");
        assertFalse(lower.contains("-0.45"), "lower front ornament must not protrude outside the block");
        assertTrue(count(lower, "\"texture\":\"#gold\"") >= 24, "gold rails lost dimensional faces");
        assertTrue(count(lower, "\"texture\":\"#crystal2\"") >= 24, "purple End rim lost dimensional faces");
    }

    @Test
    void allEightLidFramesPreserveExactVerticalAnimationAndSlimDetail() throws IOException {
        double[] bodyY = {8.0, 8.7, 9.4, 10.1, 10.8, 11.5, 12.2, 13.0};
        double[] bodyTopY = {16.0, 16.7, 17.4, 18.1, 18.8, 19.5, 20.2, 21.0};
        double[] crystalTopY = {16.55, 17.25, 17.95, 18.65, 19.35, 20.05, 20.75, 21.55};

        for (int frame = 0; frame <= 7; frame++) {
            String lid = compact(resource("/assets/linkedshulker/models/block/linked_shulker_lid_" + frame + ".json"));
            assertTrue(lid.contains("\"from\":[0," + number(bodyY[frame]) + ",0],\"to\":[16," + number(bodyTopY[frame]) + ",16]"), "lid body Y drifted at frame " + frame);
            assertTrue(lid.contains("\"to\":[9.2," + number(crystalTopY[frame]) + ",9.2]"), "top crystal height drifted at frame " + frame);
            assertTrue(lid.contains("\"from\":[0," + number(bodyY[frame] - 0.15) + ",0],\"to\":[1.1," + number(bodyTopY[frame] + 0.15) + ",1.1]"), "slim gold rail drifted at frame " + frame);
            assertFalse(lid.contains("-0.45"), "front ornament protrudes at frame " + frame);
            assertFalse(lid.contains("\"axis\":\"z\""), "old front crystal returned at frame " + frame);
            assertTrue(lid.contains("\"axis\":\"y\",\"angle\":45"), "top crystal facet rotation drifted at frame " + frame);
            assertTrue(lid.contains("\"texture\":\"#inner\""), "inner face missing at frame " + frame);
            assertTrue(count(lid, "\"texture\":\"#gold\"") >= 30, "slim gold rails/top accent lost dimensional faces at frame " + frame);
            assertTrue(count(lid, "\"texture\":\"#crystal2\"") >= 24, "purple End rim lost dimensional faces at frame " + frame);
        }
    }

    @Test
    void premiumTexturePaletteIsCustomAndComplete() throws IOException {
        String textures = compact(resource("/assets/linkedshulker/models/block/linked_shulker_textures.json"));
        assertTrue(textures.contains("\"body\":\"linkedshulker:block/premium_body\""));
        assertTrue(textures.contains("\"gold\":\"minecraft:block/raw_gold_block\""), "gold accents no longer use faded raw-gold palette");
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
        assertTrue(item.contains("\"scale\":[0.58,0.58,0.58]"), "GUI scale drifted");
        assertTrue(item.contains("\"scale\":[0.36,0.36,0.36]"), "first-person scale drifted");
        assertTrue(item.contains("\"scale\":[0.34,0.34,0.34]"), "third-person scale drifted");
    }

    private static String number(double value) {
        double rounded = Math.round(value * 100.0) / 100.0;
        return Double.toString(rounded);
    }

    private static String compact(String text) {
        return text.replace(" ", "").replace("\n", "").replace("\r", "").replace("\t", "");
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
