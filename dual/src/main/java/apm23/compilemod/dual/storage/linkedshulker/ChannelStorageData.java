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
            for (int i = 0; i < Math.min(SIZE, record.items().size()); i++) list.set(i, record.items().get(i));
            // Keep the key exactly as saved. Old worlds contain lower-case keys; inventory()
            // migrates those lazily when an exact-name shulker first claims them.
            data.channels.put(displayName(record.name()), list);
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

    public NonNullList<ItemStack> inventory(String rawChannel) {
        String exact = displayName(rawChannel);
        NonNullList<ItemStack> existing = channels.get(exact);
        if (existing != null) return existing;

        // Backward compatibility for worlds saved by the old case-insensitive system.
        // The first exact spelling to access an old lower-case key takes ownership of its
        // storage. Any differently-capitalized name created later receives a new channel.
        String legacy = exact.toLowerCase(Locale.ROOT);
        if (!legacy.equals(exact)) {
            NonNullList<ItemStack> old = channels.remove(legacy);
            if (old != null) {
                channels.put(exact, old);
                setDirty();
                return old;
            }
        }

        NonNullList<ItemStack> created = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        channels.put(exact, created);
        setDirty();
        return created;
    }

    public static String displayName(String raw) {
        if (raw == null || raw.isBlank()) return "default";
        return raw;
    }

    /** Exact channel key; retained method name so old call sites do not need a behavior rewrite. */
    public static String normalize(String raw) {
        return displayName(raw);
    }

    public static ChannelStorageData get(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        if (level == null) throw new IllegalStateException("Overworld is unavailable");
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
