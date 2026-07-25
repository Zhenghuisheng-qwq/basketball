package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketCaptainQualification;
import com.zhs.basket.domain.BasketMemberAccount;
import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.enums.CaptainQualificationStatusEnum;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.mapper.BasketCaptainQualificationMapper;
import com.zhs.basket.mapper.BasketMemberAccountMapper;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.BasketTeamMemberMapper;
import com.zhs.basket.query.CaptainQualificationQuery;
import com.zhs.basket.req.CaptainQualificationGrantReq;
import com.zhs.basket.req.CaptainQualificationRevokeReq;
import com.zhs.basket.req.CaptainQualificationUpdateReq;
import com.zhs.basket.service.IBasketCaptainQualificationService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.support.CaptainQualificationValiditySupport;
import com.zhs.basket.vo.CaptainQualificationDetailVo;
import com.zhs.basket.vo.CaptainQualificationListVo;
import com.zhs.basket.vo.EligibleCaptainVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketCaptainQualificationServiceImpl implements IBasketCaptainQualificationService {

    private static final Integer NOT_DELETED = 0;
    private static final int CURRENT = 1;

    @Autowired private BasketCaptainQualificationMapper qualificationMapper;
    @Autowired private BasketPlayerProfileMapper profileMapper;
    @Autowired private BasketMemberAccountMapper memberAccountMapper;
    @Autowired private BasketTeamMemberMapper teamMemberMapper;
    @Autowired private PlayerNoGenerator playerNoGenerator;
    @Autowired private BasketAdminMessageService messageService;

    @Override
    public List<EligibleCaptainVo> selectEligibleList() {
        return profileMapper.selectEligibleCaptainList().stream().peek(vo -> vo.setUserId(vo.getMemberId())).collect(Collectors.toList());
    }

    @Override
    public List<CaptainQualificationListVo> selectList(CaptainQualificationQuery query) {
        Date now = new Date();
        return qualificationMapper.selectList(query).stream().map(q -> {
            CaptainQualificationListVo vo = new CaptainQualificationListVo();
            BeanUtils.copyBeanProp(vo, q);
            enrichRuntimeValidity(vo, q, now);
            enrichAccountPlayerDisplay(vo.getMemberId(), vo.getPlayerId(), vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public CaptainQualificationDetailVo selectDetail(Long qualificationId) {
        BasketCaptainQualification q = requireQualification(qualificationId);
        CaptainQualificationDetailVo vo = new CaptainQualificationDetailVo();
        BeanUtils.copyBeanProp(vo, q);
        enrichRuntimeValidity(vo, q, new Date());
        enrichAccountPlayerDisplay(vo.getMemberId(), vo.getPlayerId(), vo);
        return vo;
    }

    private void enrichAccountPlayerDisplay(Long memberId, Long playerId, CaptainQualificationListVo vo) {
        vo.setUserId(memberId);
        if (memberId != null) {
            BasketMemberAccount account = memberAccountMapper.selectById(memberId);
            if (account != null) {
                vo.setUsername(account.getUsername());
                vo.setEmail(account.getEmail());
            }
        }
        if (playerId != null) {
            BasketPlayerProfile profile = profileMapper.selectById(playerId);
            if (profile != null) {
                vo.setPlayerNo(profile.getPlayerNo());
                vo.setGameAccount(profile.getGameAccount());
                vo.setPositionCode(profile.getPositionCode());
                vo.setNickname(profile.getGameNickname());
                if (profile.getCertificationStatus() != null) {
                    vo.setCertificationStatusName(
                            PlayerCertStatusEnum.fromCode(profile.getCertificationStatus()).getDescription());
                }
            }
        }
    }

    private void enrichAccountPlayerDisplay(Long memberId, Long playerId, CaptainQualificationDetailVo vo) {
        vo.setUserId(memberId);
        if (memberId != null) {
            BasketMemberAccount account = memberAccountMapper.selectById(memberId);
            if (account != null) {
                vo.setUsername(account.getUsername());
                vo.setEmail(account.getEmail());
            }
        }
        if (playerId != null) {
            BasketPlayerProfile profile = profileMapper.selectById(playerId);
            if (profile != null) {
                vo.setPlayerNo(profile.getPlayerNo());
                vo.setGameAccount(profile.getGameAccount());
                vo.setPositionCode(profile.getPositionCode());
                vo.setNickname(profile.getGameNickname());
                if (profile.getCertificationStatus() != null) {
                    vo.setCertificationStatusName(
                            PlayerCertStatusEnum.fromCode(profile.getCertificationStatus()).getDescription());
                }
            }
        }
    }

    private void enrichRuntimeValidity(CaptainQualificationListVo vo, BasketCaptainQualification q, Date now) {
        Integer certStatus = null;
        if (q.getPlayerId() != null) {
            BasketPlayerProfile profile = profileMapper.selectById(q.getPlayerId());
            if (profile != null) {
                certStatus = profile.getCertificationStatus();
            }
        }
        String invalidReason = CaptainQualificationValiditySupport.resolveInvalidReason(q, certStatus, now);
        vo.setRuntimeValid(invalidReason == null ? 1 : 0);
        vo.setRuntimeInvalidReason(invalidReason);
    }

    private void enrichRuntimeValidity(CaptainQualificationDetailVo vo, BasketCaptainQualification q, Date now) {
        Integer certStatus = null;
        if (q.getPlayerId() != null) {
            BasketPlayerProfile profile = profileMapper.selectById(q.getPlayerId());
            if (profile != null) {
                certStatus = profile.getCertificationStatus();
            }
        }
        String invalidReason = CaptainQualificationValiditySupport.resolveInvalidReason(q, certStatus, now);
        vo.setRuntimeValid(invalidReason == null ? 1 : 0);
        vo.setRuntimeInvalidReason(invalidReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(CaptainQualificationGrantReq req) {
        BasketPlayerProfile profile = profileMapper.selectById(req.getPlayerId());
        if (profile == null || profile.getCertificationStatus() == null
                || profile.getCertificationStatus() != PlayerCertStatusEnum.APPROVED.getCode()) {
            throw new ServiceException("球员未通过认证，无法授予队长资格");
        }
        if (qualificationMapper.countCurrentActiveByPlayerId(req.getPlayerId()) > 0) {
            throw new ServiceException("该球员已有有效队长资格");
        }
        qualificationMapper.clearCurrentByPlayerId(req.getPlayerId());
        Date now = new Date();
        BasketCaptainQualification record = new BasketCaptainQualification();
        record.setQualificationNo(playerNoGenerator.generateBizNo("CQ"));
        record.setMemberId(profile.getMemberId());
        record.setPlayerId(profile.getId());
        record.setQualificationStatus(CaptainQualificationStatusEnum.ACTIVE.getCode());
        record.setIsCurrent(CURRENT);
        record.setPaymentConfirmed(req.getPaymentConfirmed());
        record.setPaymentReference(req.getPaymentReference());
        record.setGrantReason(req.getGrantRemark());
        record.setGrantedBy(SecurityUtils.getUserId());
        record.setGrantedTime(now);
        record.setValidFrom(req.getValidFrom());
        record.setValidTo(req.getValidTo());
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        qualificationMapper.insert(record);
        messageService.sendSiteMessage(profile.getMemberId(), SecurityUtils.getUserId(), "CAPTAIN_QUAL", record.getId(),
                "队长资格已授予", "您已获得队长资格，有效期至 " + req.getValidTo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long qualificationId, CaptainQualificationUpdateReq req) {
        BasketCaptainQualification record = requireQualification(qualificationId);
        record.setPaymentConfirmed(req.getPaymentConfirmed());
        record.setPaymentReference(req.getPaymentReference());
        record.setGrantReason(req.getGrantRemark());
        record.setValidFrom(req.getValidFrom());
        record.setValidTo(req.getValidTo());
        if (qualificationMapper.updateById(record) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long qualificationId, CaptainQualificationRevokeReq req) {
        BasketCaptainQualification record = requireQualification(qualificationId);
        if (teamMemberMapper.countActiveCaptainTeams(record.getPlayerId()) > 0) {
            throw new ServiceException("该球员仍是活跃球队队长，无法撤销资格");
        }
        record.setQualificationStatus(CaptainQualificationStatusEnum.REVOKED.getCode());
        record.setIsCurrent(0);
        record.setRevokedBy(SecurityUtils.getUserId());
        record.setRevokedTime(new Date());
        record.setRevokeReason(req.getRevokeReason());
        if (qualificationMapper.updateById(record) != 1) {
            throw new ServiceException("撤销失败");
        }
        messageService.sendSiteMessage(record.getMemberId(), SecurityUtils.getUserId(), "CAPTAIN_QUAL", record.getId(),
                "队长资格已撤销", req.getRevokeReason());
    }

    private BasketCaptainQualification requireQualification(Long id) {
        BasketCaptainQualification q = qualificationMapper.selectById(id);
        if (q == null) {
            throw new ServiceException("队长资格不存在");
        }
        return q;
    }
}
