package net.unladenswallow.minecraft.autofish;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = ModAutoFish.MODID, useMetadata = true, acceptedMinecraftVersions="[1.12,1.13)", acceptableRemoteVersions="[1.12,1.13)",
guiFactory = "net.unladenswallow.minecraft.autofish.AutoFishGuiFactory")
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
    
    @SidedProxy(clientSide="net.unladenswallow.minecraft.autofish.ClientProxy", serverSide="net.unladenswallow.minecraft.autofish.ServerProxy")
    public static CommonProxy proxy;
    
    public static AutoFishEventHandler eventHandler = new AutoFishEventHandler();
    
    public static ModAutoFish instance = new ModAutoFish();
            
    @EventHandler
    public void preInit(FMLPreInitializationEvent preInitEvent) {
        ModAutoFish.proxy.preInit(preInitEvent);
        configFile = new Configuration(preInitEvent.getSuggestedConfigurationFile());
        syncConfig();
    }
    
    @EventHandler
    public void init (FMLInitializationEvent event) {
        AutoFishLogger.info("Initializing " + ModAutoFish.MODID);
        ModAutoFish.proxy.init(event);
    }

    public static void syncConfig() {
        config_autofish_enable = configFile.getBoolean("Enable AutoFish", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_ENABLE, "Automatically reel in and re-cast when a fish nibbles the hook.");
        config_autofish_multirod = configFile.getBoolean("Enable MultiRod", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_MULTIROD, "Automatically switch to a new fishing rod when the current rod breaks, if one is available in the hotbar.");
        config_autofish_preventBreak = configFile.getBoolean("Enable Break Protection", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_PREVENTBREAK, "Stop fishing or switch to a new rod before the current rod breaks.");
        config_autofish_entityClearProtect = configFile.getBoolean("Enable Enity Clear Protection", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_ENTITYCLEARPROTECT, "Re-cast after the server clears entities. EXPERIMENTAL");
        config_autofish_recastDelay = configFile.getInt("Re-Cast Delay", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_RECASTDELAY, 1, 10, "Time (in seconds) to wait before automatically re-casting. Increase this value if server lag causes re-casting to fail.");
        config_autofish_fastFishing = configFile.getBoolean("Fast Fishing", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_FASTFISHING, "(Single Player Only) Fish will bite right after casting.");
        
        if (configFile.hasChanged()) {
            configFile.save();
        }
    }

}
