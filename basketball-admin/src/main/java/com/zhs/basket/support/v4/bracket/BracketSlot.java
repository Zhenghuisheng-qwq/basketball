package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.enums.v4.ParticipantResolutionStatusEnum;
import com.zhs.basket.enums.v4.ParticipantSourceTypeEnum;
import lombok.Data;

/**
 * One first-round bracket slot. BYE has no eventTeamId and is not a Match.
 * GROUP_RANK placeholders also have null eventTeamId until resolution.
 */
@Data
public class BracketSlot {

    private int slotNo;
    private Long eventTeamId;
    private Integer seedNo;
    private Integer rankingNo;
    private Integer sourceRank;
    private String sourceType;
    private Long sourceStageId;
    private Long sourceGroupId;
    private String sourceGroupCode;
    private Long sourceSeriesId;
    private Long resolvedEventTeamId;
    private String resolutionStatus;
    private String displayCode;
    private String displayName;
    private boolean bye;

    public static BracketSlot team(int slotNo, Long eventTeamId, Integer seedNo, Integer rankingNo, Integer sourceRank) {
        BracketSlot slot = new BracketSlot();
        slot.setSlotNo(slotNo);
        slot.setEventTeamId(eventTeamId);
        slot.setResolvedEventTeamId(eventTeamId);
        slot.setSeedNo(seedNo);
        slot.setRankingNo(rankingNo);
        slot.setSourceRank(sourceRank);
        slot.setSourceType(ParticipantSourceTypeEnum.DIRECT_EVENT_TEAM.getCode());
        slot.setResolutionStatus(ParticipantResolutionStatusEnum.RESOLVED.getCode());
        slot.setBye(false);
        return slot;
    }

    public static BracketSlot groupRank(int slotNo, Long sourceStageId, Long sourceGroupId,
                                        String sourceGroupCode, int sourceRank) {
        BracketSlot slot = new BracketSlot();
        slot.setSlotNo(slotNo);
        slot.setSourceType(ParticipantSourceTypeEnum.GROUP_RANK.getCode());
        slot.setSourceStageId(sourceStageId);
        slot.setSourceGroupId(sourceGroupId);
        slot.setSourceGroupCode(sourceGroupCode);
        slot.setSourceRank(sourceRank);
        slot.setResolutionStatus(ParticipantResolutionStatusEnum.PENDING.getCode());
        slot.setDisplayCode(ParticipantSourceLabels.displayCode(sourceGroupCode, sourceRank));
        slot.setDisplayName(ParticipantSourceLabels.displayName(sourceGroupCode, sourceRank));
        slot.setBye(false);
        return slot;
    }

    public static BracketSlot bye(int slotNo) {
        BracketSlot slot = new BracketSlot();
        slot.setSlotNo(slotNo);
        slot.setBye(true);
        slot.setSourceType(ParticipantSourceTypeEnum.BYE.getCode());
        slot.setResolutionStatus(ParticipantResolutionStatusEnum.BYE.getCode());
        slot.setDisplayCode("BYE");
        slot.setDisplayName("BYE");
        return slot;
    }

    public boolean isUnresolvedPlaceholder() {
        return ParticipantSourceTypeEnum.GROUP_RANK.getCode().equals(sourceType)
                && !bye
                && resolvedEventTeamId == null
                && !ParticipantResolutionStatusEnum.RESOLVED.getCode().equals(resolutionStatus);
    }

    public Long effectiveEventTeamId() {
        if (bye) {
            return null;
        }
        if (resolvedEventTeamId != null) {
            return resolvedEventTeamId;
        }
        return eventTeamId;
    }
}
