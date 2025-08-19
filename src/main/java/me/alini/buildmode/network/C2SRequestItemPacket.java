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

            Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) return;

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