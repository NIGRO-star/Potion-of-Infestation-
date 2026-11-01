package com.infestationpotion;

import net.minecraftforge.common.ForgeConfigSpec;

public class InfestationConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue SPAWN_CHANCE;
    public static final ForgeConfigSpec.IntValue MAX_SILVERFISH;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("infestation");
        SPAWN_CHANCE = builder
                .comment("Chance (0.0 - 1.0) that silverfish spawn each time an infested entity takes damage. Vanilla 1.21 = 0.10")
                .defineInRange("spawnChance", 0.10D, 0.0D, 1.0D);
        MAX_SILVERFISH = builder
                .comment("Maximum silverfish spawned per trigger (1 to this value). Vanilla 1.21 = 2")
                .defineInRange("maxSilverfish", 2, 1, 10);
        builder.pop();
        SPEC = builder.build();
    }
}
