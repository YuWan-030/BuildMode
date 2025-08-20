// 请求物品
package me.alini.buildmode.network;


import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SRequestItemPacket {
    private final ResourceLocation itemId;
    private static final java.util.Map<java.util.UUID, Long> cooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 500;

    public C2SRequestItemPacket(ResourceLocation itemId) {
        this.itemId = itemId;
    }

    public C2SRequestItemPacket(FriendlyByteBuf buf) {
        this.itemId = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(itemId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            long now = System.currentTimeMillis();
            long last = cooldowns.getOrDefault(player.getUUID(), 0L);
            if (now - last < COOLDOWN_MS) {
                // 如果在冷却时间内，提示
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("buildmode.request_cooldown"),
                        false
                );
                return;
            }
            cooldowns.put(player.getUUID(), now);

            Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) return;

            if (!me.alini.buildmode.region.RegionManager.isInBuildRegion(player)) {
                return;
            }

            // 检查物品是否在白名单
            if (!me.alini.buildmode.whitelist.WhitelistManager.isWhitelisted(item)) {
                return;
            }

            // 限制数量，最多一组
            int maxGive = Math.min(item.getMaxStackSize(), 64);
            ItemStack give = new ItemStack(item, maxGive);

            // 尝试放入背包
            boolean success = player.getInventory().add(give);
            if (!success) {
                player.drop(give, false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}