package apm23.compilemod.dual.functional.godvillager;

import com.anjas.godvillagers.GodHorseRuntime;
import com.anjas.godvillagers.SpecialistSpawnEggItem;
import com.anjas.godvillagers.StormcallRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Registers the proven-working God Villager specialist spawn eggs and runtimes inside the consolidated dual jar. */
public final class GodVillagerRegistry implements ModInitializer {
    private static final String NAMESPACE = "godvillagers";

    public static Item GOD_HELMET_EGG;
    public static Item GOD_CHESTPLATE_EGG;
    public static Item GOD_LEGGINGS_EGG;
    public static Item GOD_BOOTS_EGG;
    public static Item GOD_ELYTRA_EGG;
    public static Item GOD_HORSE_ARMOR_EGG;
    public static Item GOD_TOOLS_EGG;
    public static Item GOD_WEAPONS_EGG;
    public static Item GOD_CLERK_EGG;
    public static Item GOD_LOOTING_EGG;
    public static Item GOD_SKELETON_HORSE_EGG;

    private static Item registerEgg(String path, String summonSuffix) {
        Identifier id = Identifier.fromNamespaceAndPath(NAMESPACE, path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item item = new SpecialistSpawnEggItem(new Item.Properties().setId(key).stacksTo(16), summonSuffix);
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    @Override
    public void onInitialize() {
        GOD_HELMET_EGG = registerEgg("god_helmet_villager_spawn_egg", CommandsData.helmetVillager());
        GOD_CHESTPLATE_EGG = registerEgg("god_chestplate_villager_spawn_egg", CommandsData.chestplateVillager());
        GOD_LEGGINGS_EGG = registerEgg("god_leggings_villager_spawn_egg", CommandsData.leggingsVillager());
        GOD_BOOTS_EGG = registerEgg("god_boots_villager_spawn_egg", CommandsData.bootsVillager());
        GOD_ELYTRA_EGG = registerEgg("god_elytra_villager_spawn_egg", CommandsData.elytraVillager());
        GOD_HORSE_ARMOR_EGG = registerEgg("god_horse_armor_villager_spawn_egg", CommandsData.horseArmorVillager());
        GOD_TOOLS_EGG = registerEgg("god_tools_villager_spawn_egg", CommandsData.toolsVillager());
        GOD_WEAPONS_EGG = registerEgg("god_weapons_villager_spawn_egg", CommandsData.weaponsVillager());
        GOD_CLERK_EGG = registerEgg("god_clerk_villager_spawn_egg", CommandsData.clerkVillager());
        GOD_LOOTING_EGG = registerEgg("god_looting_villager_spawn_egg", CommandsData.lootingVillager());
        GOD_SKELETON_HORSE_EGG = registerEgg("god_skeleton_horse_spawn_egg", CommandsData.godSkeletonHorse());

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> {
            output.accept(GOD_HELMET_EGG);
            output.accept(GOD_CHESTPLATE_EGG);
            output.accept(GOD_LEGGINGS_EGG);
            output.accept(GOD_BOOTS_EGG);
            output.accept(GOD_ELYTRA_EGG);
            output.accept(GOD_HORSE_ARMOR_EGG);
            output.accept(GOD_TOOLS_EGG);
            output.accept(GOD_WEAPONS_EGG);
            output.accept(GOD_CLERK_EGG);
            output.accept(GOD_LOOTING_EGG);
            output.accept(GOD_SKELETON_HORSE_EGG);
        });

        GodHorseRuntime.register();
        StormcallRuntime.register();
    }
}
