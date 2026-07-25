package com.zhs.basket.support;

import com.zhs.basket.enums.TournamentRegistrationWindowStatusEnum;

import java.util.Date;

public final class TournamentRegistrationWindowSupport {

    private TournamentRegistrationWindowSupport() {
    }

    public static TournamentRegistrationWindowStatusEnum resolve(Date registrationStartTime,
                                                                 Date registrationEndTime,
                                                                 Date now) {
        Date reference = now == null ? new Date() : now;
        if (registrationStartTime == null || registrationEndTime == null) {
            return TournamentRegistrationWindowStatusEnum.UNCONFIGURED;
        }
        if (reference.before(registrationStartTime)) {
            return TournamentRegistrationWindowStatusEnum.NOT_STARTED;
        }
        if (reference.after(registrationEndTime)) {
            return TournamentRegistrationWindowStatusEnum.CLOSED;
        }
        return TournamentRegistrationWindowStatusEnum.OPEN;
    }
}
