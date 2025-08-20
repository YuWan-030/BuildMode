package me.alini.buildmode;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class Config {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static class Common {
        public final ForgeConfigSpec.BooleanValue enableWhitelist;
        public final ForgeConfigSpec.IntValue maxRegionCount;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("general");
            enableWhitelist = builder
                    .comment("是否启用物品白名单")
                    .define("enableWhitelist", true);
            maxRegionCount = builder
                    .comment("最大区域数量")
                    .defineInRange("maxRegionCount", 32, 1, 256);
            builder.pop();
        }
    }
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "buildmode-common.toml");
    }
}