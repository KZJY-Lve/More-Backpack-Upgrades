package com.kzjy.mobackup;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 模组配置类
 */
public class Config {
    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        Pair<Common, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = commonSpecPair.getLeft();
        COMMON_SPEC = commonSpecPair.getRight();
    }

    /**
     * 通用配置部分
     */
    public static class Common {
        // 次元磁吸升级的吸附范围半径
        public final ForgeConfigSpec.IntValue dimensionalMagnetRange;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("Upgrades");

            dimensionalMagnetRange = builder.comment("次元磁吸升级的吸附范围")
                    .defineInRange("dimensionalMagnetRange", 5, 1, 64);

            builder.pop();
        }
    }
}