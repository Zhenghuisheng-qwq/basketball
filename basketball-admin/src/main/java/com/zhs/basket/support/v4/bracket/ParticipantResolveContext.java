package com.zhs.basket.support.v4.bracket;

import java.util.List;
import java.util.Map;

public class ParticipantResolveContext {

    private Long divisionId;
    private Long knockoutStageId;
    private Long sourceStageId;
    private Long rosterVersionId;
    private Map<GroupRankKey, ConfirmedQualification> confirmedByGroupRank;
    private Map<Long, Long> eventTeamGroupId;
    private java.util.Set<Long> formalEventTeamIds;

    public Long getDivisionId() { return divisionId; }
    public void setDivisionId(Long divisionId) { this.divisionId = divisionId; }
    public Long getKnockoutStageId() { return knockoutStageId; }
    public void setKnockoutStageId(Long knockoutStageId) { this.knockoutStageId = knockoutStageId; }
    public Long getSourceStageId() { return sourceStageId; }
    public void setSourceStageId(Long sourceStageId) { this.sourceStageId = sourceStageId; }
    public Long getRosterVersionId() { return rosterVersionId; }
    public void setRosterVersionId(Long rosterVersionId) { this.rosterVersionId = rosterVersionId; }
    public Map<GroupRankKey, ConfirmedQualification> getConfirmedByGroupRank() { return confirmedByGroupRank; }
    public void setConfirmedByGroupRank(Map<GroupRankKey, ConfirmedQualification> confirmedByGroupRank) {
        this.confirmedByGroupRank = confirmedByGroupRank;
    }
    public Map<Long, Long> getEventTeamGroupId() { return eventTeamGroupId; }
    public void setEventTeamGroupId(Map<Long, Long> eventTeamGroupId) { this.eventTeamGroupId = eventTeamGroupId; }
    public java.util.Set<Long> getFormalEventTeamIds() { return formalEventTeamIds; }
    public void setFormalEventTeamIds(java.util.Set<Long> formalEventTeamIds) { this.formalEventTeamIds = formalEventTeamIds; }

    public record GroupRankKey(Long sourceStageId, Long groupId, Integer sourceRank) {
    }

    public record ConfirmedQualification(Long qualificationId, Long eventTeamId, Long groupId, Integer sourceRank) {
    }
}
