package com.zhs.basket.support.v4.template;

import com.zhs.basket.enums.v4.ByeRuleEnum;
import com.zhs.basket.enums.v4.DrawRuleEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.SeedRuleEnum;
import com.zhs.basket.enums.v4.SeriesCodeEnum;
import com.zhs.basket.enums.v4.TemplateTypeEnum;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class OpenStandardTemplateBuilder implements StandardFormatTemplateBuilder {

    private static final Set<String> SEEDS = Set.of(
            SeedRuleEnum.NONE.getCode(), SeedRuleEnum.ADMIN.getCode(), SeedRuleEnum.RANDOM.getCode());
    private static final Set<String> DRAWS = Set.of(
            DrawRuleEnum.RANDOM_CONFIRM.getCode(), DrawRuleEnum.MANUAL_SLOT.getCode());
    private static final Set<String> BYES = Set.of(
            ByeRuleEnum.RANDOM.getCode(), ByeRuleEnum.SEED_PRIORITY.getCode(), ByeRuleEnum.MANUAL.getCode());

    @Override
    public SeriesCodeEnum support() {
        return SeriesCodeEnum.OPEN;
    }

    @Override
    public String templateCode() {
        return StandardTemplateCodes.OPEN_STANDARD_V1;
    }

    @Override
    public StandardTemplateDefinition capabilities() {
        return build(defaultContext());
    }

    @Override
    public void validate(StandardTemplateContext context) {
        StandardTemplateContext resolved = resolve(context);
        RegistrationModeEnum.fromCode(resolved.getRegistrationMode());
        if (!SEEDS.contains(resolved.getSeedRule())) {
            throw new ServiceException("Open seedRule is not allowed");
        }
        if (!DRAWS.contains(resolved.getDrawRule())) {
            throw new ServiceException("Open drawRule is not allowed");
        }
        if (!BYES.contains(resolved.getByeRule())) {
            throw new ServiceException("Open byeRule is not allowed");
        }
        if (resolved.getKnockoutBestOf() == null
                || !com.zhs.basket.support.v4.BestOfValues.isLegal(resolved.getKnockoutBestOf())) {
            throw new ServiceException("Open BO only allows 1/3/5/7");
        }
    }

    @Override
    public StandardTemplateDefinition build(StandardTemplateContext context) {
        validate(context);
        StandardTemplateContext resolved = resolve(context);
        StandardTemplateDefinition definition = catalogHeader(resolved.getRegistrationMode());
        definition.getStages().add(StandardTemplateStages.knockout(
                StandardTemplateCodes.STAGE_KNOCKOUT, "\u6dd8\u6c70\u8d5b", 1, resolved.getKnockoutBestOf(),
                resolved.getSemifinalBestOf(), resolved.getFinalBestOf(),
                pairingOf(resolved.getDrawRule()), resolved.getSeedRule(), resolved.getDrawRule(), resolved.getByeRule()));
        if (RegistrationModeEnum.PERSONAL.getCode().equals(resolved.getRegistrationMode())) {
            definition.setFormationNote("PERSONAL \u62a5\u540d \u2192 Formation \u2192 \u4e34\u65f6\u961f\u4f0d \u2192 \u6dd8\u6c70\u8d5b");
        }
        return definition;
    }

    private StandardTemplateDefinition catalogHeader(String registrationMode) {
        StandardTemplateDefinition definition = new StandardTemplateDefinition();
        definition.setTemplateCode(templateCode());
        definition.setTemplateName("Open \u6807\u51c6\u6a21\u677f");
        definition.setTemplateType(TemplateTypeEnum.STANDARD.getCode());
        definition.setSeriesCode(SeriesCodeEnum.OPEN.getCode());
        definition.setDefaultRegistrationMode(RegistrationModeEnum.PERSONAL.getCode());
        definition.setRegistrationMode(registrationMode);
        definition.setPreviewSummary("\u6dd8\u6c70\u8d5b");
        definition.setOptions(List.of(
                StandardTemplateOptionDefinition.select("registrationMode", "\u62a5\u540d\u65b9\u5f0f",
                        List.of("TEAM", "PERSONAL"), "PERSONAL"),
                StandardTemplateOptionDefinition.select("seedRule", "\u79cd\u5b50\u89c4\u5219",
                        List.of("NONE", "ADMIN", "RANDOM"), "NONE"),
                StandardTemplateOptionDefinition.select("drawRule", "\u62bd\u7b7e\u89c4\u5219",
                        List.of("RANDOM_CONFIRM", "MANUAL_SLOT"), "RANDOM_CONFIRM"),
                StandardTemplateOptionDefinition.select("byeRule", "BYE \u89c4\u5219",
                        List.of("RANDOM", "SEED_PRIORITY", "MANUAL"), "RANDOM"),
                StandardTemplateOptionDefinition.select("knockoutBestOf", "\u6dd8\u6c70\u8d5b Early BO",
                        List.of("1", "3", "5", "7"), "3"),
                StandardTemplateOptionDefinition.select("semifinalBestOf", "\u534a\u51b3\u8d5b BO",
                        List.of("1", "3", "5", "7"), "5"),
                StandardTemplateOptionDefinition.select("finalBestOf", "\u51b3\u8d5b BO",
                        List.of("1", "3", "5", "7"), "5")
        ));
        return definition;
    }

    private StandardTemplateContext defaultContext() {
        StandardTemplateContext context = new StandardTemplateContext();
        context.setRegistrationMode(RegistrationModeEnum.PERSONAL.getCode());
        return resolve(context);
    }

    private StandardTemplateContext resolve(StandardTemplateContext context) {
        StandardTemplateContext resolved = context == null ? new StandardTemplateContext() : context;
        if (resolved.getRegistrationMode() == null || resolved.getRegistrationMode().isEmpty()) {
            resolved.setRegistrationMode(RegistrationModeEnum.PERSONAL.getCode());
        }
        if (resolved.getSeedRule() == null || resolved.getSeedRule().isEmpty()) {
            resolved.setSeedRule(SeedRuleEnum.NONE.getCode());
        }
        if (resolved.getDrawRule() == null || resolved.getDrawRule().isEmpty()) {
            resolved.setDrawRule(DrawRuleEnum.RANDOM_CONFIRM.getCode());
        }
        if (resolved.getByeRule() == null || resolved.getByeRule().isEmpty()) {
            resolved.setByeRule(ByeRuleEnum.RANDOM.getCode());
        }
        if (resolved.getKnockoutBestOf() == null) {
            resolved.setKnockoutBestOf(3);
        }
        if (resolved.getSemifinalBestOf() == null) {
            resolved.setSemifinalBestOf(5);
        }
        if (resolved.getFinalBestOf() == null) {
            resolved.setFinalBestOf(5);
        }
        return resolved;
    }

    private static String pairingOf(String drawRule) {
        if (DrawRuleEnum.MANUAL_SLOT.getCode().equals(drawRule)) {
            return PairingRuleEnum.MANUAL.getCode();
        }
        return PairingRuleEnum.RANDOM_DRAW.getCode();
    }
}
