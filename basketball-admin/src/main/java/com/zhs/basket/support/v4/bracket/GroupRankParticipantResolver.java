package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.enums.v4.ParticipantSourceTypeEnum;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

@Component
public class GroupRankParticipantResolver implements ParticipantSourceResolver {

    @Override
    public boolean supports(String sourceType) {
        return ParticipantSourceTypeEnum.GROUP_RANK.getCode().equals(sourceType);
    }

    @Override
    public Long resolve(BracketSlot slot, ParticipantResolveContext context) {
        if (slot == null || slot.getSourceStageId() == null || slot.getSourceGroupId() == null
                || slot.getSourceRank() == null) {
            throw new ServiceException("GROUP_RANK 来源缺少 source_stage / source_group / source_rank");
        }
        ParticipantResolveContext.GroupRankKey key = new ParticipantResolveContext.GroupRankKey(
                slot.getSourceStageId(), slot.getSourceGroupId(), slot.getSourceRank());
        ParticipantResolveContext.ConfirmedQualification qualification =
                context.getConfirmedByGroupRank() == null ? null : context.getConfirmedByGroupRank().get(key);
        if (qualification == null || qualification.eventTeamId() == null) {
            throw new ServiceException("已确认晋级缺少 "
                    + ParticipantSourceLabels.displayCode(slot.getSourceGroupCode(), slot.getSourceRank())
                    + "，无法解析占位");
        }
        if (context.getFormalEventTeamIds() != null
                && !context.getFormalEventTeamIds().contains(qualification.eventTeamId())) {
            throw new ServiceException("解析球队不属于当前 Locked Roster");
        }
        Long actualGroupId = context.getEventTeamGroupId() == null
                ? null : context.getEventTeamGroupId().get(qualification.eventTeamId());
        if (actualGroupId == null || !actualGroupId.equals(slot.getSourceGroupId())) {
            throw new ServiceException("晋级记录声称的小组与球队实际小组不一致");
        }
        return qualification.eventTeamId();
    }
}
