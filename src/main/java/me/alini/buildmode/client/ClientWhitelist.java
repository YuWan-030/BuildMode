// 白名单缓存
package me.alini.buildmode.client;

import net.minecraft.resources.ResourceLocation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClientWhitelist {
    private static Set<ResourceLocation> whitelist = new HashSet<>();

    public static void setWhitelist(Set<ResourceLocation> list) {
        whitelist = new HashSet<>(list);
    }

    public static boolean isWhitelisted(ResourceLocation id) {
        return whitelist.contains(id);
    }

    public static Set<ResourceLocation> getWhitelist() {
        return Collections.unmodifiableSet(whitelist);
    }
}