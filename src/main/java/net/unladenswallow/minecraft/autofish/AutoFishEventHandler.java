package net.unladenswallow.minecraft.autofish;

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

    private static final int QUEUE_TICK_LENGTH = 30;
    private static final int AUTOFISH_BREAKPREVENT_THRESHOLD = 5;
    private static final double MOTION_Y_THRESHOLD = -0.02d;
    private boolean caughtFish = false;
    private long castQueuedAt = -1;
    private Minecraft minecraft;
    private EntityPlayer player;

    @SubscribeEvent
    public void onClientTickEvent(ClientTickEvent event) {
        this.minecraft = Minecraft.getMinecraft();
        if (!minecraft.isGamePaused() && minecraft.thePlayer != null && ModAutoFish.config_autofish_enable) {
            this.player = this.minecraft.thePlayer;

            if (playerCanFish()) {
                if (ModAutoFish.config_autofish_multirod && playerWasFishing()) {
                    switchRods();
                }

                if (fishBites()) {
                    caughtFish = true;
                    playerUseRod();
                    castQueuedAt = minecraft.theWorld.getTotalWorldTime();
                } else if (playerWasFishing() && minecraft.theWorld.getTotalWorldTime() > castQueuedAt + QUEUE_TICK_LENGTH) {
                    caughtFish = false;
                    playerUseRod();
                    castQueuedAt = -1;
                }
            }
        }
    }

    private EnumActionResult playerUseRod() {
        return this.minecraft.playerController.processRightClick(this.player, this.minecraft.theWorld, this.player.getHeldItemMainhand(), EnumHand.MAIN_HAND);
    }

    private boolean fishBites() {
        EntityFishHook fishHook = this.player.fishEntity;

        return (fishHook != null &&
                !this.caughtFish && fishHook.posX == fishHook.prevPosX &&
                fishHook.posZ == fishHook.prevPosZ &&
                fishHook.motionY <= MOTION_Y_THRESHOLD);
    }

    private boolean playerWasFishing() {
        return (this.castQueuedAt > 0 && this.caughtFish);
    }

    private void switchRods() {
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

    private boolean playerCanFish() {
        ItemStack heldItem = this.player.getHeldItemMainhand();

        return (ModAutoFish.config_autofish_enable
                && heldItem != null
                && heldItem.getItem() == Items.fishing_rod
                && heldItem.getItemDamage() <= heldItem.getMaxDamage()
                && (!ModAutoFish.config_autofish_preventBreak
                || (heldItem.getMaxDamage() - heldItem.getItemDamage() > AUTOFISH_BREAKPREVENT_THRESHOLD))
        );
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
        if (eventArgs.getModID().equals(ModAutoFish.MODID)) {
            ModAutoFish.syncConfig();
        }
    }

}
