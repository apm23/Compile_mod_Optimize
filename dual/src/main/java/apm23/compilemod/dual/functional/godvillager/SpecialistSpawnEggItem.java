package com.anjas.godvillagers;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class SpecialistSpawnEggItem extends Item {
    private static final String GOD_FISHING_ROD_BOOK = "{buy:{id:emerald,count:35},buyB:{id:book,count:15},sell:{id:enchanted_book,count:1,components:{custom_name:{text:\"God Fishing Rod\",color:\"aqua\",bold:true,italic:false},stored_enchantments:{luck_of_the_sea:6,lure:10,unbreaking:3,mending:2}}},maxUses:999999,rewardExp:0b,priceMultiplier:0f}";
    private static final String GOD_SHIELD_BOOK = "{buy:{id:emerald,count:35},buyB:{id:book,count:15},sell:{id:enchanted_book,count:1,components:{custom_name:{text:\"God Shield\",color:\"dark_red\",bold:true,italic:false},stored_enchantments:{unbreaking:4,mending:3,thorns:5}}},maxUses:999999,rewardExp:0b,priceMultiplier:0f}";
    private static final String MAGNET_I_BOOK = bookTrade("Magnet I", "godvillagers:magnet", 1, 32);
    private static final String LIFE_STEAL_I_BOOK = bookTrade("Life Steal I", "godvillagers:life_steal", 1, 32);
    private static final String LIFE_STEAL_II_BOOK = bookTrade("Life Steal II", "godvillagers:life_steal", 2, 40);
    private static final String LIFE_STEAL_III_BOOK = bookTrade("Life Steal III", "godvillagers:life_steal", 3, 48);
    private static final String LOOTING_III_BOOK = bookTrade("Looting III", "minecraft:looting", 3, 24);
    private static final String LOOTING_IV_BOOK = bookTrade("Looting IV", "minecraft:looting", 4, 32);
    private static final String LOOTING_V_BOOK = bookTrade("Looting V", "minecraft:looting", 5, 40);

    private static final String CLERK_STRING = simpleTrade("string", 1, "emerald", 1);
    private static final String CLERK_ARROW = simpleTrade("arrow", 1, "emerald", 1);
    private static final String CLERK_COAL_GUNPOWDER = simpleTrade("coal", 2, "gunpowder", 1);
    private static final String CLERK_COPPER_GUNPOWDER = simpleTrade("copper_ingot", 3, "gunpowder", 1);
    private static final String CLERK_BONE_BOOK = simpleTrade("bone", 1, "book", 1);
    private static final String CLERK_COAL_BOOK = simpleTrade("coal", 2, "book", 1);
    private static final String CLERK_COPPER_BOOK = simpleTrade("copper_ingot", 2, "book", 1);
    private static final String CLERK_PHANTOM_EMERALD = simpleTrade("phantom_membrane", 1, "emerald", 1);
    private static final String CLERK_PHANTOM_BOOK = simpleTrade("phantom_membrane", 1, "book", 1);

    private final String summonSuffix;

    public SpecialistSpawnEggItem(Properties properties, String summonSuffix) {
        super(properties);
        this.summonSuffix = enhance(summonSuffix);
    }

    private static String bookTrade(String name, String enchantmentId, int level, int emeralds) {
        return "{buy:{id:emerald,count:" + emeralds + "},buyB:{id:book,count:1},sell:{id:enchanted_book,count:1,components:{custom_name:{text:\"" + name + "\",color:\"gold\",italic:false},stored_enchantments:{\"" + enchantmentId + "\":" + level + "}}},maxUses:999999,rewardExp:0b,priceMultiplier:0f}";
    }

    private static String simpleTrade(String buyId, int buyCount, String sellId, int sellCount) {
        return "{buy:{id:" + buyId + ",count:" + buyCount + "},sell:{id:" + sellId + ",count:" + sellCount + "},maxUses:999999,rewardExp:0b,priceMultiplier:0f}";
    }

    private static String enhance(String original) {
        String result = original
            .replace("CustomNameVisible:1b,Glowing:1b,", "CustomNameVisible:0b,Glowing:0b,")
            .replace("Glowing:1b,", "Glowing:0b,")
            .replace("CustomNameVisible:1b,", "CustomNameVisible:0b,");

        if (result.contains("Tool Sage")) result = appendTrade(result, GOD_FISHING_ROD_BOOK);
        if (result.contains("Arms Sage")) {
            result = appendTrade(result, GOD_SHIELD_BOOK);
            result = appendTrade(result, MAGNET_I_BOOK);
            result = appendTrade(result, LIFE_STEAL_I_BOOK);
            result = appendTrade(result, LIFE_STEAL_II_BOOK);
            result = appendTrade(result, LIFE_STEAL_III_BOOK);
        }

        if (isLootSpecialist(result)) {
            result = appendTrade(result, LOOTING_III_BOOK);
            result = appendTrade(result, LOOTING_IV_BOOK);
            result = appendTrade(result, LOOTING_V_BOOK);
        }

        if (isClerkSpecialist(result)) {
            result = removeTradesBuying(result, "string", "arrow");
            result = appendTrade(result, CLERK_STRING);
            result = appendTrade(result, CLERK_ARROW);
            result = appendTrade(result, CLERK_COAL_GUNPOWDER);
            result = appendTrade(result, CLERK_COPPER_GUNPOWDER);
            result = appendTrade(result, CLERK_BONE_BOOK);
            result = appendTrade(result, CLERK_COAL_BOOK);
            result = appendTrade(result, CLERK_COPPER_BOOK);
            result = appendTrade(result, CLERK_PHANTOM_EMERALD);
            result = appendTrade(result, CLERK_PHANTOM_BOOK);
        }

        return booksFirst(result);
    }

    private static boolean isLootSpecialist(String command) {
        return command.contains("Loot Sage") || command.contains("Loot Villager") || command.contains("Loot Master");
    }

    private static boolean isClerkSpecialist(String command) {
        return command.contains("Clerk Sage") || command.contains("Clerk Villager") || command.contains("God Clerk") || command.contains("Grand Clerk");
    }

    private static String appendTrade(String command, String trade) {
        int recipesEnd = command.lastIndexOf("]}}");
        if (recipesEnd < 0) throw new IllegalArgumentException("Villager summon command has no Offers.Recipes terminator");
        return command.substring(0, recipesEnd) + "," + trade + command.substring(recipesEnd);
    }

    private static String removeTradesBuying(String command, String... itemIds) {
        final String marker = "Recipes:[";
        int markerPos = command.indexOf(marker);
        if (markerPos < 0) return command;
        int listStart = markerPos + marker.length();
        int listEnd = findMatchingRecipesEnd(command, listStart);
        if (listEnd < 0) throw new IllegalArgumentException("Villager summon command has malformed Offers.Recipes list");
        List<String> kept = new ArrayList<>();
        for (String recipe : splitTopLevelRecipes(command.substring(listStart, listEnd))) {
            String compact = recipe.replace(" ", "").replace("\"minecraft:", "\"");
            boolean remove = false;
            for (String id : itemIds) {
                if (compact.contains("buy:{id:" + id + ",") || compact.contains("buy:{id:\"" + id + "\",")) {
                    remove = true;
                    break;
                }
            }
            if (!remove) kept.add(recipe);
        }
        return command.substring(0, listStart) + String.join(",", kept) + command.substring(listEnd);
    }

    private static String booksFirst(String command) {
        final String marker = "Recipes:[";
        int markerPos = command.indexOf(marker);
        if (markerPos < 0) return command;
        int listStart = markerPos + marker.length();
        int listEnd = findMatchingRecipesEnd(command, listStart);
        if (listEnd < 0) throw new IllegalArgumentException("Villager summon command has malformed Offers.Recipes list");
        List<String> recipes = splitTopLevelRecipes(command.substring(listStart, listEnd));
        if (recipes.size() < 2) return command;
        List<String> books = new ArrayList<>();
        List<String> other = new ArrayList<>();
        for (String recipe : recipes) {
            if (isEnchantedBookTrade(recipe)) books.add(recipe); else other.add(recipe);
        }
        if (books.isEmpty() || other.isEmpty()) return command;
        List<String> ordered = new ArrayList<>(recipes.size());
        ordered.addAll(books);
        ordered.addAll(other);
        return command.substring(0, listStart) + String.join(",", ordered) + command.substring(listEnd);
    }

    private static int findMatchingRecipesEnd(String text, int contentStart) {
        int squareDepth = 0, curlyDepth = 0;
        boolean quoted = false, escaped = false;
        for (int i = contentStart; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (escaped) escaped = false; else if (c == '\\') escaped = true; else if (c == '"') quoted = false;
                continue;
            }
            if (c == '"') { quoted = true; continue; }
            if (c == '{') curlyDepth++; else if (c == '}') curlyDepth--; else if (c == '[') squareDepth++;
            else if (c == ']') { if (squareDepth == 0 && curlyDepth == 0) return i; squareDepth--; }
        }
        return -1;
    }

    private static List<String> splitTopLevelRecipes(String body) {
        List<String> out = new ArrayList<>();
        int start = 0, curlyDepth = 0, squareDepth = 0;
        boolean quoted = false, escaped = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (quoted) {
                if (escaped) escaped = false; else if (c == '\\') escaped = true; else if (c == '"') quoted = false;
                continue;
            }
            if (c == '"') { quoted = true; continue; }
            if (c == '{') curlyDepth++; else if (c == '}') curlyDepth--; else if (c == '[') squareDepth++; else if (c == ']') squareDepth--;
            else if (c == ',' && curlyDepth == 0 && squareDepth == 0) { out.add(body.substring(start, i)); start = i + 1; }
        }
        out.add(body.substring(start));
        return out;
    }

    private static boolean isEnchantedBookTrade(String recipe) {
        String compact = recipe.replace(" ", "");
        return compact.contains("sell:{id:enchanted_book,") || compact.contains("sell:{id:\"minecraft:enchanted_book\",") || compact.contains("id:minecraft:enchanted_book");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        Vec3 pos = new Vec3(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        CommandSourceStack source = serverLevel.getServer().createCommandSourceStack().withLevel(serverLevel).withPosition(pos).withSuppressedOutput();
        int split = summonSuffix.indexOf(' ');
        String entity = split < 0 ? summonSuffix : summonSuffix.substring(0, split);
        String nbt = split < 0 ? "" : summonSuffix.substring(split + 1);
        String command = "summon " + entity + " " + spawnPos.getX() + " " + spawnPos.getY() + " " + spawnPos.getZ();
        if (!nbt.isEmpty()) command += " " + nbt;
        serverLevel.getServer().getCommands().performPrefixedCommand(source, command);
        return InteractionResult.SUCCESS;
    }
}
