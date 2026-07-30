package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketMatchDao;
import com.zhs.dao.BasketMatchExceptionApplicationDao;
import com.zhs.dao.BasketMatchExceptionMaterialDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTournamentRegistrationDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.enums.MatchDataMaterialTypeEnum;
import com.zhs.enums.MatchExceptionApplicationStatusEnum;
import com.zhs.enums.MatchExceptionTypeEnum;
import com.zhs.enums.TournamentRegistrationStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMatch;
import com.zhs.model.BasketMatchExceptionApplication;
import com.zhs.model.BasketMatchExceptionMaterial;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTournamentRegistration;
import com.zhs.request.matchdata.PlayerMatchExceptionSubmitReq;
import com.zhs.response.matchdata.PlayerMatchExceptionAvailableResp;
import com.zhs.response.matchdata.PlayerMatchExceptionHistoryItem;
import com.zhs.response.matchdata.PlayerMatchExceptionStatusResp;
import com.zhs.response.matchdata.PlayerMatchExceptionSubmitResultResp;
import com.zhs.service.IAccountFileService;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.IMPlayerMatchExceptionService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.PlayerMatchDisputeExceptionSupport;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlayerMatchExceptionServiceImpl implements IMPlayerMatchExceptionService {

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketMatchDao matchDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTournamentRegistrationDao registrationDao;
    @Resource
    private BasketMatchExceptionApplicationDao exceptionApplicationDao;
    @Resource
    private BasketMatchExceptionMaterialDao exceptionMaterialDao;
    @Resource
    private IAccountFileService accountFileService;
    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private PlayerMatchDisputeExceptionSupport disputeExceptionSupport;

    @Override
    public PlayerMatchExceptionAvailableResp getAvailable(Long matchId) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);
        BasketMatch match = requireMatch(matchId);
        assertMemberCanViewMatch(memberId, match);

        boolean captain = !findCaptainTeamIdsForMatch(memberId, match).isEmpty();
        PlayerMatchDisputeExceptionSupport.ExceptionAvailability availability =
                disputeExceptionSupport.evaluateExceptionAvailability(match, captain);
        Map<Long, String> teamNames = loadTeamNames(Collections.singletonList(match));

        PlayerMatchExceptionAvailableResp resp = new PlayerMatchExceptionAvailableResp();
        resp.setMatchId(String.valueOf(match.getId()));
        resp.setCanSubmitException(availability.getCanSubmitException());
        resp.setCannotReason(availability.getCannotReason());
        if (availability.getCurrentExceptionId() != null) {
            resp.setCurrentExceptionId(String.valueOf(availability.getCurrentExceptionId()));
        }
        resp.setCurrentExceptionStatus(availability.getCurrentExceptionStatus());
        resp.setHomeTeamName(teamNames.get(match.getHomeTeamId()));
        resp.setAwayTeamName(teamNames.get(match.getAwayTeamId()));
        resp.setHomeScore(match.getHomeScore());
        resp.setAwayScore(match.getAwayScore());
        resp.setDataStatus(match.getDataStatus());
        resp.setDataStatusText(resolveDataStatusText(match.getDataStatus()));
        return resp;
    }

    @Override
    public PlayerMatchExceptionStatusResp getStatus(Long matchId) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);
        BasketMatch match = requireMatch(matchId);
        assertMemberCanViewMatch(memberId, match);

        BasketMatchExceptionApplication application = disputeExceptionSupport.findLatestException(matchId);
        if (application == null) {
            throw new ServiceException("该比赛暂无异常申请记录");
        }

        boolean captain = !findCaptainTeamIdsForMatch(memberId, match).isEmpty();
        PlayerMatchDisputeExceptionSupport.ExceptionAvailability availability =
                disputeExceptionSupport.evaluateExceptionAvailability(match, captain);

        PlayerMatchExceptionStatusResp resp = new PlayerMatchExceptionStatusResp();
        resp.setMatchId(String.valueOf(match.getId()));
        resp.setMatchNo(match.getMatchNo());
        resp.setApplicationId(String.valueOf(application.getId()));
        resp.setApplicationNo(application.getApplicationNo());
        resp.setExceptionType(application.getExceptionType());
        resp.setExceptionTypeText(resolveExceptionTypeText(application.getExceptionType()));
        resp.setApplicationStatus(application.getApplicationStatus());
        resp.setApplicationStatusText(resolveExceptionStatusText(application.getApplicationStatus()));
        resp.setDecisionType(application.getDecisionType());
        resp.setDecisionResult(application.getDecisionResult());
        resp.setSubmittedTime(application.getCreateTime());
        resp.setProcessedTime(application.getProcessedTime());
        resp.setProcessedByName(application.getProcessedByName());
        resp.setRejectReason(application.getDecisionResult());
        resp.setStatusText(resolveExceptionStatusText(application.getApplicationStatus()));
        resp.setCanSubmitException(availability.getCanSubmitException());
        resp.setCannotExceptionReason(availability.getCannotReason());
        return resp;
    }

    @Override
    public List<PlayerMatchExceptionHistoryItem> getHistory(Long matchId) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);
        BasketMatch match = requireMatch(matchId);
        assertMemberCanViewMatch(memberId, match);

        List<BasketMatchExceptionApplication> applications = disputeExceptionSupport.listExceptionHistory(matchId);
        Map<Long, String> teamNames = loadTeamNames(Collections.singletonList(match));
        List<PlayerMatchExceptionHistoryItem> items = new ArrayList<>();
        for (BasketMatchExceptionApplication application : applications) {
            PlayerMatchExceptionHistoryItem item = new PlayerMatchExceptionHistoryItem();
            item.setApplicationId(String.valueOf(application.getId()));
            item.setApplicationNo(application.getApplicationNo());
            item.setExceptionType(application.getExceptionType());
            item.setExceptionTypeText(resolveExceptionTypeText(application.getExceptionType()));
            item.setApplicationStatus(application.getApplicationStatus());
            item.setApplicationStatusText(resolveExceptionStatusText(application.getApplicationStatus()));
            if (application.getApplicantTeamId() != null) {
                item.setApplicantTeamId(String.valueOf(application.getApplicantTeamId()));
                item.setApplicantTeamName(teamNames.get(application.getApplicantTeamId()));
            }
            item.setSubmittedTime(application.getCreateTime());
            item.setProcessedTime(application.getProcessedTime());
            item.setDecisionType(application.getDecisionType());
            item.setDecisionResult(application.getDecisionResult());
            if (MatchExceptionApplicationStatusEnum.REJECTED.getCode().equalsIgnoreCase(application.getApplicationStatus())) {
                item.setRejectReason(application.getDecisionResult());
            }
            items.add(item);
        }
        return items;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlayerMatchExceptionSubmitResultResp submit(Long matchId, PlayerMatchExceptionSubmitReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        if (!StringUtils.hasText(req.getExceptionReason())) {
            throw new ServiceException("异常说明不能为空");
        }
        if (CollectionUtils.isEmpty(req.getMaterialFileIds())) {
            throw new ServiceException("请上传异常证明材料");
        }
        MatchExceptionTypeEnum exceptionType = MatchExceptionTypeEnum.fromCode(req.getExceptionType());

        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile profile = captainPermissionService.requireCertifiedPlayer(memberId);

        BasketMatch match = matchDao.selectByIdForUpdate(matchId);
        if (match == null || !NOT_DELETED.equals(match.getIsDelete())) {
            throw new ServiceException("比赛不存在");
        }

        List<Long> captainTeamIds = findCaptainTeamIdsForMatch(memberId, match);
        if (captainTeamIds.isEmpty()) {
            throw new ServiceException("仅参赛球队队长可提交异常申请", 403);
        }
        Long applicantTeamId = captainTeamIds.get(0);
        Long opponentTeamId = applicantTeamId.equals(match.getHomeTeamId())
                ? match.getAwayTeamId() : match.getHomeTeamId();

        PlayerMatchDisputeExceptionSupport.ExceptionAvailability availability =
                disputeExceptionSupport.evaluateExceptionAvailability(match, true);
        if (!Boolean.TRUE.equals(availability.getCanSubmitException())) {
            throw new ServiceException(availability.getCannotReason() != null
                    ? availability.getCannotReason() : "当前不可提交异常申请");
        }

        List<BasketMatchExceptionMaterial> materials = new ArrayList<>();
        Set<Long> fileIdSet = new HashSet<>();
        int sort = 0;
        for (String fileIdText : req.getMaterialFileIds()) {
            Long fileId = parseLongId(fileIdText, "材料文件");
            if (!fileIdSet.add(fileId)) {
                continue;
            }
            BasketFileAsset asset = accountFileService.requireOwnedFile(fileId, memberId);
            if (!FileUsageTypeEnum.MATCH_DATA_SCREENSHOT.getCode().equalsIgnoreCase(asset.getUsageType())) {
                throw new ServiceException("材料文件用途不匹配，请使用比赛数据截图");
            }
            BasketMatchExceptionMaterial material = new BasketMatchExceptionMaterial();
            material.setMatchId(matchId);
            material.setFileId(fileId);
            material.setMaterialType(MatchDataMaterialTypeEnum.EXCEPTION_EVIDENCE.getCode());
            material.setMaterialName(asset.getOriginalName());
            material.setSortNo(sort++);
            materials.add(material);
        }
        if (materials.isEmpty()) {
            throw new ServiceException("请至少上传一份异常证明材料");
        }

        Date now = new Date();
        BasketMatchExceptionApplication application = new BasketMatchExceptionApplication();
        application.setApplicationNo("EXC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        application.setMatchId(matchId);
        application.setSeasonId(match.getSeasonId());
        application.setStageId(match.getStageId());
        application.setApplicantMemberId(memberId);
        application.setApplicantPlayerId(profile.getId());
        application.setApplicantTeamId(applicantTeamId);
        application.setOpponentTeamId(opponentTeamId);
        application.setExceptionType(exceptionType.getCode());
        application.setExceptionReason(req.getExceptionReason().trim());
        application.setApplicationStatus(MatchExceptionApplicationStatusEnum.PENDING.getCode());
        application.setRemark(trimToNull(req.getRemark()));
        application.setCreateTime(now);
        application.setUpdateTime(now);
        application.setIsDelete(NOT_DELETED);

        try {
            exceptionApplicationDao.insert(application);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("该比赛已有待处理异常申请，请勿重复提交");
        }

        for (BasketMatchExceptionMaterial material : materials) {
            material.setApplicationId(application.getId());
            material.setCreateTime(now);
            material.setUpdateTime(now);
            material.setIsDelete(NOT_DELETED);
            exceptionMaterialDao.insert(material);
        }

        PlayerMatchExceptionSubmitResultResp resp = new PlayerMatchExceptionSubmitResultResp();
        resp.setApplicationId(String.valueOf(application.getId()));
        resp.setApplicationNo(application.getApplicationNo());
        return resp;
    }

    private BasketMatch requireMatch(Long matchId) {
        if (matchId == null) {
            throw new ServiceException("比赛主键不能为空");
        }
        BasketMatch match = matchDao.selectById(matchId);
        if (match == null || !NOT_DELETED.equals(match.getIsDelete())) {
            throw new ServiceException("比赛不存在");
        }
        return match;
    }

    private void assertMemberCanViewMatch(Long memberId, BasketMatch match) {
        if (!isCaptainOfTeam(memberId, match.getSeasonId(), match.getHomeTeamId())
                && !isCaptainOfTeam(memberId, match.getSeasonId(), match.getAwayTeamId())) {
            throw new ServiceException("无权查看该比赛异常申请", 403);
        }
    }

    private List<Long> findCaptainTeamIdsForMatch(Long memberId, BasketMatch match) {
        List<Long> result = new ArrayList<>();
        if (isCaptainOfTeam(memberId, match.getSeasonId(), match.getHomeTeamId())) {
            result.add(match.getHomeTeamId());
        }
        if (isCaptainOfTeam(memberId, match.getSeasonId(), match.getAwayTeamId())) {
            result.add(match.getAwayTeamId());
        }
        return result;
    }

    private boolean isCaptainOfTeam(Long memberId, Long seasonId, Long teamId) {
        LambdaQueryWrapper<BasketTournamentRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketTournamentRegistration::getSeasonId, seasonId)
                .eq(BasketTournamentRegistration::getTeamId, teamId)
                .eq(BasketTournamentRegistration::getCaptainMemberId, memberId)
                .eq(BasketTournamentRegistration::getRegistrationStatus, TournamentRegistrationStatusEnum.APPROVED.getCode())
                .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED);
        return registrationDao.selectCount(wrapper) > 0;
    }

    private Map<Long, String> loadTeamNames(List<BasketMatch> matches) {
        Set<Long> teamIds = new HashSet<>();
        for (BasketMatch match : matches) {
            if (match.getHomeTeamId() != null) {
                teamIds.add(match.getHomeTeamId());
            }
            if (match.getAwayTeamId() != null) {
                teamIds.add(match.getAwayTeamId());
            }
        }
        if (teamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = new HashMap<>();
        for (BasketTeam team : teamDao.selectBatchIds(teamIds)) {
            if (team != null && NOT_DELETED.equals(team.getIsDelete())) {
                map.put(team.getId(), team.getTeamName());
            }
        }
        return map;
    }

    private Long parseLongId(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new ServiceException(label + "不能为空");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(label + "格式无效");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveDataStatusText(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case BasketPublicConstants.DATA_DRAFT -> "草稿";
            case BasketPublicConstants.DATA_SUBMITTED -> "已提交";
            case BasketPublicConstants.DATA_CONFIRMED -> "已确认";
            case BasketPublicConstants.DATA_REOPENED -> "已重开";
            case BasketPublicConstants.DATA_REJECTED -> "已驳回";
            default -> "未知";
        };
    }

    private String resolveExceptionTypeText(String exceptionType) {
        if (!StringUtils.hasText(exceptionType)) {
            return null;
        }
        try {
            return MatchExceptionTypeEnum.fromCode(exceptionType).getDescription();
        } catch (ServiceException ex) {
            return exceptionType;
        }
    }

    private String resolveExceptionStatusText(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return MatchExceptionApplicationStatusEnum.fromCode(status).getDescription();
        } catch (ServiceException ex) {
            return status;
        }
    }
}
