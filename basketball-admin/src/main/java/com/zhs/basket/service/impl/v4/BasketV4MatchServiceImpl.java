package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.dto.v4.BasketV4MatchUpdateReq;
import com.zhs.basket.enums.v4.MatchPublishStatusEnum;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.query.v4.BasketV4MatchQuery;
import com.zhs.basket.service.v4.IBasketV4MatchService;
import com.zhs.basket.support.ExternalUrlValidator;
import com.zhs.basket.support.v4.AdminOfficialResultCapability;
import com.zhs.basket.support.v4.schedule.ScheduleAdjustmentGuard;
import com.zhs.basket.vo.v4.BasketV4MatchListVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BasketV4MatchServiceImpl implements IBasketV4MatchService {

    @Autowired
    private BasketV4MatchMapper matchMapper;

    @Override
    public List<BasketV4MatchListVo> selectList(BasketV4MatchQuery query) {
        List<BasketV4MatchListVo> rows = matchMapper.selectVoList(query);
        if (rows != null) {
            for (BasketV4MatchListVo row : rows) {
                fillRuntimeAdjust(row);
            }
        }
        return rows;
    }

    @Override
    public BasketV4MatchListVo selectById(Long id) {
        if (id == null) {
            throw new ServiceException("ID 不能为空");
        }
        BasketV4MatchQuery query = new BasketV4MatchQuery();
        query.setId(id);
        List<BasketV4MatchListVo> rows = matchMapper.selectVoList(query);
        if (rows.isEmpty()) {
            throw new ServiceException("比赛不存在");
        }
        fillRuntimeAdjust(rows.get(0));
        return rows.get(0);
    }

    private void fillRuntimeAdjust(BasketV4MatchListVo row) {
        if (row == null) {
            return;
        }
        row.setStandaloneMatch(!ScheduleAdjustmentGuard.isSeriesChildMatch(row.getSeriesId()));
        String block = ScheduleAdjustmentGuard.matchManagementMatchBlockReason(
                row.getSeriesId(), row.getPublishStatus(), row.getMatchStatus());
        row.setAdjustScheduleBlockReason(block);
        row.setCanAdjustSchedule(block == null);
        BasketV4Match probe = new BasketV4Match();
        probe.setHomeEventTeamId(row.getHomeEventTeamId());
        probe.setAwayEventTeamId(row.getAwayEventTeamId());
        probe.setPublishStatus(row.getPublishStatus());
        probe.setMatchStatus(row.getMatchStatus());
        row.setCanEnterOfficialResult(AdminOfficialResultCapability.canEnterOfficialResult(probe));
        row.setCanCorrectOfficialResult(AdminOfficialResultCapability.canCorrectOfficialResult(probe));
        row.setCanForfeitMatch(AdminOfficialResultCapability.canForfeitMatch(probe));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDraft(Long id, BasketV4MatchUpdateReq req) {
        BasketV4Match match = requireMatch(id);
        if (req == null) {
            throw new ServiceException("请求体不能为空");
        }
        boolean isDraft = MatchPublishStatusEnum.DRAFT.getCode().equals(match.getPublishStatus());
        boolean editingCore = req.getMatchTime() != null || req.getVenueText() != null || req.getRemark() != null;
        if (!isDraft && editingCore) {
            throw new ServiceException("仅草稿/未发布比赛可编辑");
        }
        if (isDraft) {
            if (req.getMatchTime() != null) {
                match.setMatchTime(req.getMatchTime());
            }
            if (req.getVenueText() != null) {
                match.setVenueText(req.getVenueText());
            }
            if (req.getRemark() != null) {
                match.setRemark(req.getRemark());
            }
        }
        if (req.getReplayUrl() != null) {
            String replayUrl = req.getReplayUrl().trim();
            ExternalUrlValidator.validateHttpUrl(replayUrl, "比赛录像");
            match.setReplayUrl(replayUrl.isEmpty() ? "" : replayUrl);
        }
        match.setUpdateTime(new Date());
        if (matchMapper.updateById(match) != 1) {
            throw new ServiceException("更新比赛失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long id) {
        BasketV4Match match = requireMatch(id);
        if (!MatchPublishStatusEnum.DRAFT.getCode().equals(match.getPublishStatus())) {
            throw new ServiceException("已发布比赛不可删除");
        }
        if (matchMapper.softDelete(id) != 1) {
            throw new ServiceException("删除比赛失败");
        }
    }

    private BasketV4Match requireMatch(Long id) {
        BasketV4Match match = matchMapper.selectById(id);
        if (match == null) {
            throw new ServiceException("比赛不存在");
        }
        return match;
    }
}
