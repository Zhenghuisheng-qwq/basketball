package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.service.v4.IBasketV4RuleVersionService;
import com.zhs.basket.service.v4.IBasketV4StructureVersionService;
import com.zhs.basket.support.v4.version.RosterWorkflowSupport;
import com.zhs.basket.support.v4.version.RuleVersionView;
import com.zhs.basket.support.v4.version.StructureVersionContext;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BasketV4StructureVersionServiceImpl implements IBasketV4StructureVersionService {

    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private IBasketV4RuleVersionService ruleVersionService;
    @Autowired
    private IBasketV4RosterVersionService rosterVersionService;
    @Autowired
    private BasketDivisionMapper divisionMapper;

    @Override
    public StructureVersionContext resolveForGenerate(Long divisionId) {
        StructureVersionContext context = new StructureVersionContext();
        context.setDivisionId(divisionId);
        RuleVersionView rule = ruleVersionService.getCurrent(divisionId);
        if (rule != null) {
            context.setRuleVersionId(rule.getRuleVersionId());
            context.setRuleVersionNo(rule.getRuleVersionNo());
        }
        BasketDivisionRosterVersion roster = rosterVersionService.getCurrentLockedVersion(divisionId);
        BasketDivision division = divisionMapper.selectById(divisionId);
        RosterWorkflowSupport.assertStructureRosterReady(rule, roster, division);
        if (roster != null) {
            context.setRosterVersionId(roster.getId());
            context.setRosterVersionNo(roster.getVersionNo());
        }
        return context;
    }

    @Override
    public void bindOnCreate(BasketScheduleBatch batch, StructureVersionContext context) {
        if (batch == null) {
            return;
        }
        if (context != null) {
            batch.setRuleVersionId(context.getRuleVersionId());
            batch.setRosterVersionId(context.getRosterVersionId());
        }
        if (batch.getStructureStatus() == null || batch.getStructureStatus().isEmpty()) {
            batch.setStructureStatus(StructureStatusEnum.ACTIVE.getCode());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markStale(Long batchId) {
        BasketScheduleBatch batch = requireBatch(batchId);
        if (StructureStatusEnum.STALE.getCode().equals(batch.getStructureStatus())) {
            return;
        }
        if (scheduleBatchMapper.markStructureStaleById(batchId) != 1) {
            throw new ServiceException("标记赛程结构失效失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int invalidateByRuleVersion(Long ruleVersionId) {
        if (ruleVersionId == null) {
            return 0;
        }
        return scheduleBatchMapper.markStructureStaleByRuleVersionId(ruleVersionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int invalidateByRosterVersion(Long rosterVersionId) {
        if (rosterVersionId == null) {
            return 0;
        }
        return scheduleBatchMapper.markStructureStaleByRosterVersionId(rosterVersionId);
    }

    @Override
    public boolean isStructureUsable(BasketScheduleBatch batch) {
        if (batch == null) {
            return false;
        }
        String status = batch.getStructureStatus();
        if (status == null || status.isEmpty()) {
            return true;
        }
        try {
            return StructureStatusEnum.fromCode(status).isUsable();
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }

    @Override
    public boolean isStructureUsable(Long batchId) {
        if (batchId == null) {
            return false;
        }
        return isStructureUsable(scheduleBatchMapper.selectById(batchId));
    }

    @Override
    public void assertBoundRuleVersion(BasketScheduleBatch batch, Long expectedRuleVersionId) {
        if (batch == null || batch.getRuleVersionId() == null || expectedRuleVersionId == null) {
            return;
        }
        if (!batch.getRuleVersionId().equals(expectedRuleVersionId)) {
            throw new ServiceException("赛程结构绑定的规则版本与当前规则不一致");
        }
    }

    private BasketScheduleBatch requireBatch(Long batchId) {
        BasketScheduleBatch batch = scheduleBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new ServiceException("赛程批次不存在");
        }
        return batch;
    }
}
