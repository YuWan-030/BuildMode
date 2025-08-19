package me.alini.buildmode;
import me.alini.buildmode.effect.ModEffects;
import me.alini.buildmode.network.ModMessages;
import me.alini.buildmode.items.ModItems;
import me.alini.buildmode.whitelist.WhitelistManager;
import me.alini.buildmode.region.RegionManager;
import net.minecraft.server.level.ServerPlayer;
import me.alini.buildmode.effect.ModPotions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BuildMode.MODID)
public class BuildMode {
    public static final String MODID = "buildmode";

    public BuildMode() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // 注册药水
        ModEffects.register();
        // 注册物品
        ModItems.register();
        // 注册药水效果
        ModPotions.register();

        // 注册网络
        ModMessages.register();

        // 注册事件
        MinecraftForge.EVENT_BUS.register(this);
    }

    // Tick 检测玩家是否在区域内
    private void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            RegionManager.tickPlayer((ServerPlayer) event.player);
        }
    }
}