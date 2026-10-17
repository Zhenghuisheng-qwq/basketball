package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMemberProfileChange;
import com.zhs.basket.enums.MemberProfileChangeAuditStatusEnum;
import com.zhs.basket.mapper.BasketMemberProfileChangeMapper;
import com.zhs.basket.query.MemberProfileChangeQuery;
import com.zhs.basket.req.MemberProfileChangeApproveReq;
import com.zhs.basket.req.MemberProfileChangeRejectReq;
import com.zhs.basket.service.IBasketMemberProfileChangeService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.v4.CurrentDisplayCacheEvictor;
import com.zhs.basket.support.NoticeParams;
import com.zhs.basket.vo.MemberProfileChangeDetailVo;
import com.zhs.basket.vo.MemberProfileChangeListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class BasketMemberProfileChangeServiceImpl implements IBasketMemberProfileChangeService {

    private static final List<Integer> PENDING_STATUSES =
            Collections.singletonList(MemberProfileChangeAuditStatusEnum.PENDING.getCode());

    @Autowired
    private BasketMemberProfileChangeMapper profileChangeMapper;
    @Autowired
    private BasketAdminMessageService messageService;
    @Autowired
    private CurrentDisplayCacheEvictor currentDisplayCacheEvictor;

    @Override
    public List<MemberProfileChangeListVo> selectList(MemberProfileChangeQuery query) {
        List<MemberProfileChangeListVo> list = profileChangeMapper.selectList(query == null ? new MemberProfileChangeQuery() : query);
        list.forEach(this::enrichListVo);
        return list;
    }

    @Override
    public MemberProfileChangeDetailVo selectDetail(Long id) {
        MemberProfileChangeDetailVo vo = profileChangeMapper.selectDetailVo(id);
        if (vo == null) {
            throw new ServiceException("用户资料变更申请不存在");
        }
        enrichListVo(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, MemberProfileChangeApproveReq req) {
        BasketMemberProfileChange change = requirePendingChange(id);
        if (!StringUtils.hasText(change.getNewNickname())) {
            throw new ServiceException("申请昵称无效");
        }

        int profileRows = profileChangeMapper.updateMemberOfficialProfile(
                change.getMemberId(),
                change.getNewNickname().trim(),
                change.getNewAvatarFileId());
        if (profileRows != 1) {
            throw new ServiceException("更新正式用户资料失败");
        }

        Date now = new Date();
        Long reviewerId = SecurityUtils.getUserId();
        int rows = profileChangeMapper.updateStatusIfMatch(
                id,
                PENDING_STATUSES,
                MemberProfileChangeAuditStatusEnum.APPROVED.getCode(),
                reviewerId,
                req == null ? null : StringUtils.trim(req.getAuditRemark()),
                now,
                0);
        assertSingleRow(rows, "审核通过失败，状态已变更");

        messageService.sendSiteMessage(
                change.getMemberId(),
                reviewerId,
                "MEMBER_PROFILE_CHANGE",
                id,
                "资料修改已通过",
                "你的昵称 / 头像修改已审核通过，公开资料已更新。",
                "PROFILE_CHANGE_APPROVED",
                null);
        currentDisplayCacheEvictor.afterMemberProfile(change.getMemberId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, MemberProfileChangeRejectReq req) {
        if (req == null || !StringUtils.hasText(req.getAuditRemark())) {
            throw new ServiceException("驳回原因不能为空");
        }
        BasketMemberProfileChange change = requirePendingChange(id);
        Date now = new Date();
        Long reviewerId = SecurityUtils.getUserId();
        int rows = profileChangeMapper.updateStatusIfMatch(
                id,
                PENDING_STATUSES,
                MemberProfileChangeAuditStatusEnum.REJECTED.getCode(),
                reviewerId,
                req.getAuditRemark().trim(),
                now,
                0);
        assertSingleRow(rows, "驳回失败，状态已变更");

        messageService.sendSiteMessage(
                change.getMemberId(),
                reviewerId,
                "MEMBER_PROFILE_CHANGE",
                id,
                "资料修改未通过",
                "你的资料修改未通过审核：" + req.getAuditRemark().trim(),
                "PROFILE_CHANGE_REJECTED",
                NoticeParams.json("remark", req.getAuditRemark().trim()));
    }

    private BasketMemberProfileChange requirePendingChange(Long id) {
        BasketMemberProfileChange change = profileChangeMapper.selectById(id);
        if (change == null) {
            throw new ServiceException("用户资料变更申请不存在");
        }
        if (!MemberProfileChangeAuditStatusEnum.isPending(change.getAuditStatus())) {
            throw new ServiceException("申请状态不可审核");
        }
        if (!Objects.equals(change.getActiveFlag(), 1)) {
            throw new ServiceException("申请已失效");
        }
        return change;
    }

    private void enrichListVo(MemberProfileChangeListVo vo) {
        MemberProfileChangeAuditStatusEnum status = MemberProfileChangeAuditStatusEnum.fromCode(vo.getAuditStatus());
        vo.setAuditStatusName(status == null ? null : status.getLabel());
        if (!StringUtils.hasText(vo.getChangeSummary())) {
            vo.setChangeSummary("资料变更");
        }
    }

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ServiceException(message);
        }
    }
}
