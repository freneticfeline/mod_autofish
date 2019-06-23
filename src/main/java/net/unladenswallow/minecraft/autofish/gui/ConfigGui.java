package net.unladenswallow.minecraft.autofish.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.ForgeI18n;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.unladenswallow.minecraft.autofish.config.AutoFishModConfig;
import net.unladenswallow.minecraft.autofish.config.ConfigOption;
import net.unladenswallow.minecraft.autofish.util.Logger;

public class ConfigGui extends Screen {

    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_START_Y = 45;
    private static final int X_PADDING = 30;
    private int _longestConfigLabelWidth = 0;
    // I don't know what this magic number means, but I saw it in Forge code, so there you go.
    public static final int DRAW_STRING_MAGIC_NUMBER = 16777215;
    
    private List<Label> labels = new ArrayList<Label>();
    
    
    public ConfigGui() {
        // TODO Auto-generated constructor stub
        super(new StringTextComponent("Test String"));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        drawCenteredString(this.font, ForgeI18n.parseMessage("gui.autofish.config.title"), this.width / 2, 10, DRAW_STRING_MAGIC_NUMBER);
        drawCenteredString(this.font, AutoFishModConfig.ConfigFilePath, this.width / 2, 25, DRAW_STRING_MAGIC_NUMBER);
        int bottomLabelY = 0;
        for (Label label : labels) {
            label.render(mouseX, mouseY, 1f);
            bottomLabelY = Math.max(bottomLabelY, label.y);
        }
        super.render(mouseX, mouseY, partialTicks);
        for (Label label : labels) {
            if (label.isHovered()) {
                label.renderToolTip(mouseX, mouseY);
            }
        }
    }
    
    private void closeGui() {
        this.onClose();
    }

    @Override
    protected void init() {
        super.init();
        int maxAllowedLabelWidth = this.width - BUTTON_WIDTH - (3 * X_PADDING);
        int labelWidth = Math.min(maxAllowedLabelWidth, getLongestConfigLabelWidth());
        int labelX = (this.width - (labelWidth + X_PADDING + BUTTON_WIDTH)) / 2;
        int buttonX = this.width - labelX - BUTTON_WIDTH;
        int buttonIndex = 1;
        this.labels = new ArrayList<Label>();
        for (ConfigOption option : AutoFishModConfig.getOrderedConfigValues()) {
            int rowY = BUTTON_START_Y + (int)((buttonIndex-1) * BUTTON_HEIGHT * 1.2);
            this.labels.add(new Label(
                    ForgeI18n.parseMessage(option.configLabelI18nPattern), 
                    ForgeI18n.parseMessage(String.format("%s.description", option.configLabelI18nPattern)), 
                    labelX, 
                    rowY+2,
                    this.width,
                    this.height));
            addButton(new ConfigGuiButton(this.font, buttonX, rowY, BUTTON_WIDTH, BUTTON_HEIGHT, option));
            buttonIndex++;
        }
        addButton(new GuiButtonExt((this.width - BUTTON_WIDTH) / 2, this.height - 20 - BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT, ForgeI18n.parseMessage("gui.autofish.config.done"),
                b -> {this.closeGui();}));
    }

    private int getLongestConfigLabelWidth() {
        if (_longestConfigLabelWidth > 0) {
            return _longestConfigLabelWidth;
        }
        _longestConfigLabelWidth = 1;
        for (ConfigOption option : AutoFishModConfig.getOrderedConfigValues()) {
            int labelWidth = this.font.getStringWidth(ForgeI18n.parseMessage(option.configLabelI18nPattern));
            if (labelWidth > _longestConfigLabelWidth) {
                _longestConfigLabelWidth = labelWidth;
            }
        }
        return _longestConfigLabelWidth;
    }

    class Label extends Widget {
        private int x;
        private int y;
        private String text;
        private String tooltip;
        private int screenWidth;
        private int screenHeight;

        public Label(String text, String tooltip, int x, int y, int screenWidth, int screenHeight) {
            super(x, y, text);
            this.x = x;
            this.y = y;
            this.height = font.FONT_HEIGHT + 2;
            this.text = text;
            this.tooltip = tooltip;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
        }
        

        @Override
        public void render(int mouseX, int mouseY, float p_render_3_) {
            super.render(mouseX, mouseY, p_render_3_);
            drawString(font, this.text, this.x, this.y, DRAW_STRING_MAGIC_NUMBER);
        }

        
        @Override
        public void renderButton(int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
        }


        @Override
        public void renderToolTip(int mouseX, int mouseY) {
            net.minecraftforge.fml.client.config.GuiUtils.drawHoveringText(
                    Arrays.asList(this.tooltip), 
                    mouseX, 
                    mouseY, 
                    this.screenWidth, 
                    this.screenHeight, 
                    -1, 
                    font);
        }
        
        
    }

    
}
