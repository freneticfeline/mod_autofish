package net.unladenswallow.minecraft.autofish.events;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.unladenswallow.minecraft.autofish.AutoFish;
import net.unladenswallow.minecraft.autofish.ModAutoFish;
import net.unladenswallow.minecraft.autofish.util.Logger;

public class EventListener implements IWorldAccess {
    
    // The core AutoFish logic that we will use to respond to events
    private AutoFish _autoFish;
    
    private static final String BOBBER_SPLASH_SOUND_NAME = "random.splash";
    
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
        if (ModAutoFish.config_autofish_enable && event.phase == Phase.END) {
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
        if (ModAutoFish.config_autofish_enable && ModAutoFish.config_autofish_fastFishing && event.phase == Phase.END) {
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
        if (ModAutoFish.config_autofish_enable && BOBBER_SPLASH_SOUND_NAME.equals(event.name)) {
            _autoFish.onBobberSplashDetected(event.sound.getXPosF(), event.sound.getYPosF(), event.sound.getZPosF());
        }
    }
    
    /**
     * When the world is loaded, attach ourself as a WorldEventListener
     * 
     * @param event
     */
    @SubscribeEvent
    public void onWorldEvent_Load(WorldEvent.Load event) {
        if (event.world != null && event.world.isRemote) {
            Logger.info("Attaching WorldEventListener");
            event.world.addWorldAccess(this);
        }
    }
    
    /**
     * Use the RightClickItem Event to track some state information whenever
     * the user stops or starts fishing.
     * 
     * @param event
     */
    @SubscribeEvent
    public void onPlayerUseItem(PlayerInteractEvent event) {
        // Only do this on the client side
        if (ModAutoFish.config_autofish_enable && event.action == Action.RIGHT_CLICK_AIR && event.world.isRemote) {
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
        if (event.modID.equals(ModAutoFish.MODID)) {
            ModAutoFish.syncConfig();
        }
    }
    

    /**********************************************************************/
    /******************   WorldEventListener Events  **********************/
    /**********************************************************************/
    
    
    @Override
    public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord,
            double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        if (ModAutoFish.config_autofish_enable && particleID == EnumParticleTypes.WATER_WAKE.getParticleID()) {
//            AutoFishLogger.info("Particle WATER_WAKE spawned with id %d at [%f, %f, %f]", particleID, xCoord, yCoord, zCoord);
            _autoFish.onWaterWakeDetected(xCoord, yCoord, zCoord);
        }
        
    }

    @Override
    public void onEntityAdded(Entity entityIn) {
        if (ModAutoFish.config_autofish_enable && entityIn instanceof EntityXPOrb) {
            _autoFish.onXpOrbAdded(entityIn.posX, entityIn.posY, entityIn.posZ);
        }
    }

    /********  Ignored World Events *********/
    
    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}

    @Override
    public void onEntityRemoved(Entity entityIn) {}

    @Override
    public void markBlockForUpdate(BlockPos pos) {}


    @Override
    public void notifyLightSet(BlockPos pos) {}


    @Override
    public void playSound(String soundName, double x, double y, double z, float volume, float pitch) {}


    @Override
    public void playSoundToNearExcept(EntityPlayer except, String soundName, double x, double y, double z, float volume,
            float pitch) {}


    @Override
    public void playRecord(String recordName, BlockPos blockPosIn) {}


    @Override
    public void broadcastSound(int p_180440_1_, BlockPos p_180440_2_, int p_180440_3_) {}


    @Override
    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {}


    @Override
    public void playAuxSFX(EntityPlayer player, int sfxType, BlockPos blockPosIn, int p_180439_4_) {}
}
