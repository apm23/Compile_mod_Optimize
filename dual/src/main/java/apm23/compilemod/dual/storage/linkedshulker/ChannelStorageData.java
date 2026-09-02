package com.anjas.linkedshulker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class ChannelStorageData extends SavedData {
    public static final int SIZE = 27;

    public record ChannelRecord(String name, List<ItemStack> items) {
        static final Codec<ChannelRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ChannelRecord::name),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(ChannelRecord::items)
        ).apply(instance, ChannelRecord::new));
    }

    private static final Codec<ChannelStorageData> CODEC = ChannelRecord.CODEC.listOf().xmap(
        ChannelStorageData::fromRecords,
        ChannelStorageData::toRecords
    );

    private static final SavedDataType<ChannelStorageData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(LinkedShulkerMod.MOD_ID, "channels"),
        ChannelStorageData::new,
        CODEC,
        null
    );

    private final Map<String, NonNullList<ItemStack>> channels = new HashMap<>();

    public ChannelStorageData() {}

    private static ChannelStorageData fromRecords(List<ChannelRecord> records) {
        ChannelStorageData data = new ChannelStorageData();
        for (ChannelRecord record : records) {
            NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            for (int i = 0; i < Math.min(SIZE, record.items().size()); i++) {
                list.set(i, record.items().get(i));
            }
            data.channels.put(normalize(record.name()), list);
        }
        return data;
    }

    private List<ChannelRecord> toRecords() {
        List<ChannelRecord> out = new ArrayList<>(channels.size());
        channels.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
            out.add(new ChannelRecord(entry.getKey(), new ArrayList<>(entry.getValue())))
        );
        return out;
    }

    public NonNullList<ItemStack> inventory(String channel) {
        return channels.computeIfAbsent(normalize(channel), ignored -> NonNullList.withSize(SIZE, ItemStack.EMPTY));
    }

    public static String displayName(String raw) {
        if (raw == null) return "default";
        String s = raw.strip().replaceAll("\\s+", " ");
        return s.isBlank() ? "default" : s;
    }

    public static String normalize(String raw) {
        return displayName(raw).toLowerCase(Locale.ROOT);
    }

    public static ChannelStorageData get(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        if (level == null) throw new IllegalStateException("Overworld is unavailable");
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
