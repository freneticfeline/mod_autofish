package net.unladenswallow.minecraft.autofish;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class AutoFishEventHandler {

	private long ticksInWater = 0;
	private boolean caughtFish = false;
	private boolean fishing = false;
	private long castQueuedAt = -1;
	private static final int QUEUE_TICK_LENGTH = 30;
	private static final int INWATER_DELAY_TICK_LENGTH = 100;
	private static final int AUTOFISH_BREAKPREVENT_THRESHOLD = 5;
	private static final double MOTION_Y_THRESHOLD = 0.02d;
	
	@SubscribeEvent
	public void onClientTickEvent(ClientTickEvent event) {
		Minecraft minecraft = Minecraft.getMinecraft();
		if (!minecraft.isGamePaused() && minecraft.thePlayer != null) {
			EntityPlayer player = minecraft.thePlayer;
			if (ModAutoFish.config_autofish_enable) {
				if (ModAutoFish.config_autofish_multirod && fishing && castQueued() && !canCast(player)) {
					switchRods(player);
				}
				if (canCast(player)) {
					fishing = true;
					if (player.fishEntity != null) {
						if (!caughtFish && player.fishEntity.isInWater()) {
							ticksInWater++;
							if (ticksInWater > INWATER_DELAY_TICK_LENGTH && Math.abs(player.fishEntity.motionY) > MOTION_Y_THRESHOLD) {
								caughtFish = true;
								minecraft.playerController.sendUseItem(player, minecraft.theWorld, player.getHeldItem());
								castQueuedAt = minecraft.theWorld.getTotalWorldTime();
							}
						}
					} else if (castQueued() && minecraft.theWorld.getTotalWorldTime() > castQueuedAt + QUEUE_TICK_LENGTH) {
						minecraft.playerController.sendUseItem(player, minecraft.theWorld, player.getHeldItem());
						castQueuedAt = -1;
					} else {
						ticksInWater = 0;
						caughtFish = false;
					}
				} else if (fishing) { // Not able to cast.  Reset everything
					fishing = false;
					ticksInWater = 0;
					caughtFish = false;
					castQueuedAt = -1;
				}
			}
		}
	}
	
	private boolean castQueued() {
		return (castQueuedAt > 0);
	}

	private void switchRods(EntityPlayer player) {
		InventoryPlayer inventory = player.inventory;
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

	private boolean canCast(EntityPlayer player) {
		ItemStack heldItem = player.getHeldItem();
		if (heldItem != null && heldItem.getItem() == Items.fishing_rod) {
		}
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
		if (eventArgs.modID.equals(ModAutoFish.MODID)) {
			ModAutoFish.syncConfig();
		}
	}
	
}
