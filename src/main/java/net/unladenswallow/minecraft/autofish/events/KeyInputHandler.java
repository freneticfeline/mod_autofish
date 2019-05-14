package net.unladenswallow.minecraft.autofish.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFishingRod;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.unladenswallow.minecraft.autofish.util.Logger;

public class KeyInputHandler {

    public KeyBinding options;
    
    public KeyInputHandler() {
        init();
    }
    
    public void init() {
        options = new KeyBinding("key.options", InputMappings.getInputByName("key.keyboard.o").getKeyCode(), "key.categories.mod_autofish");
        ClientRegistry.registerKeyBinding(options);
    }
    
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (this.options.isPressed()) {
            EntityPlayer player = Minecraft.getInstance().player;
            if (playerIsHoldingFishingRod(player)) {
//                Minecraft.getInstance().displayGuiScreen(new ConfigGui(Minecraft.getInstance().currentScreen));
            }
        }
    }

    private boolean playerIsHoldingFishingRod(EntityPlayer player) {
        return (!Minecraft.getInstance().isGamePaused()
                && player != null
                && player.getHeldItemMainhand() != null
                && player.getHeldItemMainhand().getItem() instanceof ItemFishingRod);
    }
    
}
