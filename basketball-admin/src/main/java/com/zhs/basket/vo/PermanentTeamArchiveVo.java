package com.zhs.basket.vo;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.BasketTeamAuditRecord;
import com.zhs.basket.resp.HonorRecordListResp;
import lombok.Data;

import java.util.List;

@Data
public class PermanentTeamArchiveVo {
    private BasketTeam team;
    private List<BasketTeamAuditRecord> auditHistory;
    private List<BasketDisciplineRecord> disciplineRecords;
    private List<HonorRecordListResp> honors;
}
