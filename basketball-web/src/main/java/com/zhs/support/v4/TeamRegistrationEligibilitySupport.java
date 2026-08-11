package com.zhs.support.v4;

import com.zhs.enums.TeamAuditStatusEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.enums.v4.DivisionStatusEnum;
import com.zhs.enums.v4.RegistrationEligibilityReasonEnum;
import com.zhs.enums.v4.RegistrationStatusEnum;
import com.zhs.enums.v4.RegistrationWindowStatusEnum;
import com.zhs.model.BasketTeam;
import com.zhs.model.v4.BasketCaptainGrant;
import com.zhs.model.v4.BasketDivision;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

/**
 * 球队报名资格统一计算：My Events 列表、报名页 Context、save/submit 共用。
 */
@Component
public class TeamRegistrationEligibilitySupport {

    private static final Integer NOT_DELETED = 0;

    public RegistrationWindowStatusEnum resolveWindowStatus(Date start, Date end, Date now) {
        Date clock = now == null ? new Date() : now;
        if (start != null && start.after(clock)) {
            return RegistrationWindowStatusEnum.NOT_STARTED;
        }
        if (end != null && end.before(clock)) {
            return RegistrationWindowStatusEnum.CLOSED;
        }
        return RegistrationWindowStatusEnum.OPEN;
    }

    public RegistrationWindowStatusEnum resolveWindowStatus(BasketDivision division, Date now) {
        if (division == null) {
            return RegistrationWindowStatusEnum.CLOSED;
        }
        return resolveWindowStatus(division.getRegistrationStartTime(), division.getRegistrationEndTime(), now);
    }

    public boolean isGrantUsable(BasketCaptainGrant grant, Date now) {
        if (grant == null) {
            return false;
        }
        Date clock = now == null ? new Date() : now;
        if (grant.getValidFrom() != null && grant.getValidFrom().after(clock)) {
            return false;
        }
        return grant.getValidTo() == null || !grant.getValidTo().before(clock);
    }

    /**
     * @param myRegistrationStatus 当前用户在该 Division 的报名状态（可空）
     * @param remainingTeamCount   剩余名额（null 表示不限）
     */
    public TeamRegistrationEligibility evaluate(BasketDivision division,
                                                BasketCaptainGrant grant,
                                                BasketTeam team,
                                                String myRegistrationStatus,
                                                Integer remainingTeamCount,
                                                Date now) {
        Date clock = now == null ? new Date() : now;
        if (division == null || !Objects.equals(NOT_DELETED, division.getIsDelete())) {
            return TeamRegistrationEligibility.deny(RegistrationWindowStatusEnum.CLOSED,
                    RegistrationEligibilityReasonEnum.DIVISION_NOT_FOUND, false);
        }

        RegistrationWindowStatusEnum window = resolveWindowStatus(division, clock);
        boolean hasExistingRegistration = myRegistrationStatus != null && !myRegistrationStatus.isBlank();
        boolean canView = hasExistingRegistration;

        String divisionStatus = division.getDivisionStatus();
        if (DivisionStatusEnum.FINISHED.getCode().equals(divisionStatus)
                || DivisionStatusEnum.ARCHIVED.getCode().equals(divisionStatus)) {
            return TeamRegistrationEligibility.deny(window,
                    RegistrationEligibilityReasonEnum.DIVISION_FINISHED, canView);
        }
        if (!DivisionStatusEnum.REGISTRATION_OPEN.getCode().equals(divisionStatus)) {
            // 状态已非报名开放时，按截止处理（仍可能查看历史报名）
            return TeamRegistrationEligibility.deny(
                    window == RegistrationWindowStatusEnum.NOT_STARTED ? window : RegistrationWindowStatusEnum.CLOSED,
                    RegistrationEligibilityReasonEnum.DIVISION_NOT_OPEN, canView);
        }

        if (window == RegistrationWindowStatusEnum.NOT_STARTED) {
            return TeamRegistrationEligibility.deny(window,
                    RegistrationEligibilityReasonEnum.NOT_STARTED, canView);
        }
        if (window == RegistrationWindowStatusEnum.CLOSED) {
            return TeamRegistrationEligibility.deny(window,
                    RegistrationEligibilityReasonEnum.REGISTRATION_CLOSED, canView);
        }

        if (!isGrantUsable(grant, clock)) {
            return TeamRegistrationEligibility.deny(window,
                    RegistrationEligibilityReasonEnum.NO_CAPTAIN_GRANT, canView);
        }

        if (team == null || !Objects.equals(NOT_DELETED, team.getIsDelete())) {
            return TeamRegistrationEligibility.deny(window,
                    RegistrationEligibilityReasonEnum.NO_TEAM, canView);
        }
        if (team.getDivisionId() == null || !Objects.equals(team.getDivisionId(), division.getId())) {
            return TeamRegistrationEligibility.deny(window,
                    RegistrationEligibilityReasonEnum.TEAM_DIVISION_MISMATCH, canView);
        }
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            return TeamRegistrationEligibility.deny(window,
                    RegistrationEligibilityReasonEnum.TEAM_ARCHIVED, canView);
        }
        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            return TeamRegistrationEligibility.deny(window,
                    RegistrationEligibilityReasonEnum.TEAM_NOT_CERTIFIED, canView);
        }

        if (remainingTeamCount != null && remainingTeamCount <= 0) {
            return TeamRegistrationEligibility.deny(window,
                    RegistrationEligibilityReasonEnum.QUOTA_FULL, canView);
        }

        if (RegistrationStatusEnum.PENDING_REVIEW.getCode().equals(myRegistrationStatus)) {
            return TeamRegistrationEligibility.deny(window,
                    RegistrationEligibilityReasonEnum.ALREADY_PENDING, true);
        }
        if (RegistrationStatusEnum.APPROVED.getCode().equals(myRegistrationStatus)) {
            return TeamRegistrationEligibility.deny(window,
                    RegistrationEligibilityReasonEnum.ALREADY_APPROVED, true);
        }

        return TeamRegistrationEligibility.allow(window);
    }

    public void assertCanMutateRegistration(TeamRegistrationEligibility eligibility) {
        if (eligibility == null || !eligibility.isCanRegister()) {
            String msg = eligibility == null || eligibility.getReasonMessage() == null
                    ? "当前不可报名"
                    : eligibility.getReasonMessage();
            throw new com.zhs.exception.ServiceException(msg);
        }
    }
}
