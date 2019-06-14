package net.unladenswallow.minecraft.autofish.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.unladenswallow.minecraft.autofish.config.AutoFishModConfig;
import net.unladenswallow.minecraft.autofish.config.ConfigOption;

public class ConfigGui extends Screen {

    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_START_Y = 40;
    private static final int LABEL_X = 60;
    private static final int BUTTON_X = 260;
    // I don't know what this magic number means, but I saw it in Forge code, so there you go.
    private static final int DRAW_STRING_MAGIC_NUMBER = 16777215;
    
    private List<Label> labels = new ArrayList<Label>();
    
    
    public ConfigGui() {
        // TODO Auto-generated constructor stub
        super(new StringTextComponent("Test String"));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        drawCenteredString(this.font, "AutoFish Forge Mod Options", this.width / 2, 20, DRAW_STRING_MAGIC_NUMBER);
        int bottomLabelY = 0;
        for (Label label : labels) {
            drawString(this.font, label.text, label.x, label.y, DRAW_STRING_MAGIC_NUMBER);
            bottomLabelY = Math.max(bottomLabelY, label.y);
        }
        super.render(mouseX, mouseY, partialTicks);
    }
    
    private void closeGui() {
        this.onClose();
    }

    @Override
    protected void init() {
        super.init();
        int buttonIndex = 1;
        for (ConfigOption option : AutoFishModConfig.getOrderedConfigValues()) {
            int rowY = BUTTON_START_Y + (int)((buttonIndex-1) * BUTTON_HEIGHT * 1.2);
            this.labels.add(new Label(option.configLabel, LABEL_X, rowY+2));
            addButton(new ConfigGuiButton(BUTTON_X, rowY, BUTTON_WIDTH, BUTTON_HEIGHT, option));
            buttonIndex++;
        }
        addButton(new GuiButtonExt((this.width - BUTTON_WIDTH) / 2, this.height - 20 - BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT, "Done",
                b -> {this.closeGui();}));
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

    
}
