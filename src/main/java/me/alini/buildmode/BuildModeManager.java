package me.alini.buildmode;

import me.alini.buildmode.network.ModMessages;
import me.alini.buildmode.network.S2CUpdateBuildModePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BuildModeManager {
    private static final Map<UUID, Boolean> buildModeStates = new HashMap<>();

    public static boolean isInBuildMode(ServerPlayer player) {
        return buildModeStates.getOrDefault(player.getUUID(), false);
    }

    public static void toggleBuildMode(ServerPlayer player) {
        UUID playerId = player.getUUID();
        boolean currentState = buildModeStates.getOrDefault(playerId, false);
        buildModeStates.put(playerId, !currentState);

        // 通知玩家建造模式状态
        player.displayClientMessage(
                Component.literal("建造模式已" + (currentState ? "关闭" : "开启")),
                true
        );
        ModMessages.getChannel().send(PacketDistributor.PLAYER.with(() -> player), new S2CUpdateBuildModePacket(!currentState));
    }
}