package net.unladenswallow.minecraft.autofish.gui;

import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.unladenswallow.minecraft.autofish.config.AutoFishModConfig;
import net.unladenswallow.minecraft.autofish.config.ConfigOption;
import net.unladenswallow.minecraft.autofish.config.ConfigOption.ValueType;
import net.unladenswallow.minecraft.autofish.util.Logger;

class ConfigGuiButton extends GuiButtonExt {

    private static final String BUTTON_TRUE_DISPLAY = "ON";
    private static final String BUTTON_FALSE_DISPLAY = "OFF";

    private ConfigOption configOption;

    public ConfigGuiButton(int x, int y, int widthIn, int heightIn, String buttonText, IPressable handler) {
        super(x, y, widthIn, heightIn, buttonText, handler);
    }
    
    public ConfigGuiButton(int x, int y, int widthIn, int heightIn, ConfigOption configOption) {
        super(x, y, widthIn, heightIn, getButtonText(configOption), b -> {
            Logger.info("button press handler");
            AutoFishModConfig.toggleConfigValue(configOption.configPath);
        });
        this.configOption = configOption;
    }
    
    private static String getButtonText(ConfigOption configOption) {
        if (configOption.valueType == ValueType.BOOL) {
            return configOption.boolValue ? BUTTON_TRUE_DISPLAY : BUTTON_FALSE_DISPLAY;
        } else if (configOption.valueType == ValueType.INT) {
            return Integer.toString(configOption.intValue);
        } else {
            Logger.info("Don't understand %s", configOption.valueType.name());
        }
        return "UNSUPPORTED-CONFIG";
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        AutoFishModConfig.toggleConfigValue(this.configOption.configPath);
        this.setMessage(getButtonText(this.configOption));
    }
    
}
