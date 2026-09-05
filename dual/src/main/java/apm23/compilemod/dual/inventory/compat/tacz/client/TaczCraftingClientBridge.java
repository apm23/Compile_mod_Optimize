package com.anjas.custominventory.client;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side TACZ workbench ingredient accounting across all paged inventory slots. */
public final class TaczCraftingClientBridge {
    private static final ConcurrentHashMap<Class<?>, ScreenAccess> SCREEN_ACCESS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> RECIPE_INPUTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> INPUT_INGREDIENT = new ConcurrentHashMap<>();

    private TaczCraftingClientBridge() {}

    public static void refresh(Object screen) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || screen == null) return;
        try {
            ScreenAccess screenAccess = SCREEN_ACCESS.computeIfAbsent(screen.getClass(), TaczCraftingClientBridge::resolveScreenAccess);
            Object recipe = screenAccess.selectedRecipe.get(screen);
            if (recipe == null) {
                screenAccess.playerIngredientCount.set(screen, null);
                return;
            }

            Method getInputs = RECIPE_INPUTS.computeIfAbsent(recipe.getClass(), TaczCraftingClientBridge::resolveRecipeInputs);
            @SuppressWarnings("unchecked") List<Object> ingredients = (List<Object>) getInputs.invoke(recipe);
            Int2IntArrayMap counts = new Int2IntArrayMap(ingredients.size());
            List<ItemStack> hidden = HiddenRecipeContentsClient.view();

            for (int i = 0; i < ingredients.size(); i++) {
                Object input = ingredients.get(i);
                Method getIngredient = INPUT_INGREDIENT.computeIfAbsent(input.getClass(), TaczCraftingClientBridge::resolveInputIngredient);
                Ingredient ingredient = (Ingredient) getIngredient.invoke(input);
                int count = 0;
                if (ingredient != null) {
                    for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                        if (stack != null && !stack.isEmpty() && ingredient.test(stack)) count += stack.getCount();
                    }
                    for (ItemStack stack : hidden) {
                        if (stack != null && !stack.isEmpty() && ingredient.test(stack)) count += stack.getCount();
                    }
                }
                counts.put(i, count);
            }
            screenAccess.playerIngredientCount.set(screen, counts);
        } catch (ReflectiveOperationException | LinkageError | ReflectionResolutionException ignored) {
            // TACZ is optional; its native visible-page count remains as the safe fallback.
        }
    }

    private static ScreenAccess resolveScreenAccess(Class<?> type) {
        try {
            return new ScreenAccess(findField(type, "selectedRecipe"), findField(type, "playerIngredientCount"));
        } catch (NoSuchFieldException e) {
            throw new ReflectionResolutionException(e);
        }
    }

    private static Method resolveRecipeInputs(Class<?> type) {
        try {
            return type.getMethod("getInputs");
        } catch (NoSuchMethodException e) {
            throw new ReflectionResolutionException(e);
        }
    }

    private static Method resolveInputIngredient(Class<?> type) {
        try {
            return type.getMethod("getIngredient");
        } catch (NoSuchMethodException e) {
            throw new ReflectionResolutionException(e);
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                Field field = cursor.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private record ScreenAccess(Field selectedRecipe, Field playerIngredientCount) {}
    private static final class ReflectionResolutionException extends RuntimeException {
        private ReflectionResolutionException(Throwable cause) { super(cause); }
    }
}
