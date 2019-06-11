package net.unladenswallow.minecraft.autofish.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.unladenswallow.minecraft.autofish.ModAutoFish;
import net.unladenswallow.minecraft.autofish.gui.ConfigGui;

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
            if (ModAutoFish.autoFish.playerIsHoldingRod()) {
                Minecraft.getInstance().displayGuiScreen(new ConfigGui(Minecraft.getInstance().currentScreen));
            }
        }
    }

}
