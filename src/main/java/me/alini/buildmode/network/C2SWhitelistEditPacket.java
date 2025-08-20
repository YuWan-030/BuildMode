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
    private static final java.util.Map<java.util.UUID, Long> cooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 500;

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
            LuckPermsApi.checkPlayerPermissionAsync(player, "buildmode.admin").thenAccept(hasAdminPerm -> {
                if (!isOp && !hasAdminPerm) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("buildmode.no_permission"), false
                    );
                    return;
                }
                long now = System.currentTimeMillis();
                long last = cooldowns.getOrDefault(player.getUUID(), 0L);
                if (now - last < COOLDOWN_MS) {
                    // 可选：提示冷却中
                    return;
                }
                cooldowns.put(player.getUUID(), now);

                Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
                if (item == null) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("buildmode.item_not_found"), false
                    );
                    return;
                }

                if (add) {
                    WhitelistManager.add(item);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("buildmode.whitelist_add_success"), false
                    );
                } else {
                    WhitelistManager.remove(item);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("buildmode.whitelist_remove_success"), false
                    );
                }

                WhitelistManager.save();
                S2CSyncWhitelistPacket.syncToAll();
            });
        });
        ctx.get().setPacketHandled(true);
    }
}