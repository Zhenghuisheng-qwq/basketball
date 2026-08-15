package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.enums.v4.DrawRuleEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.SeedRuleEnum;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.service.v4.IBasketV4RuleVersionService;
import com.zhs.basket.service.v4.IStagePairingExecutionApplicationService;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.basket.support.v4.version.RosterWorkflowSupport;
import com.zhs.basket.support.v4.version.RuleVersionView;
import com.zhs.basket.vo.v4.StagePairingEntryVo;
import com.zhs.basket.vo.v4.StagePairingExecutionVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * VERSIONED Structure Generate readiness: Rule + Roster + Pairing.
 * Does not re-interpret Registration.
 */
@Component
public class StructureGenerationPrerequisiteValidator {

    public static final String MSG_RULE_NOT_LOCKED = "规则未锁定";
    public static final String MSG_ROSTER_NOT_LOCKED = "名单未锁定";
    public static final String MSG_FORMAL_INSUFFICIENT = "正式参赛单位不足";
    public static final String MSG_SEED_NOT_SET = "Seed 未设置";
    public static final String MSG_DRAW_NOT_CONFIRMED = "Draw 未确认";
    public static final String MSG_SLOT_NOT_CONFIRMED = "Slot 未确认";

    @Autowired
    private IBasketV4RuleVersionService ruleVersionService;
    @Autowired
    private IBasketV4RosterVersionService rosterVersionService;
    @Autowired
    private TeamRosterSupport teamRosterSupport;
    @Autowired
    private IStagePairingExecutionApplicationService pairingExecutionApplicationService;

    public boolean isVersioned(Long divisionId) {
        return RosterWorkflowSupport.isVersioned(ruleVersionService.getCurrent(divisionId));
    }

    public void assertCanGenerate(BasketDivision division, BasketDivisionStage stage) {
        String reason = readinessBlockReason(division, stage);
        if (reason != null) {
            throw new ServiceException(reason);
        }
    }

    public String readinessBlockReason(BasketDivision division, BasketDivisionStage stage) {
        if (division == null || stage == null) {
            return MSG_RULE_NOT_LOCKED;
        }
        RuleVersionView rule = ruleVersionService.getCurrent(division.getId());
        if (rule == null || !rule.isLocked()) {
            return MSG_RULE_NOT_LOCKED;
        }
        BasketDivisionRosterVersion roster = rosterVersionService.getCurrentLockedVersion(division.getId());
        if (roster == null || !"LOCKED".equals(roster.getStatus())) {
            return MSG_ROSTER_NOT_LOCKED;
        }
        List<BasketEventTeam> formal = teamRosterSupport.selectFormalEventTeams(division.getId());
        if (formal == null || formal.size() < 2) {
            return MSG_FORMAL_INSUFFICIENT;
        }
        StagePairingExecutionVo pairing = pairingExecutionApplicationService.getCurrent(stage.getId());
        return pairingBlockReason(stage.getSeedRule(), stage.getDrawRule(), stage.getPairingRule(), pairing);
    }

    public String pairingBlockReason(String seedRule, String drawRule, String pairingRule,
                                     StagePairingExecutionVo pairing) {
        if (seedRequired(seedRule) && !hasSeedOrRanking(seedRule, pairing)) {
            return MSG_SEED_NOT_SET;
        }
        if (drawRequired(drawRule) && !isConfirmed(pairing)) {
            return MSG_DRAW_NOT_CONFIRMED;
        }
        if (slotRequired(drawRule, pairingRule) && !isConfirmed(pairing)) {
            return MSG_SLOT_NOT_CONFIRMED;
        }
        return null;
    }

    public boolean pairingRequired(String seedRule, String drawRule, String pairingRule) {
        return seedRequired(seedRule) || drawRequired(drawRule) || slotRequired(drawRule, pairingRule);
    }

    private static boolean seedRequired(String seedRule) {
        return SeedRuleEnum.ADMIN_SEED.getCode().equals(seedRule)
                || SeedRuleEnum.EXTERNAL_RANKING.getCode().equals(seedRule);
    }

    private static boolean drawRequired(String drawRule) {
        return DrawRuleEnum.RANDOM_CONFIRM.getCode().equals(drawRule);
    }

    private static boolean slotRequired(String drawRule, String pairingRule) {
        return DrawRuleEnum.MANUAL_SLOT.getCode().equals(drawRule)
                || PairingRuleEnum.MANUAL.getCode().equals(pairingRule)
                || PairingRuleEnum.GROUP_CROSS.getCode().equals(pairingRule);
    }

    private static boolean isConfirmed(StagePairingExecutionVo pairing) {
        return pairing != null && "CONFIRMED".equals(pairing.getStatus());
    }

    private static boolean hasSeedOrRanking(String seedRule, StagePairingExecutionVo pairing) {
        if (pairing == null || pairing.getEntries() == null) {
            return false;
        }
        boolean ranking = SeedRuleEnum.EXTERNAL_RANKING.getCode().equals(seedRule);
        for (StagePairingEntryVo entry : pairing.getEntries()) {
            if (entry == null || Boolean.TRUE.equals(entry.getBye()) || entry.getEventTeamId() == null) {
                continue;
            }
            if (ranking) {
                if (entry.getRankingNo() != null && entry.getRankingNo() > 0) {
                    return true;
                }
            } else if (entry.getSeedNo() != null && entry.getSeedNo() > 0) {
                return true;
            }
        }
        return false;
    }
}
