package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.enums.PenaltyExecutionStatusEnum;
import com.zhs.basket.mapper.BasketDisciplineExecutionDetailMapper;
import com.zhs.basket.mapper.BasketDisciplineRecordMapper;
import com.zhs.basket.service.IPenaltyExecutionService;
import com.zhs.basket.support.DisciplinePenaltyTargetCompatibilityPolicy;
import com.zhs.basket.support.penalty.PenaltyExecutionStrategy;
import com.zhs.basket.support.penalty.PenaltyExecutionStrategyFactory;
import com.zhs.basket.vo.PenaltyExecutionResultVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class PenaltyExecutionServiceImpl implements IPenaltyExecutionService {

    @Autowired
    private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Autowired
    private BasketDisciplineExecutionDetailMapper executionDetailMapper;
    @Autowired
    private PenaltyExecutionStrategyFactory strategyFactory;

    @Override
    public PenaltyExecutionResultVo execute(Long disciplineId) {
        BasketDisciplineRecord record = requireRecord(disciplineId);
        DisciplinePenaltyTargetCompatibilityPolicy.validateRecord(record);
        PenaltyExecutionStrategy strategy = strategyFactory.find(record);
        if (strategy == null) {
            PenaltyExecutionResultVo skipped = new PenaltyExecutionResultVo();
            skipped.setExecutionStatus(PenaltyExecutionStatusEnum.SKIPPED.name());
            skipped.setExecutedTime(new Date());
            persistResult(record.getId(), skipped);
            return skipped;
        }
        if (PenaltyExecutionStatusEnum.EFFECTIVE.name().equals(record.getExecutionStatus())) {
            return readExisting(record);
        }
        markProcessing(record.getId());
        PenaltyExecutionResultVo result;
        try {
            result = strategy.execute(record);
        } catch (Exception ex) {
            result = new PenaltyExecutionResultVo();
            result.setExecutionStatus(PenaltyExecutionStatusEnum.FAILED.name());
            result.setErrorMessage(ex.getMessage());
            result.setExecutedTime(new Date());
        }
        persistResult(record.getId(), result);
        result.setDetails(executionDetailMapper.selectByDisciplineId(record.getId()));
        return result;
    }

    @Override
    public void restoreEligibility(Long disciplineId) {
        BasketDisciplineRecord record = requireRecord(disciplineId);
        PenaltyExecutionStrategy strategy = strategyFactory.find(record);
        if (strategy == null) {
            return;
        }
        strategy.restoreEligibility(record);
        PenaltyExecutionResultVo result = readExisting(record);
        if (result == null) {
            result = new PenaltyExecutionResultVo();
        }
        result.setExecutionStatus(PenaltyExecutionStatusEnum.REVOKED.name());
        result.setExecutedTime(new Date());
        persistResult(record.getId(), result);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markProcessing(Long id) {
        PenaltyExecutionResultVo processing = new PenaltyExecutionResultVo();
        processing.setExecutionStatus(PenaltyExecutionStatusEnum.PROCESSING.name());
        persistResult(id, processing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void persistResult(Long id, PenaltyExecutionResultVo result) {
        disciplineRecordMapper.updateExecution(
                id,
                result.getExecutionStatus(),
                result.getExecutedTime(),
                result.getErrorMessage(),
                result.getFutureMatchForfeitCount(),
                result.getSeriesForfeitCount(),
                result.getQualificationRemovedCount(),
                result.getWarningCount() > 0 ? result.getWarningCount() : result.getWarnings().size());
    }

    private PenaltyExecutionResultVo readExisting(BasketDisciplineRecord record) {
        PenaltyExecutionResultVo vo = new PenaltyExecutionResultVo();
        vo.setExecutionStatus(record.getExecutionStatus());
        vo.setExecutedTime(record.getExecutedTime());
        vo.setErrorMessage(record.getExecutionError());
        vo.setEventTeamId(record.getEventTeamId());
        vo.setFutureMatchForfeitCount(nz(record.getExecutionMatchForfeitCount()));
        vo.setSeriesForfeitCount(nz(record.getExecutionSeriesForfeitCount()));
        vo.setQualificationRemovedCount(nz(record.getExecutionQualificationRemovedCount()));
        vo.setWarningCount(nz(record.getExecutionWarningCount()));
        vo.setDetails(executionDetailMapper.selectByDisciplineId(record.getId()));
        return vo;
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private BasketDisciplineRecord requireRecord(Long id) {
        BasketDisciplineRecord record = disciplineRecordMapper.selectById(id);
        if (record == null) {
            throw new ServiceException("\u5904\u7f5a\u8bb0\u5f55\u4e0d\u5b58\u5728");
        }
        return record;
    }
}
