package net.unladenswallow.minecraft.autofish.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.unladenswallow.minecraft.autofish.AutoFish;
import net.unladenswallow.minecraft.autofish.config.AutoFishModConfig;

public class EventListener { // implements IWorldEventListener {
    
    // The core AutoFish logic that we will use to respond to events
    private AutoFish _autoFish;
    
    public EventListener(AutoFish autoFish) {
        _autoFish = autoFish;
    }

    
    /**********************************************************************/
    /***************   MinecraftForge.EVENT_BUS Events  *******************/
    /**********************************************************************/
    
    
    /**
     * The main AutoFish algorithm occurs on each client tick
     * 
     * @param event
     */
    @SubscribeEvent
    public void onClientTickEvent(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            _autoFish.onClientTick();
        }
    }
    
    /**
     * The Fast Fishing option is implemented on server tick
     * 
     * @param event
     */
    @SubscribeEvent
    public void onServerTickEvent(ServerTickEvent event) {
        /*
         * Easter Egg:  Perform the Fast Fishing on the server tick event and apply to
         * all players, so that it will also affect all players that join a single player game that has
         * been opened to LAN play.
         */
        if (AutoFishModConfig.autofishEnabled() && AutoFishModConfig.fashFishingEnabled() && event.phase == Phase.END) {
            _autoFish.triggerBites();
        }
    }
    
    /**
     * Use the PlaySoundEvent to listen for bobber splashing
     * 
     * @param event
     */
    @SubscribeEvent
    public void onPlaySoundEvent(PlaySoundEvent event) {
        if (AutoFishModConfig.autofishEnabled() && SoundEvents.ENTITY_FISHING_BOBBER_SPLASH.getName() == event.getSound().getSoundLocation()) {
            _autoFish.onBobberSplashDetected(event.getSound().getX(), event.getSound().getY(), event.getSound().getZ());
        }
    }
    
    
    /**
     * Use the RightClickItem Event to track some state information whenever
     * the user stops or starts fishing.
     * 
     * @param event
     */
    @SubscribeEvent
    public void onPlayerUseItem(PlayerInteractEvent.RightClickItem event) {
        // Only do this on the client side
        if (AutoFishModConfig.autofishEnabled() && event.getWorld().isRemote) {
            _autoFish.onPlayerUseItem(event.getHand());
        }
    }
    
    
    /**
     * Trigger saving of configuration when changed by the user
     * 
     * @param event
     */
//    @SubscribeEvent
//    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
//        if (event.getModID().equals(ModAutoFish.MODID)) {
//            ModAutoFish.syncConfig();
//        }
//    }

    @SubscribeEvent
    public void onEntitySpawned(EntityJoinWorldEvent event) {
        if (AutoFishModConfig.autofishEnabled() && event.getWorld().isRemote) {
            Entity entity = event.getEntity();
            if (entity instanceof ExperienceOrbEntity) {
                _autoFish.onXpOrbAdded(entity.posX, entity.posY, entity.posZ);
            }
        }
    }


    /**********************************************************************/
    /******************   WorldEventListener Events  **********************/
    /**********************************************************************/
    
    
//    @Override
//    public void addParticle(IParticleData particleData, boolean alwaysRender, double x, double y, double z,
//            double xSpeed, double ySpeed, double zSpeed) {
//        addParticle(particleData, alwaysRender, false, x, y, z, xSpeed, ySpeed, zSpeed);
//        
//    }
//
//
//    @Override
//    public void addParticle(IParticleData particleData, boolean ignoreRange, boolean minimizeLevel, double x, double y,
//            double z, double xSpeed, double ySpeed, double zSpeed) {
//        if (AutoFishModConfig.autofishEnabled() && particleData.getType() == Particles.FISHING) {
//            _autoFish.onWaterWakeDetected(x, y, z);
//        }
//    }
//
//    @Override
//    public void onEntityAdded(Entity entityIn) {
//        if (AutoFishModConfig.autofishEnabled() && entityIn instanceof EntityXPOrb) {
//            _autoFish.onXpOrbAdded(entityIn.posX, entityIn.posY, entityIn.posZ);
//        }
//    }

}
