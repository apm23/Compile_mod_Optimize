package com.anjas.custominventory.client;

import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Client-only cache used by recipe-book checks and optional inventory-aware compat such as TACZ. */
public final class HiddenRecipeContentsClient {
    private static volatile List<ItemStack> hiddenStacks=List.of();
    private static volatile long revision;
    private HiddenRecipeContentsClient() {}

    public static void replace(List<ItemStack> stacks){
        // Hidden recipe/TACZ consumers only care about item contents, never physical slot positions.
        // Drop empty entries once at packet-receive time so all later recipe/HUD scans iterate only
        // meaningful stacks while preserving exact counts/components of every non-empty stack.
        hiddenStacks=stacks.stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
        revision++;
    }

    public static void clear(){
        hiddenStacks=List.of();
        revision++;
    }

    /** Defensive-copy API for callers that may mutate returned stacks. */
    public static List<ItemStack> snapshot(){return hiddenStacks.stream().map(ItemStack::copy).toList();}

    /** Immutable stable view for hot read-only paths such as TACZ HUD rendering. */
    public static List<ItemStack> view(){return hiddenStacks;}

    /** Changes only when the server replaces/clears the hidden-page snapshot. */
    public static long revision(){return revision;}

    public static void accountInto(StackedItemContents contents){for(ItemStack stack:hiddenStacks)contents.accountSimpleStack(stack);}
}
