package net.unladenswallow.minecraft.autofish;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;

public class AutoFishConfigGui extends GuiConfig {

	public AutoFishConfigGui(GuiScreen parent) {
		super(parent,
			new ConfigElement(ModAutoFish.configFile.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(),
			ModAutoFish.MODID, false, false, "AutoFish Forge Mod Options"
			);
		this.titleLine2 = GuiConfig.getAbridgedConfigPath(ModAutoFish.configFile.toString());
	}
}
