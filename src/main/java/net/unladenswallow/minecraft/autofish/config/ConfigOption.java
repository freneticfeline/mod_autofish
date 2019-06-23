package net.unladenswallow.minecraft.autofish.config;

import java.util.List;

public class ConfigOption {
    public ConfigOption(List<String> path, boolean value, String labelI18nPattern) {
        valueType = ValueType.BOOL;
        configPath = path;
        boolValue = value;
        configLabelI18nPattern = labelI18nPattern;
    }
    
    public ConfigOption(List<String> path, int value, String labelI18nPattern) {
        valueType = ValueType.INT;
        configPath = path;
        intValue = value;
        configLabelI18nPattern = labelI18nPattern;
    }

    public enum ValueType {
        BOOL,
        INT 
    }
    
    public ValueType valueType;
    public List<String> configPath;
    public boolean boolValue;
    public int intValue;
    public String configLabelI18nPattern;
}
