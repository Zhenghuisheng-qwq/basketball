package com.zhs.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketTeamJoinApplicationDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketTeamMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamMembershipDivisionEligibilityTest {

    @Mock
    private BasketTeamMemberDao teamMemberDao;
    @Mock
    private BasketTeamJoinApplicationDao joinApplicationDao;

    @InjectMocks
    private TeamMembershipSupport support;

    @Test
    void assertEligible_sameDivisionBlocked() {
        BasketTeamMember existing = new BasketTeamMember();
        existing.setId(1L);
        existing.setDivisionId(118L);
        existing.setMembershipStatus(TeamMembershipStatusEnum.ACTIVE.getCode());
        when(teamMemberDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        assertThrows(ServiceException.class, () -> support.assertEligibleToJoinTeam(208L, 118L));
    }

    @Test
    void assertEligible_otherDivisionAllowed_whenNoMembershipInTarget() {
        when(teamMemberDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertDoesNotThrow(() -> support.assertEligibleToJoinTeam(208L, 120L));
    }

    @Test
    void legacyNullDivisionDoesNotAppearInDivisionQuery() {
        // findActiveMembership(memberId, divisionId) 仅查 division_id = ?，Legacy NULL 不会命中
        when(teamMemberDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertNull(support.resolveJoinIneligibilityReason(208L, 118L));
    }

    @Test
    void applyActiveMemberSlot_writesDivisionScopedSlot() {
        BasketTeamMember member = new BasketTeamMember();
        support.applyActiveMemberSlot(member, 118L, 2081010979604316162L);
        assertEquals(118L, member.getDivisionId());
        assertEquals("118_2081010979604316162", member.getActiveMemberSlot());
        assertNull(member.getActivePlatformSlot());
    }
}
