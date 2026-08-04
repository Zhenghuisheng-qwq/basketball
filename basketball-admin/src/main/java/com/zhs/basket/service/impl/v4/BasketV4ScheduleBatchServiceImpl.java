package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.MatchPublishStatusEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.query.v4.BasketScheduleBatchQuery;
import com.zhs.basket.query.v4.BasketV4MatchQuery;
import com.zhs.basket.service.v4.IBasketV4ScheduleBatchService;
import com.zhs.basket.vo.v4.BasketScheduleBatchDetailVo;
import com.zhs.basket.vo.v4.BasketScheduleBatchListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BasketV4ScheduleBatchServiceImpl implements IBasketV4ScheduleBatchService {

    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;

    @Override
    public List<BasketScheduleBatchListVo> selectList(BasketScheduleBatchQuery query) {
        return scheduleBatchMapper.selectVoList(query);
    }

    @Override
    public BasketScheduleBatchDetailVo selectById(Long id) {
        BasketScheduleBatch batch = requireBatch(id);
        BasketScheduleBatchQuery query = new BasketScheduleBatchQuery();
        query.setId(id);
        List<BasketScheduleBatchListVo> rows = scheduleBatchMapper.selectVoList(query);
        if (rows.isEmpty()) {
            throw new ServiceException("赛程批次不存在");
        }
        BasketScheduleBatchDetailVo detail = new BasketScheduleBatchDetailVo();
        BeanUtils.copyProperties(rows.get(0), detail);
        detail.setBatchNo(batch.getBatchNo());
        detail.setGenerateType(batch.getGenerateType());

        BasketV4MatchQuery matchQuery = new BasketV4MatchQuery();
        matchQuery.setScheduleBatchId(id);
        detail.setMatches(matchMapper.selectVoList(matchQuery));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        BasketScheduleBatch batch = requireBatch(id);
        if (!PublishStatusEnum.DRAFT.getCode().equals(batch.getPublishStatus())) {
            throw new ServiceException("仅草稿批次可发布");
        }
        if (matchMapper.countByBatchId(id) <= 0) {
            throw new ServiceException("批次下无比赛，无法发布");
        }

        Date now = new Date();
        batch.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        batch.setPublishTime(now);
        batch.setPublishedBy(SecurityUtils.getUserId());
        batch.setUpdateTime(now);
        if (scheduleBatchMapper.updateById(batch) != 1) {
            throw new ServiceException("发布赛程批次失败");
        }
        matchMapper.publishByBatchId(id);

        BasketDivision division = requireDivision(batch.getDivisionId());
        division.setDivisionStatus(DivisionStatusEnum.SCHEDULED.getCode());
        division.setUpdateTime(now);
        divisionMapper.updateById(division);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        BasketScheduleBatch batch = requireBatch(id);
        if (!PublishStatusEnum.DRAFT.getCode().equals(batch.getPublishStatus())) {
            throw new ServiceException("仅草稿批次可作废");
        }
        matchMapper.softDeleteDraftByBatchId(id);
        if (scheduleBatchMapper.softDelete(id) != 1) {
            throw new ServiceException("作废赛程批次失败");
        }
    }

    private BasketScheduleBatch requireBatch(Long id) {
        if (id == null) {
            throw new ServiceException("ID 不能为空");
        }
        BasketScheduleBatch batch = scheduleBatchMapper.selectById(id);
        if (batch == null) {
            throw new ServiceException("赛程批次不存在");
        }
        return batch;
    }

    private BasketDivision requireDivision(Long id) {
        BasketDivision division = divisionMapper.selectById(id);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }
}
