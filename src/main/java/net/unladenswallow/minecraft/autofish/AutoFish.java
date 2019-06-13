package net.unladenswallow.minecraft.autofish;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
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
import net.minecraft.world.dimension.DimensionType;
import net.unladenswallow.minecraft.autofish.config.AutoFishModConfig;
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
//    private long exactWaterWakeDetectedAt = 0L;
    private long xpLastAddedAt = 0L;
    private long closeBobberSplashDetectedAt = 0L;
//    private long exactBobberSplashDetectedAt = 0L;
    private Random rand;
    
    private static final String NOTIFICATION_TEXT_AUTOFISH_ENABLED = "AutoFish is %s.  Type 'o' while holding a fishing rod for more options";

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
    private static final int MAX_HOOK_FLYING_TIME_TICKS = 120;
    
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
    
    
    /*************  CONSTRUCTOR  ***************/
    
    
    public AutoFish() {
        this.minecraftClient = Minecraft.getInstance();
        this.rand = new Random();
    }

    
    
    /*************  EVENTS *************/
    
    /**
     * Callback from EventListener for ClientTickEvent
     */
    public void onClientTick() {
        this.player = this.minecraftClient.player;
        if (this.player != null && !this.notificationShownToPlayer) {
            showNotificationToPlayer();
        }
        if (AutoFishModConfig.autofishEnabled()) {
            update();
        }
    }
    
    public void onBobberSplashDetected(float x, float y, float z) {
        if (playerHookInWater(this.player)) {
            EntityFishHook hook = this.player.fishEntity;
//                double yDifference = Math.abs(hook.posY - y);
            // Ignore Y component when calculating distance from hook
            double xzDistanceFromHook = hook.getDistanceSq(x, hook.posY, z);
            if (xzDistanceFromHook <= CLOSE_BOBBER_SPLASH_THRESHOLD) {
//                    AutoFishLogger.info("[%d] Close bobber splash at %f /  %f", getGameTime(), xzDistanceFromHook, yDifference);
                this.closeBobberSplashDetectedAt = getGameTime();
//                    if (xzDistanceFromHook <= EXACT_BOBBER_SPLASH_THRESHOLD) {
//    //                    AutoFishLogger.info("[%d] Exact bobber splash at %f /  %f", getGameTime(), xzDistanceFromHook, yDifference);
//                        this.exactBobberSplashDetectedAt = getGameTime();
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
    public void onPlayerUseItem(EnumHand hand) {
        if (!playerIsHoldingRod()) {
            return;
        }
        // If player is holding a usable item in MAIN_HAND and a fishing rod in OFF_HAND,
        // then this function will be called twice in the same tick, one for each hand.
        // We need to ignore the non-fishing rod call.
        if (isUseOfNonRodInMainHand(hand)) {
            return;
        }
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
            // Bug in Forge that doesn't delete the fishing hook entity
//                Logger.info("fishEntity: %s", this.player.fishEntity);
            this.player.fishEntity = null;
        }
    }
    
    private boolean isUseOfNonRodInMainHand(EnumHand hand) {
        return hand == EnumHand.MAIN_HAND && !isUsableFishingRod(this.player.getHeldItemMainhand());
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
        if (this.minecraftClient != null && this.minecraftClient.player != null && playerHookInWater(this.minecraftClient.player)) {
            EntityFishHook hook = this.minecraftClient.player.fishEntity;
            double distanceFromHook = new BlockPos(x, y, z).distanceSq(hook.posX, hook.posY, hook.posZ);
            if (distanceFromHook <= CLOSE_WATER_WAKE_THRESHOLD) {
                if (this.closeWaterWakeDetectedAt <= 0) {
//                    AutoFishLogger.info("[%d] Close water wake at %f", getGameTime(), distanceFromHook);
                    this.closeWaterWakeDetectedAt = getGameTime();
                }
//                if (distanceFromHook <= EXACT_WATER_WAKE_THRESHOLD) {
//                    if (this.exactWaterWakeDetectedAt <=0) {
////                        AutoFishLogger.info("[%d] Exact water wake at %f", getGameTime(), distanceFromHook);
//                        this.exactWaterWakeDetectedAt = getGameTime();
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
            if (distanceFromPlayer < 2.0d) {
                this.xpLastAddedAt = getGameTime();
            }
        }
    }

    
    /***********  CORE LOGIC ****************/

    
    /**
     * Update the state of everything related to AutoFish functionality,
     * and trigger appropriate actions.
     */
    private void update() {
        if (!this.minecraftClient.isGamePaused() && this.player != null) {
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
                        Logger.debug("Player cast manually while recast was scheduled");
                    }                        
                    resetReelDelay();
                    resetCastSchedule();
                    resetBiteTracking();
                }
                
                if (AutoFishModConfig.entityClearProtectEnabled()) {
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
            /** If multiplayer, we must rely on client world conditions to guess when a bite occurs **/
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
        if (AutoFishModConfig.aggressiveBiteDetectionEnabled() && this.closeBobberSplashDetectedAt > 0) {
            Logger.debug("Detected bite by BOBBER_SPLASH");
            return true;
        }
        return false;
    }
    
    private boolean isFishBiting_fromWaterWake() {
        /** An water wake indicates a probable bite "very soon", so make sure enough time has passed **/
        if (AutoFishModConfig.aggressiveBiteDetectionEnabled()
                && this.closeWaterWakeDetectedAt > 0 
                && getGameTime() > this.closeWaterWakeDetectedAt + CLOSE_WATER_WAKE_DELAY_TICKS) {
            Logger.debug("Detected bite by WATER_WAKE");
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
            Logger.debug("Detected bite by MOVEMENT");
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
//            long totalWorldTime = getGameTime();
            if (recentCloseBobberSplash() || recentCloseWaterWake()) {
                Logger.debug("Detected bite by ALL");
                return true;
            }
        }
        return false;
    }
    
    
    /******************  STATE HELPERS  *****************/
    
    
    private boolean isDuringReelDelay() {
        return (this.startedReelDelayAt != 0 && getGameTime() < this.startedReelDelayAt + REEL_TICK_DELAY);
    }
    
    private boolean isDuringCastDelay() {
        return (this.startedCastDelayAt != 0 && getGameTime() < this.startedCastDelayAt + CAST_TICK_DELAY);
    }
    
    private boolean playerHookInWater(EntityPlayer player) {
        if (player == null || player.fishEntity == null) {
            return false;
        }
        // Sometimes, particularly around the time of a bite, the hook entity comes slightly out of the
        // water block, so also check a fraction of a block distance lower to see if that is water.
        // (EntityFishHook.isInWater() seems to be completely broken in 1.13)
        IBlockState hookBlockState = player.fishEntity.getEntityWorld().getBlockState(new BlockPos(player.fishEntity));
        IBlockState justBelowHookBlockState = player.fishEntity.getEntityWorld().getBlockState(new BlockPos(player.fishEntity.posX, player.fishEntity.posY - 0.25, player.fishEntity.posZ));
        boolean hookIsInWater = hookBlockState.getMaterial() == Material.WATER || justBelowHookBlockState.getMaterial() == Material.WATER;
        return hookIsInWater;
    }

    public boolean playerIsHoldingRod() {
        return findActiveFishingRod() != null;
    }
    
    private boolean isUsableFishingRod(ItemStack itemStack) {
        return (itemStack != null
                && itemStack.getItem() instanceof ItemFishingRod
                && itemStack.getDamage() <= itemStack.getMaxDamage());
    }

    private boolean recentCloseBobberSplash() {
        /** Close bobber sound must have been quite recent to indicate probable bite **/
        if (this.closeBobberSplashDetectedAt > 0 
                && getGameTime() < this.closeBobberSplashDetectedAt + 20) {
            return true;
        }
        return false;
    }
    
    private boolean recentCloseWaterWake() {
        /** A close water wake indicates probable bite "soon", so make sure enough time has passed **/
        if (this.closeWaterWakeDetectedAt > 0
                && getGameTime() > this.closeWaterWakeDetectedAt + CLOSE_WATER_WAKE_DELAY_TICKS) {
            return true;
        }
        return false;
    }
    
    private boolean somethingSeemsWrong() {
        if (rodIsCast() && !isDuringCastDelay() && !isDuringReelDelay() && hookShouldBeInWater()) {
            if ((playerHookInWater(this.player) || AutoFishModConfig.handleProblemsEnabled()) && waitedLongEnough()) {
                Logger.info("We should have caught something by now.");
                return true;
            }
            if (AutoFishModConfig.handleProblemsEnabled()) {
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
        return this.startedCastDelayAt > 0 && getGameTime() > this.startedCastDelayAt + (MAX_WAITING_TIME_SECONDS * TICKS_PER_SECOND);
    }
    
    private boolean hookShouldBeInWater() {
        return this.startedCastDelayAt > 0 && getGameTime() > this.startedCastDelayAt + MAX_HOOK_FLYING_TIME_TICKS;
    }
    
    private boolean rodIsCast() {
        if (!playerIsHoldingRod()) {
            return false;
        }
        ItemStack activeFishingRod = findActiveFishingRod();
        return activeFishingRod.getItem().getPropertyGetter(new ResourceLocation("cast")).call(activeFishingRod, this.minecraftClient.world, this.player) > 0F;
    }
    
    private boolean needToSwitchRods() {
        return AutoFishModConfig.multiRodEnabled() && !playerCanCast();
    }

    private boolean isTimeToCast() {
        return (this.castScheduledAt > 0 && getGameTime() > this.castScheduledAt + (AutoFishModConfig.recastDelay() * TICKS_PER_SECOND));
    }
    
    private boolean waitingToRecast() {
        return (this.castScheduledAt > 0);
    }

    private boolean playerCanCast() {
        if (!playerIsHoldingRod()) {
            return false;
        } else {
            ItemStack activeFishingRod = findActiveFishingRod();
            if (activeFishingRod == null) {
                return false;
            }
    
            return (!AutoFishModConfig.breakPreventEnabled() 
                    || (activeFishingRod.getMaxDamage() - activeFishingRod.getDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD)
                    );
        }
    }
    
    private ItemStack findActiveFishingRod() {
        if (this.player == null) {
            return null;
        }
        ItemStack heldItem = this.player.getHeldItemMainhand();
        ItemStack heldItemOffhand = this.player.getHeldItemOffhand();
        return isUsableFishingRod(heldItem) ? heldItem :
            isUsableFishingRod(heldItemOffhand) ? heldItemOffhand : null;
    }


    /******************  ACTION HELPERS  *****************/

    
    private void showNotificationToPlayer() {
        String notification = String.format(NOTIFICATION_TEXT_AUTOFISH_ENABLED, AutoFishModConfig.autofishEnabled() ? "enabled" : "disabled");
        this.player.sendMessage(new TextComponentString(notification));
        this.notificationShownToPlayer = true;
    }
    
    private void reelIn() {
        playerUseRod();
        // Bug in Forge that doesn't delete the fishing hook entity
        this.player.fishEntity = null;
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
        this.castScheduledAt = getGameTime();
    }

    /*
     *  Trigger a delay so we don't use the rod multiple times for the same bite,
     *  which can persist for 2-3 ticks.
     */
    private void startReelDelay() {
        this.startedReelDelayAt = getGameTime();
    }

    /*
     * Trigger a delay so that entity clear protection doesn't kick in during cast.
     */
    private void startCastDelay() {
        this.startedCastDelayAt = getGameTime();
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
        MinecraftServer server = Minecraft.getInstance().getIntegratedServer();
        if (server != null) {
            for (EntityPlayer player : server.getPlayerList().getPlayers()) {
                if (playerHookInWater(player)) {
                    int ticks = FAST_FISH_CATCHABLE_DELAY_TICKS + MathHelper.nextInt(this.rand, 0, FAST_FISH_DELAY_VARIANCE);
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
            int ticks = FAST_FISH_CATCHABLE_DELAY_TICKS + MathHelper.nextInt(this.rand, 0, FAST_FISH_DELAY_VARIANCE);
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
        if (this.minecraftClient.getIntegratedServer() == null || this.minecraftClient.getIntegratedServer().getWorld(DimensionType.OVERWORLD) == null) {
            return null;
        } else {
            return this.minecraftClient.getIntegratedServer().getWorld(DimensionType.OVERWORLD).getPlayerEntityByName(this.minecraftClient.player.getName().getString());
        }
    }

    private EnumActionResult playerUseRod() {
        return this.minecraftClient.playerController.processRightClick(
                this.player, 
                this.minecraftClient.world,
                isUsableFishingRod(this.player.getHeldItemMainhand()) ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND);
    }
    
//    private boolean isInOffHand(ItemStack itemStack) {
//        return findActiveFishingRod().getEquipmentSlot() == EntityEquipmentSlot.OFFHAND;
//    }
    
    private void resetBiteTracking() {
        this.xpLastAddedAt = 0L;
        this.closeWaterWakeDetectedAt = 0L;
//        this.exactWaterWakeDetectedAt = 0L;
        this.closeBobberSplashDetectedAt = 0L;
//        this.exactBobberSplashDetectedAt = 0L;
    }

    private void tryToSwitchRods() {
        InventoryPlayer inventory = this.player.inventory;
        for (int i = 0; i < 9; i++) {
            ItemStack curItemStack = inventory.mainInventory.get(i);
            if (curItemStack != null 
                    && curItemStack.getItem() instanceof ItemFishingRod
                    && (!AutoFishModConfig.breakPreventEnabled() || (curItemStack.getMaxDamage() - curItemStack.getDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD))
                ) {
                inventory.currentItem = i;
                break;
            }
        }
    }
    
    private void checkForEntityClear() {
        if (this.isFishing && !isDuringCastDelay() && (this.player.fishEntity == null || !this.player.fishEntity.isAddedToWorld())) {
            Logger.info("Entity Clear detected.  Re-casting.");
            this.isFishing = false;
            startFishing();
        }
    }
    
    private void checkForMissedBite() {
        if (playerHookInWater(this.player)) {
          if (this.closeBobberSplashDetectedAt > 0 && getGameTime() > this.closeBobberSplashDetectedAt + 45) {
              Logger.debug("I think we missed a fish");
              resetBiteTracking();
          }
      }
    }
    
    private long getGameTime() {
        return this.minecraftClient.world.getGameTime();
    }

}
