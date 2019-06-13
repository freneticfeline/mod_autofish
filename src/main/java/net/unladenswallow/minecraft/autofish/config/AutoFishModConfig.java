package net.unladenswallow.minecraft.autofish.config;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.common.base.Joiner;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.unladenswallow.minecraft.autofish.config.ConfigOption.ValueType;
import net.unladenswallow.minecraft.autofish.util.Logger;
import net.minecraftforge.fml.ModLoadingContext;

public class AutoFishModConfig {
    /*
     * This is probably an overly-complicated way of handling configs, but I can't for the
     * life of me figure out how to write changes to the config file any other way.
     */

    private static final boolean CONFIG_DEFAULT_AUTOFISH_ENABLE = true;
    private static final boolean CONFIG_DEFAULT_AUTOFISH_MULTIROD = false;
    private static final boolean CONFIG_DEFAULT_AUTOFISH_PREVENTBREAK = false;
    private static final boolean CONFIG_DEFAULT_AUTOFISH_ENTITYCLEARPROTECT = false;
    private static final int CONFIG_DEFAULT_AUTOFISH_RECASTDELAY = 2;
    private static final boolean CONFIG_DEFAULT_AUTOFISH_FASTFISHING = false;
    private static final boolean CONFIG_DEFAULT_AUTOFISH_AGGRESSIVEBITEDETECTION = false;
    private static final boolean CONFIG_DEFAULT_AUTOFISH_HANDLEPROBLEMS = false;

    private static final Joiner DotJoiner = Joiner.on(".");
    private static final ForgeConfigSpec clientSpec;
    private static final ClientConfig CLIENT;
    private static String ConfigFilePath;
    private static List<ConfigOption> configOptions;
    
    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = specPair.getLeft();
        clientSpec = specPair.getRight();
    }
    
    public static class ClientConfig {
        public BooleanValue config_autofish_enable;
        public BooleanValue config_autofish_multirod;
        public BooleanValue config_autofish_preventBreak;
        public BooleanValue config_autofish_entityClearProtect;
        public IntValue config_autofish_recastDelay;
        public BooleanValue config_autofish_fastFishing;
        public BooleanValue config_autofish_aggressiveBiteDetection;
        public BooleanValue config_autofish_handleProblems;

        ClientConfig(final ForgeConfigSpec.Builder builder) {
            builder.comment("Client config settings")
                .push("client");
            config_autofish_enable = builder
                .comment("Enable AutoFish", "Automatically reel in and re-cast when a fish nibbles the hook. If set to false, all other AutoFish functionality is disabled.")
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
        Logger.info("Loaded AutoFishMod config file {}", configEvent.getConfig().getFileName());
        ConfigFilePath = configEvent.getConfig().getFullPath().toString();
        configOptions = new ArrayList<ConfigOption>();
        configOptions.add(new ConfigOption(CLIENT.config_autofish_enable.getPath(), CLIENT.config_autofish_enable.get(), "AutoFish"));
        configOptions.add(new ConfigOption(CLIENT.config_autofish_multirod.getPath(), CLIENT.config_autofish_multirod.get(), "MultiRod"));
        configOptions.add(new ConfigOption(CLIENT.config_autofish_preventBreak.getPath(), CLIENT.config_autofish_preventBreak.get(), "Break Protection"));
        configOptions.add(new ConfigOption(CLIENT.config_autofish_recastDelay.getPath(), CLIENT.config_autofish_recastDelay.get(), "Re-Cast Delay"));
        configOptions.add(new ConfigOption(CLIENT.config_autofish_entityClearProtect.getPath(), CLIENT.config_autofish_entityClearProtect.get(), "Entity Clear Protection"));
        configOptions.add(new ConfigOption(CLIENT.config_autofish_aggressiveBiteDetection.getPath(),CLIENT.config_autofish_aggressiveBiteDetection.get(), "Aggressive Bite Detection"));
        configOptions.add(new ConfigOption(CLIENT.config_autofish_handleProblems.getPath(), CLIENT.config_autofish_handleProblems.get(), "Handle Problems"));
        configOptions.add(new ConfigOption(CLIENT.config_autofish_fastFishing.getPath(), CLIENT.config_autofish_fastFishing.get(), "Fast Fishing"));
    }

    public static List<ConfigOption> getOrderedConfigValues() {
        return configOptions;
    }



    public static void toggleConfigValue(List<String> configPath) {
        ConfigOption option = findConfigOption(configPath);
        if (option == null) {
            Logger.warning("Unknown config option path: %s", configPath);
        }
        if (option.valueType == ValueType.BOOL) {
            option.boolValue = !option.boolValue;
            saveConfigChange(option.configPath, option.boolValue);
        } else if (option.valueType == ValueType.INT) {
            int minVal, maxVal;
            if (DotJoiner.join(option.configPath).equals(DotJoiner.join(CLIENT.config_autofish_recastDelay.getPath()))) {
                minVal = 1;
                maxVal = 10;
            } else {
                minVal = 0;
                maxVal = 0;
            }
            option.intValue++;
            if (option.intValue > maxVal) {
                option.intValue = minVal;
            }
            saveConfigChange(option.configPath, option.intValue);
        }
    }


    private static void saveConfigChange(List<String> configPath, boolean newValue) {
        CommentedFileConfig commentedFileConfig = CommentedFileConfig.of(AutoFishModConfig.ConfigFilePath);
        commentedFileConfig.load();
        commentedFileConfig.set(configPath, newValue);
        AutoFishModConfig.clientSpec.setConfig(commentedFileConfig);
        commentedFileConfig.save();
        commentedFileConfig.close();
    }

    private static void saveConfigChange(List<String> configPath, int newValue) {
        CommentedFileConfig commentedFileConfig = CommentedFileConfig.of(AutoFishModConfig.ConfigFilePath);
        commentedFileConfig.load();
        commentedFileConfig.set(configPath, newValue);
        AutoFishModConfig.clientSpec.setConfig(commentedFileConfig);
        commentedFileConfig.save();
        commentedFileConfig.close();
    }

    private static ConfigOption findConfigOption(List<String> configPath) {
        if (configPath == null) return null;
        
        for (ConfigOption option : configOptions) {
            if (option.configPath.equals(configPath)) {
                return option;
            }
        }
        
        return null;
    }

    public static boolean autofishEnabled() {
        ConfigOption option = findConfigOption(CLIENT.config_autofish_enable.getPath());
        return option == null ? false : option.boolValue;
    }
    public static boolean multiRodEnabled() {
        ConfigOption option = findConfigOption(CLIENT.config_autofish_multirod.getPath());
        return option == null ? false : option.boolValue;
    }
    public static boolean breakPreventEnabled() {
        ConfigOption option = findConfigOption(CLIENT.config_autofish_preventBreak.getPath());
        return option == null ? false : option.boolValue;
    }
    public static boolean entityClearProtectEnabled() {
        ConfigOption option = findConfigOption(CLIENT.config_autofish_entityClearProtect.getPath());
        return option == null ? false : option.boolValue;
    }
    public static boolean fashFishingEnabled() {
        ConfigOption option = findConfigOption(CLIENT.config_autofish_fastFishing.getPath());
        return option == null ? false : option.boolValue;
    }
    public static boolean aggressiveBiteDetectionEnabled() {
        ConfigOption option = findConfigOption(CLIENT.config_autofish_aggressiveBiteDetection.getPath());
        return option == null ? false : option.boolValue;
    }
    public static boolean handleProblemsEnabled() {
        ConfigOption option = findConfigOption(CLIENT.config_autofish_handleProblems.getPath());
        return option == null ? false : option.boolValue;
    }
    public static int recastDelay() {
        ConfigOption option = findConfigOption(CLIENT.config_autofish_recastDelay.getPath());
        return option == null ? CONFIG_DEFAULT_AUTOFISH_RECASTDELAY : option.intValue;
    }

}

