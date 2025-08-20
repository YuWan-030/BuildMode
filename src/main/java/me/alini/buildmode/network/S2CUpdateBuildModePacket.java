package me.alini.buildmode.network;

import me.alini.buildmode.client.ClientBuildModeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CUpdateBuildModePacket {
    private final boolean isInBuildMode;

    public S2CUpdateBuildModePacket(boolean isInBuildMode) {
        this.isInBuildMode = isInBuildMode;
    }

    public static S2CUpdateBuildModePacket fromBytes(FriendlyByteBuf buf) {
        return new S2CUpdateBuildModePacket(buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(isInBuildMode);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端处理建造模式状态
            ClientBuildModeManager.setBuildMode(isInBuildMode);
        });
        ctx.get().setPacketHandled(true);
    }
}