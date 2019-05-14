package net.unladenswallow.minecraft.autofish.config;

import org.apache.commons.lang3.tuple.Pair;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.unladenswallow.minecraft.autofish.util.Logger;
import net.minecraftforge.fml.ModLoadingContext;

public class AutoFishModConfig {

    private static final ForgeConfigSpec clientSpec;
    public static final ClientConfig CLIENT;
    
    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = specPair.getLeft();
        clientSpec = specPair.getRight();
    }
    
    public static class ClientConfig {
        public static BooleanValue config_autofish_enable;
        private static final boolean CONFIG_DEFAULT_AUTOFISH_ENABLE = true;
        public static BooleanValue config_autofish_multirod;
        private static final boolean CONFIG_DEFAULT_AUTOFISH_MULTIROD = false;
        public static BooleanValue config_autofish_preventBreak;
        private static final boolean CONFIG_DEFAULT_AUTOFISH_PREVENTBREAK = false;
        public static BooleanValue config_autofish_entityClearProtect;
        private static final boolean CONFIG_DEFAULT_AUTOFISH_ENTITYCLEARPROTECT = false;
        public static IntValue config_autofish_recastDelay;
        private static final int CONFIG_DEFAULT_AUTOFISH_RECASTDELAY = 2;
        public static BooleanValue config_autofish_fastFishing;
        private static final boolean CONFIG_DEFAULT_AUTOFISH_FASTFISHING = false;
        public static BooleanValue config_autofish_aggressiveBiteDetection;
        private static final boolean CONFIG_DEFAULT_AUTOFISH_AGGRESSIVEBITEDETECTION = false;
        public static BooleanValue config_autofish_handleProblems;
        private static final boolean CONFIG_DEFAULT_AUTOFISH_HANDLEPROBLEMS = false;

        ClientConfig(final ForgeConfigSpec.Builder builder) {
            builder.comment("Client config settings")
                .push("client");
            config_autofish_enable = builder
                .comment("Enable AutoFish")
                .define("config_autofish_enable", CONFIG_DEFAULT_AUTOFISH_ENABLE);
            config_autofish_multirod = builder
                    .comment("Enable MultiRod")
                    .define("config_autofish_multirod", CONFIG_DEFAULT_AUTOFISH_MULTIROD);
            config_autofish_preventBreak = builder
                    .comment("Enable Break Protection")
                    .define("config_autofish_preventBreak", CONFIG_DEFAULT_AUTOFISH_PREVENTBREAK);
            config_autofish_entityClearProtect = builder
                    .comment("Enable Entity Clear Protection")
                    .define("config_autofish_entityClearProtect", CONFIG_DEFAULT_AUTOFISH_ENTITYCLEARPROTECT);
            config_autofish_recastDelay = builder
                    .comment("Re-Cast Delay")
                    .defineInRange("config_autofish_recastDelay", CONFIG_DEFAULT_AUTOFISH_RECASTDELAY, 1, 10);
            config_autofish_fastFishing = builder
                    .comment("Enable Fast Fishing")
                    .define("config_autofish_fastFishing", CONFIG_DEFAULT_AUTOFISH_FASTFISHING);
            config_autofish_aggressiveBiteDetection = builder
                    .comment("Enable Aggressive Bite Detection")
                    .define("config_autofish_aggressiveBiteDetection", CONFIG_DEFAULT_AUTOFISH_AGGRESSIVEBITEDETECTION);
            config_autofish_handleProblems = builder
                    .comment("Handle Problems")
                    .define("config_autofish_handleProblems", CONFIG_DEFAULT_AUTOFISH_HANDLEPROBLEMS);
            
            builder.pop();
        }
    }
    
    public static void register(final ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, clientSpec);
    }
    
    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {
        Logger.debug("Loaded AutoFishMod config file {}", configEvent.getConfig().getFileName());
    }

    @SubscribeEvent
    public static void onFileChange(final ModConfig.ConfigReloading configEvent) {
        Logger.fatal("AutoFishMod config just got changed on the file system!");
    }


}
