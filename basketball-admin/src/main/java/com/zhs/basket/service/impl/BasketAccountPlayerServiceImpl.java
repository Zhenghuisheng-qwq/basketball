package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.CaptainQualificationStatusEnum;
import com.zhs.basket.enums.PlatformAccountStatusEnum;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.enums.TeamAuditStatusEnum;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.AccountPlayerQuery;
import com.zhs.basket.service.IBasketAccountPlayerService;
import com.zhs.basket.vo.*;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketAccountPlayerServiceImpl implements IBasketAccountPlayerService {

    @Autowired private BasketMemberAccountMapper memberAccountMapper;
    @Autowired private BasketPlayerPlatformAccountMapper platformAccountMapper;
    @Autowired private BasketPlayerCertApplicationMapper certApplicationMapper;
    @Autowired private BasketCaptainQualificationMapper captainQualificationMapper;
    @Autowired private BasketTeamMemberMapper teamMemberMapper;

    @Override
    public List<AccountPlayerListVo> selectList(AccountPlayerQuery query) {
        return memberAccountMapper.selectList(query).stream().peek(vo -> {
            if (vo.getCertificationStatus() != null) {
                vo.setCertificationStatusName(
                        PlayerCertStatusEnum.fromCode(vo.getCertificationStatus()).getDescription());
            }
        }).collect(Collectors.toList());
    }

    @Override
    public AccountPlayerFullDetailVo selectDetail(Long memberId) {
        AccountPlayerDetailVo base = memberAccountMapper.selectDetail(memberId);
        if (base == null) {
            throw new ServiceException("账号不存在");
        }
        AccountPlayerFullDetailVo detail = new AccountPlayerFullDetailVo();
        detail.setAccount(buildAccountSection(base));
        detail.setProfile(buildProfileSection(base));
        detail.setPlatformAccounts(loadPlatformAccounts(base.getPlayerId()));
        detail.setCertHistory(loadCertHistory(memberId));
        detail.setCaptainQual(loadCaptainQual(base.getPlayerId()));
        detail.setTeamSummary(loadTeamSummary(memberId));
        return detail;
    }

    private AccountPlayerAccountVo buildAccountSection(AccountPlayerDetailVo base) {
        AccountPlayerAccountVo account = new AccountPlayerAccountVo();
        account.setMemberId(base.getMemberId());
        account.setAccountNo(base.getAccountNo());
        account.setUsername(base.getUsername());
        account.setEmail(base.getEmail());
        account.setAccountStatus(base.getAccountStatus());
        account.setCreateTime(base.getAccountCreateTime());
        account.setUpdateTime(base.getAccountUpdateTime());
        return account;
    }

    private AccountPlayerProfileVo buildProfileSection(AccountPlayerDetailVo base) {
        AccountPlayerProfileVo profile = new AccountPlayerProfileVo();
        profile.setPlayerId(base.getPlayerId());
        profile.setMemberId(base.getMemberId());
        profile.setNickname(base.getNickname());
        profile.setGender(base.getGender());
        profile.setProfileStatus(base.getProfileStatus());
        profile.setPlayerNo(base.getPlayerNo());
        profile.setGameTerminal(base.getGameTerminal());
        profile.setGameAccount(base.getGameAccount());
        profile.setGameNickname(base.getGameNickname());
        profile.setPositionCode(base.getPositionCode());
        profile.setCertificationStatus(base.getCertificationStatus());
        if (base.getCertificationStatus() != null) {
            profile.setCertificationStatusName(
                    PlayerCertStatusEnum.fromCode(base.getCertificationStatus()).getDescription());
        }
        profile.setPlayerStatus(base.getPlayerStatus());
        profile.setCertifiedTime(base.getCertifiedTime());
        profile.setCurrentTeamId(base.getCurrentTeamId());
        return profile;
    }

    private List<AccountPlayerPlatformAccountVo> loadPlatformAccounts(Long playerId) {
        if (playerId == null) {
            return Collections.emptyList();
        }
        return platformAccountMapper.selectByPlayerId(playerId).stream().map(record -> {
            AccountPlayerPlatformAccountVo vo = new AccountPlayerPlatformAccountVo();
            BeanUtils.copyBeanProp(vo, record);
            vo.setAccountStatusName(PlatformAccountStatusEnum.fromCode(record.getAccountStatus()).getDescription());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<AccountPlayerCertHistoryVo> loadCertHistory(Long memberId) {
        return certApplicationMapper.selectByMemberId(memberId).stream().map(app -> {
            AccountPlayerCertHistoryVo vo = new AccountPlayerCertHistoryVo();
            vo.setApplicationId(app.getId());
            vo.setApplicationNo(app.getApplicationNo());
            vo.setGameTerminal(app.getGameTerminal());
            vo.setGameAccount(app.getGameAccount());
            vo.setApplicationStatus(app.getApplicationStatus());
            vo.setApplicationStatusName(PlayerCertStatusEnum.fromCode(app.getApplicationStatus()).getDescription());
            vo.setSubmittedTime(app.getSubmittedTime());
            vo.setReviewedTime(app.getReviewedTime());
            vo.setReviewRemark(app.getReviewRemark());
            return vo;
        }).collect(Collectors.toList());
    }

    private AccountPlayerCaptainQualVo loadCaptainQual(Long playerId) {
        if (playerId == null) {
            return null;
        }
        BasketCaptainQualification qual = captainQualificationMapper.selectCurrentByPlayerId(playerId);
        if (qual == null) {
            return null;
        }
        AccountPlayerCaptainQualVo vo = new AccountPlayerCaptainQualVo();
        vo.setQualificationId(qual.getId());
        vo.setQualificationNo(qual.getQualificationNo());
        vo.setQualificationStatus(qual.getQualificationStatus());
        vo.setQualificationStatusName(
                CaptainQualificationStatusEnum.fromCode(qual.getQualificationStatus()).getDescription());
        vo.setIsCurrent(qual.getIsCurrent());
        vo.setGrantedTime(qual.getGrantedTime());
        vo.setValidFrom(qual.getValidFrom());
        vo.setValidTo(qual.getValidTo());
        vo.setGrantReason(qual.getGrantReason());
        return vo;
    }

    private List<AccountPlayerTeamSummaryVo> loadTeamSummary(Long memberId) {
        return teamMemberMapper.selectTeamSummaryByMemberId(memberId).stream().peek(vo -> {
            if (vo.getAuditStatus() != null) {
                vo.setAuditStatusName(TeamAuditStatusEnum.fromCode(vo.getAuditStatus()).getDescription());
            }
        }).collect(Collectors.toList());
    }
}
