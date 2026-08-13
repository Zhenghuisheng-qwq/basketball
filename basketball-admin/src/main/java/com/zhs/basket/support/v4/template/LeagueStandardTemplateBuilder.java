package com.zhs.basket.support.v4.template;

import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.SeriesCodeEnum;
import com.zhs.basket.enums.v4.TemplateTypeEnum;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LeagueStandardTemplateBuilder implements StandardFormatTemplateBuilder {

    static final List<Integer> PLAYOFF_SLOT_PRESETS = List.of(4, 8, 16, 32);

    @Override
    public SeriesCodeEnum support() {
        return SeriesCodeEnum.LEAGUE;
    }

    @Override
    public String templateCode() {
        return StandardTemplateCodes.LEAGUE_STANDARD_V1;
    }

    @Override
    public StandardTemplateDefinition capabilities() {
        return build(defaultContext());
    }

    @Override
    public void validate(StandardTemplateContext context) {
        StandardTemplateContext resolved = resolve(context);
        int cycles = resolved.getRoundRobinCycles();
        if (cycles != 1 && cycles != 2) {
            throw new ServiceException("League cycles only allow 1 or 2");
        }
        if (resolved.getPlayoffSlots() == null || resolved.getPlayoffSlots() < 2) {
            throw new ServiceException("League playoffSlots must be >= 2");
        }
        validateBestOf(resolved.getRegularBestOf(), "League regular BO");
        validateBestOf(resolved.getKnockoutBestOf(), "League playoff early BO");
        validateBestOf(resolved.getSemifinalBestOf(), "League semifinal BO");
        validateBestOf(resolved.getFinalBestOf(), "League final BO");
        RegistrationModeEnum.fromCode(resolved.getRegistrationMode());
    }

    @Override
    public StandardTemplateDefinition build(StandardTemplateContext context) {
        validate(context);
        StandardTemplateContext resolved = resolve(context);
        StandardTemplateDefinition definition = catalogHeader(resolved.getRegistrationMode());
        definition.getStages().add(StandardTemplateStages.roundRobin(
                StandardTemplateCodes.STAGE_REGULAR, "\u5e38\u89c4\u8d5b", 1, "REGULAR",
                resolved.getRoundRobinCycles(), resolved.getRegularBestOf(),
                AdvancementTypeEnum.OVERALL_TOP_N.getCode(), resolved.getPlayoffSlots(),
                StandardTemplateCodes.STAGE_PLAYOFF));
        definition.getStages().add(StandardTemplateStages.knockout(
                StandardTemplateCodes.STAGE_PLAYOFF, "\u5b63\u540e\u8d5b", 2, resolved.getKnockoutBestOf(),
                resolved.getSemifinalBestOf(), resolved.getFinalBestOf(),
                PairingRuleEnum.HIGH_VS_LOW.getCode(), null, null, null));
        return definition;
    }

    private StandardTemplateDefinition catalogHeader(String registrationMode) {
        StandardTemplateDefinition definition = new StandardTemplateDefinition();
        definition.setTemplateCode(templateCode());
        definition.setTemplateName("League \u6807\u51c6\u6a21\u677f");
        definition.setTemplateType(TemplateTypeEnum.STANDARD.getCode());
        definition.setSeriesCode(SeriesCodeEnum.LEAGUE.getCode());
        definition.setDefaultRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        definition.setRegistrationMode(registrationMode);
        definition.setPreviewSummary("\u5e38\u89c4\u8d5b \u2192 \u5b63\u540e\u8d5b");
        definition.setOptions(List.of(
                StandardTemplateOptionDefinition.select("registrationMode", "\u62a5\u540d\u65b9\u5f0f",
                        List.of("TEAM", "PERSONAL"), "TEAM"),
                StandardTemplateOptionDefinition.select("roundRobinCycles", "\u5faa\u73af\u6b21\u6570",
                        List.of("1", "2"), "1"),
                StandardTemplateOptionDefinition.number("playoffSlots", "\u5b63\u540e\u8d5b\u540d\u989d", 2, 64, "8",
                        List.of("4", "8", "16", "32")),
                StandardTemplateOptionDefinition.select("regularBestOf", "\u5e38\u89c4\u8d5b BO",
                        List.of("1", "3", "5", "7"), "1"),
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
        context.setSeriesCode(SeriesCodeEnum.LEAGUE.getCode());
        return resolve(context);
    }

    private StandardTemplateContext resolve(StandardTemplateContext context) {
        StandardTemplateContext resolved = context == null ? new StandardTemplateContext() : context;
        if (resolved.getRegistrationMode() == null || resolved.getRegistrationMode().isEmpty()) {
            resolved.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        }
        if (resolved.getRoundRobinCycles() == null) {
            resolved.setRoundRobinCycles(1);
        }
        if (resolved.getPlayoffSlots() == null) {
            resolved.setPlayoffSlots(8);
        }
        if (resolved.getRegularBestOf() == null) {
            resolved.setRegularBestOf(1);
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

    private static void validateBestOf(Integer bestOf, String label) {
        com.zhs.basket.support.v4.BestOfValues.requireLegal(bestOf, label);
    }
}
