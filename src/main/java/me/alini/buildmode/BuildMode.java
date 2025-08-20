package me.alini.buildmode;
import me.alini.buildmode.effect.ModEffects;
import me.alini.buildmode.network.ModMessages;
import me.alini.buildmode.items.ModItems;
import me.alini.buildmode.whitelist.WhitelistManager;
import me.alini.buildmode.region.RegionManager;
import net.minecraft.server.level.ServerPlayer;
import me.alini.buildmode.effect.ModPotions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Path;

@Mod(BuildMode.MODID)
@Mod.EventBusSubscriber
public class BuildMode {
    public static final String MODID = "buildmode";

    public BuildMode() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEffects.register();
        ModItems.register();
        ModPotions.register();
        Config.register();
        ModMessages.register();
        // 不再需要 register(this)

        // 配置初始化
        Path configDir = FMLPaths.CONFIGDIR.get();
        RegionManager.init(configDir);
        WhitelistManager.init(configDir);
    }

    // Tick 检测玩家是否在区域内
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            if (event.player instanceof ServerPlayer serverPlayer) {
                RegionManager.tickPlayer(serverPlayer);
            }
        }
    }
}