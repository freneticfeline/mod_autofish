package net.unladenswallow.minecraft.autofish;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

public class AutoFishEventHandler {

    private Minecraft minecraft;
    private EntityPlayer player;
    private boolean notificationShownToPlayer = false;
    private long castScheduledAt = 0L;
    private long startedReelDelayAt = 0L;
    private long startedCastDelayAt = 0L;
    private boolean isFishing = false;
    private long closeWaterWakeDetectedAt = 0L;
//    private long exactWaterWakeDetectedAt = 0L;
    private long xpLastAddedAt = 0L;
    private long closeBobberSplashDetectedAt = 0L;
//    private long exactBobberSplashDetectedAt = 0L;
    private Random rand;
    
    private static final String NOTIFICATION_TEXT_AUTOFISH_ENABLED = "AutoFish is enabled.  Type 'o' while holding a fishing rod for more options";

    private static final int TICKS_PER_SECOND = 20;

    /** How long to suppress checking for a bite after starting to reel in.  If we check for a bite while reeling
        in, we may think we have a bite and try to reel in again, which will actually cause a re-cast and lose the fish */
    private static final int REEL_TICK_DELAY = 15;

    /** How long to wait after casting to check for Entity Clear.  If we check too soon, the hook entity
        isn't in the world yet, and will trigger a false alarm and cause infinite recasting. */
    private static final int CAST_TICK_DELAY = 20;

    /** When Break Prevention is enabled, how low to let the durability get before stopping or switching rods */
    private static final int AUTOFISH_BREAKPREVENT_THRESHOLD = 2;

    /** The threshold for vertical movement of the fish hook that determines when a fish is biting, if using
        the movement method of detection. 
        and the movement threshold that, combined with other factors, is a probable indicator that a fish is biting */
    private static final double MOTION_Y_THRESHOLD = -0.05d;
    private static final double MOTION_Y_MAYBE_THRESHOLD = -0.03d;
    
    /** The number of ticks to set as the "catchable delay" when Fast Fishing is enabled. *
     * (Vanilla ticksCatchableDelay is random between 20 and 80, but we seem to have trouble catching
     * it if it is less than 40) **/
    private static final int FAST_FISH_CATCHABLE_DELAY_TICKS = 40;
    private static final int FAST_FISH_DELAY_VARIANCE = 40;
    
    /** The maximum number of ticks that is is reasonable for a fish hook to be flying in the air after a cast */
    private static final int MAX_HOOK_FLYING_TIME_TICKS = 80;
    
    /** The amount of time to wait for a fish before something seems wrong and we want to recast **/
    private static final int MAX_WAITING_TIME_SECONDS = 60;
    
    /** The distance (squared) threshold for determining that a water wake is "close" to the fish Hook 
     * and "most certainly at" the fish Hook **/
    private static final double CLOSE_WATER_WAKE_THRESHOLD = 1.0d;
//    private static final double EXACT_WATER_WAKE_THRESHOLD = 0.3d;
    
    /** The number of ticks to wait after detecting a "close" or "exact" water wake before reeling in **/
    private static final int CLOSE_WATER_WAKE_DELAY_TICKS = 30;
//    private static final int EXACT_WATER_WAKE_DELAY_TICKS = 20;
    
    /** The distance (squared) threshold for determining that a bobber splash sound is "close" to the fish Hook
     * and "most certainly at" the fish Hook **/
    private static final double CLOSE_BOBBER_SPLASH_THRESHOLD = 2.0d;
//    private static final double EXACT_BOBBER_SPLASH_THRESHOLD = 0.5d;
    
    
    public AutoFishEventHandler() {
        this.minecraft = FMLClientHandler.instance().getClient();
        this.rand = new Random();
    }

    @SubscribeEvent
    public void onServerTickEvent(ServerTickEvent event) {
        /*
         * Easter Egg:  Perform the Fast Fishing on the server event and apply to
         * all players, so that it will also affect all players that join a single player game that has
         * been opened to LAN play.
         */
        if (ModAutoFish.config_autofish_fastFishing && event.phase == Phase.END) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                triggerBites(server);
            }
        }
    }
    
    @SubscribeEvent
    public void onPlaySoundEvent(PlaySoundEvent event) {
        if (playerHookInWater(this.player)) {
            if (SoundEvents.ENTITY_BOBBER_SPLASH.getSoundName() == event.getSound().getSoundLocation()) {
                EntityFishHook hook = this.player.fishEntity;
//                double yDifference = Math.abs(hook.posY - event.getSound().getYPosF());
                double xzDistanceFromHook = hook.getDistanceSq(event.getSound().getXPosF(), hook.posY, event.getSound().getZPosF());
                if (xzDistanceFromHook <= CLOSE_BOBBER_SPLASH_THRESHOLD) {
//                    AutoFishLogger.info("[%d] Close bobber splash at %f /  %f", this.minecraft.world.getTotalWorldTime(), xzDistanceFromHook, yDifference);
                    this.closeBobberSplashDetectedAt = this.minecraft.world.getTotalWorldTime();
//                    if (xzDistanceFromHook <= EXACT_BOBBER_SPLASH_THRESHOLD) {
//    //                    AutoFishLogger.info("[%d] Exact bobber splash at %f /  %f", this.minecraft.world.getTotalWorldTime(), xzDistanceFromHook, yDifference);
//                        this.exactBobberSplashDetectedAt = this.minecraft.world.getTotalWorldTime();
//                    } 
                }
            }
        }
    }
    
    @SubscribeEvent
    public void onWorldEvent_Load(WorldEvent.Load event) {
        if (event.getWorld() != null && event.getWorld().isRemote) {
            AutoFishLogger.info("Attaching WorldEventListener");
            event.getWorld().addEventListener(new WorldEventListener(this));
        }
    }
    
    @SubscribeEvent
    public void onClientTickEvent(ClientTickEvent event) {
        if (ModAutoFish.config_autofish_enable && !this.minecraft.isGamePaused() && this.minecraft.player != null && event.phase == Phase.END) {
            this.player = this.minecraft.player;
            if (!this.notificationShownToPlayer) {
                showNotificationToPlayer();
            }

            if (playerIsHoldingRod() || waitingToRecast()) {
                if ((playerHookInWater(this.player) && !isDuringReelDelay() && isFishBiting())
                        || somethingSeemsWrong()) {
                    startReelDelay();
                    reelIn();
                    scheduleNextCast();
                } else if (isTimeToCast()) {
                    if (!rodIsCast()) {
                        if (needToSwitchRods()) {
                            tryToSwitchRods();
                        }
                        if (playerCanCast()) {
                            startFishing();
                        }
                    } else {
                        AutoFishLogger.debug("Player cast manually while recast was scheduled");
                    }                        
                    resetReelDelay();
                    resetCastSchedule();
                    resetBiteTracking();
                }
                
                if (ModAutoFish.config_autofish_entityClearProtect && this.isFishing && !isDuringCastDelay() && this.player.fishEntity == null) {
                    AutoFishLogger.info("Entity Clear detected.  Re-casting.");
                    this.isFishing = false;
                    startFishing();
                }
                
                if (playerHookInWater(this.player)) {
//                    recordLastHookPosition(this.player.fishEntity);
                    if (this.closeBobberSplashDetectedAt > 0 && this.minecraft.world.getTotalWorldTime() > this.closeBobberSplashDetectedAt + 45) {
                        AutoFishLogger.info("[%d] I think we missed a fish", this.minecraft.world.getTotalWorldTime());
                        resetBiteTracking();
                    }
                }
                
                /**
                 * This method works, but has been disabled in favor of the method that affects all
                 * players in the single-player world.
                 * See onServerTickEvent()
                 * 
                if (ModAutoFish.config_autofish_fastFishing && playerHookInWater() && !isDuringReelDelay()) {
                    triggerBite();
                }
                */                
            } else {
                this.isFishing = false;
            }
        }
    }
    
    private void showNotificationToPlayer() {
        this.player.sendMessage(new TextComponentString(NOTIFICATION_TEXT_AUTOFISH_ENABLED));
        this.notificationShownToPlayer = true;
    }
    
//    private void recordLastHookPosition(EntityFishHook fishEntity) {
//        this.lastHookPosition = fishEntity.getPosition();
//    }
//
    @SubscribeEvent
    public void onPlayerUseItem(PlayerInteractEvent.RightClickItem event) {
        // Only do this on the client side
        if (event.getWorld().isRemote && playerIsHoldingRod()) {
            if (!rodIsCast()) {
                resetReelDelay();
                resetCastSchedule();
                resetBiteTracking();
                this.isFishing = true;
                startCastDelay();
            } else {
                this.isFishing = false;
                resetCastDelay();
            }
        }
    }
    
    public void onWaterWakeDetected(double x, double y, double z) {
        if (this.minecraft != null && this.minecraft.player != null && playerHookInWater(this.minecraft.player)) {
            EntityFishHook hook = this.minecraft.player.fishEntity;
            double distanceFromHook = new BlockPos(x, y, z).distanceSq(hook.posX, hook.posY, hook.posZ);
            if (distanceFromHook <= CLOSE_WATER_WAKE_THRESHOLD) {
                if (this.closeWaterWakeDetectedAt <= 0) {
//                    AutoFishLogger.info("[%d] Close water wake at %f", this.minecraft.world.getTotalWorldTime(), distanceFromHook);
                    this.closeWaterWakeDetectedAt = this.minecraft.world.getTotalWorldTime();
                }
//                if (distanceFromHook <= EXACT_WATER_WAKE_THRESHOLD) {
//                    if (this.exactWaterWakeDetectedAt <=0) {
////                        AutoFishLogger.info("[%d] Exact water wake at %f", this.minecraft.world.getTotalWorldTime(), distanceFromHook);
//                        this.exactWaterWakeDetectedAt = this.minecraft.world.getTotalWorldTime();
//                    }
//                }
            }
        }
    }
    
    public void onEntityAdded(Entity entity) {
        if (this.player != null && entity instanceof EntityXPOrb) {
            double distanceFromPlayer = this.player.getPosition().distanceSq(entity.posX, entity.posY, entity.posZ);
//            AutoFishLogger.info("Entity [%s] spawned at distance %f from player", entity.getDisplayName().getFormattedText(), distanceFromPlayer);
            if (distanceFromPlayer < 2.0d) {
                this.xpLastAddedAt = this.minecraft.world.getTotalWorldTime();
            }
        }
    }
    
    private boolean somethingSeemsWrong() {
        if (rodIsCast() && !isDuringCastDelay() && !isDuringReelDelay() && hookShouldBeInWater()) {
            if ((playerHookInWater(this.player) || ModAutoFish.config_autofish_handleProblems) && waitedLongEnough()) {
                AutoFishLogger.info("We should have caught something by now.");
                return true;
            }
            if (ModAutoFish.config_autofish_handleProblems) {
                if (hookedAnEntity()) {
                    AutoFishLogger.info("Oops, we hooked an Entity");
                    return true;
                }
                if (!playerHookInWater(this.player)) {
                    AutoFishLogger.info("Hook should be in water but isn't.");
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean hookedAnEntity() {
        if (this.player.fishEntity != null && this.player.fishEntity.caughtEntity != null) {
            return true;
        }
        return false;
    }
    
    private boolean waitedLongEnough() {
        return this.startedCastDelayAt > 0 && this.minecraft.world.getTotalWorldTime() > this.startedCastDelayAt + (MAX_WAITING_TIME_SECONDS * TICKS_PER_SECOND);
    }
    
    private boolean hookShouldBeInWater() {
        return this.startedCastDelayAt > 0 && this.minecraft.world.getTotalWorldTime() > this.startedCastDelayAt + MAX_HOOK_FLYING_TIME_TICKS;
    }
    
    private boolean rodIsCast() {
        if (!playerIsHoldingRod()) {
            return false;
        }
        ItemStack heldItemStack = this.player.getHeldItemMainhand();
        return this.player.getHeldItemMainhand().getItem().getPropertyGetter(new ResourceLocation("cast")).apply(heldItemStack, this.minecraft.world, this.player) > 0F;
    }
    
    private void reelIn() {
        playerUseRod();
    }

    private void startFishing() {
        if (this.xpLastAddedAt <= 0) {
            AutoFishLogger.debug("No XP found since last cast.  Maybe nothing was caught");
        }
        playerUseRod();
        startCastDelay();
    }

    private void resetBiteTracking() {
        this.xpLastAddedAt = 0L;
//        this.lastHookPosition = null;
        this.closeWaterWakeDetectedAt = 0L;
//        this.exactWaterWakeDetectedAt = 0L;
        this.closeBobberSplashDetectedAt = 0L;
//        this.exactBobberSplashDetectedAt = 0L;
    }

    private void resetCastSchedule() {
        this.castScheduledAt = 0L;
    }
    
    private void resetCastDelay() {
        this.startedCastDelayAt = 0L;
    }
    
    private boolean needToSwitchRods() {
        return ModAutoFish.config_autofish_multirod && !playerCanCast();
    }

    private void scheduleNextCast() {
        this.castScheduledAt = this.minecraft.world.getTotalWorldTime();
    }

    /*
     *  Trigger a delay so we don't use the rod multiple times for the same bite,
     *  which can persist for 2-3 ticks.
     */
    private void startReelDelay() {
        this.startedReelDelayAt = this.minecraft.world.getTotalWorldTime();
    }

    /*
     * Trigger a delay so that entity clear protection doesn't kick in during cast.
     */
    private void startCastDelay() {
        this.startedCastDelayAt = this.minecraft.world.getTotalWorldTime();
    }

    private void resetReelDelay() {
        startedReelDelayAt = 0;
    }

    private boolean isDuringReelDelay() {
        return (this.startedReelDelayAt != 0 && this.minecraft.world.getTotalWorldTime() < this.startedReelDelayAt + REEL_TICK_DELAY);
    }
    
    private boolean isDuringCastDelay() {
        return (this.startedCastDelayAt != 0 && this.minecraft.world.getTotalWorldTime() < this.startedCastDelayAt + CAST_TICK_DELAY);
    }
    
    private boolean playerHookInWater(EntityPlayer player) {
        return player != null && player.fishEntity != null
                && player.fishEntity.isInWater();
    }

    private boolean playerIsHoldingRod() {
        ItemStack heldItem = this.player.getHeldItemMainhand();

        return (heldItem != null
                && heldItem.getItem() instanceof ItemFishingRod
                && heldItem.getItemDamage() <= heldItem.getMaxDamage());
    }

    private boolean isFishBiting() {
        EntityPlayer serverPlayerEntity = getServerPlayerEntity();
        if (serverPlayerEntity != null) {
            /** If single player (integrated server), we can actually check to see if something
             * is catchable, but it's fragile (other mods could break it)
             * If anything goes wrong, fall back to the safer but less reliable method
             */
            try {
                return isFishBiting_fromServerEntity(serverPlayerEntity);
            } catch (Exception e) {
                return isFishBiting_fromClientWorld();
            }
        } else {
            /** If multiplayer, we must rely on client world conditions to guess when a bit occurs **/
            return isFishBiting_fromClientWorld();
        }
    }

    /**
     * Determine whether a fish is biting the player's hook.  This only works in Single Player, but
     * is 100% accurate.
     * 
     * @param serverPlayerEntity
     * @return
     * @throws NumberFormatException
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private boolean isFishBiting_fromServerEntity(EntityPlayer serverPlayerEntity) throws NumberFormatException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        /*
         * The fish hook entity on the server side knows whether a fish is catchable at any given time.  However,
         * that field is private and not exposed in any way.  So we must use reflection to access that field.
         */
        EntityFishHook serverFishEntity = serverPlayerEntity.fishEntity;
        int ticksCatchable = ReflectionUtils.getPrivateIntFieldFromObject(serverFishEntity, "ticksCatchable", "field_146045_ax");
        if (ticksCatchable > 0) {
            return true;
        }
        return false;
    }
    

    /**
     * Try to determine if a fish is biting the player's hook based on the state of the client world.
     * It is not 100% accurate, but it's pretty good.
     * 
     * @return
     */
    private boolean isFishBiting_fromClientWorld() {
        /**
         * Strategies:
         * (-) MOVEMENT of the fish hook entity.  This is about 85% accurate by itself, with a very low chance
         *     of false positives, if the threshold is set right.
         * (-) BOBBER_SPLASH sound played at the fish hook.  Very accurate indication of a bite,
         *     but event may not always trigger from remote worlds, and another player's fish hook very
         *     near to our fish hook can cause a false positive, so it's not suitable by itself.
         * (-) WATER_WAKE particle spawned near the fish hook.  Accurate indication that a bite is about
         *     to occur, but a bit of a guess at exactly when the bite will occur.  Used by itself,
         *     this may trigger a reel-in too soon or too late; and another player's fish hook
         *     very near to our fish hook can cause a false positive.  So it's not suitable by itself.
         * (-) Combination of the above.  The hook MOVEMENT method catches most of the fish, but if we see that
         *     there is a little hook movement (not enough to cross the threshold) and also another indication,
         *     then it's probably a bite, and we can get about 10% more accuracy, still with very few false positives.
         */
        return isFishBiting_fromMovement() || isFishBiting_fromBobberSound() || isFishBiting_fromWaterWake() || isFishBiting_fromAll(); 
    }
    
    private boolean isFishBiting_fromBobberSound() {
        /** If a bobber sound has been played at the fish hook, a fish is already biting **/
        if (ModAutoFish.config_autofish_aggressiveBiteDetection && this.closeBobberSplashDetectedAt > 0) {
            AutoFishLogger.info("[%d] Detected bite by BOBBER_SPLASH", this.minecraft.world.getTotalWorldTime());
            return true;
        }
        return false;
    }
    
    private boolean isFishBiting_fromWaterWake() {
        /** An exact water wake indicated probable bite "very soon", so make sure enough time has passed **/
        if (ModAutoFish.config_autofish_aggressiveBiteDetection
                && this.closeWaterWakeDetectedAt > 0 
                && this.minecraft.world.getTotalWorldTime() > this.closeWaterWakeDetectedAt + CLOSE_WATER_WAKE_DELAY_TICKS) {
            AutoFishLogger.info("[%d] Detected bite by WATER_WAKE", this.minecraft.world.getTotalWorldTime());
            return true;
        }
        return false;
    }

    private boolean isFishBiting_fromMovement() {
        EntityFishHook fishEntity = this.player.fishEntity;
        if (fishEntity != null 
                // Checking for no X and Z motion prevents a false alarm when the hook is moving through the air
                && fishEntity.motionX == 0 
                && fishEntity.motionZ == 0 
                && fishEntity.motionY < MOTION_Y_THRESHOLD) {
            AutoFishLogger.debug("[%d] Detected bite by MOVEMENT", this.minecraft.world.getTotalWorldTime());
            return true;
        }
        return false;
    }
    
    private boolean isFishBiting_fromAll() {
        /** Assume a bit if the following conditions are true:
         * (1) There is at least a little Y motion of the fish hook
         * (2) Either (a) There has been a "close" bobber splash very recently; OR
         *            (b) A "close" water wake was detected long enough ago  
         */
        EntityFishHook fishEntity = this.player.fishEntity;
        if (fishEntity != null 
                // Checking for no X and Z motion prevents a false alarm when the hook is moving through the air
                && fishEntity.motionX == 0 
                && fishEntity.motionZ == 0 
                && fishEntity.motionY < MOTION_Y_MAYBE_THRESHOLD) {
//            long totalWorldTime = this.minecraft.world.getTotalWorldTime();
            if (recentCloseBobberSplash() || recentCloseWaterWake()) {
                AutoFishLogger.debug("[%d] Detected bite by ALL", this.minecraft.world.getTotalWorldTime());
                return true;
            }
        }
        return false;
    }
    
    private boolean recentCloseBobberSplash() {
        /** Close bobber sound must have been quite recent to indicate probable bite **/
        if (this.closeBobberSplashDetectedAt > 0 
                && this.minecraft.world.getTotalWorldTime() < this.closeBobberSplashDetectedAt + 20) {
            return true;
        }
        return false;
    }
    
    private boolean recentCloseWaterWake() {
        /** A close water wake indicates probable bite "soon", so make sure enough time has passed **/
        if (this.closeWaterWakeDetectedAt > 0
                && this.minecraft.world.getTotalWorldTime() > this.closeWaterWakeDetectedAt + CLOSE_WATER_WAKE_DELAY_TICKS) {
            return true;
        }
        return false;
    }

    /**
     * For all players in the specified world, if they are fishing, trigger a bite.
     * 
     * @param world
     */
    private void triggerBites(MinecraftServer server) {
        for (EntityPlayer player : server.getPlayerList().getPlayers()) {
            if (playerHookInWater(player)) {
                int ticks = FAST_FISH_CATCHABLE_DELAY_TICKS + MathHelper.getInt(this.rand, 0, FAST_FISH_DELAY_VARIANCE);
                setTicksCatchableDelay(player.fishEntity, ticks);
            }
        }
    }
    
    /**
     * [Currently unused]
     * For the current player, trigger a bite on the fish hook.
     * 
     */
    @SuppressWarnings("unused")
    private void triggerBite() {
        EntityPlayer serverPlayerEntity = getServerPlayerEntity();
        if (serverPlayerEntity != null) {
            /*
             * If we are single player and have access to the server player entity, try to hack the fish hook entity
             * to make fish bite sooner.
             */
            EntityFishHook serverFishEntity = serverPlayerEntity.fishEntity;
            int ticks = FAST_FISH_CATCHABLE_DELAY_TICKS + MathHelper.getInt(this.rand, 0, FAST_FISH_DELAY_VARIANCE);
            setTicksCatchableDelay(serverFishEntity, ticks);
        }
    }
    
    private void setTicksCatchableDelay(EntityFishHook hook, int ticks) {
        String forgeFieldName = "ticksCatchableDelay";
        String vanillaFieldName = "field_146038_az";
        try {
            int currentTicksCatchableDelay = ReflectionUtils.getPrivateIntFieldFromObject(hook, forgeFieldName, vanillaFieldName);
            if (currentTicksCatchableDelay <= 0) {
                try {
                    ReflectionUtils.setPrivateIntFieldOfObject(hook, forgeFieldName, vanillaFieldName, ticks);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }

    private EntityPlayer getServerPlayerEntity() {
        if (this.minecraft.getIntegratedServer() == null || this.minecraft.getIntegratedServer().getEntityWorld() == null) {
            return null;
        } else {
            return this.minecraft.getIntegratedServer().getEntityWorld().getPlayerEntityByName(this.minecraft.player.getName());
        }
    }

    private EnumActionResult playerUseRod() {
        return this.minecraft.playerController.processRightClick(
                this.player, 
                this.minecraft.world, 
                EnumHand.MAIN_HAND);
    }
    
    private boolean isTimeToCast() {
        return (this.castScheduledAt > 0 && this.minecraft.world.getTotalWorldTime() > this.castScheduledAt + (ModAutoFish.config_autofish_recastDelay * TICKS_PER_SECOND));
    }
    
    private boolean waitingToRecast() {
        return (this.castScheduledAt > 0);
    }

    private void tryToSwitchRods() {
        InventoryPlayer inventory = this.player.inventory;
        for (int i = 0; i < 9; i++) {
            ItemStack curItemStack = inventory.mainInventory.get(i);
            if (curItemStack != null 
                    && curItemStack.getItem() instanceof ItemFishingRod
                    && (!ModAutoFish.config_autofish_preventBreak || (curItemStack.getMaxDamage() - curItemStack.getItemDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD))
                ) {
                inventory.currentItem = i;
                break;
            }
        }
    }

    private boolean playerCanCast() {
        if (!playerIsHoldingRod()) {
            return false;
        } else {
            ItemStack heldItem = this.player.getHeldItemMainhand();
    
            return (!ModAutoFish.config_autofish_preventBreak 
                    || (heldItem.getMaxDamage() - heldItem.getItemDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD)
                    );
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
        if (eventArgs.getModID().equals(ModAutoFish.MODID)) {
            ModAutoFish.syncConfig();
        }
    }
    
}
