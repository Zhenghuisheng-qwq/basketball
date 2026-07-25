package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchDataDispute;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.mapper.BasketMatchDataDisputeMapper;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.query.MatchDisputeQuery;
import com.zhs.basket.req.MatchDisputeCreateReq;
import com.zhs.basket.req.MatchDisputeHandleReq;
import com.zhs.basket.service.IBasketMatchDisputeService;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.basket.vo.MatchDisputeListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class BasketMatchDisputeServiceImpl implements IBasketMatchDisputeService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private IBasketMatchValidationService validationService;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private BasketMatchDataDisputeMapper disputeMapper;

    @Override
    public List<MatchDisputeListVo> list(MatchDisputeQuery query) {
        return disputeMapper.selectList(query != null ? query : new MatchDisputeQuery());
    }

    @Override
    public BasketMatchDataDispute getById(Long id) {
        BasketMatchDataDispute dispute = disputeMapper.selectById(id);
        if (dispute == null) {
            throw new ServiceException("争议记录不存在");
        }
        return dispute;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MatchDisputeCreateReq req) {
        if (req == null || req.getMatchId() == null) {
            throw new ServiceException("比赛ID不能为空");
        }
        if (StringUtils.isEmpty(req.getDisputeType()) || StringUtils.isEmpty(req.getDisputeReason())) {
            throw new ServiceException("争议类型与说明不能为空");
        }
        BasketMatch match = validationService.requireMatch(req.getMatchId());
        if (match.getDataStatus() == null || match.getDataStatus() != MatchDataStatusEnum.CONFIRMED.getCode()) {
            throw new ServiceException("仅已确认正式结果的比赛可发起争议");
        }
        if (disputeMapper.countPendingByMatchAndType(req.getMatchId(), req.getDisputeType()) > 0) {
            throw new ServiceException("同一事项存在待处理争议");
        }
        Date now = new Date();
        BasketMatchDataDispute row = new BasketMatchDataDispute();
        row.setDisputeNo("DSP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        row.setMatchId(req.getMatchId());
        row.setInitiatorType(StringUtils.isNotEmpty(req.getInitiatorType()) ? req.getInitiatorType() : "ADMIN");
        row.setInitiatorId(SecurityUtils.getUserId());
        row.setDisputeType(req.getDisputeType());
        row.setOriginalValue(req.getOriginalValue());
        row.setExpectedValue(req.getExpectedValue());
        row.setDisputeReason(req.getDisputeReason());
        row.setEvidenceFileIds(req.getEvidenceFileIds());
        row.setDisputeStatus("PENDING");
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        disputeMapper.insert(row);
        return row.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, MatchDisputeHandleReq req) {
        handle(id, req, "PENDING", "REJECTED", "REJECTED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uphold(Long id, MatchDisputeHandleReq req) {
        handle(id, req, "PENDING", "UPHELD", "UPHELD");
    }

    private void handle(Long id, MatchDisputeHandleReq req, String fromStatus, String toStatus, String handleResult) {
        if (req == null || StringUtils.isEmpty(req.getHandleRemark())) {
            throw new ServiceException("处理备注不能为空");
        }
        if (disputeMapper.updateHandle(id, fromStatus, toStatus, SecurityUtils.getUserId(),
                handleResult, req.getHandleRemark(), req.getRevisionId()) != 1) {
            throw new ServiceException("争议处理失败，可能已被其他管理员处理");
        }
    }
}
