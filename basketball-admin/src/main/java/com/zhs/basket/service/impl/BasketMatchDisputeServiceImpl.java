package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchDataDispute;
import com.zhs.basket.domain.BasketMatchDataDisputeMaterial;
import com.zhs.basket.domain.BasketMatchOfficialResult;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.mapper.BasketMatchDataDisputeMapper;
import com.zhs.basket.mapper.BasketMatchDataDisputeMaterialMapper;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchOfficialResultMapper;
import com.zhs.basket.query.MatchDisputeQuery;
import com.zhs.basket.req.MatchDisputeCorrectionReq;
import com.zhs.basket.req.MatchDisputeCreateReq;
import com.zhs.basket.req.MatchDisputeHandleReq;
import com.zhs.basket.service.IBasketMatchDisputeService;
import com.zhs.basket.service.IBasketMatchOfficialDataCorrectionService;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.basket.support.BasketMatchMessageHelper;
import com.zhs.basket.vo.MatchDisputeDetailVo;
import com.zhs.basket.vo.MatchDisputeListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class BasketMatchDisputeServiceImpl implements IBasketMatchDisputeService {

    private static final Integer NOT_DELETED = 0;
    private static final int DISPUTE_WINDOW_HOURS = 12;
    private static final String SOURCE_DISPUTE_UPHELD = "DISPUTE_UPHELD";

    @Autowired private IBasketMatchValidationService validationService;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private BasketMatchDataDisputeMapper disputeMapper;
    @Autowired private BasketMatchDataDisputeMaterialMapper disputeMaterialMapper;
    @Autowired private BasketMatchOfficialResultMapper officialResultMapper;
    @Autowired private IBasketMatchOfficialDataCorrectionService correctionService;
    @Autowired private BasketMatchMessageHelper messageHelper;

    @Override
    public List<MatchDisputeListVo> list(MatchDisputeQuery query) {
        return disputeMapper.selectList(query != null ? query : new MatchDisputeQuery());
    }

    @Override
    public MatchDisputeDetailVo getDetail(Long id) {
        BasketMatchDataDispute dispute = getById(id);
        MatchDisputeDetailVo vo = new MatchDisputeDetailVo();
        vo.setDispute(dispute);
        vo.setMaterials(disputeMaterialMapper.selectVoByDisputeId(id));
        BasketMatch match = matchMapper.selectById(dispute.getMatchId());
        if (match != null) {
            vo.setMatchNo(match.getMatchNo());
            vo.setHomeTeamId(match.getHomeTeamId());
            vo.setAwayTeamId(match.getAwayTeamId());
            vo.setHomeScore(match.getHomeScore());
            vo.setAwayScore(match.getAwayScore());
        }
        return vo;
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
        if (disputeMapper.countActiveByMatchId(req.getMatchId()) > 0) {
            throw new ServiceException("当前比赛已有待处理争议");
        }
        Date deadline = resolveDisputeDeadline(match);
        if (deadline != null && new Date().after(deadline)) {
            throw new ServiceException("争议窗口已结束，请联系管理员");
        }
        Date now = new Date();
        BasketMatchDataDispute row = new BasketMatchDataDispute();
        row.setDisputeNo("DSP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        row.setMatchId(req.getMatchId());
        row.setSubmissionId(req.getSubmissionId());
        row.setInitiatorType(StringUtils.isNotEmpty(req.getInitiatorType()) ? req.getInitiatorType() : "ADMIN");
        row.setInitiatorId(req.getInitiatorId() != null ? req.getInitiatorId() : SecurityUtils.getUserId());
        row.setInitiatorMemberId(req.getInitiatorMemberId());
        row.setInitiatorPlayerId(req.getInitiatorPlayerId());
        row.setInitiatorTeamId(req.getInitiatorTeamId());
        row.setDisputeType(req.getDisputeType());
        row.setOriginalValue(req.getOriginalValue());
        row.setExpectedValue(req.getExpectedValue());
        row.setDisputeReason(req.getDisputeReason());
        row.setEvidenceFileIds(req.getEvidenceFileIds());
        row.setDisputeStatus("PENDING");
        row.setDisputeDeadlineTime(deadline);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        disputeMapper.insert(row);
        saveMaterials(row.getId(), req.getMatchId(), req.getMaterialFileIds(), now);
        return row.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, MatchDisputeHandleReq req) {
        if (req == null || StringUtils.isEmpty(req.getHandleRemark())) {
            throw new ServiceException("处理备注不能为空");
        }
        BasketMatchDataDispute dispute = getById(id);
        Long operatorId = SecurityUtils.getUserId();
        String operatorName = SecurityUtils.getUsername();
        if (disputeMapper.updateHandle(id, "PENDING", "REJECTED", operatorId, operatorName,
                "REJECTED", req.getHandleRemark(), req.getHandleRemark(), null) != 1) {
            throw new ServiceException("争议处理失败，可能已被其他管理员处理");
        }
        BasketMatch match = matchMapper.selectById(dispute.getMatchId());
        if (match != null) {
            try {
                messageHelper.notifyCaptains(match, operatorId, "数据争议已驳回",
                        "比赛数据争议已被驳回：" + req.getHandleRemark());
            } catch (Exception ignored) {
                // 通知失败不回滚
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uphold(Long id, MatchDisputeHandleReq req) {
        if (req == null || StringUtils.isEmpty(req.getHandleRemark())) {
            throw new ServiceException("处理备注不能为空");
        }
        Long operatorId = SecurityUtils.getUserId();
        String operatorName = SecurityUtils.getUsername();
        if (disputeMapper.updateHandle(id, "PENDING", "UPHELD", operatorId, operatorName,
                "UPHELD", req.getHandleRemark(), null, req.getRevisionId()) != 1) {
            throw new ServiceException("争议处理失败，可能已被其他管理员处理");
        }
        BasketMatchDataDispute dispute = getById(id);
        BasketMatch match = matchMapper.selectById(dispute.getMatchId());
        if (match != null) {
            try {
                messageHelper.notifyCaptains(match, operatorId, "数据争议已采纳",
                        "比赛数据争议已采纳：" + req.getHandleRemark());
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upholdAndCorrect(Long id, MatchDisputeCorrectionReq req) {
        if (req == null || StringUtils.isEmpty(req.getHandleRemark())) {
            throw new ServiceException("处理备注不能为空");
        }
        BasketMatchDataDispute dispute = getById(id);
        if (!"PENDING".equals(dispute.getDisputeStatus())) {
            throw new ServiceException("争议状态不可修正");
        }
        Long operatorId = SecurityUtils.getUserId();
        String operatorName = SecurityUtils.getUsername();
        Long revisionId = correctionService.applyCorrection(dispute.getMatchId(), req, SOURCE_DISPUTE_UPHELD,
                operatorId, id, req.getCorrectionReason() != null ? req.getCorrectionReason() : req.getHandleRemark());
        if (disputeMapper.updateHandle(id, "PENDING", "UPHELD", operatorId, operatorName,
                "UPHELD", req.getHandleRemark(), null, revisionId) != 1) {
            throw new ServiceException("争议采纳失败，可能已被其他管理员处理");
        }
        BasketMatch match = matchMapper.selectById(dispute.getMatchId());
        if (match != null) {
            try {
                messageHelper.notifyCaptains(match, operatorId, "数据争议已采纳并修正",
                        "比赛数据已按争议修正并重新结算：" + req.getHandleRemark());
            } catch (Exception ignored) {
            }
        }
    }

    private Date resolveDisputeDeadline(BasketMatch match) {
        BasketMatchOfficialResult official = officialResultMapper.selectCurrentByMatchId(match.getId());
        Date base = null;
        if (official != null) {
            base = official.getEffectiveTime() != null ? official.getEffectiveTime() : official.getConfirmedTime();
        }
        if (base == null) {
            base = match.getUpdateTime();
        }
        if (base == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.HOUR_OF_DAY, DISPUTE_WINDOW_HOURS);
        return cal.getTime();
    }

    private void saveMaterials(Long disputeId, Long matchId, List<Long> fileIds, Date now) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return;
        }
        List<BasketMatchDataDisputeMaterial> list = new ArrayList<>();
        int sort = 0;
        for (Long fileId : fileIds) {
            BasketMatchDataDisputeMaterial m = new BasketMatchDataDisputeMaterial();
            m.setDisputeId(disputeId);
            m.setMatchId(matchId);
            m.setFileId(fileId);
            m.setMaterialType("DISPUTE_EVIDENCE");
            m.setSortNo(sort++);
            m.setCreateTime(now);
            m.setUpdateTime(now);
            m.setIsDelete(NOT_DELETED);
            list.add(m);
        }
        disputeMaterialMapper.batchInsert(list);
    }
}
