package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.RosterVersionStatusEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionFormatSnapshotMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionRosterVersionMapper;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketV4RosterVersionServiceImplTest {

    @Mock private BasketDivisionRosterVersionMapper rosterVersionMapper;
    @Mock private BasketDivisionMapper divisionMapper;
    @Mock private BasketDivisionFormatSnapshotMapper snapshotMapper;

    @InjectMocks
    private BasketV4RosterVersionServiceImpl service;

    private BasketDivision division;
    private BasketDivisionFormatSnapshot snapshot;

    @BeforeEach
    void setUp() {
        division = new BasketDivision();
        division.setId(100L);
        division.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());

        snapshot = new BasketDivisionFormatSnapshot();
        snapshot.setId(11L);
        snapshot.setDivisionId(100L);
        snapshot.setSnapshotVersion(1);
        snapshot.setSnapshotStatus(SnapshotStatusEnum.LOCKED.getCode());
        snapshot.setIsCurrent(1);
    }

    @Test
    void createLockQuery_v1Current() {
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        when(snapshotMapper.selectActiveByDivisionId(100L)).thenReturn(snapshot);
        when(rosterVersionMapper.selectMaxVersionByDivisionId(100L)).thenReturn(0);
        when(rosterVersionMapper.insert(any())).thenAnswer(invocation -> {
            BasketDivisionRosterVersion created = invocation.getArgument(0);
            created.setId(201L);
            return 1;
        });

        BasketDivisionRosterVersion draft = service.createVersion(100L, 9L);
        assertEquals(1, draft.getVersionNo());
        assertEquals(RosterVersionStatusEnum.DRAFT.getCode(), draft.getStatus());
        assertEquals(11L, draft.getRuleVersionId());
        assertEquals(0, draft.getIsCurrent());

        BasketDivisionRosterVersion v1 = new BasketDivisionRosterVersion();
        v1.setId(201L);
        v1.setDivisionId(100L);
        v1.setVersionNo(1);
        v1.setStatus(RosterVersionStatusEnum.DRAFT.getCode());
        v1.setRuleVersionId(11L);
        when(rosterVersionMapper.selectById(201L)).thenReturn(v1);
        when(rosterVersionMapper.selectCurrentLockedByDivisionId(100L)).thenReturn(null);
        when(rosterVersionMapper.updateById(any())).thenReturn(1);

        BasketDivisionRosterVersion locked = service.lockVersion(201L, 9L);
        assertEquals(RosterVersionStatusEnum.LOCKED.getCode(), locked.getStatus());
        assertEquals(1, locked.getIsCurrent());
        assertEquals(BasketV4RosterVersionServiceImpl.buildActiveUniqueKey(100L), locked.getActiveUniqueKey());

        when(rosterVersionMapper.selectCurrentLockedByDivisionId(100L)).thenReturn(locked);
        assertEquals(201L, service.getCurrentLockedVersion(100L).getId());
    }

    @Test
    void lockV2_invalidatesV1() {
        BasketDivisionRosterVersion v1 = new BasketDivisionRosterVersion();
        v1.setId(201L);
        v1.setDivisionId(100L);
        v1.setVersionNo(1);
        v1.setStatus(RosterVersionStatusEnum.LOCKED.getCode());
        v1.setIsCurrent(1);

        BasketDivisionRosterVersion v2 = new BasketDivisionRosterVersion();
        v2.setId(202L);
        v2.setDivisionId(100L);
        v2.setVersionNo(2);
        v2.setStatus(RosterVersionStatusEnum.DRAFT.getCode());

        when(rosterVersionMapper.selectById(202L)).thenReturn(v2);
        when(rosterVersionMapper.selectCurrentLockedByDivisionId(100L)).thenReturn(v1);
        when(rosterVersionMapper.clearActiveUniqueKey(201L)).thenReturn(1);
        when(rosterVersionMapper.markSuperseded(201L)).thenReturn(1);
        when(rosterVersionMapper.updateById(any())).thenReturn(1);

        BasketDivisionRosterVersion locked = service.lockVersion(202L, 9L);
        assertEquals(RosterVersionStatusEnum.LOCKED.getCode(), locked.getStatus());
        assertEquals(1, locked.getIsCurrent());
        verify(rosterVersionMapper).markSuperseded(201L);

        v1.setIsCurrent(0);
        v1.setStatus(RosterVersionStatusEnum.INVALID.getCode());
        when(rosterVersionMapper.selectCurrentLockedByDivisionId(100L)).thenReturn(locked);
        assertEquals(202L, service.getCurrentLockedVersion(100L).getId());
    }

    @Test
    void createVersion_withoutSnapshot_rejected() {
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        when(snapshotMapper.selectActiveByDivisionId(100L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.createVersion(100L, 9L));
        verify(rosterVersionMapper, never()).insert(any());
    }

    @Test
    void getCurrentLockedVersion_none_returnsNull() {
        when(rosterVersionMapper.selectCurrentLockedByDivisionId(100L)).thenReturn(null);
        assertNull(service.getCurrentLockedVersion(100L));
    }
}
