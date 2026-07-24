# -*- coding: utf-8 -*-
"""Service interface and impl generation for Round 4."""


def generate(w):
    w(
        "src/main/java/com/zhs/basket/service/IBasketPlayerCertService.java",
        """package com.zhs.basket.service;

import com.zhs.basket.query.PlayerCertQuery;
import com.zhs.basket.req.PlayerCertApproveReq;
import com.zhs.basket.req.PlayerCertRejectReq;
import com.zhs.basket.vo.PlayerCertAuditRecordVo;
import com.zhs.basket.vo.PlayerCertDetailVo;
import com.zhs.basket.vo.PlayerCertListVo;
import java.util.List;

public interface IBasketPlayerCertService {
    List<PlayerCertListVo> selectList(PlayerCertQuery query);
    PlayerCertDetailVo selectDetail(Long applicationId);
    List<PlayerCertAuditRecordVo> selectAuditRecords(Long applicationId);
    void startReview(Long applicationId);
    void approve(Long applicationId, PlayerCertApproveReq req);
    void reject(Long applicationId, PlayerCertRejectReq req);
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/impl/BasketPlayerCertServiceImpl.java",
        """package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.PlayerCertQuery;
import com.zhs.basket.req.PlayerCertApproveReq;
import com.zhs.basket.req.PlayerCertRejectReq;
import com.zhs.basket.service.IBasketPlayerCertService;
import com.zhs.basket.support.BasketAccountSecurityLogWriter;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.PlayerNoGenerator;
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
public class BasketPlayerCertServiceImpl implements IBasketPlayerCertService {

    private static final Integer NOT_DELETED = 0;
    private static final int CERT_APPROVED = PlayerCertStatusEnum.APPROVED.getCode();

    @Autowired private BasketPlayerCertApplicationMapper applicationMapper;
    @Autowired private BasketPlayerCertAuditMapper auditMapper;
    @Autowired private BasketPlayerCertMaterialMapper materialMapper;
    @Autowired private BasketPlayerProfileMapper profileMapper;
    @Autowired private PlayerNoGenerator playerNoGenerator;
    @Autowired private BasketAdminMessageService messageService;
    @Autowired private BasketAccountSecurityLogWriter securityLogWriter;

    @Override
    public List<PlayerCertListVo> selectList(PlayerCertQuery query) {
        return applicationMapper.selectList(query).stream().map(this::toListVo).collect(Collectors.toList());
    }

    @Override
    public PlayerCertDetailVo selectDetail(Long applicationId) {
        BasketPlayerCertApplication app = requireApplication(applicationId);
        PlayerCertDetailVo vo = new PlayerCertDetailVo();
        BeanUtils.copyBeanProp(vo, app);
        vo.setApplicationStatusName(PlayerCertStatusEnum.fromCode(app.getApplicationStatus()).getDescription());
        return vo;
    }

    @Override
    public List<PlayerCertAuditRecordVo> selectAuditRecords(Long applicationId) {
        return auditMapper.selectByApplicationId(applicationId).stream().map(r -> {
            PlayerCertAuditRecordVo vo = new PlayerCertAuditRecordVo();
            BeanUtils.copyBeanProp(vo, r);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startReview(Long applicationId) {
        BasketPlayerCertApplication app = requireApplication(applicationId);
        Long auditorId = SecurityUtils.getUserId();
        int rows = applicationMapper.updateStatusIfMatch(applicationId,
                Collections.singletonList(PlayerCertStatusEnum.SUBMITTED.getCode()),
                PlayerCertStatusEnum.REVIEWING.getCode(), auditorId, null, null, null);
        assertSingleRow(rows, "开始审核失败，状态已变更");
        writeAudit(app, "START_REVIEW", app.getApplicationStatus(), PlayerCertStatusEnum.REVIEWING.getCode(), "开始审核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long applicationId, PlayerCertApproveReq req) {
        BasketPlayerCertApplication app = requireApplication(applicationId);
        Long auditorId = SecurityUtils.getUserId();
        Date now = new Date();
        int rows = applicationMapper.updateStatusIfMatch(applicationId,
                Arrays.asList(PlayerCertStatusEnum.SUBMITTED.getCode(), PlayerCertStatusEnum.REVIEWING.getCode()),
                PlayerCertStatusEnum.APPROVED.getCode(), auditorId, req.getAuditRemark(), now, auditorId);
        assertSingleRow(rows, "审核通过失败，状态已变更");
        upsertProfile(app, req.getPositionCode(), now);
        writeAudit(app, "APPROVE", app.getApplicationStatus(), PlayerCertStatusEnum.APPROVED.getCode(), req.getAuditRemark());
        messageService.sendSiteMessage(app.getMemberId(), auditorId, "PLAYER_CERT", applicationId,
                "球员认证已通过", "您的球员认证申请已通过审核。");
        securityLogWriter.write(app.getMemberId(), "PLAYER_CERT_APPROVED", "球员认证通过", null, "已通过", auditorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long applicationId, PlayerCertRejectReq req) {
        BasketPlayerCertApplication app = requireApplication(applicationId);
        Long auditorId = SecurityUtils.getUserId();
        String remark = StringUtils.hasText(req.getRejectReason()) ? req.getRejectReason() : req.getAuditRemark();
        int rows = applicationMapper.updateStatusIfMatch(applicationId,
                Collections.singletonList(PlayerCertStatusEnum.REVIEWING.getCode()),
                PlayerCertStatusEnum.REJECTED.getCode(), auditorId, remark, new Date(), auditorId);
        assertSingleRow(rows, "驳回失败，状态已变更");
        writeAudit(app, "REJECT", app.getApplicationStatus(), PlayerCertStatusEnum.REJECTED.getCode(), remark);
        messageService.sendSiteMessage(app.getMemberId(), auditorId, "PLAYER_CERT", applicationId,
                "球员认证已驳回", "您的球员认证申请已被驳回：" + remark);
        securityLogWriter.write(app.getMemberId(), "PLAYER_CERT_REJECTED", "球员认证驳回", null, remark, auditorId);
    }

    private void upsertProfile(BasketPlayerCertApplication app, String positionCode, Date certifiedTime) {
        BasketPlayerProfile profile = profileMapper.selectByMemberId(app.getMemberId());
        if (profile == null) {
            profile = new BasketPlayerProfile();
            profile.setMemberId(app.getMemberId());
            profile.setPlayerNo(playerNoGenerator.generatePlayerNo());
            profile.setIsDelete(NOT_DELETED);
        } else if (!StringUtils.hasText(profile.getPlayerNo())) {
            profile.setPlayerNo(playerNoGenerator.generatePlayerNo());
        }
        profile.setRealName(app.getRealName());
        profile.setIdType(app.getIdType());
        profile.setIdNoCipher(app.getIdNoCipher());
        profile.setIdNoMasked(app.getIdNoMasked());
        profile.setGameTerminal(app.getGameTerminal());
        profile.setGameAccount(app.getGameAccount());
        profile.setGameNickname(app.getGameNickname());
        profile.setPositionCode(StringUtils.hasText(positionCode) ? positionCode : app.getPositionCode());
        profile.setCertificationStatus(CERT_APPROVED);
        profile.setCertifiedTime(certifiedTime);
        if (profile.getId() == null) {
            profileMapper.insert(profile);
        } else {
            profileMapper.updateById(profile);
        }
    }

    private void writeAudit(BasketPlayerCertApplication app, String action, Integer before, Integer after, String remark) {
        BasketPlayerCertAudit audit = new BasketPlayerCertAudit();
        audit.setApplicationId(app.getId());
        audit.setAuditAction(action);
        audit.setBeforeStatus(before);
        audit.setAfterStatus(after);
        audit.setAuditorId(SecurityUtils.getUserId());
        audit.setAuditorName(SecurityUtils.getUsername());
        audit.setAuditRemark(remark);
        audit.setAuditTime(new Date());
        audit.setIsDelete(NOT_DELETED);
        auditMapper.insert(audit);
    }

    private BasketPlayerCertApplication requireApplication(Long id) {
        BasketPlayerCertApplication app = applicationMapper.selectById(id);
        if (app == null) {
            throw new ServiceException("认证申请不存在");
        }
        return app;
    }

    private PlayerCertListVo toListVo(BasketPlayerCertApplication app) {
        PlayerCertListVo vo = new PlayerCertListVo();
        BeanUtils.copyBeanProp(vo, app);
        vo.setApplicationStatusName(PlayerCertStatusEnum.fromCode(app.getApplicationStatus()).getDescription());
        return vo;
    }

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ServiceException(message);
        }
    }
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/IBasketPrivateFileService.java",
        """package com.zhs.basket.service;

import jakarta.servlet.http.HttpServletResponse;

public interface IBasketPrivateFileService {
    void streamCertMaterialFile(Long fileId, HttpServletResponse response);
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/impl/BasketPrivateFileServiceImpl.java",
        """package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketFileAsset;
import com.zhs.basket.mapper.BasketFileAssetMapper;
import com.zhs.basket.mapper.BasketPlayerCertMaterialMapper;
import com.zhs.basket.service.IBasketPrivateFileService;
import com.zhs.common.config.RuoYiConfig;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.file.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class BasketPrivateFileServiceImpl implements IBasketPrivateFileService {

    @Autowired private BasketFileAssetMapper fileAssetMapper;
    @Autowired private BasketPlayerCertMaterialMapper materialMapper;

    @Value("${CNPA_FILE_ROOT:./upload}")
    private String cnpaFileRoot;

    @Override
    public void streamCertMaterialFile(Long fileId, HttpServletResponse response) {
        if (materialMapper.countByFileIdForCert(fileId) <= 0) {
            throw new ServiceException("无权访问该文件");
        }
        BasketFileAsset asset = fileAssetMapper.selectById(fileId);
        if (asset == null) {
            throw new ServiceException("文件不存在");
        }
        try {
            byte[] bytes = readBytes(asset);
            String fileName = StringUtils.hasText(asset.getOriginalName()) ? asset.getOriginalName() : asset.getFileName();
            response.setContentType(StringUtils.hasText(asset.getMimeType()) ? asset.getMimeType() : "application/octet-stream");
            FileUtils.setAttachmentResponseHeader(response, fileName);
            response.getOutputStream().write(bytes);
        } catch (IOException e) {
            throw new ServiceException("读取文件失败");
        }
    }

    private byte[] readBytes(BasketFileAsset asset) throws IOException {
        if (StringUtils.hasText(asset.getAccessUrl()) && asset.getAccessUrl().startsWith("http")) {
            try (InputStream in = new URL(asset.getAccessUrl()).openStream()) {
                return in.readAllBytes();
            }
        }
        Path path = resolveLocalPath(asset);
        if (Files.exists(path)) {
            return Files.readAllBytes(path);
        }
        throw new ServiceException("文件不存在");
    }

    private Path resolveLocalPath(BasketFileAsset asset) {
        String objectKey = asset.getObjectKey();
        if (!StringUtils.hasText(objectKey)) {
            throw new ServiceException("文件路径无效");
        }
        Path[] roots = new Path[] {
                Paths.get(cnpaFileRoot).toAbsolutePath().normalize(),
                Paths.get(RuoYiConfig.getProfile()).toAbsolutePath().normalize(),
                Paths.get(RuoYiConfig.getUploadPath()).toAbsolutePath().normalize()
        };
        for (Path root : roots) {
            Path target = root.resolve(objectKey).normalize();
            if (target.startsWith(root) && Files.exists(target)) {
                return target;
            }
        }
        return roots[0].resolve(objectKey).normalize();
    }
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/IBasketCaptainQualificationService.java",
        """package com.zhs.basket.service;

import com.zhs.basket.query.CaptainQualificationQuery;
import com.zhs.basket.req.CaptainQualificationGrantReq;
import com.zhs.basket.req.CaptainQualificationRevokeReq;
import com.zhs.basket.req.CaptainQualificationUpdateReq;
import com.zhs.basket.vo.CaptainQualificationDetailVo;
import com.zhs.basket.vo.CaptainQualificationListVo;
import com.zhs.basket.vo.EligibleCaptainVo;
import java.util.List;

public interface IBasketCaptainQualificationService {
    List<EligibleCaptainVo> selectEligibleList();
    List<CaptainQualificationListVo> selectList(CaptainQualificationQuery query);
    CaptainQualificationDetailVo selectDetail(Long qualificationId);
    void grant(CaptainQualificationGrantReq req);
    void update(Long qualificationId, CaptainQualificationUpdateReq req);
    void revoke(Long qualificationId, CaptainQualificationRevokeReq req);
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/impl/BasketCaptainQualificationServiceImpl.java",
        """package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketCaptainQualification;
import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.enums.CaptainQualificationStatusEnum;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.mapper.BasketCaptainQualificationMapper;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.BasketTeamMemberMapper;
import com.zhs.basket.query.CaptainQualificationQuery;
import com.zhs.basket.req.CaptainQualificationGrantReq;
import com.zhs.basket.req.CaptainQualificationRevokeReq;
import com.zhs.basket.req.CaptainQualificationUpdateReq;
import com.zhs.basket.service.IBasketCaptainQualificationService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.PlayerNoGenerator;
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
    @Autowired private BasketTeamMemberMapper teamMemberMapper;
    @Autowired private PlayerNoGenerator playerNoGenerator;
    @Autowired private BasketAdminMessageService messageService;

    @Override
    public List<EligibleCaptainVo> selectEligibleList() {
        return profileMapper.selectEligibleForCaptain().stream().map(p -> {
            EligibleCaptainVo vo = new EligibleCaptainVo();
            vo.setPlayerId(p.getId());
            vo.setMemberId(p.getMemberId());
            vo.setPlayerNo(p.getPlayerNo());
            vo.setRealName(p.getRealName());
            vo.setCertificationStatus(p.getCertificationStatus());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CaptainQualificationListVo> selectList(CaptainQualificationQuery query) {
        return qualificationMapper.selectList(query).stream().map(q -> {
            CaptainQualificationListVo vo = new CaptainQualificationListVo();
            BeanUtils.copyBeanProp(vo, q);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public CaptainQualificationDetailVo selectDetail(Long qualificationId) {
        BasketCaptainQualification q = requireQualification(qualificationId);
        CaptainQualificationDetailVo vo = new CaptainQualificationDetailVo();
        BeanUtils.copyBeanProp(vo, q);
        return vo;
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
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/IBasketTeamAuditService.java",
        """package com.zhs.basket.service;

import com.zhs.basket.query.TeamAuditQuery;
import com.zhs.basket.req.TeamAuditApproveReq;
import com.zhs.basket.req.TeamAuditRejectReq;
import com.zhs.basket.vo.TeamAuditDetailVo;
import com.zhs.basket.vo.TeamAuditListVo;
import com.zhs.basket.vo.TeamAuditRecordVo;
import java.util.List;

public interface IBasketTeamAuditService {
    List<TeamAuditListVo> selectList(TeamAuditQuery query);
    TeamAuditDetailVo selectDetail(Long teamId);
    List<TeamAuditRecordVo> selectAuditRecords(Long teamId);
    void startReview(Long teamId);
    void approve(Long teamId, TeamAuditApproveReq req);
    void reject(Long teamId, TeamAuditRejectReq req);
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/impl/BasketTeamAuditServiceImpl.java",
        """package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.CaptainQualificationStatusEnum;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.enums.TeamAuditStatusEnum;
import com.zhs.basket.enums.TeamMemberRoleEnum;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.TeamAuditQuery;
import com.zhs.basket.req.TeamAuditApproveReq;
import com.zhs.basket.req.TeamAuditRejectReq;
import com.zhs.basket.service.IBasketTeamAuditService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.vo.TeamAuditDetailVo;
import com.zhs.basket.vo.TeamAuditListVo;
import com.zhs.basket.vo.TeamAuditRecordVo;
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
public class BasketTeamAuditServiceImpl implements IBasketTeamAuditService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTeamMapper teamMapper;
    @Autowired private BasketTeamAuditRecordMapper auditRecordMapper;
    @Autowired private BasketTeamMemberMapper teamMemberMapper;
    @Autowired private BasketPlayerProfileMapper profileMapper;
    @Autowired private BasketCaptainQualificationMapper qualificationMapper;
    @Autowired private BasketAdminMessageService messageService;

    @Override
    public List<TeamAuditListVo> selectList(TeamAuditQuery query) {
        return teamMapper.selectAuditList(query).stream().map(t -> {
            TeamAuditListVo vo = new TeamAuditListVo();
            BeanUtils.copyBeanProp(vo, t);
            vo.setAuditStatusName(TeamAuditStatusEnum.fromCode(t.getAuditStatus()).getDescription());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public TeamAuditDetailVo selectDetail(Long teamId) {
        BasketTeam team = requireTeam(teamId);
        TeamAuditDetailVo vo = new TeamAuditDetailVo();
        BeanUtils.copyBeanProp(vo, team);
        vo.setAuditStatusName(TeamAuditStatusEnum.fromCode(team.getAuditStatus()).getDescription());
        return vo;
    }

    @Override
    public List<TeamAuditRecordVo> selectAuditRecords(Long teamId) {
        return auditRecordMapper.selectByTeamId(teamId).stream().map(r -> {
            TeamAuditRecordVo vo = new TeamAuditRecordVo();
            BeanUtils.copyBeanProp(vo, r);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startReview(Long teamId) {
        BasketTeam team = requireTeam(teamId);
        int rows = teamMapper.updateAuditStatusIfMatch(teamId,
                Collections.singletonList(TeamAuditStatusEnum.PENDING.getCode()),
                TeamAuditStatusEnum.REVIEWING.getCode(), SecurityUtils.getUserId());
        assertSingleRow(rows, "开始审核失败");
        writeAudit(team, "START_REVIEW", team.getAuditStatus(), TeamAuditStatusEnum.REVIEWING.getCode(), "开始审核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long teamId, TeamAuditApproveReq req) {
        BasketTeam team = requireTeam(teamId);
        validateCaptainForApprove(team);
        int rows = teamMapper.updateAuditStatusIfMatch(teamId,
                Arrays.asList(TeamAuditStatusEnum.PENDING.getCode(), TeamAuditStatusEnum.REVIEWING.getCode()),
                TeamAuditStatusEnum.APPROVED.getCode(), SecurityUtils.getUserId());
        assertSingleRow(rows, "审核通过失败");
        int memberCount = teamMemberMapper.countCurrentMembers(teamId);
        teamMapper.updateMemberCount(teamId, memberCount);
        writeAudit(team, "APPROVE", team.getAuditStatus(), TeamAuditStatusEnum.APPROVED.getCode(), req.getAuditRemark());
        messageService.sendSiteMessage(team.getCaptainMemberId(), SecurityUtils.getUserId(), "TEAM_AUDIT", teamId,
                "球队审核已通过", "您的球队「" + team.getTeamName() + "」已通过审核。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long teamId, TeamAuditRejectReq req) {
        BasketTeam team = requireTeam(teamId);
        String remark = StringUtils.hasText(req.getRejectReason()) ? req.getRejectReason() : req.getAuditRemark();
        int rows = teamMapper.updateAuditStatusIfMatch(teamId,
                Arrays.asList(TeamAuditStatusEnum.PENDING.getCode(), TeamAuditStatusEnum.REVIEWING.getCode()),
                TeamAuditStatusEnum.REJECTED.getCode(), SecurityUtils.getUserId());
        assertSingleRow(rows, "驳回失败");
        writeAudit(team, "REJECT", team.getAuditStatus(), TeamAuditStatusEnum.REJECTED.getCode(), remark);
        messageService.sendSiteMessage(team.getCaptainMemberId(), SecurityUtils.getUserId(), "TEAM_AUDIT", teamId,
                "球队审核已驳回", remark);
    }

    private void validateCaptainForApprove(BasketTeam team) {
        BasketTeamMember captain = teamMemberMapper.selectCaptainMember(team.getId());
        if (captain == null || !TeamMemberRoleEnum.CAPTAIN.getCode().equals(captain.getRoleType())) {
            throw new ServiceException("缺少有效队长成员");
        }
        BasketPlayerProfile profile = profileMapper.selectById(captain.getPlayerId());
        if (profile == null || !Objects.equals(profile.getCertificationStatus(), PlayerCertStatusEnum.APPROVED.getCode())) {
            throw new ServiceException("队长未完成球员认证");
        }
        if (qualificationMapper.countCurrentActiveByPlayerId(captain.getPlayerId()) <= 0) {
            throw new ServiceException("队长缺少有效队长资格");
        }
    }

    private void writeAudit(BasketTeam team, String action, Integer before, Integer after, String remark) {
        BasketTeamAuditRecord record = new BasketTeamAuditRecord();
        record.setTeamId(team.getId());
        record.setAuditType("TEAM_CREATE");
        record.setAuditAction(action);
        record.setBeforeStatus(before);
        record.setAfterStatus(after);
        record.setAuditorId(SecurityUtils.getUserId());
        record.setAuditorName(SecurityUtils.getUsername());
        record.setAuditRemark(remark);
        record.setAuditTime(new Date());
        record.setIsDelete(NOT_DELETED);
        auditRecordMapper.insert(record);
    }

    private BasketTeam requireTeam(Long id) {
        BasketTeam team = teamMapper.selectById(id);
        if (team == null) {
            throw new ServiceException("球队不存在");
        }
        return team;
    }

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ServiceException(message);
        }
    }
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/IBasketTournamentService.java",
        """package com.zhs.basket.service;

import com.zhs.basket.query.TournamentQuery;
import com.zhs.basket.req.TournamentCreateReq;
import com.zhs.basket.req.TournamentUpdateReq;
import com.zhs.basket.vo.TournamentDetailVo;
import com.zhs.basket.vo.TournamentListVo;
import java.util.List;

public interface IBasketTournamentService {
    List<TournamentListVo> selectList(TournamentQuery query);
    TournamentDetailVo selectById(Long id);
    Long create(TournamentCreateReq req);
    void update(Long id, TournamentUpdateReq req);
    void publish(Long id);
    void unpublish(Long id);
    void close(Long id);
    void delete(Long id);
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/impl/BasketTournamentServiceImpl.java",
        """package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.enums.TournamentStatusEnum;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.query.TournamentQuery;
import com.zhs.basket.req.TournamentCreateReq;
import com.zhs.basket.req.TournamentUpdateReq;
import com.zhs.basket.service.IBasketTournamentService;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.vo.TournamentDetailVo;
import com.zhs.basket.vo.TournamentListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketTournamentServiceImpl implements IBasketTournamentService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentMapper tournamentMapper;
    @Autowired private PlayerNoGenerator playerNoGenerator;

    @Override
    public List<TournamentListVo> selectList(TournamentQuery query) {
        return tournamentMapper.selectList(query).stream().map(this::toListVo).collect(Collectors.toList());
    }

    @Override
    public TournamentDetailVo selectById(Long id) {
        BasketTournament t = requireTournament(id);
        TournamentDetailVo vo = new TournamentDetailVo();
        BeanUtils.copyBeanProp(vo, t);
        vo.setTournamentStatusName(TournamentStatusEnum.fromCode(t.getTournamentStatus()).getDescription());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TournamentCreateReq req) {
        BasketTournament t = new BasketTournament();
        BeanUtils.copyBeanProp(t, req);
        t.setTournamentNo(playerNoGenerator.generateBizNo("TN"));
        t.setTournamentStatus(TournamentStatusEnum.DRAFT.getCode());
        t.setIsDelete(NOT_DELETED);
        tournamentMapper.insert(t);
        return t.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TournamentUpdateReq req) {
        BasketTournament t = requireTournament(id);
        BeanUtils.copyBeanProp(t, req);
        if (tournamentMapper.updateById(t) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        int rows = tournamentMapper.updateStatusIfMatch(id,
                Arrays.asList(TournamentStatusEnum.DRAFT.getCode(), TournamentStatusEnum.DISABLED.getCode()),
                TournamentStatusEnum.ACTIVE.getCode());
        assertSingleRow(rows, "发布失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublish(Long id) {
        int rows = tournamentMapper.updateStatusIfMatch(id,
                Arrays.asList(TournamentStatusEnum.ACTIVE.getCode()),
                TournamentStatusEnum.DISABLED.getCode());
        assertSingleRow(rows, "下架失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        int rows = tournamentMapper.updateStatusIfMatch(id,
                Arrays.asList(TournamentStatusEnum.ACTIVE.getCode(), TournamentStatusEnum.DRAFT.getCode()),
                TournamentStatusEnum.ENDED.getCode());
        assertSingleRow(rows, "关闭失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (tournamentMapper.softDelete(id) != 1) {
            throw new ServiceException("删除失败");
        }
    }

    private BasketTournament requireTournament(Long id) {
        BasketTournament t = tournamentMapper.selectById(id);
        if (t == null) {
            throw new ServiceException("赛事不存在");
        }
        return t;
    }

    private TournamentListVo toListVo(BasketTournament t) {
        TournamentListVo vo = new TournamentListVo();
        BeanUtils.copyBeanProp(vo, t);
        vo.setTournamentStatusName(TournamentStatusEnum.fromCode(t.getTournamentStatus()).getDescription());
        return vo;
    }

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ServiceException(message);
        }
    }
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/IBasketSeasonService.java",
        """package com.zhs.basket.service;

import com.zhs.basket.req.SeasonCreateReq;
import com.zhs.basket.req.SeasonUpdateReq;
import com.zhs.basket.vo.SeasonDetailVo;
import com.zhs.basket.vo.SeasonListVo;
import java.util.List;

public interface IBasketSeasonService {
    List<SeasonListVo> selectByTournamentId(Long tournamentId);
    SeasonDetailVo selectById(Long seasonId);
    Long create(Long tournamentId, SeasonCreateReq req);
    void update(Long seasonId, SeasonUpdateReq req);
    void publish(Long seasonId);
    void openRegistration(Long seasonId);
    void closeRegistration(Long seasonId);
    void start(Long seasonId);
    void finish(Long seasonId);
    void delete(Long seasonId);
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/impl/BasketSeasonServiceImpl.java",
        """package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.enums.SeasonStatusEnum;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.req.SeasonCreateReq;
import com.zhs.basket.req.SeasonUpdateReq;
import com.zhs.basket.service.IBasketSeasonService;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.vo.SeasonDetailVo;
import com.zhs.basket.vo.SeasonListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketSeasonServiceImpl implements IBasketSeasonService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentSeasonMapper seasonMapper;
    @Autowired private PlayerNoGenerator playerNoGenerator;

    @Override
    public List<SeasonListVo> selectByTournamentId(Long tournamentId) {
        return seasonMapper.selectByTournamentId(tournamentId).stream().map(this::toListVo).collect(Collectors.toList());
    }

    @Override
    public SeasonDetailVo selectById(Long seasonId) {
        BasketTournamentSeason season = requireSeason(seasonId);
        SeasonDetailVo vo = new SeasonDetailVo();
        BeanUtils.copyBeanProp(vo, season);
        vo.setSeasonStatusName(SeasonStatusEnum.fromCode(season.getSeasonStatus()).getDescription());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long tournamentId, SeasonCreateReq req) {
        validateSeasonTimes(req.getRegistrationStartTime(), req.getRegistrationEndTime(),
                req.getSeasonStartTime(), req.getSeasonEndTime(), req.getMinRosterCount(), req.getMaxRosterCount());
        BasketTournamentSeason season = new BasketTournamentSeason();
        BeanUtils.copyBeanProp(season, req);
        season.setTournamentId(tournamentId);
        season.setSeasonNo(playerNoGenerator.generateBizNo("SN"));
        season.setSeasonStatus(SeasonStatusEnum.PREPARE.getCode());
        season.setIsDelete(NOT_DELETED);
        seasonMapper.insert(season);
        return season.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long seasonId, SeasonUpdateReq req) {
        BasketTournamentSeason season = requireSeason(seasonId);
        validateSeasonTimes(req.getRegistrationStartTime(), req.getRegistrationEndTime(),
                req.getSeasonStartTime(), req.getSeasonEndTime(), req.getMinRosterCount(), req.getMaxRosterCount());
        BeanUtils.copyBeanProp(season, req);
        if (seasonMapper.updateById(season) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long seasonId) {
        requireSeason(seasonId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openRegistration(Long seasonId) {
        changeStatus(seasonId, Arrays.asList(SeasonStatusEnum.PREPARE.getCode(), SeasonStatusEnum.REGISTRATION_CLOSED.getCode()),
                SeasonStatusEnum.REGISTRATION_OPEN);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeRegistration(Long seasonId) {
        changeStatus(seasonId, Arrays.asList(SeasonStatusEnum.REGISTRATION_OPEN.getCode()),
                SeasonStatusEnum.REGISTRATION_CLOSED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long seasonId) {
        changeStatus(seasonId, Arrays.asList(SeasonStatusEnum.REGISTRATION_OPEN.getCode(), SeasonStatusEnum.REGISTRATION_CLOSED.getCode()),
                SeasonStatusEnum.IN_PROGRESS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long seasonId) {
        changeStatus(seasonId, Arrays.asList(SeasonStatusEnum.IN_PROGRESS.getCode()),
                SeasonStatusEnum.FINISHED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long seasonId) {
        if (seasonMapper.softDelete(seasonId) != 1) {
            throw new ServiceException("删除失败");
        }
    }

    private void changeStatus(Long seasonId, java.util.List<Integer> from, SeasonStatusEnum to) {
        int rows = seasonMapper.updateStatusIfMatch(seasonId, from, to.getCode());
        if (rows != 1) {
            throw new ServiceException("状态变更失败");
        }
    }

    private void validateSeasonTimes(Date regStart, Date regEnd, Date seasonStart, Date seasonEnd,
                                     Integer minRoster, Integer maxRoster) {
        if (regStart == null || regEnd == null || seasonStart == null || seasonEnd == null) {
            throw new ServiceException("时间字段不能为空");
        }
        if (!regStart.before(regEnd)) {
            throw new ServiceException("报名开始时间必须早于报名结束时间");
        }
        if (regEnd.after(seasonStart)) {
            throw new ServiceException("报名结束时间不能晚于赛季开始时间");
        }
        if (!seasonStart.before(seasonEnd)) {
            throw new ServiceException("赛季开始时间必须早于赛季结束时间");
        }
        if (minRoster == null || minRoster < 5) {
            throw new ServiceException("最小 roster 人数不能少于 5");
        }
        if (maxRoster != null && maxRoster < minRoster) {
            throw new ServiceException("最大 roster 人数不能小于最小人数");
        }
    }

    private BasketTournamentSeason requireSeason(Long id) {
        BasketTournamentSeason season = seasonMapper.selectById(id);
        if (season == null) {
            throw new ServiceException("赛季不存在");
        }
        return season;
    }

    private SeasonListVo toListVo(BasketTournamentSeason season) {
        SeasonListVo vo = new SeasonListVo();
        BeanUtils.copyBeanProp(vo, season);
        vo.setSeasonStatusName(SeasonStatusEnum.fromCode(season.getSeasonStatus()).getDescription());
        return vo;
    }
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/IBasketTournamentRuleService.java",
        """package com.zhs.basket.service;

import com.zhs.basket.req.TournamentRuleCreateReq;
import com.zhs.basket.req.TournamentRuleUpdateReq;
import com.zhs.basket.vo.TournamentRuleVo;
import java.util.List;

public interface IBasketTournamentRuleService {
    List<TournamentRuleVo> selectBySeasonId(Long seasonId);
    Long create(Long seasonId, TournamentRuleCreateReq req);
    void update(Long ruleId, TournamentRuleUpdateReq req);
    void delete(Long ruleId);
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/impl/BasketTournamentRuleServiceImpl.java",
        """package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTournamentRule;
import com.zhs.basket.mapper.BasketTournamentRuleMapper;
import com.zhs.basket.req.TournamentRuleCreateReq;
import com.zhs.basket.req.TournamentRuleUpdateReq;
import com.zhs.basket.service.IBasketTournamentRuleService;
import com.zhs.basket.vo.TournamentRuleVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketTournamentRuleServiceImpl implements IBasketTournamentRuleService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentRuleMapper ruleMapper;

    @Override
    public List<TournamentRuleVo> selectBySeasonId(Long seasonId) {
        return ruleMapper.selectBySeasonId(seasonId).stream().map(r -> {
            TournamentRuleVo vo = new TournamentRuleVo();
            BeanUtils.copyBeanProp(vo, r);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long seasonId, TournamentRuleCreateReq req) {
        BasketTournamentRule rule = new BasketTournamentRule();
        BeanUtils.copyBeanProp(rule, req);
        rule.setSeasonId(seasonId);
        rule.setIsDelete(NOT_DELETED);
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long ruleId, TournamentRuleUpdateReq req) {
        BasketTournamentRule rule = requireRule(ruleId);
        BeanUtils.copyBeanProp(rule, req);
        if (ruleMapper.updateById(rule) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long ruleId) {
        if (ruleMapper.softDelete(ruleId) != 1) {
            throw new ServiceException("删除失败");
        }
    }

    private BasketTournamentRule requireRule(Long id) {
        BasketTournamentRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new ServiceException("规则不存在");
        }
        return rule;
    }
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/IBasketRegistrationAuditService.java",
        """package com.zhs.basket.service;

import com.zhs.basket.query.RegistrationAuditQuery;
import com.zhs.basket.req.RegistrationAuditApproveReq;
import com.zhs.basket.req.RegistrationAuditRejectReq;
import com.zhs.basket.vo.RegistrationAuditDetailVo;
import com.zhs.basket.vo.RegistrationAuditListVo;
import com.zhs.basket.vo.RegistrationAuditRecordVo;
import com.zhs.basket.vo.RegistrationPlayerVo;
import java.util.List;

public interface IBasketRegistrationAuditService {
    List<RegistrationAuditListVo> selectList(RegistrationAuditQuery query);
    RegistrationAuditDetailVo selectDetail(Long id);
    List<RegistrationPlayerVo> revalidate(Long id);
    void startReview(Long id);
    void approve(Long id, RegistrationAuditApproveReq req);
    void reject(Long id, RegistrationAuditRejectReq req);
    List<RegistrationAuditRecordVo> selectAuditRecords(Long id);
}""",
    )

    w(
        "src/main/java/com/zhs/basket/service/impl/BasketRegistrationAuditServiceImpl.java",
        """package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.enums.RegistrationPlayerValidationStatusEnum;
import com.zhs.basket.enums.RegistrationStatusEnum;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.RegistrationAuditQuery;
import com.zhs.basket.req.RegistrationAuditApproveReq;
import com.zhs.basket.req.RegistrationAuditRejectReq;
import com.zhs.basket.service.IBasketRegistrationAuditService;
import com.zhs.basket.support.BasketAdminMessageService;
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
    @Autowired private BasketAdminMessageService messageService;

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
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RegistrationPlayerVo> revalidate(Long id) {
        BasketTournamentRegistration reg = requireRegistration(id);
        List<BasketTournamentRegistrationPlayer> players = registrationPlayerMapper.selectByRegistrationId(id);
        Date now = new Date();
        List<Long> memberIds = players.stream().map(BasketTournamentRegistrationPlayer::getMemberId).filter(Objects::nonNull).collect(Collectors.toList());
        Set<Long> disciplined = disciplineRecordMapper.selectActiveBySeasonAndMembers(reg.getSeasonId(), memberIds, now)
                .stream().map(BasketDisciplineRecord::getMemberId).collect(Collectors.toSet());
        List<RegistrationPlayerVo> result = new ArrayList<>();
        for (BasketTournamentRegistrationPlayer player : players) {
            String reason = validatePlayer(player, disciplined);
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

    private String validatePlayer(BasketTournamentRegistrationPlayer player, Set<Long> disciplined) {
        if (player.getMemberId() != null && disciplined.contains(player.getMemberId())) {
            return "球员处于禁赛期";
        }
        BasketPlayerProfile profile = profileMapper.selectById(player.getPlayerId());
        if (profile == null || !Objects.equals(profile.getCertificationStatus(), PlayerCertStatusEnum.APPROVED.getCode())) {
            return "球员未完成认证";
        }
        return null;
    }

    private void writeAudit(BasketTournamentRegistration reg, String action, Integer before, Integer after, String remark) {
        BasketTournamentRegistrationAudit audit = new BasketTournamentRegistrationAudit();
        audit.setRegistrationId(reg.getId());
        audit.setAuditAction(action);
        audit.setBeforeStatus(before);
        audit.setAfterStatus(after);
        audit.setAuditorId(SecurityUtils.getUserId());
        audit.setAuditorName(SecurityUtils.getUsername());
        audit.setAuditRemark(remark);
        audit.setAuditTime(new Date());
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
}""",
    )
