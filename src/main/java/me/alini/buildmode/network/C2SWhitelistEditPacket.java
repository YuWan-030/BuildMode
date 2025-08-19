// 请求修改白名单
package me.alini.buildmode.network;


import me.alini.buildmode.util.LuckPermsApi;
import me.alini.buildmode.whitelist.WhitelistManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SWhitelistEditPacket {
    private final ResourceLocation itemId;
    private final boolean add; // true = 添加，false = 删除

    public C2SWhitelistEditPacket(ResourceLocation itemId, boolean add) {
        this.itemId = itemId;
        this.add = add;
    }

    public C2SWhitelistEditPacket(FriendlyByteBuf buf) {
        this.itemId = buf.readResourceLocation();
        this.add = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(itemId);
        buf.writeBoolean(add);
    }


    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            boolean isOp = player.hasPermissions(2);
            // LuckPerms 权限判断（需集成 LuckPerms API）
            boolean hasAdminPerm = LuckPermsApi.checkPlayerPermission(player, "buildmode.admin");

            if (!isOp && !hasAdminPerm) {
                return;
            }

            Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) return;

            if (add) {
                WhitelistManager.add(item);
            } else {
                WhitelistManager.remove(item);
            }

            WhitelistManager.save();
            S2CSyncWhitelistPacket.syncToAll();
        });
        ctx.get().setPacketHandled(true);
    }
}