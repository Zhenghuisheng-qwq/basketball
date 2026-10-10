package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.domain.BasketPlayerPlatformChangeRequest;
import com.zhs.basket.enums.PlatformAccountStatusEnum;
import com.zhs.basket.enums.PlatformChangeStatusEnum;
import com.zhs.basket.enums.PlatformChangeTypeEnum;
import com.zhs.basket.mapper.BasketPlayerPlatformAccountMapper;
import com.zhs.basket.mapper.BasketPlayerPlatformChangeRequestMapper;
import com.zhs.basket.query.PlayerPlatformChangeQuery;
import com.zhs.basket.req.BatchAuditOperationReq;
import com.zhs.basket.req.PlayerPlatformChangeApproveReq;
import com.zhs.basket.req.PlayerPlatformChangeRejectReq;
import com.zhs.basket.support.BatchAuditSupport;
import com.zhs.basket.vo.BatchAuditResultVo;
import com.zhs.basket.service.IBasketPlayerPlatformChangeService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.NoticeParams;
import com.zhs.basket.support.PlatformIdentityHistoryWriter;
import com.zhs.basket.vo.PlayerPlatformChangeDetailVo;
import com.zhs.basket.vo.PlayerPlatformChangeListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketPlayerPlatformChangeServiceImpl implements IBasketPlayerPlatformChangeService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketPlayerPlatformChangeRequestMapper changeRequestMapper;
    @Autowired private BasketPlayerPlatformAccountMapper platformAccountMapper;
    @Autowired private BasketAdminMessageService messageService;
    @Autowired private PlatformIdentityHistoryWriter platformIdentityHistoryWriter;

    @Override
    public List<PlayerPlatformChangeListVo> selectList(PlayerPlatformChangeQuery query) {
        List<PlayerPlatformChangeListVo> list = changeRequestMapper.selectList(query);
        list.forEach(this::enrichListVo);
        return list;
    }

    @Override
    public PlayerPlatformChangeDetailVo selectDetail(Long id) {
        PlayerPlatformChangeDetailVo vo = changeRequestMapper.selectDetailVo(id);
        if (vo == null) {
            throw new ServiceException("平台变更申请不存在");
        }
        enrichDetailVo(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, PlayerPlatformChangeApproveReq req) {
        BasketPlayerPlatformChangeRequest request = requirePendingRequest(id);
        assertGameIdAvailable(request);
        Date now = new Date();
        Long reviewerId = SecurityUtils.getUserId();
        String oldDisplayGameId = resolveOldDisplayGameId(request);
        Long resultAccountId = applyPlatformChange(request, reviewerId, now);
        BasketPlayerPlatformAccount account = platformAccountMapper.selectById(resultAccountId);
        if (account == null) {
            throw new ServiceException("平台身份更新失败");
        }
        platformIdentityHistoryWriter.writeApprovedPlatformChange(request, account, oldDisplayGameId, reviewerId, now);
        int rows = changeRequestMapper.updateStatusIfMatch(id,
                Collections.singletonList(PlatformChangeStatusEnum.PENDING.getCode()),
                PlatformChangeStatusEnum.APPROVED.getCode(), reviewerId,
                req != null ? req.getAuditRemark() : null, now, resultAccountId);
        assertSingleRow(rows, "审核通过失败，状态已变更");
        messageService.sendSiteMessage(request.getMemberId(), reviewerId, "PLATFORM_CHANGE", id,
                "平台游戏ID变更已通过", "您的平台游戏ID变更申请已通过审核。",
                "PLATFORM_CHANGE_APPROVED", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, PlayerPlatformChangeRejectReq req) {
        BasketPlayerPlatformChangeRequest request = requirePendingRequest(id);
        Long reviewerId = SecurityUtils.getUserId();
        String remark = req != null && StringUtils.hasText(req.getRejectReason())
                ? req.getRejectReason()
                : (req != null ? req.getAuditRemark() : null);
        int rows = changeRequestMapper.updateStatusIfMatch(id,
                Collections.singletonList(PlatformChangeStatusEnum.PENDING.getCode()),
                PlatformChangeStatusEnum.REJECTED.getCode(), reviewerId, remark, new Date(), null);
        assertSingleRow(rows, "驳回失败，状态已变更");
        messageService.sendSiteMessage(request.getMemberId(), reviewerId, "PLATFORM_CHANGE", id,
                "平台游戏ID变更已驳回", "您的平台游戏ID变更申请已被驳回：" + remark,
                "PLATFORM_CHANGE_REJECTED", NoticeParams.json("remark", remark));
    }

    @Override
    public BatchAuditResultVo batchApprove(BatchAuditOperationReq req) {
        PlayerPlatformChangeApproveReq approveReq = new PlayerPlatformChangeApproveReq();
        approveReq.setAuditRemark(req.getAuditRemark());
        return BatchAuditSupport.run(req.getIds(), id -> approve(id, approveReq));
    }

    @Override
    public BatchAuditResultVo batchReject(BatchAuditOperationReq req) {
        PlayerPlatformChangeRejectReq rejectReq = new PlayerPlatformChangeRejectReq();
        rejectReq.setRejectReason(req.getRejectReason());
        rejectReq.setAuditRemark(req.getAuditRemark());
        return BatchAuditSupport.run(req.getIds(), id -> reject(id, rejectReq));
    }

    private String resolveOldDisplayGameId(BasketPlayerPlatformChangeRequest request) {
        PlatformChangeTypeEnum changeType = PlatformChangeTypeEnum.fromCode(request.getChangeType());
        if (changeType != PlatformChangeTypeEnum.CHANGE_GAME_ID) {
            return null;
        }
        if (StringUtils.hasText(request.getOldDisplayGameId())) {
            return request.getOldDisplayGameId();
        }
        BasketPlayerPlatformAccount existing = platformAccountMapper.selectById(request.getPlatformAccountId());
        return existing == null ? null : existing.getDisplayGameId();
    }

    private Long applyPlatformChange(BasketPlayerPlatformChangeRequest request, Long reviewerId, Date now) {
        PlatformChangeTypeEnum changeType = PlatformChangeTypeEnum.fromCode(request.getChangeType());
        if (changeType == PlatformChangeTypeEnum.ADD_PLATFORM) {
            return insertPlatformAccount(request, reviewerId, now, 0);
        }
        BasketPlayerPlatformAccount existing = platformAccountMapper.selectById(request.getPlatformAccountId());
        if (existing == null) {
            throw new ServiceException("原平台账号不存在");
        }
        existing.setDisplayGameId(request.getNewDisplayGameId());
        existing.setNormalizedGameId(request.getNewNormalizedGameId());
        existing.setApprovedBy(reviewerId);
        existing.setApprovedTime(now);
        existing.setEffectiveTime(now);
        if (platformAccountMapper.updateById(existing) != 1) {
            throw new ServiceException("更新平台账号失败");
        }
        return existing.getId();
    }

    private Long insertPlatformAccount(BasketPlayerPlatformChangeRequest request, Long reviewerId, Date now,
            int isPrimary) {
        BasketPlayerPlatformAccount account = new BasketPlayerPlatformAccount();
        account.setPlayerId(request.getPlayerId());
        account.setPlatform(normalizePlatformCode(request.getPlatform()));
        account.setDisplayGameId(request.getNewDisplayGameId());
        account.setNormalizedGameId(request.getNewNormalizedGameId());
        account.setAccountStatus(PlatformAccountStatusEnum.ACTIVE.getCode());
        account.setIsPrimary(isPrimary);
        account.setApprovedBy(reviewerId);
        account.setApprovedTime(now);
        account.setEffectiveTime(now);
        account.setCreateTime(now);
        account.setUpdateTime(now);
        account.setIsDelete(NOT_DELETED);
        platformAccountMapper.insert(account);
        return account.getId();
    }

    private void assertGameIdAvailable(BasketPlayerPlatformChangeRequest request) {
        int count = platformAccountMapper.countActiveByPlatformAndNormalizedExcludePlayer(
                normalizePlatformCode(request.getPlatform()), request.getNewNormalizedGameId(), request.getPlayerId());
        if (count > 0) {
            throw new ServiceException("该平台游戏ID已被其他球员使用");
        }
    }

    private BasketPlayerPlatformChangeRequest requirePendingRequest(Long id) {
        BasketPlayerPlatformChangeRequest request = changeRequestMapper.selectById(id);
        if (request == null) {
            throw new ServiceException("平台变更申请不存在");
        }
        if (!PlatformChangeStatusEnum.PENDING.getCode().equals(request.getRequestStatus())) {
            throw new ServiceException("申请状态不可审核");
        }
        return request;
    }

    private void enrichListVo(PlayerPlatformChangeListVo vo) {
        vo.setChangeTypeName(PlatformChangeTypeEnum.fromCode(vo.getChangeType()).getDescription());
        vo.setRequestStatusName(PlatformChangeStatusEnum.fromCode(vo.getRequestStatus()).getAuditDisplayDescription());
    }

    private void enrichDetailVo(PlayerPlatformChangeDetailVo vo) {
        vo.setChangeTypeName(PlatformChangeTypeEnum.fromCode(vo.getChangeType()).getDescription());
        vo.setRequestStatusName(PlatformChangeStatusEnum.fromCode(vo.getRequestStatus()).getAuditDisplayDescription());
    }

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ServiceException(message);
        }
    }

    private String normalizePlatformCode(String platform) {
        if (!StringUtils.hasText(platform)) {
            throw new ServiceException("平台代码不能为空");
        }
        String normalized = platform.trim().toUpperCase();
        if ("PS5".equals(normalized) || "PSN".equals(normalized)) {
            return "PS";
        }
        return normalized;
    }
}
