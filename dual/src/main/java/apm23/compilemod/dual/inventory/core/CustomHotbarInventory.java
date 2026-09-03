package com.anjas.custominventory;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CustomHotbarInventory implements ModInitializer {
    public static final String MOD_ID="custom_hotbar_inventory";
    public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
    public static Identifier id(String path){return Identifier.fromNamespaceAndPath(MOD_ID,path);}

    @Override public void onInitialize(){
        InventoryStorage.register();
        PayloadTypeRegistry.playC2S().register(ModPayloads.CyclePage.TYPE,ModPayloads.CyclePage.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.SwapHotbar.TYPE,ModPayloads.SwapHotbar.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.SortAll.TYPE,ModPayloads.SortAll.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.MergeAll.TYPE,ModPayloads.MergeAll.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.BrowseOpen.TYPE,ModPayloads.BrowseOpen.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.BrowseClose.TYPE,ModPayloads.BrowseClose.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.DirectPage.P1.TYPE,ModPayloads.DirectPage.P1.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.DirectPage.P2.TYPE,ModPayloads.DirectPage.P2.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.DirectPage.P3.TYPE,ModPayloads.DirectPage.P3.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.DirectPage.P4.TYPE,ModPayloads.DirectPage.P4.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.DirectPage.P5.TYPE,ModPayloads.DirectPage.P5.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.DirectPage.P6.TYPE,ModPayloads.DirectPage.P6.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.DirectPage.P7.TYPE,ModPayloads.DirectPage.P7.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPayloads.DirectPage.P8.TYPE,ModPayloads.DirectPage.P8.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPayloads.PageState.TYPE,ModPayloads.PageState.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPayloads.HiddenRecipeContents.TYPE,ModPayloads.HiddenRecipeContents.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ModPayloads.CyclePage.TYPE,(p,c)->c.server().execute(()->{if(canSwitchPages(c.player())){InventoryStorage.cycle(c.player());sendState(c.player());}}));
        ServerPlayNetworking.registerGlobalReceiver(ModPayloads.BrowseOpen.TYPE,(p,c)->c.server().execute(()->{InventoryStorage.setBrowsing(c.player(),true);InventoryStorage.switchPage(c.player(),0);sendState(c.player());}));
        ServerPlayNetworking.registerGlobalReceiver(ModPayloads.BrowseClose.TYPE,(p,c)->c.server().execute(()->{InventoryStorage.setBrowsing(c.player(),false);InventoryStorage.snapshotLive(c.player());sendHiddenRecipeState(c.player());}));
        ServerPlayNetworking.registerGlobalReceiver(ModPayloads.SortAll.TYPE,(p,c)->c.server().execute(()->{if(canReorganize(c.player())){InventoryAlgorithms.sortAll(c.player());sendState(c.player());}}));
        ServerPlayNetworking.registerGlobalReceiver(ModPayloads.MergeAll.TYPE,(p,c)->c.server().execute(()->{if(canReorganize(c.player())){InventoryAlgorithms.mergeAll(c.player());sendState(c.player());}}));
        ServerPlayNetworking.registerGlobalReceiver(ModPayloads.SwapHotbar.TYPE,(p,c)->c.server().execute(()->swapHotbar(c.player())));
        registerDirectPage(ModPayloads.DirectPage.P1.TYPE,0);registerDirectPage(ModPayloads.DirectPage.P2.TYPE,1);registerDirectPage(ModPayloads.DirectPage.P3.TYPE,2);registerDirectPage(ModPayloads.DirectPage.P4.TYPE,3);registerDirectPage(ModPayloads.DirectPage.P5.TYPE,4);registerDirectPage(ModPayloads.DirectPage.P6.TYPE,5);registerDirectPage(ModPayloads.DirectPage.P7.TYPE,6);registerDirectPage(ModPayloads.DirectPage.P8.TYPE,7);

        ServerPlayerEvents.JOIN.register(p->{InventoryStorage.migrateLegacy(p);InventoryStorage.snapshotLive(p);sendState(p);});
        ServerPlayerEvents.COPY_FROM.register((oldP,newP,alive)->InventoryStorage.copyState(oldP,newP));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldP,newP,alive)->sendState(newP));
        ServerLifecycleEvents.SERVER_STARTED.register(server->InventoryAlgorithms.runStartupSelfTests());
        ServerTickEvents.END_SERVER_TICK.register(server->{for(ServerPlayer p:server.getPlayerList().getPlayers())InventoryStorage.routeOverflow(p);});
        TaczAmmoCompat.init();
        LOGGER.info("Custom Hotbar Inventory initialized");
    }
    private static boolean canSwitchPages(ServerPlayer p){return InventoryStorage.isBrowsing(p);}
    private static boolean canReorganize(ServerPlayer p){return InventoryStorage.isBrowsing(p)&&p.containerMenu.getCarried().isEmpty();}
    private static <T extends CustomPacketPayload> void registerDirectPage(CustomPacketPayload.Type<T> type,int page){ServerPlayNetworking.registerGlobalReceiver(type,(p,c)->c.server().execute(()->{if(canSwitchPages(c.player())){InventoryStorage.switchPage(c.player(),page);sendState(c.player());}}));}
    private static void sendState(ServerPlayer p){sendPageState(p);sendHiddenRecipeState(p);}
    private static void sendPageState(ServerPlayer p){if(ServerPlayNetworking.canSend(p,ModPayloads.PageState.TYPE))ServerPlayNetworking.send(p,new ModPayloads.PageState(InventoryStorage.active(p)));}
    public static void sendHiddenRecipeState(ServerPlayer p){
        if(!ServerPlayNetworking.canSend(p,ModPayloads.HiddenRecipeContents.TYPE))return;
        InventoryStorage.snapshotLive(p);
        int active=InventoryStorage.active(p);
        ArrayList<ItemStack> hidden=new ArrayList<>((InventoryStorage.PAGE_COUNT-1)*InventoryStorage.PAGE_SIZE);
        for(int page=0;page<InventoryStorage.PAGE_COUNT;page++){if(page==active)continue;for(ItemStack stack:InventoryStorage.read(p,page))hidden.add(stack.copy());}
        ServerPlayNetworking.send(p,new ModPayloads.HiddenRecipeContents(List.copyOf(hidden)));
    }
    private static void swapHotbar(ServerPlayer p){
        List<ItemStack> alt=InventoryStorage.readAltHotbar(p), current=new ArrayList<>(9);
        for(int i=0;i<9;i++)current.add(p.getInventory().getItem(i).copy());
        InventoryStorage.writeAltHotbar(p,current);
        for(int i=0;i<9;i++)p.getInventory().setItem(i,alt.get(i).copy());
        InventoryStorage.sync(p);sendHiddenRecipeState(p);
    }
}
