package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketCaptainQualification;
import com.zhs.basket.domain.BasketMemberAccount;
import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.enums.CaptainQualificationStatusEnum;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.enums.SeasonStatusEnum;
import com.zhs.basket.mapper.BasketCaptainQualificationMapper;
import com.zhs.basket.mapper.BasketMemberAccountMapper;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.query.CaptainQualificationQuery;
import com.zhs.basket.req.CaptainQualificationBindTeamReq;
import com.zhs.basket.req.CaptainQualificationGrantReq;
import com.zhs.basket.req.CaptainQualificationRevokeReq;
import com.zhs.basket.req.CaptainQualificationUpdateReq;
import com.zhs.basket.service.IBasketCaptainQualificationService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.CaptainGrantSeasonSupport;
import com.zhs.basket.support.CaptainQualificationValiditySupport;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.vo.CaptainGrantSeasonOptionVo;
import com.zhs.basket.vo.CaptainQualificationDetailVo;
import com.zhs.basket.vo.CaptainQualificationListVo;
import com.zhs.basket.vo.EligibleCaptainVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BasketCaptainQualificationServiceImpl implements IBasketCaptainQualificationService {

    private static final Integer NOT_DELETED = 0;
    private static final int CURRENT = 1;
    private static final int PAYMENT_CONFIRMED = 1;

    @Autowired private BasketCaptainQualificationMapper qualificationMapper;
    @Autowired private BasketPlayerProfileMapper profileMapper;
    @Autowired private BasketMemberAccountMapper memberAccountMapper;
    @Autowired private BasketTeamMapper teamMapper;
    @Autowired private BasketTournamentSeasonMapper seasonMapper;
    @Autowired private BasketTournamentMapper tournamentMapper;
    @Autowired private PlayerNoGenerator playerNoGenerator;
    @Autowired private BasketAdminMessageService messageService;

    @Override
    public List<EligibleCaptainVo> selectEligibleList() {
        return profileMapper.selectEligibleCaptainList().stream()
                .peek(vo -> vo.setUserId(vo.getMemberId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CaptainGrantSeasonOptionVo> selectGrantSeasonOptions() {
        return qualificationMapper.selectGrantableSeasonOptions().stream().peek(vo -> {
            if (vo.getSeasonStatus() != null) {
                vo.setSeasonStatusName(SeasonStatusEnum.fromCode(vo.getSeasonStatus()).getDescription());
            }
        }).collect(Collectors.toList());
    }

    @Override
    public List<CaptainQualificationListVo> selectList(CaptainQualificationQuery query) {
        Date now = new Date();
        return qualificationMapper.selectList(query).stream().map(q -> {
            CaptainQualificationListVo vo = new CaptainQualificationListVo();
            BeanUtils.copyBeanProp(vo, q);
            enrichSeasonTeamDisplay(vo, q.getSeasonId(), q.getTeamId());
            enrichAccountPlayerDisplay(vo.getMemberId(), vo.getPlayerId(), vo);
            enrichRuntimeValidity(vo, q, now);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public CaptainQualificationDetailVo selectDetail(Long qualificationId) {
        BasketCaptainQualification q = requireQualification(qualificationId);
        CaptainQualificationDetailVo vo = new CaptainQualificationDetailVo();
        BeanUtils.copyBeanProp(vo, q);
        enrichSeasonTeamDisplay(vo, q.getSeasonId(), q.getTeamId());
        enrichAccountPlayerDisplay(vo.getMemberId(), vo.getPlayerId(), vo);
        enrichRuntimeValidity(vo, q, new Date());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(CaptainQualificationGrantReq req) {
        if (req == null || req.getSeasonId() == null) {
            throw new ServiceException("请选择授权赛事");
        }
        if (req.getPlayerId() == null) {
            throw new ServiceException("请选择球员");
        }
        if (!Integer.valueOf(PAYMENT_CONFIRMED).equals(req.getPaymentConfirmed())) {
            throw new ServiceException("请先确认缴费后再授予队长权限");
        }

        BasketPlayerProfile profile = profileMapper.selectById(req.getPlayerId());
        if (profile == null || profile.getCertificationStatus() == null
                || profile.getCertificationStatus() != PlayerCertStatusEnum.APPROVED.getCode()) {
            throw new ServiceException("当前用户尚未完成球员认证，不能授予队长权限");
        }

        BasketTournamentSeason season = requireSeason(req.getSeasonId());
        CaptainGrantSeasonSupport.assertGrantableSeason(season);
        BasketTournament tournament = season.getTournamentId() == null
                ? null : tournamentMapper.selectById(season.getTournamentId());
        Date expireTime = CaptainGrantSeasonSupport.requireExpireTime(season, tournament);

        if (qualificationMapper.countActiveByMemberIdAndSeasonId(profile.getMemberId(), req.getSeasonId()) > 0) {
            throw new ServiceException("当前用户在该赛事下已拥有有效队长授权");
        }

        Long teamId = req.getTeamId();
        if (teamId != null) {
            validateBindTeam(profile.getMemberId(), teamId, req.getSeasonId());
            if (qualificationMapper.countActiveByTeamIdAndSeasonId(teamId, req.getSeasonId()) > 0) {
                throw new ServiceException("该球队在当前赛事下已存在有效队长授权");
            }
        }

        qualificationMapper.clearCurrentByMemberId(profile.getMemberId());

        Date now = new Date();
        BasketCaptainQualification record = new BasketCaptainQualification();
        record.setQualificationNo(playerNoGenerator.generateBizNo("CG"));
        record.setMemberId(profile.getMemberId());
        record.setPlayerId(profile.getId());
        record.setSeasonId(req.getSeasonId());
        record.setTeamId(teamId);
        record.setQualificationStatus(CaptainQualificationStatusEnum.ACTIVE.getCode());
        record.setIsCurrent(CURRENT);
        record.setPaymentConfirmed(PAYMENT_CONFIRMED);
        record.setPaymentReference(StringUtils.trim(req.getPaymentReference()));
        record.setGrantReason(StringUtils.trim(req.getGrantRemark()));
        record.setGrantedBy(SecurityUtils.getUserId());
        record.setGrantedTime(now);
        record.setValidFrom(now);
        record.setValidTo(expireTime);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        qualificationMapper.insert(record);

        String seasonLabel = buildSeasonLabel(season, tournament);
        messageService.sendSiteMessage(profile.getMemberId(), SecurityUtils.getUserId(), "CAPTAIN_GRANT", record.getId(),
                "队长权限已授予",
                "您已获得「" + seasonLabel + "」的队长权限，有效期至 " + formatMessageTime(expireTime) + "。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long qualificationId, CaptainQualificationUpdateReq req) {
        BasketCaptainQualification record = requireQualification(qualificationId);
        if (!Objects.equals(CaptainQualificationStatusEnum.ACTIVE.getCode(), record.getQualificationStatus())) {
            throw new ServiceException("仅有效授权可编辑备注与缴费信息");
        }
        record.setPaymentReference(StringUtils.trim(req.getPaymentReference()));
        record.setGrantReason(StringUtils.trim(req.getGrantRemark()));
        if (qualificationMapper.updateById(record) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long qualificationId, CaptainQualificationRevokeReq req) {
        if (req == null || !StringUtils.hasText(req.getRevokeReason())) {
            throw new ServiceException("收回原因不能为空");
        }
        BasketCaptainQualification record = requireQualification(qualificationId);
        if (!Objects.equals(CaptainQualificationStatusEnum.ACTIVE.getCode(), record.getQualificationStatus())) {
            throw new ServiceException("只能收回有效状态的队长授权");
        }
        Date now = new Date();
        record.setQualificationStatus(CaptainQualificationStatusEnum.REVOKED.getCode());
        record.setIsCurrent(0);
        record.setRevokedBy(SecurityUtils.getUserId());
        record.setRevokedTime(now);
        record.setRevokeReason(req.getRevokeReason().trim());
        if (qualificationMapper.updateById(record) != 1) {
            throw new ServiceException("收回失败");
        }
        messageService.sendSiteMessage(record.getMemberId(), SecurityUtils.getUserId(), "CAPTAIN_GRANT", record.getId(),
                "队长权限已收回",
                "您的当前赛事队长权限已被收回，历史球队和赛事记录仍可查看。原因：" + req.getRevokeReason().trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindTeam(Long qualificationId, CaptainQualificationBindTeamReq req) {
        if (req == null || req.getTeamId() == null) {
            throw new ServiceException("请选择关联球队");
        }
        BasketCaptainQualification record = requireQualification(qualificationId);
        if (!Objects.equals(CaptainQualificationStatusEnum.ACTIVE.getCode(), record.getQualificationStatus())) {
            throw new ServiceException("仅有效授权可绑定球队");
        }
        if (record.getTeamId() != null) {
            throw new ServiceException("授权已绑定球队");
        }
        validateBindTeam(record.getMemberId(), req.getTeamId(), record.getSeasonId());
        if (qualificationMapper.countActiveByTeamIdAndSeasonId(req.getTeamId(), record.getSeasonId()) > 0) {
            throw new ServiceException("该球队在当前赛事下已存在有效队长授权");
        }
        record.setTeamId(req.getTeamId());
        if (qualificationMapper.updateById(record) != 1) {
            throw new ServiceException("绑定球队失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireBySeasonFinish(Long seasonId) {
        List<BasketCaptainQualification> activeGrants = qualificationMapper.selectActiveBySeasonId(seasonId);
        if (activeGrants.isEmpty()) {
            return;
        }
        Date now = new Date();
        Long operatorId = SecurityUtils.getUserId();
        for (BasketCaptainQualification grant : activeGrants) {
            grant.setQualificationStatus(CaptainQualificationStatusEnum.EXPIRED.getCode());
            grant.setIsCurrent(0);
            grant.setRevokedTime(now);
            grant.setRevokeReason("赛事周期已结束，队长权限自动失效");
            qualificationMapper.updateById(grant);
            messageService.sendSiteMessage(grant.getMemberId(), operatorId, "CAPTAIN_GRANT", grant.getId(),
                    "队长权限已失效",
                    "当前赛事周期已结束，您的队长权限已自动失效，历史球队和赛事记录仍可查看。");
        }
    }

    private void validateBindTeam(Long memberId, Long teamId, Long seasonId) {
        BasketTeam team = teamMapper.selectById(teamId);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        if (!memberId.equals(team.getCaptainMemberId())) {
            throw new ServiceException("仅可绑定本人担任队长的球队");
        }
    }

    private BasketTournamentSeason requireSeason(Long seasonId) {
        BasketTournamentSeason season = seasonMapper.selectById(seasonId);
        if (season == null || !NOT_DELETED.equals(season.getIsDelete())) {
            throw new ServiceException("赛事不存在");
        }
        return season;
    }

    private BasketCaptainQualification requireQualification(Long id) {
        BasketCaptainQualification q = qualificationMapper.selectById(id);
        if (q == null) {
            throw new ServiceException("队长授权记录不存在");
        }
        return q;
    }

    private void enrichSeasonTeamDisplay(CaptainQualificationListVo vo, Long seasonId, Long teamId) {
        if (seasonId != null) {
            BasketTournamentSeason season = seasonMapper.selectById(seasonId);
            if (season != null) {
                vo.setSeasonName(season.getSeasonName());
                if (season.getTournamentId() != null) {
                    BasketTournament tournament = tournamentMapper.selectById(season.getTournamentId());
                    if (tournament != null) {
                        vo.setTournamentName(tournament.getTournamentName());
                    }
                }
            }
        }
        if (teamId != null) {
            BasketTeam team = teamMapper.selectById(teamId);
            if (team != null) {
                vo.setTeamName(team.getTeamName());
            }
        }
    }

    private void enrichSeasonTeamDisplay(CaptainQualificationDetailVo vo, Long seasonId, Long teamId) {
        if (seasonId != null) {
            BasketTournamentSeason season = seasonMapper.selectById(seasonId);
            if (season != null) {
                vo.setSeasonName(season.getSeasonName());
                if (season.getTournamentId() != null) {
                    BasketTournament tournament = tournamentMapper.selectById(season.getTournamentId());
                    if (tournament != null) {
                        vo.setTournamentName(tournament.getTournamentName());
                    }
                }
            }
        }
        if (teamId != null) {
            BasketTeam team = teamMapper.selectById(teamId);
            if (team != null) {
                vo.setTeamName(team.getTeamName());
            }
        }
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
        Integer seasonStatus = null;
        if (q.getPlayerId() != null) {
            BasketPlayerProfile profile = profileMapper.selectById(q.getPlayerId());
            if (profile != null) {
                certStatus = profile.getCertificationStatus();
            }
        }
        if (q.getSeasonId() != null) {
            BasketTournamentSeason season = seasonMapper.selectById(q.getSeasonId());
            if (season != null) {
                seasonStatus = season.getSeasonStatus();
            }
        }
        String invalidReason = CaptainQualificationValiditySupport.resolveInvalidReason(q, certStatus, now, seasonStatus);
        vo.setRuntimeValid(invalidReason == null ? 1 : 0);
        vo.setRuntimeInvalidReason(invalidReason);
    }

    private void enrichRuntimeValidity(CaptainQualificationDetailVo vo, BasketCaptainQualification q, Date now) {
        Integer certStatus = null;
        Integer seasonStatus = null;
        if (q.getPlayerId() != null) {
            BasketPlayerProfile profile = profileMapper.selectById(q.getPlayerId());
            if (profile != null) {
                certStatus = profile.getCertificationStatus();
            }
        }
        if (q.getSeasonId() != null) {
            BasketTournamentSeason season = seasonMapper.selectById(q.getSeasonId());
            if (season != null) {
                seasonStatus = season.getSeasonStatus();
            }
        }
        String invalidReason = CaptainQualificationValiditySupport.resolveInvalidReason(q, certStatus, now, seasonStatus);
        vo.setRuntimeValid(invalidReason == null ? 1 : 0);
        vo.setRuntimeInvalidReason(invalidReason);
    }

    private String buildSeasonLabel(BasketTournamentSeason season, BasketTournament tournament) {
        if (tournament != null && StringUtils.hasText(tournament.getTournamentName())) {
            return tournament.getTournamentName() + " / " + season.getSeasonName();
        }
        return season.getSeasonName();
    }

    private String formatMessageTime(Date date) {
        if (date == null) {
            return "-";
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
