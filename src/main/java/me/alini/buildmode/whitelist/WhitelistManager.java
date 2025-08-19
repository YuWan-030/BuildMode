
// 白名单存储/加载
package me.alini.buildmode.whitelist;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class WhitelistManager {
    private static final Set<ResourceLocation> whitelist = new HashSet<>();
    private static final Gson GSON = new Gson();
    private static Path configFile;

    public static void init(Path configDir) {
        configFile = configDir.resolve("buildmode/whitelist.json");
        load();
    }

    public static void load() {
        try (FileReader reader = new FileReader(configFile.toFile())) {
            Set<String> items = GSON.fromJson(reader, new TypeToken<Set<String>>(){}.getType());
            whitelist.clear();
            if (items != null) {
                for (String id : items) {
                    whitelist.add(new ResourceLocation(id));
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(configFile.toFile())) {
            Set<String> items = new HashSet<>();
            for (ResourceLocation id : whitelist) {
                items.add(id.toString());
            }
            GSON.toJson(items, writer);
        } catch (Exception ignored) {}
    }

    public static boolean isWhitelisted(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return whitelist.contains(id);
    }

    public static void add(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        whitelist.add(id);
    }

    public static void remove(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        whitelist.remove(id);
    }

    public static Set<ResourceLocation> getWhitelist() {
        return whitelist;
    }
}