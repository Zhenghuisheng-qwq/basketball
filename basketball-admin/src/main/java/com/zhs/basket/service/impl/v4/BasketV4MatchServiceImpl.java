package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.dto.v4.BasketV4MatchUpdateReq;
import com.zhs.basket.enums.v4.MatchPublishStatusEnum;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.query.v4.BasketV4MatchQuery;
import com.zhs.basket.service.v4.IBasketV4MatchService;
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
        return matchMapper.selectVoList(query);
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
        return rows.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDraft(Long id, BasketV4MatchUpdateReq req) {
        BasketV4Match match = requireMatch(id);
        if (!MatchPublishStatusEnum.DRAFT.getCode().equals(match.getPublishStatus())) {
            throw new ServiceException("仅草稿/未发布比赛可编辑");
        }
        if (req == null) {
            throw new ServiceException("请求体不能为空");
        }
        if (req.getMatchTime() != null) {
            match.setMatchTime(req.getMatchTime());
        }
        if (req.getVenueText() != null) {
            match.setVenueText(req.getVenueText());
        }
        if (req.getRemark() != null) {
            match.setRemark(req.getRemark());
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
