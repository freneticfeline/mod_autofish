package net.unladenswallow.minecraft.autofish.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.unladenswallow.minecraft.autofish.config.AutoFishModConfig;
import net.unladenswallow.minecraft.autofish.config.ConfigOption;
import net.unladenswallow.minecraft.autofish.config.ConfigOption.ValueType;
import net.unladenswallow.minecraft.autofish.util.Logger;

public class ConfigGui extends GuiScreen {

    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_START_Y = 40;
    private static final int LABEL_X = 60;
    private static final int BUTTON_X = 260;
    // I don't know what this magic number means, but I saw it in Forge code, so there you go.
    private static final int DRAW_STRING_MAGIC_NUMBER = 16777215;
    private static final String BUTTON_TRUE_DISPLAY = "ON";
    private static final String BUTTON_FALSE_DISPLAY = "OFF";
    
    private List<Label> labels = new ArrayList<Label>();
    
    
    public ConfigGui(GuiScreen currentScreen) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(this.fontRenderer, "AutoFish Forge Mod Options", this.width / 2, 20, DRAW_STRING_MAGIC_NUMBER);
        int bottomLabelY = 0;
        for (Label label : labels) {
            drawString(this.fontRenderer, label.text, label.x, label.y, DRAW_STRING_MAGIC_NUMBER);
            bottomLabelY = Math.max(bottomLabelY, label.y);
        }
        drawString(this.fontRenderer, "Note: Entity Clear Protect is not supported in this version.", 20, bottomLabelY + 20, DRAW_STRING_MAGIC_NUMBER);
        drawString(this.fontRenderer, "Enable Handle Problems as a workaround.", 20, bottomLabelY + 30, DRAW_STRING_MAGIC_NUMBER);
        super.render(mouseX, mouseY, partialTicks);
    }
    
    private void closeGui() {
        this.close();
    }

    @Override
    protected void initGui() {
        super.initGui();
        int buttonIndex = 1;
        for (ConfigOption option : AutoFishModConfig.getOrderedConfigValues()) {
            int rowY = BUTTON_START_Y + (int)((buttonIndex-1) * BUTTON_HEIGHT * 1.2);
            this.labels.add(new Label(option.configLabel, LABEL_X, rowY+2));
            addButton(new Button(buttonIndex, BUTTON_X, rowY, BUTTON_WIDTH, BUTTON_HEIGHT, option));
            buttonIndex++;
        }
        addButton(new Button(buttonIndex, (this.width - BUTTON_WIDTH) / 2, this.height - 20 - BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT, "Done") {

            @Override
            public void onClick(double mouseX, double mouseY) {
                closeGui();
            }
            
        });
    }

    class Label {
        private int x;
        private int y;
        private String text;

        public Label(String text, int x, int y) {
            this.x = x;
            this.y = y;
            this.text = text;
        }
    }

    class Button extends GuiButton {

        private ConfigOption configOption;

        public Button(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
            super(buttonId, x, y, widthIn, heightIn, buttonText);
        }
        
        public Button(int buttonId, int x, int y, int widthIn, int heightIn, ConfigOption configOption) {
            super(buttonId, x, y, widthIn, heightIn, "UN-INIT");
            this.configOption = configOption;
            this.displayString = getButtonText();
        }
        
        private String getButtonText() {
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
            AutoFishModConfig.toggleConfigValue(configOption.configPath);
            this.displayString = getButtonText();
            this.render(Double.valueOf(mouseX).intValue(), Double.valueOf(mouseY).intValue(), 0);
        }
        
        
        
    }
    
}
