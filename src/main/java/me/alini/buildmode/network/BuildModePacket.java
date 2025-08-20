package me.alini.buildmode.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import me.alini.buildmode.client.ClientBuildModeManager;

import java.util.function.Supplier;

public class BuildModePacket {
    private final boolean inBuildMode;

    public BuildModePacket(boolean inBuildMode) {
        this.inBuildMode = inBuildMode;
    }

    // 解码构造函数
    public BuildModePacket(FriendlyByteBuf buf) {
        this.inBuildMode = buf.readBoolean();
    }

    // 编码方法
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(inBuildMode);
    }

    // 解码方法
    public static BuildModePacket decode(FriendlyByteBuf buf) {
        return new BuildModePacket(buf.readBoolean());
    }

    // 处理方法（只在客户端调用）
    public static void handle(BuildModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientBuildModeManager.setBuildMode(msg.inBuildMode);
        });
        ctx.get().setPacketHandled(true);
    }
}