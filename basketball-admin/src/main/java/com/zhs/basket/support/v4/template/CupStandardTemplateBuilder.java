package com.zhs.basket.support.v4.template;

import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.SeriesCodeEnum;
import com.zhs.basket.enums.v4.TemplateTypeEnum;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class CupStandardTemplateBuilder implements StandardFormatTemplateBuilder {

    private static final Set<String> GROUPINGS = Set.of(
            GroupingModeEnum.POT_RANDOM.getCode(), GroupingModeEnum.MANUAL.getCode());
    private static final Set<String> PLACEMENTS = Set.of(
            PairingRuleEnum.GROUP_CROSS.getCode(), PairingRuleEnum.MANUAL.getCode());

    @Override
    public SeriesCodeEnum support() {
        return SeriesCodeEnum.CUP;
    }

    @Override
    public String templateCode() {
        return StandardTemplateCodes.CUP_STANDARD_V1;
    }

    @Override
    public StandardTemplateDefinition capabilities() {
        return build(defaultContext());
    }

    @Override
    public void validate(StandardTemplateContext context) {
        StandardTemplateContext resolved = resolve(context);
        RegistrationModeEnum.fromCode(resolved.getRegistrationMode());
        if (resolved.getGroupCount() == null || resolved.getGroupCount() < 2) {
            throw new ServiceException("Cup groupCount must be >= 2");
        }
        if (resolved.getAdvancePerGroup() == null || resolved.getAdvancePerGroup() < 1) {
            throw new ServiceException("Cup advancePerGroup must be >= 1");
        }
        int cycles = resolved.getGroupCycles();
        if (cycles != 1 && cycles != 2) {
            throw new ServiceException("Cup groupCycles only allow 1 or 2");
        }
        if (!GROUPINGS.contains(resolved.getGroupingMode())) {
            throw new ServiceException("Cup groupingMode is not allowed");
        }
        if (!PLACEMENTS.contains(resolved.getEliminationPlacement())) {
            throw new ServiceException("Cup eliminationPlacement is not allowed");
        }
        if (resolved.getRegularBestOf() == null || !com.zhs.basket.support.v4.BestOfValues.isLegal(resolved.getRegularBestOf())) {
            throw new ServiceException("Cup group stage BO only allows 1/3/5/7");
        }
        if (resolved.getKnockoutBestOf() == null
                || !com.zhs.basket.support.v4.BestOfValues.isLegal(resolved.getKnockoutBestOf())) {
            throw new ServiceException("Cup knockout BO only allows 1/3/5/7");
        }
    }

    @Override
    public StandardTemplateDefinition build(StandardTemplateContext context) {
        validate(context);
        StandardTemplateContext resolved = resolve(context);
        StandardTemplateDefinition definition = catalogHeader(resolved.getRegistrationMode());
        definition.getStages().add(StandardTemplateStages.groupedRoundRobin(
                StandardTemplateCodes.STAGE_GROUP, "\u5c0f\u7ec4\u8d5b", 1,
                resolved.getGroupCount(), resolved.getGroupCycles(), resolved.getRegularBestOf(),
                resolved.getAdvancePerGroup(), resolved.getGroupingMode(),
                StandardTemplateCodes.STAGE_KNOCKOUT));
        definition.getStages().add(StandardTemplateStages.knockout(
                StandardTemplateCodes.STAGE_KNOCKOUT, "\u6dd8\u6c70\u8d5b", 2, resolved.getKnockoutBestOf(),
                resolved.getSemifinalBestOf(), resolved.getFinalBestOf(),
                resolved.getEliminationPlacement(), null, null, null));
        return definition;
    }

    private StandardTemplateDefinition catalogHeader(String registrationMode) {
        StandardTemplateDefinition definition = new StandardTemplateDefinition();
        definition.setTemplateCode(templateCode());
        definition.setTemplateName("Cup \u6807\u51c6\u6a21\u677f");
        definition.setTemplateType(TemplateTypeEnum.STANDARD.getCode());
        definition.setSeriesCode(SeriesCodeEnum.CUP.getCode());
        definition.setDefaultRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        definition.setRegistrationMode(registrationMode);
        definition.setPreviewSummary("\u5c0f\u7ec4\u8d5b \u2192 \u6dd8\u6c70\u8d5b");
        definition.setOptions(List.of(
                StandardTemplateOptionDefinition.select("registrationMode", "\u62a5\u540d\u65b9\u5f0f",
                        List.of("TEAM", "PERSONAL"), "TEAM"),
                StandardTemplateOptionDefinition.number("groupCount", "\u5206\u7ec4\u6570\u91cf", 2, 32, "4",
                        List.of("2", "4", "8")),
                StandardTemplateOptionDefinition.select("groupCycles", "\u5c0f\u7ec4\u5faa\u73af\u6b21\u6570",
                        List.of("1", "2"), "1"),
                StandardTemplateOptionDefinition.number("advancePerGroup", "\u6bcf\u7ec4\u664b\u7ea7", 1, 16, "2",
                        List.of("1", "2")),
                StandardTemplateOptionDefinition.select("groupingMode", "\u5206\u7ec4\u65b9\u5f0f",
                        List.of("POT_RANDOM", "MANUAL"), "POT_RANDOM"),
                StandardTemplateOptionDefinition.select("eliminationPlacement", "\u6dd8\u6c70\u843d\u4f4d",
                        List.of("GROUP_CROSS", "MANUAL"), "GROUP_CROSS"),
                StandardTemplateOptionDefinition.select("regularBestOf", "\u5c0f\u7ec4\u8d5b BO",
                        List.of("1", "3", "5", "7"), "1"),
                StandardTemplateOptionDefinition.select("knockoutBestOf", "\u6dd8\u6c70\u8d5b Early BO",
                        List.of("1", "3", "5", "7"), "3")
        ));
        return definition;
    }

    private StandardTemplateContext defaultContext() {
        return resolve(new StandardTemplateContext());
    }

    private StandardTemplateContext resolve(StandardTemplateContext context) {
        StandardTemplateContext resolved = context == null ? new StandardTemplateContext() : context;
        if (resolved.getRegistrationMode() == null || resolved.getRegistrationMode().isEmpty()) {
            resolved.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        }
        if (resolved.getGroupCount() == null) {
            resolved.setGroupCount(4);
        }
        if (resolved.getGroupCycles() == null) {
            resolved.setGroupCycles(1);
        }
        if (resolved.getAdvancePerGroup() == null) {
            resolved.setAdvancePerGroup(2);
        }
        if (resolved.getGroupingMode() == null || resolved.getGroupingMode().isEmpty()) {
            resolved.setGroupingMode(GroupingModeEnum.POT_RANDOM.getCode());
        }
        if (resolved.getEliminationPlacement() == null || resolved.getEliminationPlacement().isEmpty()) {
            resolved.setEliminationPlacement(PairingRuleEnum.GROUP_CROSS.getCode());
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
}
