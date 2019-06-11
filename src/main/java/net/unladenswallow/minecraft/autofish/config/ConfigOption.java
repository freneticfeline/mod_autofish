package net.unladenswallow.minecraft.autofish.config;

import java.util.List;

public class ConfigOption {
    public ConfigOption(List<String> path, boolean value, String label) {
        valueType = ValueType.BOOL;
        configPath = path;
        boolValue = value;
        configLabel = label;
    }
    
    public ConfigOption(List<String> path, int value, String label) {
        valueType = ValueType.INT;
        configPath = path;
        intValue = value;
        configLabel = label;
    }

    public enum ValueType {
        BOOL,
        INT 
    }
    
    public ValueType valueType;
    public List<String> configPath;
    public boolean boolValue;
    public int intValue;
    public String configLabel;
}
