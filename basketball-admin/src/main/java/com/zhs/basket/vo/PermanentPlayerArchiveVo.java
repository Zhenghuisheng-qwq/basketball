package com.zhs.basket.vo;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.domain.BasketMemberAccount;
import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.resp.HonorRecordListResp;
import lombok.Data;

import java.util.List;

@Data
public class PermanentPlayerArchiveVo {
    private BasketPlayerProfile profile;
    private BasketMemberAccount account;
    private List<AccountPlayerTeamSummaryVo> teamHistory;
    private List<BasketDisciplineRecord> disciplineRecords;
    private List<HonorRecordListResp> honors;
}
