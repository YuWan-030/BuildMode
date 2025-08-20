package me.alini.buildmode.network;

import me.alini.buildmode.BuildModeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SRequestToggleBuildModePacket {
    public C2SRequestToggleBuildModePacket() {}

    public static C2SRequestToggleBuildModePacket fromBytes(FriendlyByteBuf buf) {
        return new C2SRequestToggleBuildModePacket();
    }

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BuildModeManager.toggleBuildMode(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}