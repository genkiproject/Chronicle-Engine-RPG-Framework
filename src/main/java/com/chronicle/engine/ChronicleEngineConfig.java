package com.chronicle.engine;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ChronicleEngineConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.IntValue MAX_OWNED_TEAMS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("teams");
        MAX_OWNED_TEAMS = builder
                .comment("Maximum number of teams a single player can create.")
                .defineInRange("maxOwnedTeamsPerPlayer", 2, 0, 64);
        builder.pop();
        COMMON_SPEC = builder.build();
    }

    private ChronicleEngineConfig() {
    }

    public static int maxOwnedTeams() {
        return Math.max(0, MAX_OWNED_TEAMS.get());
    }
}
