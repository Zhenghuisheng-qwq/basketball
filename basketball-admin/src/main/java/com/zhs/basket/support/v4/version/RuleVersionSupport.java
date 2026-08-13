package com.zhs.basket.support.v4.version;

import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;

/**
 * FormatSnapshot → Rule Version 语义映射。
 */
public final class RuleVersionSupport {

    private RuleVersionSupport() {
    }

    public static RuleVersionView fromSnapshot(BasketDivisionFormatSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        RuleVersionView view = new RuleVersionView();
        view.setRuleVersionId(snapshot.getId());
        view.setRuleVersionNo(snapshot.getSnapshotVersion());
        view.setRuleVersionStatus(snapshot.getSnapshotStatus());
        view.setDivisionId(snapshot.getDivisionId());
        view.setIsCurrent(snapshot.getIsCurrent());
        view.setLocked(isLocked(snapshot));
        view.setRosterWorkflowMode(snapshot.getRosterWorkflowMode());
        return view;
    }

    public static boolean isLocked(BasketDivisionFormatSnapshot snapshot) {
        if (snapshot == null || snapshot.getSnapshotStatus() == null) {
            return false;
        }
        try {
            return SnapshotStatusEnum.fromCode(snapshot.getSnapshotStatus()).isLockedOrBeyond();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
