package com.zhs.support.v4;

import com.zhs.enums.v4.RegistrationEligibilityReasonEnum;
import com.zhs.enums.v4.RegistrationWindowStatusEnum;

public class TeamRegistrationEligibility {
    private final boolean canRegister;
    private final boolean canViewRegistration;
    private final RegistrationWindowStatusEnum windowStatus;
    private final RegistrationEligibilityReasonEnum reason;
    private final String reasonMessage;

    private TeamRegistrationEligibility(boolean canRegister,
                                        boolean canViewRegistration,
                                        RegistrationWindowStatusEnum windowStatus,
                                        RegistrationEligibilityReasonEnum reason,
                                        String reasonMessage) {
        this.canRegister = canRegister;
        this.canViewRegistration = canViewRegistration;
        this.windowStatus = windowStatus;
        this.reason = reason;
        this.reasonMessage = reasonMessage;
    }

    public static TeamRegistrationEligibility allow(RegistrationWindowStatusEnum windowStatus) {
        return new TeamRegistrationEligibility(true, true, windowStatus, null, null);
    }

    public static TeamRegistrationEligibility deny(RegistrationWindowStatusEnum windowStatus,
                                                   RegistrationEligibilityReasonEnum reason,
                                                   boolean canViewRegistration) {
        return new TeamRegistrationEligibility(false, canViewRegistration, windowStatus, reason,
                reason == null ? null : reason.getMessage());
    }

    public boolean isCanRegister() { return canRegister; }
    public boolean isCanViewRegistration() { return canViewRegistration; }
    public RegistrationWindowStatusEnum getWindowStatus() { return windowStatus; }
    public RegistrationEligibilityReasonEnum getReason() { return reason; }
    public String getReasonMessage() { return reasonMessage; }
    public String getWindowStatusCode() { return windowStatus == null ? null : windowStatus.getCode(); }
    public String getWindowStatusLabel() { return windowStatus == null ? null : windowStatus.getLabel(); }
    public String getReasonCode() { return reason == null ? null : reason.getCode(); }
}
