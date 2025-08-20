package me.alini.buildmode.items;

import me.alini.buildmode.BuildMode;
import me.alini.buildmode.effect.ModPotions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BuildMode.MODID);

    // 注册建造飞行药水物品
    public static final RegistryObject<Item> BUILDFLY_POTION = ITEMS.register("buildfly_potion", () ->
            new PotionItem(new Item.Properties()) {
                @Override
                public @NotNull ItemStack getDefaultInstance() {
                    // 绑定自定义药水效果
                    ItemStack stack = super.getDefaultInstance();
                    PotionUtils.setPotion(stack, ModPotions.BUILDFLY.get());
                    return stack;
                }
            }
    );
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