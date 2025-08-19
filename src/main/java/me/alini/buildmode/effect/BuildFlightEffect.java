// 建造飞行药水
package me.alini.buildmode.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class BuildFlightEffect extends MobEffect {
    public BuildFlightEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00FFAA); // 青绿色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof net.minecraft.server.level.ServerPlayer player) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}