package com.zhs.basket.support.v4.template;

import lombok.Data;

import java.util.List;

@Data
public class StandardTemplateOptionDefinition {
    private String key;
    private String label;
    private String inputType;
    private List<String> allowedValues;
    private String defaultValue;
    private Integer minValue;
    private Integer maxValue;

    public static StandardTemplateOptionDefinition select(String key, String label, List<String> allowed, String defaultValue) {
        StandardTemplateOptionDefinition option = new StandardTemplateOptionDefinition();
        option.setKey(key);
        option.setLabel(label);
        option.setInputType("SELECT");
        option.setAllowedValues(allowed);
        option.setDefaultValue(defaultValue);
        return option;
    }

    public static StandardTemplateOptionDefinition number(String key, String label, Integer min, Integer max,
                                                          String defaultValue, List<String> suggested) {
        StandardTemplateOptionDefinition option = new StandardTemplateOptionDefinition();
        option.setKey(key);
        option.setLabel(label);
        option.setInputType("NUMBER");
        option.setMinValue(min);
        option.setMaxValue(max);
        option.setDefaultValue(defaultValue);
        option.setAllowedValues(suggested);
        return option;
    }
}
