package net.unladenswallow.minecraft.autofish;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.unladenswallow.minecraft.autofish.config.AutoFishModConfig;
import net.unladenswallow.minecraft.autofish.events.EventListener;
import net.unladenswallow.minecraft.autofish.events.KeyInputHandler;
//import net.unladenswallow.minecraft.autofish.gui.ConfigGui;
import net.unladenswallow.minecraft.autofish.util.Logger;

@Mod(ModAutoFish.MODID)
public class ModAutoFish {
    public static final String MODID = "mod_autofish";
    
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
//        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigGui(screen));
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

}
