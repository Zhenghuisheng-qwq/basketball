package com.zhs.basket.support.v4.bracket;

import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupRankParticipantResolverR6Test {

    private final GroupRankParticipantResolver resolver = new GroupRankParticipantResolver();

    @Test
    void resolvesConfirmedGroupRankToEventTeam() {
        ParticipantResolveContext context = contextWith(qual(100L, 1L, 1, 1001L));
        context.getEventTeamGroupId().put(1001L, 1L);
        context.setFormalEventTeamIds(Set.of(1001L));
        Long teamId = resolver.resolve(slot("A", 1L, 1), context);
        assertEquals(1001L, teamId);
    }

    @Test
    void missingD2RejectsWholeSource() {
        ParticipantResolveContext context = contextWith(qual(100L, 1L, 1, 1001L));
        context.getEventTeamGroupId().put(1001L, 1L);
        context.setFormalEventTeamIds(Set.of(1001L));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> resolver.resolve(slot("D", 4L, 2), context));
        assertTrue(ex.getMessage().contains("D2") || ex.getMessage().contains("缺少"));
    }

    @Test
    void previewQualificationDoesNotResolve() {
        ParticipantResolveContext context = new ParticipantResolveContext();
        context.setConfirmedByGroupRank(new HashMap<>());
        context.setEventTeamGroupId(new HashMap<>());
        context.setFormalEventTeamIds(Set.of(1001L));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> resolver.resolve(slot("A", 1L, 1), context));
        assertTrue(ex.getMessage().contains("缺少") || ex.getMessage().contains("无法解析"));
    }

    @Test
    void wrongGroupRejected() {
        ParticipantResolveContext context = contextWith(qual(100L, 1L, 1, 1001L));
        context.getEventTeamGroupId().put(1001L, 2L);
        context.setFormalEventTeamIds(Set.of(1001L));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> resolver.resolve(slot("A", 1L, 1), context));
        assertTrue(ex.getMessage().contains("小组"));
    }

    @Test
    void teamNotInRosterRejected() {
        ParticipantResolveContext context = contextWith(qual(100L, 1L, 1, 1001L));
        context.getEventTeamGroupId().put(1001L, 1L);
        context.setFormalEventTeamIds(Set.of(9999L));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> resolver.resolve(slot("A", 1L, 1), context));
        assertTrue(ex.getMessage().contains("Roster") || ex.getMessage().contains("不属于"));
    }

    @Test
    void doesNotChangeSlotNo() {
        BracketSlot slot = slot("A", 1L, 1);
        int original = slot.getSlotNo();
        ParticipantResolveContext context = contextWith(qual(100L, 1L, 1, 1001L));
        context.getEventTeamGroupId().put(1001L, 1L);
        context.setFormalEventTeamIds(Set.of(1001L));
        resolver.resolve(slot, context);
        assertEquals(original, slot.getSlotNo());
        assertEquals(1, slot.getSourceRank());
        assertEquals(1L, slot.getSourceGroupId());
        assertEquals(null, slot.getResolvedEventTeamId());
    }

    private static BracketSlot slot(String code, Long groupId, int rank) {
        return BracketSlot.groupRank(1, 100L, groupId, code, rank);
    }

    private static ParticipantResolveContext.ConfirmedQualification qual(Long stageId, Long groupId, int rank, Long teamId) {
        return new ParticipantResolveContext.ConfirmedQualification(9000L + rank, teamId, groupId, rank);
    }

    private static ParticipantResolveContext contextWith(ParticipantResolveContext.ConfirmedQualification qualification) {
        ParticipantResolveContext context = new ParticipantResolveContext();
        Map<ParticipantResolveContext.GroupRankKey, ParticipantResolveContext.ConfirmedQualification> map = new HashMap<>();
        map.put(new ParticipantResolveContext.GroupRankKey(100L, qualification.groupId(), qualification.sourceRank()),
                qualification);
        context.setConfirmedByGroupRank(map);
        context.setEventTeamGroupId(new HashMap<>());
        return context;
    }
}
