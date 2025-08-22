package me.alini.buildmode.region;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class RegionPlayerLoginHandler {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 延迟到下一 tick，确保玩家位置已初始化，避免区域判定失效
            player.getServer().execute(() -> RegionManager.tickPlayer(player));
        }
    }
    // 在 RegionPlayerLoginHandler.java 或 RegionManager.java 添加
    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RegionManager.removeInsidePlayer(player.getUUID());
        }
    }
}
