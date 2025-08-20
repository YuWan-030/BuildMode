// 白名单缓存
package me.alini.buildmode.client;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ClientWhitelist {
    // 使用线程安全的集合
    private static final Set<ResourceLocation> whitelist = new CopyOnWriteArraySet<>();

    public static void updateWhitelist(Collection<ResourceLocation> newList) {
        whitelist.clear();
        whitelist.addAll(newList);
    }

    public static boolean isWhitelisted(ResourceLocation id) {
        return whitelist.contains(id);
    }

    public static Set<ResourceLocation> getWhitelist() {
        return whitelist;
    }
}