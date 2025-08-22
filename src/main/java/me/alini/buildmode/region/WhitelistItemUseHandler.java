package me.alini.buildmode.region;

import me.alini.buildmode.region.RegionManager;
import me.alini.buildmode.whitelist.WhitelistManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class WhitelistItemUseHandler {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItemStack();
        ResourceLocation id = stack.getItem().builtInRegistryHolder().key().location();
        // OP 或有权限直接放行
        if (player.hasPermissions(4)) return;
        // 使用服务端白名单
        if (!WhitelistManager.isWhitelisted(stack.getItem())) return;
        if (!RegionManager.isPlayerInAnyRegion(player)) {
            event.setCanceled(true);
            player.setItemInHand(event.getHand(), ItemStack.EMPTY);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("该物品只能在建造区域内使用")
                            .withStyle(style -> style.withColor(0xFF3333)), // 红色
                    true
            );
        }
    }
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItemStack();
        ResourceLocation id = stack.getItem().builtInRegistryHolder().key().location();
        // OP 或有权限直接放行
        if (player.hasPermissions(4)) return;
        // 使用服务端白名单
        if (!WhitelistManager.isWhitelisted(stack.getItem())) return;
        if (!RegionManager.isPlayerInAnyRegion(player)) {
            event.setCanceled(true);
            player.setItemInHand(event.getHand(), ItemStack.EMPTY);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("该物品只能在建造区域内使用")
                            .withStyle(style -> style.withColor(0xFF3333)), // 红色
                    true
            );
        }
    }

}