package net.unladenswallow.minecraft.autofish.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.unladenswallow.minecraft.autofish.ModAutoFish;

public class ConfigGui extends GuiConfig {

    public ConfigGui(GuiScreen parent) {
        super(parent,
            new ConfigElement(ModAutoFish.configFile.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(),
            ModAutoFish.MODID, false, false, "AutoFish Forge Mod Options"
            );
        this.titleLine2 = GuiConfig.getAbridgedConfigPath(ModAutoFish.configFile.toString());
    }
}
