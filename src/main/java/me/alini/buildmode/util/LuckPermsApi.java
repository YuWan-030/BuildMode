package me.alini.buildmode.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

import java.util.UUID;

public class LuckPermsApi {

    // 检查某个玩家是否拥有指定权限
    public static boolean checkPlayerPermission(ServerPlayer player, String permission) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();

            UUID uuid = player.getUUID();
            User user = luckPerms.getUserManager().getUser(uuid);

            if (user == null) {
                // 玩家数据可能还没加载完成
                user = luckPerms.getUserManager().loadUser(uuid).join();
            }

            if (user != null) {
                return user.getCachedData().getPermissionData(QueryOptions.defaultContextualOptions())
                        .checkPermission(permission).asBoolean();
            }
        } catch (Exception e) {
            System.out.println("[BuildMode] LuckPerms 权限检查失败: " + e.getMessage());
        }
        return false;
    }
}
