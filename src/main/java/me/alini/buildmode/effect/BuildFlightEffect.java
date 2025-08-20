// 建造飞行药水
package me.alini.buildmode.effect;


import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

import javax.annotation.Nullable;

public class BuildFlightEffect extends MobEffect {
    public BuildFlightEffect() {
        super(MobEffectCategory.BENEFICIAL,0xFFFACD);// 暖白色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof ServerPlayer player) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        if (entity instanceof ServerPlayer player) {
            if (!player.isCreative()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }
    public @Nullable net.minecraft.resources.ResourceLocation getIcon() {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("buildmode", "textures/mob_effect/buildfly.png");
    }
}