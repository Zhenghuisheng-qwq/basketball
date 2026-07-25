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
import com.zhs.basket.support.PlayerPositionSupport;
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
    private static final List<Integer> REVIEWABLE_STATUSES = Arrays.asList(
            RegistrationStatusEnum.REVIEWING.getCode(),
            RegistrationStatusEnum.SUBMITTED.getCode());

    @Autowired private BasketTournamentRegistrationMapper registrationMapper;
    @Autowired private BasketTournamentRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired private BasketTournamentRegistrationAuditMapper registrationAuditMapper;
    @Autowired private BasketTournamentSeasonMapper seasonMapper;
    @Autowired private BasketTournamentMapper tournamentMapper;
    @Autowired private BasketTeamMapper teamMapper;
    @Autowired private BasketPlayerProfileMapper profileMapper;
    @Autowired private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Autowired private BasketDisciplineRestrictionSupport disciplineRestrictionSupport;
    @Autowired private BasketAdminMessageService messageService;
    @Autowired private IBasketParticipationGenerationService participationGenerationService;

    @Override
    public List<RegistrationAuditListVo> selectList(RegistrationAuditQuery query) {
        return registrationMapper.selectAuditList(query).stream().peek(vo -> {
            RegistrationStatusEnum status = RegistrationStatusEnum.fromCode(vo.getRegistrationStatus());
            vo.setRegistrationStatusName(status.getAuditDisplayDescription());
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationAuditDetailVo selectDetail(Long id) {
        revalidate(id);
        BasketTournamentRegistration reg = requireRegistration(id);
        RegistrationAuditDetailVo vo = new RegistrationAuditDetailVo();
        BeanUtils.copyBeanProp(vo, reg);
        RegistrationStatusEnum status = RegistrationStatusEnum.fromCode(reg.getRegistrationStatus());
        vo.setRegistrationStatusName(status.getAuditDisplayDescription());
        if (Objects.equals(status, RegistrationStatusEnum.REJECTED)) {
            vo.setRejectReason(reg.getRemark());
        }
        enrichRegistrationNames(vo, reg);
        vo.setPlayers(registrationPlayerMapper.selectDetailByRegistrationId(id).stream()
                .peek(this::enrichPlayerVo)
                .collect(Collectors.toList()));
        return vo;
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
        String auditRemark = req == null ? null : req.getAuditRemark();
        int rows = registrationMapper.updateStatusIfMatch(id,
                REVIEWABLE_STATUSES,
                RegistrationStatusEnum.APPROVED.getCode(), SecurityUtils.getUserId(), auditRemark, new Date(), SecurityUtils.getUserId());
        assertSingleRow(rows, "审核通过失败");
        writeAudit(reg, "APPROVE", reg.getRegistrationStatus(), RegistrationStatusEnum.APPROVED.getCode(), auditRemark);
        participationGenerationService.generateFromTeamRegistration(id, SecurityUtils.getUserId(), auditRemark);
        messageService.sendSiteMessage(reg.getCaptainMemberId(), SecurityUtils.getUserId(), "REGISTRATION", id,
                "报名审核已通过", "您的赛事报名已通过审核。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, RegistrationAuditRejectReq req) {
        if (req == null || !StringUtils.hasText(req.getRejectReason())) {
            throw new ServiceException("驳回原因不能为空");
        }
        BasketTournamentRegistration reg = requireRegistration(id);
        String remark = req.getRejectReason().trim();
        if (StringUtils.hasText(req.getAuditRemark())) {
            remark = remark + "；" + req.getAuditRemark().trim();
        }
        int rows = registrationMapper.updateStatusIfMatch(id,
                REVIEWABLE_STATUSES,
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

    private void revalidate(Long id) {
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
        for (BasketTournamentRegistrationPlayer player : players) {
            String reason = validatePlayer(player, disciplined, reg.getSeasonId(), teamRestriction);
            int status = reason == null ? PASS : FAIL;
            registrationPlayerMapper.updateValidationStatus(player.getId(), status, reason);
        }
    }

    private void enrichPlayerVo(RegistrationPlayerVo player) {
        player.setUserId(player.getMemberId());
        player.setPositionName(PlayerPositionSupport.resolveName(player.getPositionCode()));
        player.setPositionText(PlayerPositionSupport.formatText(player.getPositionCode()));
        player.setValidationStatusName(resolveValidationStatusName(player.getValidationStatus()));
    }

    private String resolveValidationStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        if (Objects.equals(status, PASS)) {
            return "通过";
        }
        if (Objects.equals(status, FAIL)) {
            return "未通过";
        }
        return "待校验";
    }

    private void enrichRegistrationNames(RegistrationAuditDetailVo vo, BasketTournamentRegistration reg) {
        if (reg.getTeamId() != null) {
            BasketTeam team = teamMapper.selectById(reg.getTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamName());
            }
        }
        if (reg.getSeasonId() != null) {
            BasketTournamentSeason season = seasonMapper.selectById(reg.getSeasonId());
            if (season != null && season.getTournamentId() != null) {
                BasketTournament tournament = tournamentMapper.selectById(season.getTournamentId());
                if (tournament != null) {
                    vo.setTournamentName(tournament.getTournamentName());
                }
            }
        }
        if (reg.getCaptainPlayerId() != null) {
            BasketPlayerProfile captainProfile = profileMapper.selectById(reg.getCaptainPlayerId());
            if (captainProfile != null && StringUtils.hasText(captainProfile.getGameNickname())) {
                vo.setCaptainName(captainProfile.getGameNickname());
            }
        }
        if (!StringUtils.hasText(vo.getCaptainName())) {
            vo.setCaptainName(reg.getContactName());
        }
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
