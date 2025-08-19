// 药水效果绑定物品
package me.alini.buildmode.effect;


import me.alini.buildmode.BuildMode;
import me.alini.buildmode.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, BuildMode.MODID);

    public static final RegistryObject<Potion> BUILDFLY =
            POTIONS.register("buildfly", () ->
                    new Potion(new MobEffectInstance(ModEffects.BUILDFLY.get(), 20 * 60 * 3))
            );

    public static void register() {
        POTIONS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}