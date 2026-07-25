package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.enums.RegistrationPlayerValidationStatusEnum;
import com.zhs.basket.enums.RegistrationStatusEnum;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.RegistrationAuditQuery;
import com.zhs.basket.req.RegistrationAuditApproveReq;
import com.zhs.basket.req.RegistrationAuditRejectReq;
import com.zhs.basket.service.IBasketParticipationGenerationService;
import com.zhs.basket.service.IBasketRegistrationAuditService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.BasketDisciplineRestrictionSupport;
import com.zhs.basket.vo.*;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasketRegistrationAuditServiceImpl implements IBasketRegistrationAuditService {

    private static final Integer NOT_DELETED = 0;
    private static final int PASS = RegistrationPlayerValidationStatusEnum.PASS.getCode();
    private static final int FAIL = RegistrationPlayerValidationStatusEnum.FAIL.getCode();

    @Autowired private BasketTournamentRegistrationMapper registrationMapper;
    @Autowired private BasketTournamentRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired private BasketTournamentRegistrationAuditMapper registrationAuditMapper;
    @Autowired private BasketTournamentSeasonMapper seasonMapper;
    @Autowired private BasketPlayerProfileMapper profileMapper;
    @Autowired private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Autowired private BasketDisciplineRestrictionSupport disciplineRestrictionSupport;
    @Autowired private BasketAdminMessageService messageService;
    @Autowired private IBasketParticipationGenerationService participationGenerationService;

    @Override
    public List<RegistrationAuditListVo> selectList(RegistrationAuditQuery query) {
        return registrationMapper.selectAuditList(query).stream().map(r -> {
            RegistrationAuditListVo vo = new RegistrationAuditListVo();
            BeanUtils.copyBeanProp(vo, r);
            vo.setRegistrationStatusName(RegistrationStatusEnum.fromCode(r.getRegistrationStatus()).getDescription());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public RegistrationAuditDetailVo selectDetail(Long id) {
        BasketTournamentRegistration reg = requireRegistration(id);
        RegistrationAuditDetailVo vo = new RegistrationAuditDetailVo();
        BeanUtils.copyBeanProp(vo, reg);
        vo.setRegistrationStatusName(RegistrationStatusEnum.fromCode(reg.getRegistrationStatus()).getDescription());
        vo.setPlayers(registrationPlayerMapper.selectDetailByRegistrationId(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RegistrationPlayerVo> revalidate(Long id) {
        BasketTournamentRegistration reg = requireRegistration(id);
        List<BasketTournamentRegistrationPlayer> players = registrationPlayerMapper.selectByRegistrationId(id);
        Date now = new Date();
        List<Long> memberIds = players.stream().map(BasketTournamentRegistrationPlayer::getMemberId).filter(Objects::nonNull).collect(Collectors.toList());
        Set<Long> disciplined = Collections.emptySet();
        if (!memberIds.isEmpty()) {
            disciplined = disciplineRecordMapper.selectActiveBySeasonAndMembers(reg.getSeasonId(), memberIds, now)
                    .stream().map(BasketDisciplineRecord::getMemberId).collect(Collectors.toSet());
        }
        String teamRestriction = reg.getTeamId() != null
                ? disciplineRestrictionSupport.checkRegistration("PERMANENT_TEAM", reg.getTeamId(), reg.getSeasonId())
                : null;
        List<RegistrationPlayerVo> result = new ArrayList<>();
        for (BasketTournamentRegistrationPlayer player : players) {
            String reason = validatePlayer(player, disciplined, reg.getSeasonId(), teamRestriction);
            int status = reason == null ? PASS : FAIL;
            registrationPlayerMapper.updateValidationStatus(player.getId(), status, reason);
            RegistrationPlayerVo vo = new RegistrationPlayerVo();
            BeanUtils.copyBeanProp(vo, player);
            vo.setValidationStatus(status);
            vo.setRejectReason(reason);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startReview(Long id) {
        BasketTournamentRegistration reg = requireRegistration(id);
        int rows = registrationMapper.updateStatusIfMatch(id,
                Collections.singletonList(RegistrationStatusEnum.SUBMITTED.getCode()),
                RegistrationStatusEnum.REVIEWING.getCode(), SecurityUtils.getUserId(), null, null, null);
        assertSingleRow(rows, "开始审核失败");
        writeAudit(reg, "START_REVIEW", reg.getRegistrationStatus(), RegistrationStatusEnum.REVIEWING.getCode(), "开始审核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, RegistrationAuditApproveReq req) {
        revalidate(id);
        List<BasketTournamentRegistrationPlayer> players = registrationPlayerMapper.selectByRegistrationId(id);
        long failCount = players.stream().filter(p -> !Objects.equals(p.getValidationStatus(), PASS)).count();
        if (failCount > 0) {
            throw new ServiceException("阵容校验未通过，无法批准");
        }
        BasketTournamentRegistration reg = requireRegistration(id);
        BasketTournamentSeason season = seasonMapper.selectByIdForUpdate(reg.getSeasonId());
        if (season == null) {
            throw new ServiceException("赛季不存在");
        }
        int approved = registrationMapper.countApprovedBySeasonId(reg.getSeasonId());
        if (season.getMaxTeamCount() != null && approved >= season.getMaxTeamCount()) {
            throw new ServiceException("该赛季报名队伍已满");
        }
        int rows = registrationMapper.updateStatusIfMatch(id,
                Arrays.asList(RegistrationStatusEnum.SUBMITTED.getCode(), RegistrationStatusEnum.REVIEWING.getCode()),
                RegistrationStatusEnum.APPROVED.getCode(), SecurityUtils.getUserId(), req.getAuditRemark(), new Date(), SecurityUtils.getUserId());
        assertSingleRow(rows, "审核通过失败");
        writeAudit(reg, "APPROVE", reg.getRegistrationStatus(), RegistrationStatusEnum.APPROVED.getCode(), req.getAuditRemark());
        participationGenerationService.generateFromTeamRegistration(id, SecurityUtils.getUserId(), req.getAuditRemark());
        messageService.sendSiteMessage(reg.getCaptainMemberId(), SecurityUtils.getUserId(), "REGISTRATION", id,
                "报名审核已通过", "您的赛事报名已通过审核。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, RegistrationAuditRejectReq req) {
        BasketTournamentRegistration reg = requireRegistration(id);
        String remark = StringUtils.hasText(req.getRejectReason()) ? req.getRejectReason() : req.getAuditRemark();
        int rows = registrationMapper.updateStatusIfMatch(id,
                Arrays.asList(RegistrationStatusEnum.SUBMITTED.getCode(), RegistrationStatusEnum.REVIEWING.getCode()),
                RegistrationStatusEnum.REJECTED.getCode(), SecurityUtils.getUserId(), remark, new Date(), SecurityUtils.getUserId());
        assertSingleRow(rows, "驳回失败");
        writeAudit(reg, "REJECT", reg.getRegistrationStatus(), RegistrationStatusEnum.REJECTED.getCode(), remark);
        messageService.sendSiteMessage(reg.getCaptainMemberId(), SecurityUtils.getUserId(), "REGISTRATION", id,
                "报名审核已驳回", remark);
    }

    @Override
    public List<RegistrationAuditRecordVo> selectAuditRecords(Long id) {
        return registrationAuditMapper.selectByRegistrationId(id).stream().map(r -> {
            RegistrationAuditRecordVo vo = new RegistrationAuditRecordVo();
            BeanUtils.copyBeanProp(vo, r);
            return vo;
        }).collect(Collectors.toList());
    }

    private String validatePlayer(BasketTournamentRegistrationPlayer player, Set<Long> disciplined, Long seasonId, String teamRestriction) {
        if (teamRestriction != null) {
            return teamRestriction;
        }
        if (player.getMemberId() != null && disciplined.contains(player.getMemberId())) {
            return "球员处于禁赛期";
        }
        if (player.getPlayerId() != null) {
            String playerRestriction = disciplineRestrictionSupport.checkRegistration("PLAYER", player.getPlayerId(), seasonId);
            if (playerRestriction != null) {
                return playerRestriction;
            }
        }
        if (player.getMemberId() != null) {
            String accountRestriction = disciplineRestrictionSupport.checkRegistration("ACCOUNT", player.getMemberId(), seasonId);
            if (accountRestriction != null) {
                return accountRestriction;
            }
        }
        BasketPlayerProfile profile = profileMapper.selectById(player.getPlayerId());
        if (profile == null || !Objects.equals(profile.getCertificationStatus(), PlayerCertStatusEnum.APPROVED.getCode())) {
            return "球员未完成认证";
        }
        return null;
    }

    private void writeAudit(BasketTournamentRegistration reg, String action, Integer before, Integer after, String remark) {
        Date now = new Date();
        BasketTournamentRegistrationAudit audit = new BasketTournamentRegistrationAudit();
        audit.setRegistrationId(reg.getId());
        audit.setAuditAction(action);
        audit.setBeforeStatus(before);
        audit.setAfterStatus(after);
        audit.setAuditorId(SecurityUtils.getUserId());
        audit.setAuditorName(SecurityUtils.getUsername());
        audit.setAuditRemark(remark);
        audit.setAuditTime(now);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        registrationAuditMapper.insert(audit);
    }

    private BasketTournamentRegistration requireRegistration(Long id) {
        BasketTournamentRegistration reg = registrationMapper.selectById(id);
        if (reg == null) {
            throw new ServiceException("报名记录不存在");
        }
        return reg;
    }

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ServiceException(message);
        }
    }
}
