package com.apm23.custompickaxe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class RecipeRegressionTest {
    private static final String BASE = "data/custom_pickaxe/recipe/";
    @Test void allTenRecipesStayDisguisedAndKeepExpectedIngredients() throws IOException {
        Map<String,List<String>> expected = new LinkedHashMap<>();
        expected.put("iron",List.of("minecraft:raw_iron","minecraft:stick")); expected.put("copper",List.of("minecraft:raw_copper","minecraft:stick")); expected.put("gold",List.of("minecraft:raw_gold","minecraft:stick")); expected.put("diamond",List.of("minecraft:diamond","minecraft:stick")); expected.put("emerald",List.of("minecraft:emerald","minecraft:stick")); expected.put("coal",List.of("minecraft:coal","minecraft:stick")); expected.put("lapis",List.of("minecraft:lapis_lazuli","minecraft:stick")); expected.put("redstone",List.of("minecraft:redstone","minecraft:stick")); expected.put("debris",List.of("minecraft:gravel","minecraft:flint","minecraft:stick")); expected.put("amethyst",List.of("minecraft:amethyst_shard","minecraft:stick"));
        for (var entry: expected.entrySet()) { String type=entry.getKey(); String json=compact(read(BASE+"remote_"+type+"_pickaxe.json")); assertTrue(json.contains("\"id\":\"minecraft:iron_pickaxe\"")); assertTrue(json.contains("\"custom_pickaxe\":{\"type\":\""+type+"\",\"enabled\":true,\"version\":1}")); assertTrue(json.contains("\"show_notification\":false")); for(String ingredient:entry.getValue()) assertTrue(json.contains("\""+ingredient+"\"")); }
    }
    @Test void redstoneRecipeRequiresTwoRedstoneAndOneStick() throws IOException { String json=compact(read(BASE+"remote_redstone_pickaxe.json")); assertEquals(2,occurrences(json,"\"minecraft:redstone\"")); assertEquals(1,occurrences(json,"\"minecraft:stick\"")); assertTrue(json.contains("\"type\":\"minecraft:crafting_shapeless\"")); }
    @Test void debrisRecipeNeverRequiresAncientDebris() throws IOException { String json=compact(read(BASE+"remote_debris_pickaxe.json")); assertTrue(json.contains("\"minecraft:gravel\"")); assertTrue(json.contains("\"minecraft:flint\"")); assertTrue(json.contains("\"minecraft:stick\"")); assertFalse(json.contains("\"minecraft:ancient_debris\"")); }
    private static int occurrences(String value,String needle){int count=0,from=0;while((from=value.indexOf(needle,from))>=0){count++;from+=needle.length();}return count;}
    private static String read(String path)throws IOException{try(InputStream input=RecipeRegressionTest.class.getClassLoader().getResourceAsStream(path)){if(input==null)throw new IOException("Missing test resource: "+path);return new String(input.readAllBytes(),StandardCharsets.UTF_8);}}
    private static String compact(String value){return value.replaceAll("\\s+","");}
}
