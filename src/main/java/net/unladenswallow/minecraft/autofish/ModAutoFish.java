package net.unladenswallow.minecraft.autofish;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.unladenswallow.minecraft.autofish.config.AutoFishModConfig;
import net.unladenswallow.minecraft.autofish.events.EventListener;
import net.unladenswallow.minecraft.autofish.events.KeyInputHandler;
import net.unladenswallow.minecraft.autofish.util.Logger;

@Mod(ModAutoFish.MODID)
//@Mod.EventBusSubscriber(modid = ModAutoFish.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModAutoFish {
    public static final String MODID = "mod_autofish";
    
//    private static final List<String> propertyOrder = new ArrayList<String>(Arrays.asList(new String[] {
//            "Enable AutoFish",
//            "Enable MultiRod",
//            "Enable Break Protection",
//            "Re-Cast Delay",
//            "Enable Entity Clear Protection",
//            "Enable Aggressive Bite Detection",
//            "Handle Problems",
//            "Enable Fast Fishing"
//    }));
    
    public static AutoFish autoFish = new AutoFish();
    public static EventListener eventListener = new EventListener(autoFish);
    
    public static ModAutoFish instance;
            
    public ModAutoFish() {
        instance = this;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientInit);
        FMLJavaModLoadingContext.get().getModEventBus().register(AutoFishModConfig.class);
        MinecraftForge.EVENT_BUS.register(this);
        AutoFishModConfig.register(ModLoadingContext.get());
    }
    
    @SubscribeEvent
    public void preInit(final FMLCommonSetupEvent preInitEvent) {
    }
    
    @SubscribeEvent
    public void clientInit (final FMLClientSetupEvent event) {
        Logger.info("(client) Initializing " + ModAutoFish.MODID);
        MinecraftForge.EVENT_BUS.register(ModAutoFish.eventListener);
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
    }

//    public static void syncConfig() {
//        config_autofish_enable = configFile.getBoolean("Enable AutoFish", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_ENABLE, "Automatically reel in and re-cast when a fish nibbles the hook. If set to false, all other AutoFish functionality is disabled.");
//        config_autofish_multirod = configFile.getBoolean("Enable MultiRod", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_MULTIROD, "Automatically switch to a new fishing rod when the current rod breaks, if one is available in the hotbar.");
//        config_autofish_preventBreak = configFile.getBoolean("Enable Break Protection", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_PREVENTBREAK, "Stop fishing or switch to a new rod just before the current rod breaks.  Useful if you want to repair your enchanted fishing rods.");
//        config_autofish_entityClearProtect = configFile.getBoolean("Enable Entity Clear Protection", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_ENTITYCLEARPROTECT, "When playing on a server, re-cast after the server clears entities.  Useful if you are playing on a server that periodically deletes all entities (including fishing hooks) from the world, which causes you to stop fishing.");
//        config_autofish_recastDelay = configFile.getInt("Re-Cast Delay", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_RECASTDELAY, 1, 10, "Time (in seconds) to wait before automatically re-casting. Increase this value if server lag causes re-casting to fail.");
//        config_autofish_fastFishing = configFile.getBoolean("Enable Fast Fishing", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_FASTFISHING, "[SINGLE PLAYER ONLY] Fish will bite right after casting.");
//        config_autofish_aggressiveBiteDetection = configFile.getBoolean("Enable Aggressive Bite Detection", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_AGGRESSIVEBITEDETECTION, "When playing on a server, be more aggressive about detecting fish bites.  Improves multiplayer bite detection from ~85% to ~95%, but false positives will be more likely, especially if other players are fishing very close by.");
//        config_autofish_handleProblems = configFile.getBoolean("Handle Problems", Configuration.CATEGORY_GENERAL, CONFIG_DEFAULT_AUTOFISH_HANDLEPROBLEMS, "[HAS SIDE EFFECTS] Re-cast when problems detected (e.g. if not in water or if a MOB has been hooked).  If enabled, non-fishing use of the fishing rod (e.g. intentionally hooking MOBs) will be affected.");
//        
//        if (configFile.hasChanged()) {
//            configFile.save();
//        }
//    }

}
