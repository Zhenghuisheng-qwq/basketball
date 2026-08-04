package com.zhs.service;

import com.zhs.model.BasketCaptainQualification;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.response.team.CaptainQualificationDetailResp;

public interface ICaptainPermissionService {

    CaptainQualificationDetailResp getCurrentQualification(Long accountId);

    CaptainQualificationDetailResp getCurrentQualification(Long accountId, Long seasonId);

    CaptainQualificationDetailResp getCurrentQualification(Long accountId, Long seasonId, Long teamId);

    BasketCaptainQualification requireValidQualification(Long accountId);

    BasketCaptainQualification requireValidQualification(Long accountId, Long seasonId);

    /**
     * 按 userId + teamId + eventId(seasonId) 校验有效队长授权，用于赛事报名。
     */
    BasketCaptainQualification requireValidQualificationForEvent(Long accountId, Long teamId, Long seasonId);

    BasketPlayerProfile requireCertifiedPlayer(Long accountId);

    BasketTeam requireTeamCaptain(Long accountId, Long teamId);

    BasketTeamMember requireTeamMember(Long accountId, Long teamId);

    BasketTeam requireManageableTeam(Long accountId, Long teamId);

    boolean hasCurrentTeamMembership(Long accountId);

    boolean hasActiveTeamMembershipOnPlatform(Long accountId, String platformCode);

    boolean hasValidCaptainQualification(Long accountId);

    String resolveJoinIneligibilityReason(Long accountId);

    String resolveJoinIneligibilityReason(Long accountId, String platformCode);

    void assertEligibleToJoinTeam(Long accountId, String platformCode);
}
