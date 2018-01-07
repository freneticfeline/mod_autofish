package net.unladenswallow.minecraft.autofish;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.unladenswallow.minecraft.autofish.events.EventListener;
import net.unladenswallow.minecraft.autofish.proxy.CommonProxy;
import net.unladenswallow.minecraft.autofish.util.Logger;

@Mod(modid = ModAutoFish.MODID, useMetadata = true, acceptedMinecraftVersions="[1.9,1.10)", acceptableRemoteVersions="[1.9,1.10)",
guiFactory = "net.unladenswallow.minecraft.autofish.gui.GuiFactory")
public class ModAutoFish {
    public static final String MODID = "mod_autofish";
    
    public static Configuration configFile;
    public static boolean config_autofish_enable;
    public static final boolean CONFIG_DEFAULT_AUTOFISH_ENABLE = true;
    public static boolean config_autofish_multirod;
    public static final boolean CONFIG_DEFAULT_AUTOFISH_MULTIROD = false;
    public static boolean config_autofish_preventBreak;
    public static final boolean CONFIG_DEFAULT_AUTOFISH_PREVENTBREAK = false;
    public static boolean config_autofish_entityClearProtect;
    public static final boolean CONFIG_DEFAULT_AUTOFISH_ENTITYCLEARPROTECT = false;
    public static int config_autofish_recastDelay;
    public static final int CONFIG_DEFAULT_AUTOFISH_RECASTDELAY = 2;
    public static boolean config_autofish_fastFishing;
    public static final boolean CONFIG_DEFAULT_AUTOFISH_FASTFISHING = false;
    public static boolean config_autofish_aggressiveBiteDetection;
    public static final boolean CONFIG_DEFAULT_AUTOFISH_AGGRESSIVEBITEDETECTION = false;
    public static boolean config_autofish_handleProblems;
    public static final boolean CONFIG_DEFAULT_AUTOFISH_HANDLEPROBLEMS = false;
    
    private static final List<String> propertyOrder = new ArrayList<String>(Arrays.asList(new String[] {
            "Enable AutoFish",
            "Enable MultiRod",
            "Enable Break Protection",
            "Re-Cast Delay",
            "Enable Entity Clear Protection",
            "Enable Aggressive Bite Detection",
            "Handle Problems",
            "Enable Fast Fishing"
    }));
    
    @SidedProxy(clientSide="net.unladenswallow.minecraft.autofish.proxy.ClientProxy", serverSide="net.unladenswallow.minecraft.autofish.proxy.ServerProxy")
    public static CommonProxy proxy;
    
    public static AutoFish autoFish = new AutoFish();
    public static EventListener eventListener = new EventListener(autoFish);
    
    public static ModAutoFish instance = new ModAutoFish();
            
    @EventHandler
    public void preInit(FMLPreInitializationEvent preInitEvent) {
        ModAutoFish.proxy.preInit(preInitEvent);
        configFile = new Configuration(preInitEvent.getSuggestedConfigurationFile());
        configFile.renameProperty(Configuration.CATEGORY_GENERAL, "Fast Fishing", "Enable Fast Fishing");
        configFile.renameProperty(Configuration.CATEGORY_GENERAL, "Enable Enity Clear Protection", "Enable Entity Clear Protection");
        configFile.setCategoryPropertyOrder(Configuration.CATEGORY_GENERAL, propertyOrder);
        syncConfig();
    }
    
    @EventHandler
    public void init (FMLInitializationEvent event) {
        Logger.info("Initializing " + ModAutoFish.MODID);
        ModAutoFish.proxy.init(event);
    }

    public static void syncConfig() {
        config_autofish_enable = configFile.getBoolean("Enable AutoFish", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_ENABLE, "Automatically reel in and re-cast when a fish nibbles the hook. If set to false, all other AutoFish functionality is disabled.");
        config_autofish_multirod = configFile.getBoolean("Enable MultiRod", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_MULTIROD, "Automatically switch to a new fishing rod when the current rod breaks, if one is available in the hotbar.");
        config_autofish_preventBreak = configFile.getBoolean("Enable Break Protection", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_PREVENTBREAK, "Stop fishing or switch to a new rod just before the current rod breaks.  Useful if you want to repair your enchanted fishing rods.");
        config_autofish_entityClearProtect = configFile.getBoolean("Enable Entity Clear Protection", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_ENTITYCLEARPROTECT, "When playing on a server, re-cast after the server clears entities.  Useful if you are playing on a server that periodically deletes all entities (including fishing hooks) from the world, which causes you to stop fishing.");
        config_autofish_recastDelay = configFile.getInt("Re-Cast Delay", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_RECASTDELAY, 1, 10, "Time (in seconds) to wait before automatically re-casting. Increase this value if server lag causes re-casting to fail.");
        config_autofish_fastFishing = configFile.getBoolean("Enable Fast Fishing", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_FASTFISHING, "[SINGLE PLAYER ONLY] Fish will bite right after casting.");
        config_autofish_aggressiveBiteDetection = configFile.getBoolean("Enable Aggressive Bite Detection", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_AGGRESSIVEBITEDETECTION, "When playing on a server, be more aggressive about detecting fish bites.  Improves multiplayer bite detection from ~85% to ~95%, but false positives will be more likely, especially if other players are fishing very close by.");
        config_autofish_handleProblems = configFile.getBoolean("Handle Problems", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_HANDLEPROBLEMS, "[HAS SIDE EFFECTS] Re-cast when problems detected (e.g. if not in water or if a MOB has been hooked).  If enabled, non-fishing use of the fishing rod (e.g. intentionally hooking MOBs) will be affected.");
        
        if (configFile.hasChanged()) {
            configFile.save();
        }
    }

}
