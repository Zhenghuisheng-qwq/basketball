package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketPointAdjustment;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketStanding;
import com.zhs.basket.dto.v4.BasketStandingPointAdjustmentReq;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.StageExecutionStatusEnum;
import com.zhs.basket.mapper.BasketPointAdjustmentMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageQualificationMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketStandingMapper;
import com.zhs.basket.req.AuditFieldInput;
import com.zhs.basket.service.v4.IBasketV4StatsSettlementService;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.basket.service.v4.IStandingPointAdjustmentApplicationService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.v4.MatchSeriesQualificationSyncService;
import com.zhs.basket.support.v4.V4PublicReadCacheEvictEvent;
import com.zhs.basket.support.v4.StandingPointAdjustmentCapability;
import com.zhs.basket.vo.v4.BasketStandingPointAdjustmentVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class StandingPointAdjustmentApplicationServiceImpl implements IStandingPointAdjustmentApplicationService {

    public static final String MSG_KNOCKOUT = "当前阶段为淘汰赛，不支持积分调整";
    public static final String MSG_NO_STANDING = "当前阶段不支持积分榜，不能调整积分";
    public static final String MSG_CONFIRMED = "当前积分已用于正式晋级确认，如需调整积分，请先处理已确认的晋级结果。";
    public static final String MSG_DOWNSTREAM = "积分排名已影响后续正式比赛，不能直接调整，请进入赛事管理员仲裁流程。";
    public static final String ADJUSTMENT_TYPE_ADMIN_MANUAL = "ADMIN_MANUAL";

    private static final int NOT_DELETED = 0;
    private static final int DELTA_MIN = -999;
    private static final int DELTA_MAX = 999;

    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketStandingMapper standingMapper;
    @Autowired
    private BasketPointAdjustmentMapper pointAdjustmentMapper;
    @Autowired
    private BasketDivisionStageQualificationMapper qualificationMapper;
    @Autowired
    private MatchSeriesQualificationSyncService qualificationSyncService;
    @Autowired
    private IBasketV4StatsSettlementService statsSettlementService;
    @Autowired
    private IDivisionStageExecutionApplicationService stageExecutionApplicationService;
    @Autowired
    private BasketBusinessAuditWriter auditWriter;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BasketStandingPointAdjustmentVo applyAdjustment(BasketStandingPointAdjustmentReq req) {
        validateRequest(req);
        BasketDivision division = divisionMapper.selectByIdForUpdate(req.getDivisionId());
        if (division == null) {
            throw new ServiceException("赛区不存在");
        }
        BasketDivisionStage stage = requireStage(req.getStageId());
        if (!req.getDivisionId().equals(stage.getDivisionId())) {
            throw new ServiceException("阶段不属于当前赛区");
        }
        assertCanAdjust(stage);

        BasketEventTeam eventTeam = eventTeamMapper.selectById(req.getEventTeamId());
        if (eventTeam == null || Integer.valueOf(1).equals(eventTeam.getIsDelete())) {
            throw new ServiceException("参赛队不存在");
        }
        if (!req.getDivisionId().equals(eventTeam.getDivisionId())) {
            throw new ServiceException("参赛队不属于当前赛区");
        }

        String idempotencyKey = resolveIdempotencyKey(req);
        BasketPointAdjustment existing = pointAdjustmentMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return toVo(existing);
        }

        int currentAdj = sumAdjustment(req.getDivisionId(), req.getStageId(), req.getEventTeamId());
        Integer currentFinal = currentFinalPoints(req.getStageId(), req.getEventTeamId());
        int beforeTotal = currentFinal != null ? currentFinal : currentAdj;
        int afterTotal = beforeTotal + req.getDelta();

        Date now = new Date();
        BasketPointAdjustment row = new BasketPointAdjustment();
        row.setSeasonId(eventTeam.getSeasonId() != null ? eventTeam.getSeasonId() : division.getSeasonId());
        row.setDivisionId(req.getDivisionId());
        row.setStageId(req.getStageId());
        row.setEventTeamId(req.getEventTeamId());
        row.setAdjustmentType(ADJUSTMENT_TYPE_ADMIN_MANUAL);
        row.setAdjustmentPoints(req.getDelta());
        row.setAdjustmentReason(req.getReason().trim());
        row.setRemark(StringUtils.isNotEmpty(req.getRemark()) ? req.getRemark().trim() : null);
        row.setIdempotencyKey(idempotencyKey);
        row.setOperatorId(SecurityUtils.getUserId());
        row.setOperatorName(SecurityUtils.getUsername());
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        pointAdjustmentMapper.insert(row);

        statsSettlementService.rebuildStageStats(req.getStageId());
        refreshPreviewIfNeeded(stage);

        List<AuditFieldInput> fields = new ArrayList<>();
        fields.add(AuditFieldInput.of("beforeTotal", "调整前积分", beforeTotal, beforeTotal));
        fields.add(AuditFieldInput.of("delta", "调整值", null, req.getDelta()));
        fields.add(AuditFieldInput.of("afterTotal", "调整后积分", beforeTotal, afterTotal));
        fields.add(AuditFieldInput.of("reason", "原因", null, req.getReason().trim()));
        fields.add(AuditFieldInput.of("eventTeamId", "参赛队", null, req.getEventTeamId()));
        auditWriter.writeAudit("V4_STANDING", "EVENT_TEAM", req.getEventTeamId(), "POINT_ADJUSTMENT",
                "管理员调整赛事积分 " + signed(req.getDelta()), req.getReason().trim(), fields);
        eventPublisher.publishEvent(new V4PublicReadCacheEvictEvent(
                req.getDivisionId(), null, row.getSeasonId(), req.getStageId(), req.getEventTeamId(), null));
        return toVo(row);
    }

    @Override
    public List<BasketStandingPointAdjustmentVo> listHistory(Long divisionId, Long stageId, Long eventTeamId) {
        if (divisionId == null || stageId == null || eventTeamId == null) {
            throw new ServiceException("divisionId、stageId、eventTeamId 不能为空");
        }
        List<BasketPointAdjustment> rows = pointAdjustmentMapper.selectV4ByStageAndEventTeam(divisionId, stageId, eventTeamId);
        List<BasketStandingPointAdjustmentVo> result = new ArrayList<>();
        if (rows == null) {
            return result;
        }
        for (BasketPointAdjustment row : rows) {
            result.add(toVo(row));
        }
        return result;
    }

    private void validateRequest(BasketStandingPointAdjustmentReq req) {
        if (req == null || req.getDivisionId() == null || req.getStageId() == null || req.getEventTeamId() == null) {
            throw new ServiceException("divisionId、stageId、eventTeamId 不能为空");
        }
        if (req.getDelta() == null || req.getDelta() == 0) {
            throw new ServiceException("调整值不能为 0");
        }
        if (req.getDelta() < DELTA_MIN || req.getDelta() > DELTA_MAX) {
            throw new ServiceException("调整值范围为 -999 到 999");
        }
        if (StringUtils.isEmpty(req.getReason()) || req.getReason().trim().isEmpty()) {
            throw new ServiceException("调整原因不能为空");
        }
    }

    private void assertCanAdjust(BasketDivisionStage stage) {
        if (CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(stage.getCompetitionFormat())) {
            throw new ServiceException(MSG_KNOCKOUT);
        }
        if (!StandingPointAdjustmentCapability.canAdjustStandingPoints(stage)) {
            throw new ServiceException(MSG_NO_STANDING);
        }
        if (StageExecutionStatusEnum.COMPLETED.getCode().equals(stage.getExecutionStatus())
                || hasConfirmedQualification(stage.getId())) {
            throw new ServiceException(MSG_CONFIRMED);
        }
        if (qualificationSyncService.hasOfficialDownstreamStarted(stage)) {
            throw new ServiceException(MSG_DOWNSTREAM);
        }
    }

    private boolean hasConfirmedQualification(Long stageId) {
        List<?> confirmed = qualificationMapper.selectConfirmedByStageId(stageId);
        return confirmed != null && !confirmed.isEmpty();
    }

    private void refreshPreviewIfNeeded(BasketDivisionStage stage) {
        if (stage == null || stage.getId() == null) {
            return;
        }
        String status = stage.getExecutionStatus();
        if (!StageExecutionStatusEnum.RUNNING.getCode().equals(status)
                && !StageExecutionStatusEnum.RESULT_PENDING_CONFIRM.getCode().equals(status)) {
            return;
        }
        List<?> preview = qualificationMapper.selectPreviewByStageId(stage.getId());
        if (preview == null || preview.isEmpty()) {
            return;
        }
        stageExecutionApplicationService.previewQualification(stage.getId());
    }

    private int sumAdjustment(Long divisionId, Long stageId, Long eventTeamId) {
        List<BasketPointAdjustment> sums = pointAdjustmentMapper.selectV4SumByStage(divisionId, stageId);
        if (sums == null) {
            return 0;
        }
        for (BasketPointAdjustment sum : sums) {
            if (eventTeamId.equals(sum.getEventTeamId()) && sum.getAdjustmentPoints() != null) {
                return sum.getAdjustmentPoints();
            }
        }
        return 0;
    }

    private Integer currentFinalPoints(Long stageId, Long eventTeamId) {
        List<BasketStanding> rows = standingMapper.selectByStageId(stageId);
        if (rows == null) {
            return null;
        }
        for (BasketStanding row : rows) {
            if (eventTeamId.equals(row.getEventTeamId())) {
                return row.getStandingPoints();
            }
        }
        return null;
    }

    private String resolveIdempotencyKey(BasketStandingPointAdjustmentReq req) {
        if (StringUtils.isNotEmpty(req.getClientRequestId())) {
            String key = req.getClientRequestId().trim();
            if (key.length() > 128) {
                throw new ServiceException("clientRequestId 过长");
            }
            return key;
        }
        return "SPA:" + UUID.randomUUID().toString().replace("-", "");
    }

    private BasketDivisionStage requireStage(Long stageId) {
        BasketDivisionStage stage = stageMapper.selectById(stageId);
        if (stage == null) {
            throw new ServiceException("阶段不存在");
        }
        return stage;
    }

    private BasketStandingPointAdjustmentVo toVo(BasketPointAdjustment row) {
        BasketStandingPointAdjustmentVo vo = new BasketStandingPointAdjustmentVo();
        vo.setId(row.getId());
        vo.setDivisionId(row.getDivisionId());
        vo.setStageId(row.getStageId());
        vo.setEventTeamId(row.getEventTeamId());
        vo.setDelta(row.getAdjustmentPoints());
        vo.setReason(row.getAdjustmentReason());
        vo.setRemark(row.getRemark());
        vo.setOperatorId(row.getOperatorId());
        vo.setOperatorName(row.getOperatorName());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }

    private String signed(int delta) {
        return delta > 0 ? "+" + delta : String.valueOf(delta);
    }
}
