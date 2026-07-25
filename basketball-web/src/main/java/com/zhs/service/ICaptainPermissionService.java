package com.zhs.service;

import com.zhs.model.BasketCaptainQualification;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.BasketTournamentRegistration;
import com.zhs.response.team.CaptainQualificationDetailResp;

public interface ICaptainPermissionService {

    CaptainQualificationDetailResp getCurrentQualification(Long accountId);

    BasketCaptainQualification requireValidQualification(Long accountId);

    BasketPlayerProfile requireCertifiedPlayer(Long accountId);

    BasketTeam requireTeamCaptain(Long accountId, Long teamId);

    BasketTeamMember requireTeamMember(Long accountId, Long teamId);

    BasketTeam requireManageableTeam(Long accountId, Long teamId);

    BasketTournamentRegistration requireRegistrationCaptain(Long accountId, Long registrationId);

    boolean hasCurrentTeamMembership(Long accountId);

    boolean hasValidCaptainQualification(Long accountId);

    String resolveJoinIneligibilityReason(Long accountId);

    void assertEligibleToJoinTeam(Long accountId);
}
