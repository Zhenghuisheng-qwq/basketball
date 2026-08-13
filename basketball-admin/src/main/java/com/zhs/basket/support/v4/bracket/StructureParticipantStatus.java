package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.domain.v4.BasketStagePairingEntry;
import com.zhs.basket.enums.v4.ParticipantResolutionStatusEnum;
import com.zhs.basket.enums.v4.ParticipantSourceTypeEnum;

import java.util.List;

public final class StructureParticipantStatus {

    private StructureParticipantStatus() {
    }

    public static boolean hasUnresolvedPlaceholders(List<BasketStagePairingEntry> entries) {
        if (entries == null) {
            return false;
        }
        for (BasketStagePairingEntry entry : entries) {
            if (isUnresolved(entry)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isParticipantResolved(List<BasketStagePairingEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        for (BasketStagePairingEntry entry : entries) {
            if (isUnresolved(entry)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isUnresolved(BasketStagePairingEntry entry) {
        if (entry == null) {
            return false;
        }
        if (Integer.valueOf(1).equals(entry.getIsBye())
                || ParticipantSourceTypeEnum.BYE.getCode().equals(entry.getSourceType())
                || ParticipantResolutionStatusEnum.BYE.getCode().equals(entry.getResolutionStatus())) {
            return false;
        }
        if (ParticipantSourceTypeEnum.GROUP_RANK.getCode().equals(entry.getSourceType())) {
            return !ParticipantResolutionStatusEnum.RESOLVED.getCode().equals(entry.getResolutionStatus())
                    || entry.getResolvedEventTeamId() == null;
        }
        return false;
    }
}
