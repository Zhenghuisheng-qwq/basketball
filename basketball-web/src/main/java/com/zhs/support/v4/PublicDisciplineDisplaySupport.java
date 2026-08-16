package com.zhs.support.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.v4.BasketPublicDisciplineDao;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketPublicDisciplineRow;
import com.zhs.response.publicapi.PublicActiveDisciplineSummaryVo;
import com.zhs.service.support.BasketPublicConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class PublicDisciplineDisplaySupport {

    public static final String TARGET_PLAYER = "PLAYER";
    public static final String TARGET_EVENT_TEAM = "EVENT_TEAM";
    public static final String TARGET_PERMANENT_TEAM = "PERMANENT_TEAM";
    public static final String TARGET_ACCOUNT = "ACCOUNT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String CODE_EVENT_DISQUALIFICATION = "EVENT_DISQUALIFICATION";

    /** 未来若某 ACCOUNT penalty_code 需公开到 Player Profile，加入此白名单。当前为空：全部隐藏。 */
    public static final java.util.Set<String> PUBLIC_PLAYER_ACCOUNT_PENALTY_CODES = Collections.emptySet();

    @Resource
    private BasketPublicDisciplineDao publicDisciplineDao;

    public List<PublicActiveDisciplineSummaryVo> listEventTeamActive(BasketEventTeam team) {
        if (team == null || team.getId() == null) {
            return Collections.emptyList();
        }
        Date now = new Date();
        List<PublicActiveDisciplineSummaryVo> rows = new ArrayList<>();
        for (BasketPublicDisciplineRow record : selectActive(TARGET_EVENT_TEAM, team.getId(), null, null, null, team.getSeasonId(), now)) {
            PublicActiveDisciplineSummaryVo vo = toSummary(record, now);
            if (vo != null) {
                fillEventContext(vo, team);
                rows.add(vo);
            }
        }
        if (EventTeamPublicDisplaySupport.isDisqualified(team) && rows.stream()
                .noneMatch(item -> CODE_EVENT_DISQUALIFICATION.equals(item.getPenaltyCode()))) {
            rows.add(0, synthesizeEventTeamDq(team));
        }
        return rows;
    }

    public List<PublicActiveDisciplineSummaryVo> listPlayerActive(Long playerId, Long memberId, Long seasonId) {
        if (playerId == null && memberId == null) {
            return Collections.emptyList();
        }
        Date now = new Date();
        List<PublicActiveDisciplineSummaryVo> rows = new ArrayList<>();
        for (BasketPublicDisciplineRow record : selectActive(null, null, null, playerId, memberId, seasonId, now)) {
            if (!isPublicPlayerDiscipline(record.getTargetType(), record.getPenaltyCode())) {
                continue;
            }
            PublicActiveDisciplineSummaryVo vo = toSummary(record, now);
            if (vo != null) {
                rows.add(vo);
            }
        }
        return rows;
    }

    public List<PublicActiveDisciplineSummaryVo> listPermanentTeamActive(Long teamId) {
        if (teamId == null) {
            return Collections.emptyList();
        }
        Date now = new Date();
        List<PublicActiveDisciplineSummaryVo> rows = new ArrayList<>();
        for (BasketPublicDisciplineRow record : selectActive(TARGET_PERMANENT_TEAM, null, teamId, null, null, null, now)) {
            PublicActiveDisciplineSummaryVo vo = toSummary(record, now);
            if (vo != null) {
                rows.add(vo);
            }
        }
        return rows;
    }

    public static boolean isCurrentlyActive(BasketPublicDisciplineRow row, Date now) {
        if (row == null || !STATUS_ACTIVE.equalsIgnoreCase(row.getRecordStatus())) {
            return false;
        }
        Date current = now == null ? new Date() : now;
        if (row.getEffectiveEndTime() != null && row.getEffectiveEndTime().before(current)) {
            return false;
        }
        return row.getEffectiveStartTime() == null || !row.getEffectiveStartTime().after(current);
    }

    public static PublicActiveDisciplineSummaryVo toSummary(BasketPublicDisciplineRow row, Date now) {
        if (!isCurrentlyActive(row, now) || !StringUtils.hasText(row.getPenaltyCode())) {
            return null;
        }
        PublicActiveDisciplineSummaryVo vo = new PublicActiveDisciplineSummaryVo();
        vo.setTargetType(row.getTargetType());
        vo.setPenaltyCode(row.getPenaltyCode());
        vo.setPenaltyLabel(penaltyLabel(row.getPenaltyCode()));
        vo.setTagLabel(tagLabel(row.getTargetType(), row.getPenaltyCode()));
        vo.setDescription(publicDescription(row.getTargetType(), row.getPenaltyCode()));
        vo.setStatus(STATUS_ACTIVE);
        vo.setStatusLabel("生效中");
        vo.setEffectiveStartTime(row.getEffectiveStartTime());
        vo.setSeasonId(row.getSeasonId());
        vo.setPublicReason(null);
        return vo;
    }

    public static PublicActiveDisciplineSummaryVo synthesizeEventTeamDq(BasketEventTeam team) {
        PublicActiveDisciplineSummaryVo vo = new PublicActiveDisciplineSummaryVo();
        vo.setTargetType(TARGET_EVENT_TEAM);
        vo.setPenaltyCode(CODE_EVENT_DISQUALIFICATION);
        vo.setPenaltyLabel(penaltyLabel(CODE_EVENT_DISQUALIFICATION));
        vo.setTagLabel(tagLabel(TARGET_EVENT_TEAM, CODE_EVENT_DISQUALIFICATION));
        vo.setDescription(publicDescription(TARGET_EVENT_TEAM, CODE_EVENT_DISQUALIFICATION));
        vo.setStatus(STATUS_ACTIVE);
        vo.setStatusLabel("生效中");
        fillEventContext(vo, team);
        vo.setPublicReason(null);
        return vo;
    }

    public static String penaltyLabel(String penaltyCode) {
        if (!StringUtils.hasText(penaltyCode)) {
            return "纪律处罚";
        }
        switch (penaltyCode) {
            case CODE_EVENT_DISQUALIFICATION:
                return "取消赛事资格";
            case "MATCH_SUSPENSION":
            case "DATE_RANGE_SUSPENSION":
            case "MATCH_ELIGIBILITY_CANCEL":
                return "禁赛";
            case "TEAM_SUSPENSION":
                return "球队停赛";
            case "PLAYER_QUALIFICATION_SUSPENSION":
                return "限制参赛";
            case "REGISTRATION_BAN":
                return "禁止报名";
            case "TRANSFER_BAN":
                return "禁止转会";
            case "CAPTAIN_BAN":
                return "禁止担任队长";
            case "POINT_DEDUCTION":
                return "扣分";
            case "WARNING":
                return "警告";
            default:
                return penaltyCode;
        }
    }

    public static String tagLabel(String targetType, String penaltyCode) {
        if (TARGET_EVENT_TEAM.equalsIgnoreCase(targetType) && CODE_EVENT_DISQUALIFICATION.equals(penaltyCode)) {
            return "赛事资格已取消";
        }
        if (!StringUtils.hasText(penaltyCode)) {
            return "处罚中";
        }
        switch (penaltyCode) {
            case "MATCH_SUSPENSION":
            case "DATE_RANGE_SUSPENSION":
            case "MATCH_ELIGIBILITY_CANCEL":
            case "TEAM_SUSPENSION":
                return "停赛中";
            case "PLAYER_QUALIFICATION_SUSPENSION":
            case "REGISTRATION_BAN":
            case "TRANSFER_BAN":
            case "CAPTAIN_BAN":
                return "限制参赛";
            case CODE_EVENT_DISQUALIFICATION:
                return "禁赛";
            default:
                return penaltyLabel(penaltyCode);
        }
    }

    public static String publicDescription(String targetType, String penaltyCode) {
        if (TARGET_EVENT_TEAM.equalsIgnoreCase(targetType) && CODE_EVENT_DISQUALIFICATION.equals(penaltyCode)) {
            return "该球队在本届赛事中的参赛资格已取消。";
        }
        if (TARGET_PLAYER.equalsIgnoreCase(targetType) || TARGET_ACCOUNT.equalsIgnoreCase(targetType)) {
            return "该球员当前受到纪律处罚，仅作用于对应赛事上下文。";
        }
        if (TARGET_PERMANENT_TEAM.equalsIgnoreCase(targetType)) {
            return "该长期球队当前受到纪律处罚。";
        }
        return "当前存在有效纪律处罚。";
    }

    public static boolean isPublicPlayerDiscipline(String targetType, String penaltyCode) {
        if (TARGET_PLAYER.equalsIgnoreCase(targetType)) {
            return true;
        }
        if (TARGET_ACCOUNT.equalsIgnoreCase(targetType)) {
            return StringUtils.hasText(penaltyCode) && PUBLIC_PLAYER_ACCOUNT_PENALTY_CODES.contains(penaltyCode);
        }
        return false;
    }

    public static String playerTeamDqTag() {
        return "所属球队赛事资格已取消";
    }

    public static String playerTeamForfeitLabel(boolean playerTeamLost) {
        return playerTeamLost ? "球队取消资格判负" : "对手取消资格判负";
    }

    private List<BasketPublicDisciplineRow> selectActive(String targetType, Long eventTeamId, Long teamId,
                                                         Long playerId, Long memberId, Long seasonId, Date now) {
        LambdaQueryWrapper<BasketPublicDisciplineRow> wrapper = new LambdaQueryWrapper<BasketPublicDisciplineRow>()
                .eq(BasketPublicDisciplineRow::getRecordStatus, STATUS_ACTIVE)
                .eq(BasketPublicDisciplineRow::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .and(q -> q.isNull(BasketPublicDisciplineRow::getEffectiveEndTime)
                        .or().ge(BasketPublicDisciplineRow::getEffectiveEndTime, now))
                .and(q -> q.isNull(BasketPublicDisciplineRow::getEffectiveStartTime)
                        .or().le(BasketPublicDisciplineRow::getEffectiveStartTime, now))
                .orderByDesc(BasketPublicDisciplineRow::getEffectiveStartTime)
                .orderByDesc(BasketPublicDisciplineRow::getId);
        if (StringUtils.hasText(targetType)) {
            wrapper.eq(BasketPublicDisciplineRow::getTargetType, targetType);
        }
        if (eventTeamId != null) {
            wrapper.eq(BasketPublicDisciplineRow::getEventTeamId, eventTeamId);
        }
        if (teamId != null) {
            wrapper.eq(BasketPublicDisciplineRow::getTeamId, teamId);
        }
        if (seasonId != null) {
            wrapper.eq(BasketPublicDisciplineRow::getSeasonId, seasonId);
        }
        if (playerId != null && memberId != null) {
            wrapper.and(q -> q.eq(BasketPublicDisciplineRow::getPlayerId, playerId)
                    .or().eq(BasketPublicDisciplineRow::getMemberId, memberId));
        } else if (playerId != null) {
            wrapper.eq(BasketPublicDisciplineRow::getPlayerId, playerId);
        } else if (memberId != null) {
            wrapper.eq(BasketPublicDisciplineRow::getMemberId, memberId);
        }
        List<BasketPublicDisciplineRow> rows = publicDisciplineDao.selectList(wrapper);
        return CollectionUtils.isEmpty(rows) ? Collections.emptyList() : rows;
    }

    private static void fillEventContext(PublicActiveDisciplineSummaryVo vo, BasketEventTeam team) {
        if (vo == null || team == null) {
            return;
        }
        vo.setDivisionId(team.getDivisionId());
        vo.setSeasonId(team.getSeasonId() != null ? team.getSeasonId() : vo.getSeasonId());
    }
}
