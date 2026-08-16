package com.zhs.basket.support;

import com.zhs.basket.enums.DisciplinePenaltyCodeEnum;
import com.zhs.common.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class DisciplineAnnouncementDraftSupport {

    private DisciplineAnnouncementDraftSupport() {
    }

    public static String resolveTargetName(String targetType, String playerNo, String playerName,
                                           String eventTeamName, String teamName, Long eventTeamId,
                                           Long teamId, Long playerId) {
        if ("PLAYER".equalsIgnoreCase(targetType) || "ACCOUNT".equalsIgnoreCase(targetType)) {
            if (StringUtils.isNotEmpty(playerNo) && StringUtils.isNotEmpty(playerName)) {
                return playerNo + " · " + playerName;
            }
            if (StringUtils.isNotEmpty(playerName)) {
                return playerName;
            }
            if (StringUtils.isNotEmpty(playerNo)) {
                return playerNo;
            }
            return playerId != null ? "球员 #" + playerId : "球员";
        }
        if ("EVENT_TEAM".equalsIgnoreCase(targetType)) {
            if (StringUtils.isNotEmpty(eventTeamName)) {
                return eventTeamName;
            }
            if (StringUtils.isNotEmpty(teamName)) {
                return teamName;
            }
            return eventTeamId != null ? "赛事球队 #" + eventTeamId : "赛事球队";
        }
        if (StringUtils.isNotEmpty(teamName)) {
            return teamName;
        }
        return teamId != null ? "球队 #" + teamId : "球队";
    }

    public static String penaltyLabel(String penaltyCode) {
        if (!StringUtils.isNotEmpty(penaltyCode)) {
            return "纪律处罚";
        }
        if (DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION.name().equals(penaltyCode)) {
            return "取消赛事资格";
        }
        if (DisciplinePenaltyCodeEnum.MATCH_SUSPENSION.name().equals(penaltyCode)
                || DisciplinePenaltyCodeEnum.DATE_RANGE_SUSPENSION.name().equals(penaltyCode)
                || DisciplinePenaltyCodeEnum.MATCH_ELIGIBILITY_CANCEL.name().equals(penaltyCode)) {
            return "禁赛";
        }
        if (DisciplinePenaltyCodeEnum.TEAM_SUSPENSION.name().equals(penaltyCode)) {
            return "球队停赛";
        }
        if (DisciplinePenaltyCodeEnum.PLAYER_QUALIFICATION_SUSPENSION.name().equals(penaltyCode)) {
            return "限制参赛";
        }
        if (DisciplinePenaltyCodeEnum.REGISTRATION_BAN.name().equals(penaltyCode)) {
            return "禁止报名";
        }
        if (DisciplinePenaltyCodeEnum.POINT_DEDUCTION.name().equals(penaltyCode)) {
            return "扣分";
        }
        if (DisciplinePenaltyCodeEnum.WARNING.name().equals(penaltyCode)) {
            return "警告";
        }
        return penaltyCode;
    }

    public static String titleSuffix(String penaltyCode) {
        if (DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION.name().equals(penaltyCode)) {
            return "赛事资格处罚";
        }
        return penaltyLabel(penaltyCode);
    }

    public static String buildTitle(String targetName, String penaltyCode) {
        String name = StringUtils.isNotEmpty(targetName) ? targetName.trim() : "相关对象";
        return "《关于 " + name + " " + titleSuffix(penaltyCode) + "的公告》";
    }

    public static String buildEventLabel(String seriesName, String seasonName) {
        if (StringUtils.isNotEmpty(seriesName) && StringUtils.isNotEmpty(seasonName)) {
            return seriesName + " " + seasonName;
        }
        if (StringUtils.isNotEmpty(seriesName)) {
            return seriesName;
        }
        if (StringUtils.isNotEmpty(seasonName)) {
            return seasonName;
        }
        return "相关赛事";
    }

    public static String formatDate(Date time) {
        if (time == null) {
            return "即日起";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(time);
    }

    public static String buildContent(String targetName, String eventLabel, String penaltyCode,
                                      Date effectiveStartTime, String publicStatement) {
        StringBuilder body = new StringBuilder();
        body.append("处罚对象：").append(StringUtils.isNotEmpty(targetName) ? targetName : "相关对象").append('\n');
        body.append("所属赛事：").append(buildEventLabel(null, eventLabel)).append('\n');
        body.append("处罚类型：").append(penaltyLabel(penaltyCode)).append('\n');
        body.append("生效时间：自 ").append(formatDate(effectiveStartTime)).append(" 生效").append('\n');
        body.append("处罚结果：相关比赛结果将依据赛事规则进行处理。");
        if (StringUtils.isNotEmpty(publicStatement)) {
            body.append("\n\n公开说明：\n").append(publicStatement.trim());
        }
        return body.toString();
    }

    public static String buildContent(String targetName, String seriesName, String seasonName,
                                      String penaltyCode, Date effectiveStartTime, String publicStatement) {
        return buildContent(targetName, buildEventLabel(seriesName, seasonName), penaltyCode,
                effectiveStartTime, publicStatement);
    }

    /**
     * 只认仍存在且未删除的公告状态。announcement_id 残留或关联公告已删时一律「未生成」，不得据此显示「草稿」。
     */
    public static String resolveAnnouncementStatusName(Integer liveAnnouncementStatus) {
        if (liveAnnouncementStatus == null) {
            return "未生成";
        }
        try {
            return com.zhs.basket.enums.AnnouncementStatusEnum.fromCode(liveAnnouncementStatus).getDescription();
        } catch (Exception ex) {
            return "未生成";
        }
    }
}
