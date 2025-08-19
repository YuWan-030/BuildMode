// 同步白名单
package me.alini.buildmode.network;

import me.alini.buildmode.client.BuildModeScreen;
import me.alini.buildmode.client.ClientWhitelist;
import me.alini.buildmode.whitelist.WhitelistManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class S2CSyncWhitelistPacket {
    private final Set<ResourceLocation> whitelist;

    public S2CSyncWhitelistPacket(Set<ResourceLocation> whitelist) {
        this.whitelist = whitelist;
    }

    public S2CSyncWhitelistPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        whitelist = new HashSet<>();
        for (int i = 0; i < size; i++) {
            whitelist.add(buf.readResourceLocation());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(whitelist.size());
        for (ResourceLocation id : whitelist) {
            buf.writeResourceLocation(id);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 更新本地白名单
            ClientWhitelist.setWhitelist(this.whitelist);
            // 刷新界面
            if (Minecraft.getInstance().screen instanceof BuildModeScreen screen) {
                screen.updateFilteredItems();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // 工具方法：服务端广播
    public static void syncToAll() {
        ModMessages.getChannel().send(
                PacketDistributor.ALL.noArg(),
                new S2CSyncWhitelistPacket(WhitelistManager.getWhitelist())
        );
    }
}