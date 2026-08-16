package com.zhs.basket.support.restriction;

public final class DisciplineRestrictionContribution {
    private final boolean allowRegistration;
    private final boolean allowTransfer;
    private final boolean allowCaptain;
    private final boolean allowLineup;
    private final boolean allowParticipation;
    private final int remainingMatchSuspensions;
    private final Long disciplineId;

    public DisciplineRestrictionContribution(boolean allowRegistration, boolean allowTransfer, boolean allowCaptain,
            boolean allowLineup, boolean allowParticipation, int remainingMatchSuspensions, Long disciplineId) {
        this.allowRegistration = allowRegistration;
        this.allowTransfer = allowTransfer;
        this.allowCaptain = allowCaptain;
        this.allowLineup = allowLineup;
        this.allowParticipation = allowParticipation;
        this.remainingMatchSuspensions = Math.max(remainingMatchSuspensions, 0);
        this.disciplineId = disciplineId;
    }

    public static DisciplineRestrictionContribution none(Long disciplineId) {
        return new DisciplineRestrictionContribution(true, true, true, true, true, 0, disciplineId);
    }

    public boolean isAllowRegistration() {
        return allowRegistration;
    }

    public boolean isAllowTransfer() {
        return allowTransfer;
    }

    public boolean isAllowCaptain() {
        return allowCaptain;
    }

    public boolean isAllowLineup() {
        return allowLineup;
    }

    public boolean isAllowParticipation() {
        return allowParticipation;
    }

    public int getRemainingMatchSuspensions() {
        return remainingMatchSuspensions;
    }

    public Long getDisciplineId() {
        return disciplineId;
    }
}
