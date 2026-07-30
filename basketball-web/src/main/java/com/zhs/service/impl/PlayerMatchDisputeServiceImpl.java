package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketMatchDao;
import com.zhs.dao.BasketMatchDataDisputeDao;
import com.zhs.dao.BasketMatchDataDisputeMaterialDao;
import com.zhs.dao.BasketMatchDataSubmissionDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTournamentRegistrationDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.enums.MatchDataDisputeStatusEnum;
import com.zhs.enums.MatchDataDisputeTypeEnum;
import com.zhs.enums.MatchDataMaterialTypeEnum;
import com.zhs.enums.MatchDataSubmissionStatusEnum;
import com.zhs.enums.TournamentRegistrationStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMatch;
import com.zhs.model.BasketMatchDataDispute;
import com.zhs.model.BasketMatchDataDisputeMaterial;
import com.zhs.model.BasketMatchDataSubmission;
import com.zhs.model.BasketMatchOfficialResult;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTournamentRegistration;
import com.zhs.request.matchdata.PlayerMatchDisputeSubmitReq;
import com.zhs.response.matchdata.PlayerMatchDisputeAvailableResp;
import com.zhs.response.matchdata.PlayerMatchDisputeDetailResp;
import com.zhs.response.matchdata.PlayerMatchDisputeHistoryItem;
import com.zhs.response.matchdata.PlayerMatchDisputeStatusResp;
import com.zhs.response.matchdata.PlayerMatchDisputeSubmitResultResp;
import com.zhs.service.IAccountFileService;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.IMPlayerMatchDisputeService;
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
public class PlayerMatchDisputeServiceImpl implements IMPlayerMatchDisputeService {

    private static final Integer NOT_DELETED = 0;
    private static final String INITIATOR_TYPE_CAPTAIN = "CAPTAIN";

    @Resource
    private BasketMatchDao matchDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTournamentRegistrationDao registrationDao;
    @Resource
    private BasketMatchDataSubmissionDao submissionDao;
    @Resource
    private BasketMatchDataDisputeDao disputeDao;
    @Resource
    private BasketMatchDataDisputeMaterialDao disputeMaterialDao;
    @Resource
    private IAccountFileService accountFileService;
    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private PlayerMatchDisputeExceptionSupport disputeExceptionSupport;

    @Override
    public PlayerMatchDisputeAvailableResp getAvailable(Long matchId) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);
        BasketMatch match = requireMatch(matchId);
        assertMemberCanViewMatch(memberId, match);

        boolean captain = !findCaptainTeamIdsForMatch(memberId, match).isEmpty();
        PlayerMatchDisputeExceptionSupport.DisputeAvailability availability =
                disputeExceptionSupport.evaluateDisputeAvailability(match, captain);
        Map<Long, String> teamNames = loadTeamNames(Collections.singletonList(match));

        PlayerMatchDisputeAvailableResp resp = new PlayerMatchDisputeAvailableResp();
        resp.setMatchId(String.valueOf(match.getId()));
        resp.setCanDispute(availability.getCanDispute());
        resp.setCannotReason(availability.getCannotReason());
        resp.setDeadlineTime(availability.getDeadlineTime());
        resp.setRemainingSeconds(availability.getRemainingSeconds());
        if (availability.getCurrentDisputeId() != null) {
            resp.setCurrentDisputeId(String.valueOf(availability.getCurrentDisputeId()));
        }
        resp.setCurrentDisputeStatus(availability.getCurrentDisputeStatus());
        resp.setHomeTeamName(teamNames.get(match.getHomeTeamId()));
        resp.setAwayTeamName(teamNames.get(match.getAwayTeamId()));
        resp.setHomeScore(match.getHomeScore());
        resp.setAwayScore(match.getAwayScore());
        resp.setConfirmedTime(availability.getConfirmedTime());
        resp.setPublicTime(availability.getPublicTime());
        return resp;
    }

    @Override
    public PlayerMatchDisputeDetailResp getDetail(Long matchId) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);
        BasketMatch match = requireMatch(matchId);
        assertMemberCanViewMatch(memberId, match);

        BasketMatchDataDispute dispute = disputeExceptionSupport.findLatestDispute(matchId);
        if (dispute == null) {
            throw new ServiceException("该比赛暂无争议记录");
        }
        return toDetailResp(match, dispute);
    }

    @Override
    public PlayerMatchDisputeStatusResp getStatus(Long matchId) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);
        BasketMatch match = requireMatch(matchId);
        assertMemberCanViewMatch(memberId, match);

        BasketMatchDataDispute dispute = disputeExceptionSupport.findLatestDispute(matchId);
        if (dispute == null) {
            throw new ServiceException("该比赛暂无争议记录");
        }

        boolean captain = !findCaptainTeamIdsForMatch(memberId, match).isEmpty();
        PlayerMatchDisputeExceptionSupport.DisputeAvailability availability =
                disputeExceptionSupport.evaluateDisputeAvailability(match, captain);

        PlayerMatchDisputeStatusResp resp = new PlayerMatchDisputeStatusResp();
        resp.setMatchId(String.valueOf(match.getId()));
        resp.setMatchNo(match.getMatchNo());
        resp.setDisputeId(String.valueOf(dispute.getId()));
        resp.setDisputeNo(dispute.getDisputeNo());
        resp.setDisputeType(dispute.getDisputeType());
        resp.setDisputeTypeText(resolveDisputeTypeText(dispute.getDisputeType()));
        resp.setDisputeStatus(dispute.getDisputeStatus());
        resp.setDisputeStatusText(resolveDisputeStatusText(dispute.getDisputeStatus()));
        resp.setSubmittedTime(dispute.getCreateTime());
        resp.setDisputeDeadlineTime(dispute.getDisputeDeadlineTime());
        resp.setRemainingSeconds(disputeExceptionSupport.computeRemainingSeconds(dispute.getDisputeDeadlineTime()));
        resp.setProcessedTime(dispute.getProcessedTime() != null ? dispute.getProcessedTime() : dispute.getHandleTime());
        resp.setProcessedByName(dispute.getProcessedByName());
        resp.setProcessResult(dispute.getProcessResult() != null ? dispute.getProcessResult() : dispute.getHandleResult());
        resp.setRejectReason(dispute.getRejectReason());
        resp.setHandleRemark(dispute.getHandleRemark());
        resp.setStatusText(resolveDisputeStatusText(dispute.getDisputeStatus()));
        resp.setCanDispute(availability.getCanDispute());
        resp.setCannotDisputeReason(availability.getCannotReason());
        return resp;
    }

    @Override
    public List<PlayerMatchDisputeHistoryItem> getHistory(Long matchId) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);
        BasketMatch match = requireMatch(matchId);
        assertMemberCanViewMatch(memberId, match);

        List<BasketMatchDataDispute> disputes = disputeExceptionSupport.listDisputeHistory(matchId);
        Map<Long, String> teamNames = loadTeamNames(Collections.singletonList(match));
        List<PlayerMatchDisputeHistoryItem> items = new ArrayList<>();
        for (BasketMatchDataDispute dispute : disputes) {
            PlayerMatchDisputeHistoryItem item = new PlayerMatchDisputeHistoryItem();
            item.setDisputeId(String.valueOf(dispute.getId()));
            item.setDisputeNo(dispute.getDisputeNo());
            item.setDisputeType(dispute.getDisputeType());
            item.setDisputeTypeText(resolveDisputeTypeText(dispute.getDisputeType()));
            item.setDisputeStatus(dispute.getDisputeStatus());
            item.setDisputeStatusText(resolveDisputeStatusText(dispute.getDisputeStatus()));
            if (dispute.getInitiatorTeamId() != null) {
                item.setInitiatorTeamId(String.valueOf(dispute.getInitiatorTeamId()));
                item.setInitiatorTeamName(teamNames.get(dispute.getInitiatorTeamId()));
            }
            item.setSubmittedTime(dispute.getCreateTime());
            item.setProcessedTime(dispute.getProcessedTime() != null ? dispute.getProcessedTime() : dispute.getHandleTime());
            item.setProcessResult(dispute.getProcessResult() != null ? dispute.getProcessResult() : dispute.getHandleResult());
            item.setRejectReason(dispute.getRejectReason());
            items.add(item);
        }
        return items;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlayerMatchDisputeSubmitResultResp submit(Long matchId, PlayerMatchDisputeSubmitReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        if (!StringUtils.hasText(req.getDisputeReason())) {
            throw new ServiceException("争议说明不能为空");
        }
        if (CollectionUtils.isEmpty(req.getMaterialFileIds())) {
            throw new ServiceException("请上传争议证明材料");
        }
        MatchDataDisputeTypeEnum disputeType = MatchDataDisputeTypeEnum.fromCode(req.getDisputeType());

        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile profile = captainPermissionService.requireCertifiedPlayer(memberId);

        BasketMatch match = matchDao.selectByIdForUpdate(matchId);
        if (match == null || !NOT_DELETED.equals(match.getIsDelete())) {
            throw new ServiceException("比赛不存在");
        }

        List<Long> captainTeamIds = findCaptainTeamIdsForMatch(memberId, match);
        if (captainTeamIds.isEmpty()) {
            throw new ServiceException("仅参赛球队队长可发起争议", 403);
        }
        Long initiatorTeamId = captainTeamIds.get(0);

        PlayerMatchDisputeExceptionSupport.DisputeAvailability availability =
                disputeExceptionSupport.evaluateDisputeAvailability(match, true);
        if (!Boolean.TRUE.equals(availability.getCanDispute())) {
            throw new ServiceException(availability.getCannotReason() != null
                    ? availability.getCannotReason() : "当前不可发起争议");
        }

        BasketMatchOfficialResult official = disputeExceptionSupport.getCurrentOfficialResult(matchId);
        Date deadline = disputeExceptionSupport.resolveDisputeDeadline(match, official);
        BasketMatchDataSubmission submission = findLatestApprovedSubmission(matchId);

        Long memberIdForFiles = memberId;
        List<BasketMatchDataDisputeMaterial> materials = new ArrayList<>();
        Set<Long> fileIdSet = new HashSet<>();
        int sort = 0;
        for (String fileIdText : req.getMaterialFileIds()) {
            Long fileId = parseLongId(fileIdText, "材料文件");
            if (!fileIdSet.add(fileId)) {
                continue;
            }
            BasketFileAsset asset = accountFileService.requireOwnedFile(fileId, memberIdForFiles);
            if (!FileUsageTypeEnum.MATCH_DATA_SCREENSHOT.getCode().equalsIgnoreCase(asset.getUsageType())) {
                throw new ServiceException("材料文件用途不匹配，请使用比赛数据截图");
            }
            BasketMatchDataDisputeMaterial material = new BasketMatchDataDisputeMaterial();
            material.setMatchId(matchId);
            material.setFileId(fileId);
            material.setMaterialType(MatchDataMaterialTypeEnum.DISPUTE_EVIDENCE.getCode());
            material.setMaterialName(asset.getOriginalName());
            material.setSortNo(sort++);
            materials.add(material);
        }
        if (materials.isEmpty()) {
            throw new ServiceException("请至少上传一份争议证明材料");
        }

        Date now = new Date();
        String originalValue = match.getHomeScore() + ":" + match.getAwayScore();
        String expectedValue = buildExpectedValue(req.getExpectedHomeScore(), req.getExpectedAwayScore());

        BasketMatchDataDispute dispute = new BasketMatchDataDispute();
        dispute.setDisputeNo("DSP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        dispute.setMatchId(matchId);
        if (submission != null) {
            dispute.setSubmissionId(submission.getId());
        }
        dispute.setInitiatorType(INITIATOR_TYPE_CAPTAIN);
        dispute.setInitiatorId(memberId);
        dispute.setInitiatorMemberId(memberId);
        dispute.setInitiatorPlayerId(profile.getId());
        dispute.setInitiatorTeamId(initiatorTeamId);
        dispute.setDisputeType(disputeType.getCode());
        dispute.setOriginalValue(originalValue);
        dispute.setExpectedValue(expectedValue);
        dispute.setDisputeReason(req.getDisputeReason().trim());
        dispute.setEvidenceFileIds(materials.stream()
                .map(m -> String.valueOf(m.getFileId()))
                .collect(Collectors.joining(",")));
        dispute.setDisputeStatus(MatchDataDisputeStatusEnum.PENDING.getCode());
        dispute.setDisputeDeadlineTime(deadline);
        dispute.setCreateTime(now);
        dispute.setUpdateTime(now);
        dispute.setIsDelete(NOT_DELETED);

        try {
            disputeDao.insert(dispute);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("该比赛已有待处理争议，请勿重复提交");
        }

        for (BasketMatchDataDisputeMaterial material : materials) {
            material.setDisputeId(dispute.getId());
            material.setCreateTime(now);
            material.setUpdateTime(now);
            material.setIsDelete(NOT_DELETED);
            disputeMaterialDao.insert(material);
        }

        PlayerMatchDisputeSubmitResultResp resp = new PlayerMatchDisputeSubmitResultResp();
        resp.setDisputeId(String.valueOf(dispute.getId()));
        resp.setDisputeNo(dispute.getDisputeNo());
        return resp;
    }

    private PlayerMatchDisputeDetailResp toDetailResp(BasketMatch match, BasketMatchDataDispute dispute) {
        Map<Long, String> teamNames = loadTeamNames(Collections.singletonList(match));
        PlayerMatchDisputeDetailResp resp = new PlayerMatchDisputeDetailResp();
        resp.setDisputeId(String.valueOf(dispute.getId()));
        resp.setDisputeNo(dispute.getDisputeNo());
        resp.setMatchId(String.valueOf(match.getId()));
        resp.setMatchNo(match.getMatchNo());
        if (dispute.getSubmissionId() != null) {
            resp.setSubmissionId(String.valueOf(dispute.getSubmissionId()));
        }
        resp.setDisputeType(dispute.getDisputeType());
        resp.setDisputeTypeText(resolveDisputeTypeText(dispute.getDisputeType()));
        resp.setOriginalValue(dispute.getOriginalValue());
        resp.setExpectedValue(dispute.getExpectedValue());
        parseExpectedScores(dispute.getExpectedValue(), resp);
        resp.setDisputeReason(dispute.getDisputeReason());
        resp.setDisputeStatus(dispute.getDisputeStatus());
        resp.setDisputeStatusText(resolveDisputeStatusText(dispute.getDisputeStatus()));
        resp.setDisputeDeadlineTime(dispute.getDisputeDeadlineTime());
        resp.setRemainingSeconds(disputeExceptionSupport.computeRemainingSeconds(dispute.getDisputeDeadlineTime()));
        resp.setSubmittedTime(dispute.getCreateTime());
        resp.setProcessedTime(dispute.getProcessedTime() != null ? dispute.getProcessedTime() : dispute.getHandleTime());
        resp.setProcessedByName(dispute.getProcessedByName());
        resp.setProcessResult(dispute.getProcessResult() != null ? dispute.getProcessResult() : dispute.getHandleResult());
        resp.setRejectReason(dispute.getRejectReason());
        resp.setHandleRemark(dispute.getHandleRemark());
        resp.setHomeTeamName(teamNames.get(match.getHomeTeamId()));
        resp.setAwayTeamName(teamNames.get(match.getAwayTeamId()));
        resp.setHomeScore(match.getHomeScore());
        resp.setAwayScore(match.getAwayScore());
        if (dispute.getInitiatorTeamId() != null) {
            resp.setInitiatorTeamId(String.valueOf(dispute.getInitiatorTeamId()));
            resp.setInitiatorTeamName(teamNames.get(dispute.getInitiatorTeamId()));
        }
        resp.setMaterialFileIds(loadMaterialFileIds(dispute.getId()));
        return resp;
    }

    private List<String> loadMaterialFileIds(Long disputeId) {
        List<BasketMatchDataDisputeMaterial> materials = disputeMaterialDao.selectList(
                new LambdaQueryWrapper<BasketMatchDataDisputeMaterial>()
                        .eq(BasketMatchDataDisputeMaterial::getDisputeId, disputeId)
                        .eq(BasketMatchDataDisputeMaterial::getIsDelete, NOT_DELETED)
                        .orderByAsc(BasketMatchDataDisputeMaterial::getSortNo));
        return materials.stream().map(m -> String.valueOf(m.getFileId())).collect(Collectors.toList());
    }

    private void parseExpectedScores(String expectedValue, PlayerMatchDisputeDetailResp resp) {
        if (!StringUtils.hasText(expectedValue) || !expectedValue.contains(":")) {
            return;
        }
        String[] parts = expectedValue.split(":");
        if (parts.length != 2) {
            return;
        }
        try {
            resp.setExpectedHomeScore(Integer.parseInt(parts[0].trim()));
            resp.setExpectedAwayScore(Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException ignored) {
            // ignore malformed expected value
        }
    }

    private String buildExpectedValue(Integer expectedHomeScore, Integer expectedAwayScore) {
        if (expectedHomeScore == null && expectedAwayScore == null) {
            return null;
        }
        return nvl(expectedHomeScore) + ":" + nvl(expectedAwayScore);
    }

    private BasketMatchDataSubmission findLatestApprovedSubmission(Long matchId) {
        LambdaQueryWrapper<BasketMatchDataSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatchDataSubmission::getMatchId, matchId)
                .eq(BasketMatchDataSubmission::getIsDelete, NOT_DELETED)
                .eq(BasketMatchDataSubmission::getSubmissionStatus, MatchDataSubmissionStatusEnum.APPROVED.getCode())
                .orderByDesc(BasketMatchDataSubmission::getVersionNo)
                .last("LIMIT 1");
        return submissionDao.selectOne(wrapper);
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
        if (findCaptainTeamIdsForMatch(memberId, match).isEmpty()) {
            throw new ServiceException("无权查看该比赛争议", 403);
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

    private int nvl(Integer value) {
        return value != null ? value : 0;
    }

    private String resolveDisputeTypeText(String disputeType) {
        if (!StringUtils.hasText(disputeType)) {
            return null;
        }
        try {
            return MatchDataDisputeTypeEnum.fromCode(disputeType).getDescription();
        } catch (ServiceException ex) {
            return disputeType;
        }
    }

    private String resolveDisputeStatusText(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return MatchDataDisputeStatusEnum.fromCode(status).getDescription();
        } catch (ServiceException ex) {
            return status;
        }
    }
}
