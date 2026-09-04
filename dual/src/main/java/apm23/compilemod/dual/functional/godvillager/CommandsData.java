package apm23.compilemod.dual.functional.godvillager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Authoritative specialist summon data reconstructed from the verified
 * God Villagers alpha.82 artifact. Random profession/type selection and trade
 * layout intentionally match the proven-working implementation.
 */
public final class CommandsData {
    private static final List<String> PROFESSIONS = List.of(
        "librarian", "cleric", "cartographer", "armorer", "toolsmith",
        "weaponsmith", "fletcher", "mason", "farmer", "butcher",
        "leatherworker", "shepherd", "fisherman"
    );
    private static final List<String> TYPES = new ArrayList<>(List.of(
        "plains", "desert", "jungle", "savanna", "snow", "swamp", "taiga"
    ));
    private static final List<String> professionBag = new ArrayList<>();
    private static int typeIndex;

    private CommandsData() {}

    private static String nextProfession() {
        if (professionBag.isEmpty()) {
            professionBag.addAll(PROFESSIONS);
            Collections.shuffle(professionBag);
        }
        return professionBag.removeFirst();
    }

    private static String nextType() {
        if (typeIndex >= TYPES.size()) {
            typeIndex = 0;
            Collections.shuffle(TYPES);
        }
        return TYPES.get(typeIndex++);
    }

    private static String villager(String name, String color, String specialTrades) {
        String filler = randomFiller();
        String recipes = specialTrades.isEmpty() ? filler : specialTrades + "," + filler;
        return "minecraft:villager {CustomName:{text:\"" + name + "\",color:\"" + color
            + "\",bold:true},CustomNameVisible:1b,Glowing:1b,PersistenceRequired:1b,VillagerData:{type:"
            + nextType() + ",profession:" + nextProfession() + ",level:5},Offers:{Recipes:[" + recipes + "]}}";
    }

    private static int discountedBookPrice(int value) {
        return Math.max(1, Math.round(value * 0.8f));
    }

    private static String book(int emeralds, int books, String stars, String name, String color, String enchantments) {
        emeralds = discountedBookPrice(emeralds);
        books = discountedBookPrice(books);
        return "{buy:{id:emerald,count:" + emeralds + "},buyB:{id:book,count:" + books
            + "},sell:{id:enchanted_book,count:1,components:{custom_name:{text:\"" + stars + " " + name
            + "\",color:\"" + color + "\",bold:true,italic:false},stored_enchantments:{" + enchantments
            + "}}},maxUses:999999,rewardExp:0b,priceMultiplier:0f}";
    }

    private static String trade(String buyId, int buyCount, String sellId, int sellCount) {
        return "{buy:{id:" + buyId + ",count:" + buyCount + "},sell:{id:" + sellId + ",count:" + sellCount
            + "},maxUses:999999,rewardExp:0b,priceMultiplier:0f}";
    }

    private static String randomFiller() {
        List<String> trades = new ArrayList<>();
        trades.add(trade("coal", 16, "emerald", 1));
        trades.add(trade("iron_ingot", 4, "emerald", 1));
        trades.add(trade("paper", 24, "emerald", 1));
        trades.add(trade("flint", 10, "emerald", 1));
        trades.add(trade("wheat", 18, "emerald", 1));
        trades.add(trade("potato", 15, "emerald", 1));
        trades.add(trade("carrot", 15, "emerald", 1));
        trades.add(trade("string", 20, "emerald", 1));
        trades.add(trade("clay_ball", 10, "emerald", 1));
        trades.add(trade("glass_pane", 12, "emerald", 1));
        trades.add(trade("emerald", 4, "bookshelf", 1));
        trades.add(trade("emerald", 1, "bread", 6));
        trades.add(trade("emerald", 2, "apple", 4));
        trades.add(trade("emerald", 1, "torch", 16));
        Collections.shuffle(trades);
        return String.join(",", trades.subList(0, 7));
    }

    public static String helmetVillager() {
        return villager("★★★★★ Helm Sage", "aqua",
            book(38, 18, "★★★★★", "God Helmet", "aqua", "protection:6,respiration:6,unbreaking:6,mending:3,aqua_affinity:1"));
    }

    public static String chestplateVillager() {
        return villager("★★★★★ Chest Sage", "gold",
            book(43, 23, "★★★★★", "God Chest", "gold", "protection:6,blast_protection:4,fire_protection:4,projectile_protection:4,thorns:6,unbreaking:6,mending:3"));
    }

    public static String leggingsVillager() {
        return villager("★★★★ Leg Sage", "green",
            book(36, 17, "★★★★", "God Legs", "green", "protection:6,swift_sneak:6,unbreaking:6,mending:3"));
    }

    public static String bootsVillager() {
        return villager("★★★★★ Boot Sage", "yellow",
            book(42, 22, "★★★★★", "God Boots", "yellow", "protection:6,feather_falling:6,depth_strider:6,soul_speed:6,unbreaking:6,mending:3"));
    }

    public static String elytraVillager() {
        return villager("★★★ Elytra Sage", "light_purple",
            book(27, 10, "★★★", "God Elytra", "light_purple", "unbreaking:6,mending:3"));
    }

    public static String horseArmorVillager() {
        return villager("★★★★★ Horse Sage", "red",
            book(43, 23, "★★★★★", "God Horse", "red", "protection:6,blast_protection:4,fire_protection:4,projectile_protection:4,unbreaking:6,mending:3"));
    }

    public static String toolsVillager() {
        String trades = String.join(",",
            book(37, 17, "★★★★", "God Pickaxe", "aqua", "efficiency:5,fortune:3,unbreaking:5,mending:3"),
            book(37, 17, "★★★★", "God Axe", "green", "efficiency:5,fortune:3,unbreaking:5,mending:3"),
            book(34, 14, "★★★★", "God Shovel", "yellow", "efficiency:5,fortune:3,unbreaking:5,mending:3"),
            book(33, 13, "★★★★", "God Hoe", "light_purple", "efficiency:5,fortune:3,unbreaking:5,mending:3"),
            book(20, 7, "★", "Silk Touch", "white", "silk_touch:1")
        );
        return villager("★★★★ Tool Sage", "blue", trades);
    }

    public static String weaponsVillager() {
        String stormcall = "{buy:{id:emerald,count:" + discountedBookPrice(43) + "},buyB:{id:book,count:" + discountedBookPrice(23) + "},sell:{id:enchanted_book,count:1,components:{custom_name:{text:\"★★★★★ Stormcall I\",color:\"aqua\",bold:true,italic:false},custom_data:{godvillagers_stormcall_book:true}}},maxUses:999999,rewardExp:0b,priceMultiplier:0f}";
        String trades = String.join(",",
            book(43, 23, "★★★★★", "God Sword", "red", "sharpness:5,looting:3,fire_aspect:2,knockback:2,sweeping_edge:3,unbreaking:5,mending:3"),
            book(39, 19, "★★★★", "God Mace", "dark_purple", "density:5,wind_burst:3,unbreaking:5,mending:3"),
            book(43, 23, "★★★★★", "God Spear", "gold", "sharpness:5,looting:3,fire_aspect:2,knockback:2,lunge:3,unbreaking:5,mending:3"),
            book(41, 21, "★★★★", "God Trident", "aqua", "impaling:5,loyalty:3,channeling:1,unbreaking:5,mending:3"),
            book(35, 15, "★★★", "God Crossbow", "yellow", "quick_charge:3,piercing:4,unbreaking:5,mending:3"),
            book(39, 19, "★★★★", "God Bow", "green", "power:5,punch:2,flame:1,infinity:1,unbreaking:5"),
            stormcall
        );
        return villager("★★★★★ Arms Sage", "dark_red", trades);
    }

    public static String clerkVillager() {
        String trades = String.join(",",
            trade("netherrack", 64, "emerald", 10),
            trade("rotten_flesh", 1, "emerald", 1),
            trade("bamboo", 64, "emerald", 5),
            trade("string", 15, "emerald", 5),
            trade("bamboo", 8, "book", 3),
            trade("gunpowder", 5, "diamond", 4),
            trade("gold_ingot", 3, "dragon_breath", 2),
            trade("arrow", 15, "gold_ingot", 3),
            trade("amethyst_shard", 4, "emerald", 8),
            trade("diamond", 6, "netherite_ingot", 1)
        );
        return villager("★★★★★ Grand Clerk", "dark_aqua", trades);
    }

    public static String lootingVillager() {
        String trades = String.join(",",
            book(24, 8, "★★★", "Looting VI", "blue", "looting:6"),
            book(29, 11, "★★★", "Looting VII", "aqua", "looting:7"),
            book(34, 15, "★★★★", "Looting VIII", "light_purple", "looting:8"),
            book(39, 19, "★★★★", "Looting IX", "gold", "looting:9"),
            book(43, 23, "★★★★★", "Looting X", "red", "looting:10")
        );
        return villager("★★★★★ Loot Sage", "dark_purple", trades);
    }

    public static String godSkeletonHorse() {
        return "minecraft:skeleton_horse {Tame:1b,Temper:100,PersistenceRequired:1b,Tags:[\"godvillagers_god_horse\"]}";
    }
}
