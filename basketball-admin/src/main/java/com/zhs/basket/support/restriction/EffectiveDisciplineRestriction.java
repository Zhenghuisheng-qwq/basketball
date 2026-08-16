package com.zhs.basket.support.restriction;

public final class EffectiveDisciplineRestriction {
    private final boolean allowRegistration;
    private final boolean allowTransfer;
    private final boolean allowCaptain;
    private final boolean allowLineup;
    private final boolean allowParticipation;
    private final int remainingMatchSuspensions;
    private final Long effectiveDisciplineId;

    public EffectiveDisciplineRestriction(boolean allowRegistration, boolean allowTransfer, boolean allowCaptain,
            boolean allowLineup, boolean allowParticipation, int remainingMatchSuspensions, Long effectiveDisciplineId) {
        this.allowRegistration = allowRegistration;
        this.allowTransfer = allowTransfer;
        this.allowCaptain = allowCaptain;
        this.allowLineup = allowLineup;
        this.allowParticipation = allowParticipation;
        this.remainingMatchSuspensions = Math.max(remainingMatchSuspensions, 0);
        this.effectiveDisciplineId = effectiveDisciplineId;
    }

    public static EffectiveDisciplineRestriction unrestricted() {
        return new EffectiveDisciplineRestriction(true, true, true, true, true, 0, null);
    }

    public EffectiveDisciplineRestriction merge(DisciplineRestrictionContribution contribution) {
        if (contribution == null) {
            return this;
        }
        Long nextId = contribution.getDisciplineId() != null ? contribution.getDisciplineId() : effectiveDisciplineId;
        return new EffectiveDisciplineRestriction(
                allowRegistration && contribution.isAllowRegistration(),
                allowTransfer && contribution.isAllowTransfer(),
                allowCaptain && contribution.isAllowCaptain(),
                allowLineup && contribution.isAllowLineup(),
                allowParticipation && contribution.isAllowParticipation(),
                remainingMatchSuspensions + contribution.getRemainingMatchSuspensions(),
                nextId);
    }

    public boolean isUnrestricted() {
        return allowRegistration && allowTransfer && allowCaptain && allowLineup && allowParticipation
                && remainingMatchSuspensions <= 0;
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

    public Long getEffectiveDisciplineId() {
        return effectiveDisciplineId;
    }

    public int flag(boolean allowed) {
        return allowed ? 1 : 0;
    }
}
