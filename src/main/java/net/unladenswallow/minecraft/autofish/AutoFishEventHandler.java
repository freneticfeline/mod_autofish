package net.unladenswallow.minecraft.autofish;

import java.lang.reflect.Field;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.server.FMLServerHandler;

public class AutoFishEventHandler {

    private Minecraft minecraft;
    private EntityPlayer player;
    private long castScheduledAt = 0L;
    private long startedReelDelayAt = 0L;
    private long startedCastDelayAt = 0L;
    private boolean isFishing = false;
    
    private static final int TICKS_PER_SECOND = 20;

    /** How long to suppress checking for a bite after starting to reel in.  If we check for a bite while reeling
        in, we may think we have a bite and try to reel in again, which will actually cause a re-cast and lose the fish */
    private static final int REEL_TICK_DELAY = 15;

    /** How long to wait after casting to check for Entity Clear.  If we check too soon, the hook entity
        isn't in the world yet, and will trigger a false alarm and cause infinite recasting. */
    private static final int CAST_TICK_DELAY = 10;

    /** When Break Prevention is enabled, how low to let the durability get before stopping or switching rods */
    private static final int AUTOFISH_BREAKPREVENT_THRESHOLD = 2;

    /** The threshold for vertical movement of the fish hook that determines when a fish is biting, if using
        the movement method of detection. */
    private static final double MOTION_Y_THRESHOLD = -0.05d;
    
    /** The number of ticks to set as the "catchable delay" when Fast Fishing is enabled. */
    private static final int FAST_FISH_CATCHABLE_DELAY_TICKS = 42;
    
    public AutoFishEventHandler() {
        this.minecraft = FMLClientHandler.instance().getClient();
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
                for (WorldServer world : server.worlds) {
                    triggerBites(world);
                }
            }
        }
    }
    
    @SubscribeEvent
    public void onClientTickEvent(ClientTickEvent event) {
        if (ModAutoFish.config_autofish_enable && !this.minecraft.isGamePaused() && this.minecraft.player != null) {
            this.player = this.minecraft.player;

            if (playerIsHoldingRod() || waitingToRecast()) {
                if (playerHookInWater(this.player) && !isDuringReelDelay() && isFishBiting()) {
                    startReelDelay();
                    reelIn();
                    scheduleNextCast();
                } else if (isTimeToCast()) {
                    if (needToSwitchRods()) {
                        tryToSwitchRods();
                    }
                    if (playerCanCast()) {
                        startFishing();
                    }
                    resetReelDelay();
                    resetCastSchedule();
                }
                
                if (ModAutoFish.config_autofish_entityClearProtect && this.isFishing && !isDuringCastDelay() && this.player.fishEntity == null) {
                    AutoFishLogger.info("Entity Clear detected.  Re-casting.");
                    this.isFishing = false;
                    startFishing();
                }
                
                /*
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
    
    @SubscribeEvent
    public void onPlayerUseItem(PlayerInteractEvent.RightClickItem event) {
        // Only do this on the client side
        if (event.getWorld().isRemote && playerIsHoldingRod()) {
            this.isFishing = !this.isFishing;
//            AutoFishLogger.info("Player %s fishing", this.isFishing ? "started" : "stopped");
            if (this.isFishing) {
                startCastDelay();
            }
        }
    }
    
    private void reelIn() {
        playerUseRod();
    }

    private void startFishing() {
        playerUseRod();
        startCastDelay();
    }

    private void resetCastSchedule() {
        this.castScheduledAt = 0;
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
        return player.fishEntity != null
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
            /* If single player (integrated server), we can actually check to see if something
             * is catchable, but it's fragile (other mods could break it)
             * If anything goes wrong, fall back to the safer but less reliable method
             */
            try {
                return isFishBiting_fromServerEntity(serverPlayerEntity);
            } catch (Exception e) {
                return isFishBiting_fromMovement();
            }
        } else {
            return isFishBiting_fromMovement();
        }
    }

    private boolean isFishBiting_fromMovement() {
        EntityFishHook fishEntity = this.player.fishEntity;
        if (fishEntity != null 
                // Checking for no X and Z motion prevents a false alarm when the hook is moving through the air
                && fishEntity.motionX == 0 
                && fishEntity.motionZ == 0 
                && fishEntity.motionY < MOTION_Y_THRESHOLD) {
            return true;
        }
        return false;
    }

    private boolean isFishBiting_fromServerEntity(EntityPlayer serverPlayerEntity) throws NumberFormatException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        /*
         * The fish hook entity on the server side knows whether a fish is catchable at any given time.  However,
         * that field is private and not exposed in any way.  So we must use reflection to access that field.
         */
        EntityFishHook serverFishEntity = serverPlayerEntity.fishEntity;
        int ticksCatchable = getPrivateIntFieldFromObject(serverFishEntity, "ticksCatchable", "field_146045_ax");
        if (ticksCatchable > 0) {
            return true;
        }
        return false;
    }
    
    /**
     * For all players in the specified world, if they are fishing, trigger a bite.
     * 
     * @param world
     */
    private void triggerBites(World world) {
        if (!world.isRemote) {
            Predicate<EntityPlayer> p_true = new Predicate<EntityPlayer>() {
                public boolean apply(@Nullable EntityPlayer p_apply_1_)
                {
                    return true;
                }
            };
            List<EntityPlayer> allPlayers = world.getPlayers(EntityPlayer.class, p_true);
            for (EntityPlayer player : allPlayers) {
                if (playerHookInWater(player)) {
                    setTicksCatchableDelay(player.fishEntity, FAST_FISH_CATCHABLE_DELAY_TICKS);
                }
            }
        }
    }
    
    /**
     * [Currently unused]
     * For the current player, trigger a bite on the fish hook.
     * 
     */
    private void triggerBite() {
        EntityPlayer serverPlayerEntity = getServerPlayerEntity();
        if (serverPlayerEntity != null) {
            /*
             * If we are single player and have access to the server player entity, try to hack the fish hook entity
             * to make fish bite sooner.
             */
            EntityFishHook serverFishEntity = serverPlayerEntity.fishEntity;
            try {
                int currentTicksCatchableDelay = getPrivateIntFieldFromObject(serverFishEntity, "ticksCatchableDelay", "field_146038_az");
                if (currentTicksCatchableDelay == 0) {
                    try {
                        setPrivateIntFieldOfObject(serverFishEntity, "ticksCatchableDelay", "field_146038_az", FAST_FISH_CATCHABLE_DELAY_TICKS);
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }
        }
    }
    
    private void setTicksCatchableDelay(EntityFishHook hook, int ticks) {
        try {
            int currentTicksCatchableDelay = getPrivateIntFieldFromObject(hook, "ticksCatchableDelay", "field_146038_az");
            if (currentTicksCatchableDelay == 0) {
                try {
                    setPrivateIntFieldOfObject(hook, "ticksCatchableDelay", "field_146038_az", ticks);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * Using Java reflection APIs, access a private member data of type int
     * 
     * @param object The target object
     * @param fieldName The name of the private data field in object
     * 
     * @return The int value of the private member data from object with fieldName
     */
    private int getPrivateIntFieldFromObject(Object object, String forgeFieldName, String vanillaFieldName) throws NoSuchFieldException, SecurityException, NumberFormatException, IllegalArgumentException, IllegalAccessException {
        Field targetField = null;
        try {
            targetField = object.getClass().getDeclaredField(forgeFieldName);
        } catch (NoSuchFieldException e) {
            targetField = object.getClass().getDeclaredField(vanillaFieldName);
        }
        if (targetField != null) {
            targetField.setAccessible(true);
            return Integer.valueOf(targetField.get(object).toString()).intValue();
        } else {
            return 0;
        }
            
    }

    /**
     * Using Java reflection APIs, set a private member data of type int
     * 
     * @param object The target object
     * @param fieldName The name of the private data field in object
     * @param value The int value to set the private member data from object with fieldName
     * 
     */
    private void setPrivateIntFieldOfObject(Object object, String forgeFieldName, String vanillaFieldName, int value) throws NoSuchFieldException, SecurityException, NumberFormatException, IllegalArgumentException, IllegalAccessException {
        Field targetField = null;
        try {
            targetField = object.getClass().getDeclaredField(forgeFieldName);
        } catch (NoSuchFieldException e) {
            targetField = object.getClass().getDeclaredField(vanillaFieldName);
        }
        if (targetField != null) {
            targetField.setAccessible(true);
            targetField.set(object, value);
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
        return (this.castScheduledAt != 0 && this.minecraft.world.getTotalWorldTime() > this.castScheduledAt + (ModAutoFish.config_autofish_recastDelay * TICKS_PER_SECOND));
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
