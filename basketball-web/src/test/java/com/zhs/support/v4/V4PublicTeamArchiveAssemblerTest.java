package com.zhs.support.v4;

import com.zhs.enums.TeamProfileChangeAuditStatusEnum;
import com.zhs.model.BasketTeamProfileChange;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V4PublicTeamArchiveAssemblerTest {

    @Test
    void approvedOnly_keepsApprovedNameAndLogo() {
        BasketTeamProfileChange approved = change(3, "Name A", "Name B", 1L, 2L);
        BasketTeamProfileChange pending = change(1, "Name A", "Name C", 1L, 3L);
        BasketTeamProfileChange rejected = change(4, "Name A", "Name D", 1L, 4L);
        List<BasketTeamProfileChange> visible = V4PublicTeamArchiveAssembler.approvedOnly(
                Arrays.asList(approved, pending, rejected));
        assertEquals(1, visible.size());
        assertTrue(V4PublicTeamArchiveAssembler.isNameChanged(visible.get(0)));
        assertTrue(V4PublicTeamArchiveAssembler.isLogoChanged(visible.get(0)));
    }

    @Test
    void pendingAndRejected_notPublic() {
        assertEquals(0, V4PublicTeamArchiveAssembler.approvedOnly(Arrays.asList(
                change(1, "A", "B", 1L, 2L),
                change(4, "A", "B", 1L, 2L)
        )).size());
    }

    @Test
    void sameName_notNameHistory() {
        BasketTeamProfileChange row = change(3, "S12", "S12", 1L, 2L);
        assertFalse(V4PublicTeamArchiveAssembler.isNameChanged(row));
        assertTrue(V4PublicTeamArchiveAssembler.isLogoChanged(row));
    }

    private static BasketTeamProfileChange change(int status, String oldName, String newName,
                                                  Long oldLogo, Long newLogo) {
        BasketTeamProfileChange row = new BasketTeamProfileChange();
        row.setAuditStatus(status);
        row.setOldTeamName(oldName);
        row.setNewTeamName(newName);
        row.setOldLogoFileId(oldLogo);
        row.setNewLogoFileId(newLogo);
        assertEquals(TeamProfileChangeAuditStatusEnum.APPROVED.getCode(), 3);
        return row;
    }
}