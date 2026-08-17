package com.zhs.support.v4;

import com.zhs.enums.TeamProfileChangeAuditStatusEnum;
import com.zhs.model.BasketTeamProfileChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class V4PublicTeamArchiveAssembler {

    private V4PublicTeamArchiveAssembler() {
    }

    public static List<BasketTeamProfileChange> approvedOnly(List<BasketTeamProfileChange> rows) {
        List<BasketTeamProfileChange> result = new ArrayList<>();
        if (rows == null) {
            return result;
        }
        for (BasketTeamProfileChange row : rows) {
            if (row != null && Objects.equals(row.getAuditStatus(), TeamProfileChangeAuditStatusEnum.APPROVED.getCode())) {
                result.add(row);
            }
        }
        return result;
    }

    public static boolean isNameChanged(BasketTeamProfileChange row) {
        if (row == null) {
            return false;
        }
        return !Objects.equals(trimToEmpty(row.getOldTeamName()), trimToEmpty(row.getNewTeamName()));
    }

    public static boolean isLogoChanged(BasketTeamProfileChange row) {
        if (row == null) {
            return false;
        }
        return !Objects.equals(row.getOldLogoFileId(), row.getNewLogoFileId());
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}