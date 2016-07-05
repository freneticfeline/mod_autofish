package net.unladenswallow.minecraft.autofish;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class AutoFishEventHandler {

    private Minecraft minecraft;
    private EntityPlayer player;
    private long castScheduledAt = 0L;
    private long startedReelDelayAt = 0L;
    private static final int CAST_QUEUE_TICK_DELAY = 30;
    private static final int REEL_TICK_DELAY = 5;
    private static final int AUTOFISH_BREAKPREVENT_THRESHOLD = 2;
    private static final double MOTION_Y_THRESHOLD = -0.02d;
    
    @SubscribeEvent
    public void onClientTickEvent(ClientTickEvent event) {
        this.minecraft = Minecraft.getMinecraft();
        if (ModAutoFish.config_autofish_enable && !this.minecraft.isGamePaused() && this.minecraft.thePlayer != null) {
            this.player = this.minecraft.thePlayer;

            if (playerHookInWater() && !isDuringReelDelay() && isFishBiting()) {
                startReelDelay();
                playerUseRod();
                scheduleNextCast();
            } else if (isTimeToCast()) {
                if (needToSwitchRods()) {
                    tryToSwitchRods();
                }
                if (playerCanCast()) {
                    playerUseRod();
                }
                // Resetting these values is not strictly necessary, but will improve the performance
                // of the check that potentially occurs every tick.
                resetReelDelay();
                resetCastSchedule();
            }
        }
    }
    
    private void resetCastSchedule() {
        this.castScheduledAt = 0;
    }

    private boolean needToSwitchRods() {
        return ModAutoFish.config_autofish_multirod && !playerCanCast();
    }

    private void scheduleNextCast() {
        this.castScheduledAt = this.minecraft.theWorld.getTotalWorldTime();
    }

    /*
     *  Trigger a delay so we don't use the rod multiple times for the same bite,
     *  which can persist for 2-3 ticks.
     */
    private void startReelDelay() {
        this.startedReelDelayAt = this.minecraft.theWorld.getTotalWorldTime();
    }

    private void resetReelDelay() {
        startedReelDelayAt = 0;
    }

    private boolean isDuringReelDelay() {
        return (this.startedReelDelayAt != 0 && this.minecraft.theWorld.getTotalWorldTime() < this.startedReelDelayAt + REEL_TICK_DELAY);
    }
    
    private boolean playerHookInWater() {
        return this.player.fishEntity != null
                && this.player.fishEntity.isInWater();
    }

    private boolean playerIsHoldingRod() {
        ItemStack heldItem = this.player.getHeldItemMainhand();

        return (heldItem != null
                && heldItem.getItem() == Items.fishing_rod
                && heldItem.getItemDamage() <= heldItem.getMaxDamage());
    }

    private boolean isFishBiting() {
        EntityPlayer serverPlayerEntity = getServerPlayerEntity();
        if (serverPlayerEntity != null) {
            return isFishBiting_integratedServer(serverPlayerEntity);
        } else {
            return isFishBiting_remoteServer();
        }
    }

    private boolean isFishBiting_remoteServer() {
        EntityFishHook fishEntity = this.player.fishEntity;
        if (fishEntity != null 
                && fishEntity.motionX == 0 
                && fishEntity.motionZ == 0 
                && fishEntity.motionY < MOTION_Y_THRESHOLD) {
            return true;
        }
        return false;
    }

    private boolean isFishBiting_integratedServer(EntityPlayer serverPlayerEntity) {
        /*
         * The fish hook entity on the server side knows whether a fish is catchable at any given time.  However,
         * that field is private and not exposed in any way.  So we must use reflection to access that field.
         */
        EntityFishHook serverFishEntity = serverPlayerEntity.fishEntity;
        try {
            int ticksCatchable = getPrivateIntFieldFromObject(serverFishEntity, "ticksCatchable");
            if (ticksCatchable > 0) {
                return true;
            }
        } catch (Exception e) {}
        return false;
    }

    /**
     * Using Java reflection APIs, access a private member data of type int
     * 
     * @param object The target object
     * @param fieldName The name of the private data field in object
     * 
     * @return The int value of the private member data from object with fieldName
     */
    private int getPrivateIntFieldFromObject(Object object, String fieldName) throws NoSuchFieldException, SecurityException, NumberFormatException, IllegalArgumentException, IllegalAccessException {
        Field targetField = object.getClass().getDeclaredField("ticksCatchable");
        targetField.setAccessible(true);
        return Integer.valueOf(targetField.get(object).toString()).intValue();
    }

    private EntityPlayer getServerPlayerEntity() {
        if (this.minecraft.getIntegratedServer() == null || this.minecraft.getIntegratedServer().getEntityWorld() == null) {
            return null;
        } else {
            return this.minecraft.getIntegratedServer().getEntityWorld().getPlayerEntityByName(this.minecraft.thePlayer.getName());
        }
    }

    private EnumActionResult playerUseRod() {
        return this.minecraft.playerController.processRightClick(
                this.player, 
                this.minecraft.theWorld, 
                this.player.getHeldItemMainhand(), 
                EnumHand.MAIN_HAND);
    }
    
    private boolean isTimeToCast() {
        return (this.castScheduledAt != 0 && this.minecraft.theWorld.getTotalWorldTime() > this.castScheduledAt + CAST_QUEUE_TICK_DELAY);
    }

    private void tryToSwitchRods() {
        InventoryPlayer inventory = this.player.inventory;
        for (int i = 0; i < 9; i++) {
            ItemStack curItemStack = inventory.mainInventory[i];
            if (curItemStack != null 
                    && curItemStack.getItem() == Items.fishing_rod
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
