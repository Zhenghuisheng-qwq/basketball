package com.zhs.service;

import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.v4.BasketCaptainGrant;
import com.zhs.response.team.CaptainQualificationDetailResp;

import java.util.List;

public interface ICaptainPermissionService {

    CaptainQualificationDetailResp getCurrentQualification(Long accountId);

    CaptainQualificationDetailResp getCurrentQualification(Long accountId, Long seasonId);

    CaptainQualificationDetailResp getCurrentQualification(Long accountId, Long seasonId, Long teamId);

    CaptainQualificationDetailResp getCurrentQualificationByDivision(Long accountId, Long divisionId, Long teamId);

    /** 当前用户所有 ACTIVE Division 队长授权（含 canCreateTeam） */
    List<CaptainQualificationDetailResp> listActiveQualifications(Long accountId);

    BasketCaptainGrant requireValidQualification(Long accountId);

    BasketCaptainGrant requireValidQualification(Long accountId, Long seasonId);

    BasketCaptainGrant requireValidQualificationForDivision(Long accountId, Long teamId, Long divisionId);

    /**
     * 兼容旧 seasonId 入参的赛事报名校验。
     */
    BasketCaptainGrant requireValidQualificationForEvent(Long accountId, Long teamId, Long seasonId);

    BasketPlayerProfile requireCertifiedPlayer(Long accountId);

    BasketTeam requireTeamCaptain(Long accountId, Long teamId);

    BasketTeamMember requireTeamMember(Long accountId, Long teamId);

    /** 任意非删除成员关系（含 LEFT），用于历史球队只读 */
    BasketTeamMember requireAnyTeamMembership(Long accountId, Long teamId);

    boolean canManageTeam(Long accountId, Long teamId);

    BasketTeam requireManageableTeam(Long accountId, Long teamId);

    boolean hasCurrentTeamMembership(Long accountId);

    boolean hasActiveTeamMembershipOnPlatform(Long accountId, String platformCode);

    boolean hasValidCaptainQualification(Long accountId);

    String resolveJoinIneligibilityReason(Long accountId);

    String resolveJoinIneligibilityReason(Long accountId, String platformCode);

    void assertEligibleToJoinTeam(Long accountId, String platformCode);
}
