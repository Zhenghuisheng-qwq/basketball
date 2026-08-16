package com.zhs.basket.service;

import com.zhs.basket.query.DisciplineQuery;
import com.zhs.basket.req.DisciplineAnnouncementDraftReq;
import com.zhs.basket.req.DisciplineCreateReq;
import com.zhs.basket.req.DisciplineRevokeReq;
import com.zhs.basket.vo.DisciplineAnnouncementDraftVo;
import com.zhs.basket.vo.DisciplineDetailVo;
import com.zhs.basket.vo.DisciplineListVo;
import com.zhs.basket.vo.DisciplineSelectorOptionVo;

import java.util.List;

public interface IBasketDisciplineService {
    List<DisciplineListVo> list(DisciplineQuery query);

    DisciplineDetailVo detail(Long id);

    Long create(DisciplineCreateReq req);

    void activate(Long id);

    boolean needsEventExecution(Long id);

    void revoke(Long id, DisciplineRevokeReq req);

    void consumeMatchSuspension(Long matchId, Long memberId);

    List<DisciplineSelectorOptionVo> selectSeasonOptions(String keyword);

    List<DisciplineSelectorOptionVo> selectPlayerOptions(Long seasonId, String keyword);

    List<DisciplineSelectorOptionVo> selectTeamOptions(Long seasonId, String targetType, String keyword);

    DisciplineAnnouncementDraftVo generateAnnouncementDraft(Long id, DisciplineAnnouncementDraftReq req);
}
