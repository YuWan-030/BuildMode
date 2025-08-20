package me.alini.buildmode.util;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerPlayer;

public class LuckPermsApi {

    public static boolean isLuckPermsPresent() {
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static CompletableFuture<Boolean> checkPlayerPermissionAsync(ServerPlayer player, String permission) {
        if (!isLuckPermsPresent()) {
            // LuckPerms 不存在，降级为OP校验
            return CompletableFuture.completedFuture(player.hasPermissions(2));
        }
        try {
            // 反射获取 LuckPermsProvider.get()
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = providerClass.getMethod("get").invoke(null);
            // 反射获取 UserManager
            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            // 反射调用 loadUser(UUID)
            java.util.UUID uuid = player.getUUID();
            java.util.concurrent.CompletableFuture<?> userFuture =
                    (java.util.concurrent.CompletableFuture<?>) userManager.getClass().getMethod("loadUser", java.util.UUID.class).invoke(userManager, uuid);

            // thenApply 里再反射调用 getCachedData/getPermissionData/checkPermission
            return userFuture.thenApply(user -> {
                if (user == null) return player.hasPermissions(2);
                try {
                    Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
                    // QueryOptions.defaultContextualOptions()
                    Class<?> queryOptionsClass = Class.forName("net.luckperms.api.query.QueryOptions");
                    Object queryOptions = queryOptionsClass.getMethod("defaultContextualOptions").invoke(null);
                    Object permissionData = cachedData.getClass().getMethod("getPermissionData", queryOptionsClass).invoke(cachedData, queryOptions);
                    Object result = permissionData.getClass().getMethod("checkPermission", String.class).invoke(permissionData, permission);
                    boolean allowed = (boolean) result.getClass().getMethod("asBoolean").invoke(result);
                    return allowed;
                } catch (Exception e) {
                    System.out.println("[BuildMode] LuckPerms 反射权限检查失败，降级为OP校验: " + e.getMessage());
                    return player.hasPermissions(2);
                }
            }).exceptionally(e -> {
                System.out.println("[BuildMode] LuckPerms 权限异步检查异常，降级为OP校验: " + e.getMessage());
                return player.hasPermissions(2);
            });
        } catch (Exception e) {
            System.out.println("[BuildMode] LuckPerms 反射初始化失败，降级为OP校验: " + e.getMessage());
            return CompletableFuture.completedFuture(player.hasPermissions(2));
        }
    }
}