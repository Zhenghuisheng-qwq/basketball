package com.zhs.basket.support.v4;

import com.zhs.basket.enums.v4.*;
import com.zhs.basket.vo.v4.*;

import java.util.stream.Collectors;

public final class V4FormatDisplaySupport {

    private V4FormatDisplaySupport() {
    }

    public static String platformLabel(String code) {
        if (code == null || code.isEmpty()) {
            return "--";
        }
        try {
            return PlatformCodeEnum.fromCode(code).getDescription();
        } catch (IllegalArgumentException ex) {
            return code;
        }
    }

    public static String gameModeLabel(String code) {
        if (code == null || code.isEmpty()) {
            return "--";
        }
        try {
            return GameModeEnum.fromCode(code).getDescription();
        } catch (IllegalArgumentException ex) {
            return code;
        }
    }

    public static String registrationModeLabel(String code) {
        if (RegistrationModeEnum.PERSONAL.getCode().equals(code)) {
            return "个人报名";
        }
        if (RegistrationModeEnum.TEAM.getCode().equals(code)) {
            return "球队报名";
        }
        return code != null ? code : "--";
    }

    public static String snapshotStatusLabel(String code) {
        try {
            return SnapshotStatusEnum.fromCode(code).getDescription();
        } catch (Exception ex) {
            return code;
        }
    }

    public static String stageTypeLabel(String code) {
        try {
            return StageTypeEnum.fromCode(code).getDescription();
        } catch (Exception ex) {
            return code;
        }
    }

    public static String competitionFormatLabel(String code) {
        try {
            return CompetitionFormatEnum.fromCode(code).getDescription();
        } catch (Exception ex) {
            return code;
        }
    }

    public static String groupingModeLabel(String code) {
        try {
            return GroupingModeEnum.fromCode(code).getDescription();
        } catch (Exception ex) {
            return code;
        }
    }

    public static String advancementTypeLabel(String code) {
        try {
            return AdvancementTypeEnum.fromCode(code).getDescription();
        } catch (Exception ex) {
            return code;
        }
    }

    public static String pairingRuleLabel(String code) {
        try {
            return PairingRuleEnum.fromCode(code).getDescription();
        } catch (Exception ex) {
            return code;
        }
    }

    public static String tieBreakRuleLabel(String code) {
        try {
            return TieBreakRuleEnum.fromCode(code).getDescription();
        } catch (Exception ex) {
            return code;
        }
    }

    public static String buildFormatSummary(BasketDivisionFormatSnapshotVo snapshot) {
        if (snapshot == null || snapshot.getStages() == null || snapshot.getStages().isEmpty()) {
            return "暂无规则摘要";
        }
        return snapshot.getStages().stream()
                .map(s -> s.getStageOrder() + "." + s.getStageName()
                        + "（" + competitionFormatLabel(s.getCompetitionFormat())
                        + (s.getBestOf() != null ? " BO" + s.getBestOf() : "") + "）")
                .collect(Collectors.joining(" → "));
    }

    public static void enrichTemplateStageVo(BasketFormatTemplateStageVo vo) {
        vo.setStageTypeName(stageTypeLabel(vo.getStageType()));
        vo.setCompetitionFormatName(competitionFormatLabel(vo.getCompetitionFormat()));
        vo.setGroupingModeName(groupingModeLabel(vo.getGroupingMode()));
        vo.setAdvancementTypeName(advancementTypeLabel(vo.getAdvancementType()));
        vo.setPairingRuleName(pairingRuleLabel(vo.getPairingRule()));
    }

    public static void enrichDivisionStageVo(BasketDivisionStageVo vo) {
        vo.setStageTypeName(stageTypeLabel(vo.getStageType()));
        vo.setCompetitionFormatName(competitionFormatLabel(vo.getCompetitionFormat()));
        vo.setGroupingModeName(groupingModeLabel(vo.getGroupingMode()));
        vo.setAdvancementTypeName(advancementTypeLabel(vo.getAdvancementType()));
        vo.setPairingRuleName(pairingRuleLabel(vo.getPairingRule()));
    }
}
