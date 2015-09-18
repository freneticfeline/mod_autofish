package net.unladenswallow.minecraft.autofish;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

public class KeyInputHandler {

	public KeyBinding options;
	
	public KeyInputHandler() {
		init();
	}
	
	public void init() {
		options = new KeyBinding("key.options", Keyboard.KEY_O, "key.categories.mod_autofish");
		ClientRegistry.registerKeyBinding(options);
	}
	
	@SubscribeEvent
	public void onKeyInput(InputEvent.KeyInputEvent event) {
		if (this.options.isPressed()) {
			AutoFishLogger.info("KeyInputHandler onKeyInput(): handling options key");
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			if (playerIsHoldingFishingRod(player)) {
				AutoFishLogger.info("KeyInputHandler onKeyInput(): holding fishing rod");
				Minecraft.getMinecraft().displayGuiScreen(new AutoFishConfigGui(Minecraft.getMinecraft().currentScreen));
//				player.openGui(ModAutoFish.instance, 0, player.worldObj, (int) player.posX, (int) player.posY, (int) player.posZ);
			}
		}
	}

	private boolean playerIsHoldingFishingRod(EntityPlayer player) {
		return (!Minecraft.getMinecraft().isGamePaused()
				&& player != null
				&& player.getHeldItem() != null
				&& player.getHeldItem().getItem() == Items.fishing_rod);
	}
	
}
