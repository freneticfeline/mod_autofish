package net.unladenswallow.minecraft.autofish.events;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Particles;
import net.minecraft.init.SoundEvents;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldEventListener;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.unladenswallow.minecraft.autofish.AutoFish;
import net.unladenswallow.minecraft.autofish.ModAutoFish;
import net.unladenswallow.minecraft.autofish.config.AutoFishModConfig;
import net.unladenswallow.minecraft.autofish.util.Logger;

public class EventListener implements IWorldEventListener {
    
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
        if (AutoFishModConfig.CLIENT.config_autofish_enable.get() && event.phase == Phase.END) {
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
        if (AutoFishModConfig.CLIENT.config_autofish_enable.get() && AutoFishModConfig.CLIENT.config_autofish_fastFishing.get() && event.phase == Phase.END) {
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
        if (AutoFishModConfig.CLIENT.config_autofish_enable.get() && SoundEvents.ENTITY_FISHING_BOBBER_SPLASH.getName() == event.getSound().getSoundLocation()) {
            _autoFish.onBobberSplashDetected(event.getSound().getX(), event.getSound().getY(), event.getSound().getZ());
        }
    }
    
    /**
     * When the world is loaded, attach ourself as a WorldEventListener
     * 
     * @param event
     */
    @SubscribeEvent
    public void onWorldEvent_Load(WorldEvent.Load event) {
        if (event.getWorld() != null && event.getWorld().isRemote()) {
            event.getWorld().getWorld().addEventListener(this);
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
        if (AutoFishModConfig.CLIENT.config_autofish_enable.get() && event.getWorld().isRemote) {
            _autoFish.onPlayerUseItem();
        }
    }
    
    
    /**
     * Trigger saving of configuration when changed by the user
     * 
     * @param event
     */
    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(ModAutoFish.MODID)) {
//            ModAutoFish.syncConfig();
        }
    }
    

    /**********************************************************************/
    /******************   WorldEventListener Events  **********************/
    /**********************************************************************/
    
    
    @Override
    public void addParticle(IParticleData particleData, boolean alwaysRender, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed) {
        addParticle(particleData, alwaysRender, false, x, y, z, xSpeed, ySpeed, zSpeed);
        
    }


    @Override
    public void addParticle(IParticleData particleData, boolean ignoreRange, boolean minimizeLevel, double x, double y,
            double z, double xSpeed, double ySpeed, double zSpeed) {
        if (AutoFishModConfig.CLIENT.config_autofish_enable.get() && particleData.getType() == Particles.FISHING) {
            _autoFish.onWaterWakeDetected(x, y, z);
        }
    }

    @Override
    public void onEntityAdded(Entity entityIn) {
        if (AutoFishModConfig.CLIENT.config_autofish_enable.get() && entityIn instanceof EntityXPOrb) {
            _autoFish.onXpOrbAdded(entityIn.posX, entityIn.posY, entityIn.posZ);
        }
    }

    /********  Ignored World Events *********/
    
    @Override
    public void notifyBlockUpdate(IBlockReader worldIn, BlockPos pos, IBlockState oldState, IBlockState newState,
            int flags) {}


    @Override
    public void notifyLightSet(BlockPos pos) {}

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}

    @Override
    public void playSoundToAllNearExcept(EntityPlayer player, SoundEvent soundIn, SoundCategory category, double x,
            double y, double z, float volume, float pitch) {}

    @Override
    public void playRecord(SoundEvent soundIn, BlockPos pos) {}

    @Override
    public void onEntityRemoved(Entity entityIn) {}

    @Override
    public void broadcastSound(int soundID, BlockPos pos, int data) {}

    @Override
    public void playEvent(EntityPlayer player, int type, BlockPos blockPosIn, int data) {}

    @Override
    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {}


}
