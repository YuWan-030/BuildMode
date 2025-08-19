package me.alini.buildmode.items;

import me.alini.buildmode.BuildMode;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BuildMode.MODID);

    // 注册建造飞行药水物品
    public static final RegistryObject<Item> BUILDFLY_POTION =
            ITEMS.register("buildfly_potion", () -> new PotionItem(new Item.Properties().stacksTo(1)));

    public static void register() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}

@Mod.EventBusSubscriber(modid = BuildMode.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
class ModCreativeTab {
    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModItems.BUILDFLY_POTION);
        }
    }
}