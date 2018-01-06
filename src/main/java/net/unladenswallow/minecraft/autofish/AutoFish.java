package net.unladenswallow.minecraft.autofish;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.unladenswallow.minecraft.autofish.util.Logger;
import net.unladenswallow.minecraft.autofish.util.ReflectionUtils;

/**
 * The primary state and logic controller for the AutoFish functionality
 * 
 * @author FreneticFeline
 *
 */
public class AutoFish {

    private Minecraft minecraftClient;
    private EntityPlayer player;
    private boolean notificationShownToPlayer = false;
    private long castScheduledAt = 0L;
    private long startedReelDelayAt = 0L;
    private long startedCastDelayAt = 0L;
    private boolean isFishing = false;
    private long closeWaterWakeDetectedAt = 0L;
    private long xpLastAddedAt = 0L;
    private long closeBobberSplashDetectedAt = 0L;
    private Random rand;
    private long lastFishEntityServerY = 0l;
    private long hookFirstInWaterAt = 0L;
    
    private static final String NOTIFICATION_TEXT_AUTOFISH_ENABLED = "AutoFish is enabled.  Type 'o' while holding a fishing rod for more options";

    private static final int TICKS_PER_SECOND = 20;

    /** How long to suppress checking for a bite after starting to reel in.  If we check for a bite while reeling
        in, we may think we have a bite and try to reel in again, which will actually cause a re-cast and lose the fish */
    private static final int REEL_TICK_DELAY = 15;

    /** How long to wait after casting to check for Entity Clear.  If we check too soon, the hook entity
        isn't in the world yet, and will trigger a false alarm and cause infinite recasting. */
    private static final int CAST_TICK_DELAY = 20;
    
    /** How long to let the hook settle in the water before watching for bite movement */
    private static final int INWATER_DELAY_TICKS = 40;

    /** When Break Prevention is enabled, how low to let the durability get before stopping or switching rods */
    private static final int AUTOFISH_BREAKPREVENT_THRESHOLD = 2;

    /** The threshold for vertical movement of the fish hook that determines when a fish is biting, if using
        the movement method of detection. 
        and the movement threshold that, combined with other factors, is a probable indicator that a fish is biting */
    private static final double MOTION_Y_THRESHOLD = -0.02d;
    private static final double MOTION_Y_MAYBE_THRESHOLD = -0.008d;
    private static final long SERVER_MOTION_Y_THRESHOLD = -500;
    
    /** The number of ticks to set as the "catchable delay" when Fast Fishing is enabled. *
     * (Vanilla ticksCatchableDelay is random between 20 and 80, but we seem to have trouble catching
     * it if it is less than 40) **/
    private static final int FAST_FISH_CATCHABLE_DELAY_TICKS = 40;
    private static final int FAST_FISH_DELAY_VARIANCE = 40;
    
    /** The maximum number of ticks that is is reasonable for a fish hook to be flying in the air after a cast */
    private static final int MAX_HOOK_FLYING_TIME_TICKS = 80;
    
    /** The amount of time to wait for a fish before something seems wrong and we want to recast **/
    private static final int MAX_WAITING_TIME_SECONDS = 90;
    
    /** The distance (squared) threshold for determining that a water wake is "close" to the fish Hook 
     * and "most certainly at" the fish Hook **/
    private static final double CLOSE_WATER_WAKE_THRESHOLD = 1.0d;
    
    /** The number of ticks to wait after detecting a "close" or "exact" water wake before reeling in **/
    private static final int CLOSE_WATER_WAKE_DELAY_TICKS = 30;
    
    /** The distance (squared) threshold for determining that a bobber splash sound is "close" to the fish Hook
     * and "most certainly at" the fish Hook **/
    private static final double CLOSE_BOBBER_SPLASH_THRESHOLD = 2.8d;
    
    
    /*************  CONSTRUCTOR  ***************/
    
    
    public AutoFish() {
        this.minecraftClient = FMLClientHandler.instance().getClient();
        this.rand = new Random();
    }

    
    
    /*************  EVENTS *************/
    
    /**
     * Callback from EventListener for ClientTickEvent
     */
    public void onClientTick() {
        update();
    }
    
    public void onBobberSplashDetected(float x, float y, float z) {
        if (playerHookInWater(this.player)) {
            EntityFishHook hook = this.player.fishEntity;
//                double yDifference = Math.abs(hook.posY - y);
            // Ignore Y component when calculating distance from hook
            double xzDistanceFromHook = hook.getDistanceSq(x, hook.posY, z);
            if (xzDistanceFromHook <= CLOSE_BOBBER_SPLASH_THRESHOLD) {
//                    AutoFishLogger.info("[%d] Close bobber splash at %f /  %f", this.minecraft.theWorld.getTotalWorldTime(), xzDistanceFromHook, yDifference);
                this.closeBobberSplashDetectedAt = this.minecraftClient.theWorld.getTotalWorldTime();
//                    if (xzDistanceFromHook <= EXACT_BOBBER_SPLASH_THRESHOLD) {
//    //                    AutoFishLogger.info("[%d] Exact bobber splash at %f /  %f", this.minecraft.theWorld.getTotalWorldTime(), xzDistanceFromHook, yDifference);
//                        this.exactBobberSplashDetectedAt = this.minecraft.theWorld.getTotalWorldTime();
//                    } 
            }
        }
    }
    
    /**
     * Update tracking state each time the player starts or stops fishing.
     * Triggered each time the player right-clicks with an item (and when a right-click
     * has been programmatically triggered).
     * 
     */
    public void onPlayerUseItem() {
        if (playerIsHoldingRod()) {
            if (!rodIsCast()) {
                // Player is casting
                resetReelDelay();
                resetCastSchedule();
                resetBiteTracking();
                this.isFishing = true;
                startCastDelay();
            } else {
                // Player is reeling in
                this.isFishing = false;
                resetCastDelay();
            }
        }
    }
    
    /**
     * Callback from the WorldEventListener to tell us whenever a WATER_WAKE particle
     * is spawned in the world.
     * 
     * @param x
     * @param y
     * @param z
     */
    public void onWaterWakeDetected(double x, double y, double z) {
        if (this.minecraftClient != null && this.minecraftClient.thePlayer != null && playerHookInWater(this.minecraftClient.thePlayer)) {
            EntityFishHook hook = this.minecraftClient.thePlayer.fishEntity;
            double distanceFromHook = new BlockPos(x, y, z).distanceSq(hook.posX, hook.posY, hook.posZ);
            if (distanceFromHook <= CLOSE_WATER_WAKE_THRESHOLD) {
                if (this.closeWaterWakeDetectedAt <= 0) {
//                    AutoFishLogger.info("[%d] Close water wake at %f", this.minecraft.theWorld.getTotalWorldTime(), distanceFromHook);
                    this.closeWaterWakeDetectedAt = this.minecraftClient.theWorld.getTotalWorldTime();
                }
//                if (distanceFromHook <= EXACT_WATER_WAKE_THRESHOLD) {
//                    if (this.exactWaterWakeDetectedAt <=0) {
////                        AutoFishLogger.info("[%d] Exact water wake at %f", this.minecraft.theWorld.getTotalWorldTime(), distanceFromHook);
//                        this.exactWaterWakeDetectedAt = this.minecraft.theWorld.getTotalWorldTime();
//                    }
//                }
            }
        }
    }

    /**
     * Callback from the WorldEventListener to tell us whenever an XP Orb is 
     * added to the world.
     * 
     * Use this information to try to determine whether we actually caught something
     * last time we reeled in.
     * 
     * @param entity
     */
    public void onXpOrbAdded(double x, double y, double z) {
        if (this.player != null) {
            double distanceFromPlayer = this.player.getPosition().distanceSq(x, y, z);
//            AutoFishLogger.info("Entity [%s] spawned at distance %f from player", entity.getDisplayName().getFormattedText(), distanceFromPlayer);
            if (distanceFromPlayer < 2.0d) {
                this.xpLastAddedAt = this.minecraftClient.theWorld.getTotalWorldTime();
            }
        }
    }

    
    /***********  CORE LOGIC ****************/

    
    /**
     * Update the state of everything related to AutoFish functionality,
     * and trigger appropriate actions.
     */
    private void update() {
        if (!this.minecraftClient.isGamePaused() && this.minecraftClient.thePlayer != null) {
            this.player = this.minecraftClient.thePlayer;
            if (!this.notificationShownToPlayer) {
                showNotificationToPlayer();
            }

            if (playerIsHoldingRod() || waitingToRecast()) {
                if ((hookHasBeenInWaterLongEnough() && !isDuringReelDelay() && isFishBiting())
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
                        Logger.debug("Player cast manually while recast was scheduled");
                    }                        
                    resetReelDelay();
                    resetCastSchedule();
                    resetBiteTracking();
                }
                
                if (ModAutoFish.config_autofish_entityClearProtect) {
                    checkForEntityClear();
                }
                
                checkForMissedBite();
                
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
    

    /***********  BITE DETECTION ****************/
    
    
    /**
     * Determine whether a fish is currently biting the player's fish hook.
     * Different methods are used for single player and multiplayer.
     * 
     * @return
     */
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
     * Determine whether a fish is biting the player's hook, using the server-side player entity.
     * This only works in Single Player, but is 100% accurate.
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
         * (-) BOBBER_SPLASH sound played at the fish hook.  Pretty accurate indication of a bite,
         *     but event may not always trigger from remote servers, and another player's fish hook very
         *     near to our fish hook can cause a false positive.
         * (-) WATER_WAKE particle spawned near the fish hook.  Accurate indication that a bite is about
         *     to occur, but a bit of a guess as to exactly when the bite will occur.  Used alone,
         *     this may trigger a reel-in too soon or too late; and another player's fish hook
         *     very near to our fish hook can cause a false positive.
         * (-) Combination of the above.  The hook MOVEMENT method catches most of the fish, but if we see that
         *     there is a little hook movement (not enough to cross the threshold) and also another indication,
         *     then it's probably a bite, and we can get about 10% more accuracy, still with very few false positives.
         */
        return isFishBiting_fromMovement() || isFishBiting_fromBobberSound() || isFishBiting_fromWaterWake() || isFishBiting_fromAll(); 
    }
    
    private boolean isFishBiting_fromBobberSound() {
        /** If a bobber sound has been played at the fish hook, a fish is already biting **/
        if (ModAutoFish.config_autofish_aggressiveBiteDetection && this.closeBobberSplashDetectedAt > 0) {
            Logger.debug("[%d] Detected bite by BOBBER_SPLASH", this.minecraftClient.theWorld.getTotalWorldTime());
            return true;
        }
        return false;
    }
    
    private boolean isFishBiting_fromWaterWake() {
        /** An water wake indicates a probable bite "very soon", so make sure enough time has passed **/
        if (ModAutoFish.config_autofish_aggressiveBiteDetection
                && this.closeWaterWakeDetectedAt > 0 
                && this.minecraftClient.theWorld.getTotalWorldTime() > this.closeWaterWakeDetectedAt + CLOSE_WATER_WAKE_DELAY_TICKS) {
            Logger.debug("[%d] Detected bite by WATER_WAKE", this.minecraftClient.theWorld.getTotalWorldTime());
            return true;
        }
        return false;
    }

    private boolean isFishBiting_fromMovement() {
        EntityFishHook fishEntity = this.player.fishEntity;
        if (fishEntity != null 
                // Checking for no X and Z motion prevents a false alarm when the hook is moving through the air
                && Math.abs(fishEntity.motionX) < 0.01 
                && Math.abs(fishEntity.motionZ) < 0.01) {
            long calculatedServerY = fishEntity.serverPosY - this.lastFishEntityServerY; 
            this.lastFishEntityServerY = fishEntity.serverPosY;
            if (fishEntity.motionY < MOTION_Y_THRESHOLD) {
                Logger.debug("[%d] Detected bite by MOVEMENT (Y was %f; X and Z were %f and %f)", this.minecraftClient.theWorld.getTotalWorldTime(), fishEntity.motionY, fishEntity.motionX, fishEntity.motionZ);
                return true;
            }
            if (calculatedServerY < SERVER_MOTION_Y_THRESHOLD) {
                Logger.debug("[%d] Detected bite by CALC_SERVER_MOVE (%d)", this.minecraftClient.theWorld.getTotalWorldTime(), calculatedServerY);
                return true;
            }
            
        }
        return false;
    }
    
    private boolean isFishBiting_fromAll() {
        /** Assume a bite if the following conditions are true:
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
//            long totalWorldTime = this.minecraft.theWorld.getTotalWorldTime();
            if (recentCloseBobberSplash() || recentCloseWaterWake()) {
                Logger.debug("[%d] Detected bite by ALL (Y was %f)", this.minecraftClient.theWorld.getTotalWorldTime(), fishEntity.motionY);
                return true;
            }
        }
        return false;
    }
    
    
    /******************  STATE HELPERS  *****************/
    
    
    private boolean isDuringReelDelay() {
        return (this.startedReelDelayAt != 0 && this.minecraftClient.theWorld.getTotalWorldTime() < this.startedReelDelayAt + REEL_TICK_DELAY);
    }
    
    private boolean isDuringCastDelay() {
        return (this.startedCastDelayAt != 0 && this.minecraftClient.theWorld.getTotalWorldTime() < this.startedCastDelayAt + CAST_TICK_DELAY);
    }
    
    private boolean playerHookInWater() {
        if (playerHookInWater(this.player)) {
            if (this.hookFirstInWaterAt == 0) {
                this.hookFirstInWaterAt = this.minecraftClient.theWorld.getTotalWorldTime();
            }
            return true;
        }
        return false;
    }
    
    private boolean playerHookInWater(EntityPlayer player) {
        return player != null && player.fishEntity != null
                && player.fishEntity.isInWater();
    }

    private boolean hookHasBeenInWaterLongEnough() {
        return (playerHookInWater() 
                && this.minecraftClient.theWorld.getTotalWorldTime() > this.hookFirstInWaterAt + INWATER_DELAY_TICKS);
    }
    
    private boolean playerIsHoldingRod() {
        ItemStack heldItem = this.player.getHeldItemMainhand();

        return (heldItem != null
                && heldItem.getItem() instanceof ItemFishingRod
                && heldItem.getItemDamage() <= heldItem.getMaxDamage());
    }

    private boolean recentCloseBobberSplash() {
        /** Close bobber sound must have been quite recent to indicate probable bite **/
        if (this.closeBobberSplashDetectedAt > 0 
                && this.minecraftClient.theWorld.getTotalWorldTime() < this.closeBobberSplashDetectedAt + 20) {
            return true;
        }
        return false;
    }
    
    private boolean recentCloseWaterWake() {
        /** A close water wake indicates probable bite "soon", so make sure enough time has passed **/
        if (this.closeWaterWakeDetectedAt > 0
                && this.minecraftClient.theWorld.getTotalWorldTime() > this.closeWaterWakeDetectedAt + CLOSE_WATER_WAKE_DELAY_TICKS) {
            return true;
        }
        return false;
    }
    private boolean somethingSeemsWrong() {
        if (rodIsCast() && !isDuringCastDelay() && !isDuringReelDelay() && hookShouldBeInWater()) {
            if ((playerHookInWater(this.player) || ModAutoFish.config_autofish_handleProblems) && waitedLongEnough()) {
                Logger.info("We should have caught something by now.");
                return true;
            }
            if (ModAutoFish.config_autofish_handleProblems) {
                if (hookedAnEntity()) {
                    Logger.info("Oops, we hooked an Entity");
                    return true;
                }
                if (!playerHookInWater(this.player)) {
                    Logger.info("Hook should be in water but isn't.");
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
        return this.startedCastDelayAt > 0 && this.minecraftClient.theWorld.getTotalWorldTime() > this.startedCastDelayAt + (MAX_WAITING_TIME_SECONDS * TICKS_PER_SECOND);
    }
    
    private boolean hookShouldBeInWater() {
        return this.startedCastDelayAt > 0 && this.minecraftClient.theWorld.getTotalWorldTime() > this.startedCastDelayAt + MAX_HOOK_FLYING_TIME_TICKS;
    }
    
    private boolean rodIsCast() {
        if (!playerIsHoldingRod()) {
            return false;
        }
        ItemStack heldItemStack = this.player.getHeldItemMainhand();
        return this.player.getHeldItemMainhand().getItem().getPropertyGetter(new ResourceLocation("cast")).apply(heldItemStack, this.minecraftClient.theWorld, this.player) > 0F;
    }
    
    private boolean needToSwitchRods() {
        return ModAutoFish.config_autofish_multirod && !playerCanCast();
    }

    private boolean isTimeToCast() {
        return (this.castScheduledAt > 0 && this.minecraftClient.theWorld.getTotalWorldTime() > this.castScheduledAt + (ModAutoFish.config_autofish_recastDelay * TICKS_PER_SECOND));
    }
    
    private boolean waitingToRecast() {
        return (this.castScheduledAt > 0);
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


    /******************  ACTION HELPERS  *****************/

    
    private void showNotificationToPlayer() {
        this.player.addChatMessage(new TextComponentString(NOTIFICATION_TEXT_AUTOFISH_ENABLED));
        this.notificationShownToPlayer = true;
    }
    
//    private void recordLastHookPosition(EntityFishHook fishEntity) {
//        this.lastHookPosition = fishEntity.getPosition();
//    }
//

    private void reelIn() {
        playerUseRod();
    }

    private void startFishing() {
        if (this.xpLastAddedAt <= 0) {
            Logger.debug("No XP found since last cast.  Maybe nothing was caught");
        }
        playerUseRod();
        startCastDelay();
    }

    private void resetCastSchedule() {
        this.castScheduledAt = 0L;
    }
    
    private void resetCastDelay() {
        this.startedCastDelayAt = 0L;
    }
    
    private void scheduleNextCast() {
        this.castScheduledAt = this.minecraftClient.theWorld.getTotalWorldTime();
    }

    /*
     *  Trigger a delay so we don't use the rod multiple times for the same bite,
     *  which can persist for 2-3 ticks.
     */
    private void startReelDelay() {
        this.startedReelDelayAt = this.minecraftClient.theWorld.getTotalWorldTime();
    }

    /*
     * Trigger a delay so that entity clear protection doesn't kick in during cast.
     */
    private void startCastDelay() {
        this.startedCastDelayAt = this.minecraftClient.theWorld.getTotalWorldTime();
    }

    private void resetReelDelay() {
        startedReelDelayAt = 0;
    }


    /**
     * For all players in the specified world, if they are fishing, trigger a bite.
     * 
     * @param world
     */
    public void triggerBites() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            for (EntityPlayer player : server.getPlayerList().getPlayerList()) {
                if (playerHookInWater(player)) {
                    int ticks = FAST_FISH_CATCHABLE_DELAY_TICKS + MathHelper.getRandomIntegerInRange(this.rand, 0, FAST_FISH_DELAY_VARIANCE);
                    setTicksCatchableDelay(player.fishEntity, ticks);
                }
            }
        }
    }
    
    /**
     * For the current player, trigger a bite on the fish hook.
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
            int ticks = FAST_FISH_CATCHABLE_DELAY_TICKS + MathHelper.getRandomIntegerInRange(this.rand, 0, FAST_FISH_DELAY_VARIANCE);
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
        if (this.minecraftClient.getIntegratedServer() == null || this.minecraftClient.getIntegratedServer().getEntityWorld() == null) {
            return null;
        } else {
            return this.minecraftClient.getIntegratedServer().getEntityWorld().getPlayerEntityByName(this.minecraftClient.thePlayer.getName());
        }
    }

    private EnumActionResult playerUseRod() {
        return this.minecraftClient.playerController.processRightClick(
                this.player, 
                this.minecraftClient.theWorld, 
                this.player.getHeldItemMainhand(),
                EnumHand.MAIN_HAND);
    }
    
    
    private void resetBiteTracking() {
        this.hookFirstInWaterAt = 0L;
        this.lastFishEntityServerY = 0L;
        this.xpLastAddedAt = 0L;
        this.closeWaterWakeDetectedAt = 0L;
        this.closeBobberSplashDetectedAt = 0L;
    }

    private void tryToSwitchRods() {
        InventoryPlayer inventory = this.player.inventory;
        for (int i = 0; i < 9; i++) {
            ItemStack curItemStack = inventory.mainInventory[i];
            if (curItemStack != null 
                    && curItemStack.getItem() instanceof ItemFishingRod
                    && (!ModAutoFish.config_autofish_preventBreak || (curItemStack.getMaxDamage() - curItemStack.getItemDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD))
                ) {
                inventory.currentItem = i;
                break;
            }
        }
    }
    
    private void checkForEntityClear() {
        if (this.isFishing && !isDuringCastDelay() && this.player.fishEntity == null) {
            Logger.info("Entity Clear detected.  Re-casting.");
            this.isFishing = false;
            startFishing();
        }
    }
    
    private void checkForMissedBite() {
        if (playerHookInWater(this.player)) {
//          recordLastHookPosition(this.player.fishEntity);
          if (this.closeBobberSplashDetectedAt > 0 && this.minecraftClient.theWorld.getTotalWorldTime() > this.closeBobberSplashDetectedAt + 45) {
              Logger.debug("[%d] I think we missed a fish", this.minecraftClient.theWorld.getTotalWorldTime());
              resetBiteTracking();
          }
      }
    }

}
