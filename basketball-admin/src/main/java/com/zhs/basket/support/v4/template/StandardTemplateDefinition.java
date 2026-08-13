package com.zhs.basket.support.v4.template;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StandardTemplateDefinition {
    private String templateCode;
    private String templateName;
    private String templateType;
    private String seriesCode;
    private String registrationMode;
    private String defaultRegistrationMode;
    private String previewSummary;
    private String formationNote;
    private List<StandardTemplateStageDefinition> stages = new ArrayList<>();
    private List<StandardTemplateOptionDefinition> options = new ArrayList<>();
}
