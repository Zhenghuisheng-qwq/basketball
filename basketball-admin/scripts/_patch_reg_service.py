from pathlib import Path

path = Path(r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\main\java\com\zhs\service\impl\v4\BasketV4PlayerRegistrationServiceImpl.java")
text = path.read_text(encoding="utf-8")

# imports
insert_imports = '''import com.zhs.response.v4.V4RegistrationDivisionContextResp;
import com.zhs.support.v4.TeamRegistrationEligibility;
import com.zhs.support.v4.TeamRegistrationEligibilitySupport;
'''
if "TeamRegistrationEligibilitySupport" not in text:
    text = text.replace(
        "import com.zhs.response.v4.V4RegistrationPlayerItemResp;\n",
        "import com.zhs.response.v4.V4RegistrationPlayerItemResp;\n" + insert_imports,
    )

# resource field
if "eligibilitySupport" not in text:
    text = text.replace(
        "    @Resource\n    private CaptainGrantSupport captainGrantSupport;\n",
        "    @Resource\n    private CaptainGrantSupport captainGrantSupport;\n"
        "    @Resource\n    private TeamRegistrationEligibilitySupport eligibilitySupport;\n"
        "    @Resource\n    private BasketSeasonDao seasonDao;\n"
        "    @Resource\n    private BasketSeriesDao seriesDao;\n",
    )

# add import for season/series dao if missing
if "import com.zhs.dao.v4.*;" not in text:
    raise SystemExit("expected wildcard dao import")

# add getDivisionContext method after listAvailableDivisions
context_method = '''
    @Override
    public V4RegistrationDivisionContextResp getDivisionContext(Long divisionId) {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        V4RegistrationDivisionContextResp resp = new V4RegistrationDivisionContextResp();
        resp.setDivisionId(divisionId);
        resp.setDivisionExists(false);
        resp.setCanRegister(false);
        resp.setCanViewRegistration(false);

        if (divisionId == null) {
            resp.setCannotRegisterReasonCode("DIVISION_NOT_FOUND");
            resp.setCannotRegisterReason("赛事不存在或已失效");
            resp.setRegistrationWindowStatus("CLOSED");
            resp.setRegistrationWindowStatusLabel("报名已截止");
            return resp;
        }

        BasketDivision division = divisionDao.selectById(divisionId);
        Date now = new Date();
        if (division == null || !NOT_DELETED.equals(division.getIsDelete())) {
            resp.setCannotRegisterReasonCode("DIVISION_NOT_FOUND");
            resp.setCannotRegisterReason("赛事不存在或已失效");
            resp.setRegistrationWindowStatus("CLOSED");
            resp.setRegistrationWindowStatusLabel("报名已截止");
            return resp;
        }

        resp.setDivisionExists(true);
        fillDivisionContextBase(resp, division);

        BasketCaptainGrant grant = findActiveGrant(divisionId, player.getId());
        BasketTeam team = null;
        if (grant != null && grant.getTeamId() != null) {
            team = teamDao.selectById(grant.getTeamId());
            resp.setGrantTeamId(grant.getTeamId());
            if (team != null) {
                resp.setGrantTeamName(team.getTeamName());
            }
            resp.setMyGrantStatus(eligibilitySupport.isGrantUsable(grant, now)
                    ? CaptainGrantStatusEnum.ACTIVE.getCode()
                    : grant.getGrantStatus());
        }

        BasketRegistration registration = findRegistrationByDivisionMember(divisionId, memberId);
        if (registration != null) {
            resp.setMyRegistrationStatus(registration.getRegistrationStatus());
            resp.setMyRegistrationId(registration.getId());
        }

        Integer approved = playerRegistrationMapper.countApprovedByDivisionId(divisionId);
        resp.setApprovedTeamCount(approved == null ? 0 : approved);
        if (division.getMaxTeamCount() != null) {
            resp.setMaxTeamCount(division.getMaxTeamCount());
            resp.setRemainingTeamCount(Math.max(division.getMaxTeamCount() - resp.getApprovedTeamCount(), 0));
        }

        if (RegistrationModeEnum.PERSONAL.getCode().equals(division.getRegistrationMode())) {
            resp.setCanRegister(false);
            resp.setCanViewRegistration(resp.getMyRegistrationId() != null);
            resp.setCannotRegisterReasonCode("DIVISION_NOT_OPEN");
            resp.setCannotRegisterReason("该 Division 为个人报名模式，请使用个人报名入口");
            resp.setRegistrationWindowStatus(eligibilitySupport.resolveWindowStatus(division, now).getCode());
            resp.setRegistrationWindowStatusLabel(eligibilitySupport.resolveWindowStatus(division, now).getLabel());
            return resp;
        }

        applyEligibility(resp, eligibilitySupport.evaluate(
                division, grant, team, resp.getMyRegistrationStatus(), resp.getRemainingTeamCount(), now));
        return resp;
    }

'''

if "getDivisionContext" not in text:
    text = text.replace(
        "        return rows;\n    }\n\n    @Override\n    @Transactional(rollbackFor = Exception.class)\n    public Long saveRegistration",
        "        return rows;\n    }\n" + context_method + "    @Override\n    @Transactional(rollbackFor = Exception.class)\n    public Long saveRegistration",
    )

# inject eligibility assert into saveRegistration after grant checks
save_anchor = '''        BasketCaptainGrant grant = requireActiveGrant(req.getDivisionId(), player.getId());
        captainGrantSupport.assertTeamBindingForRegistration(grant, team);

        BasketRegistration registration = findRegistration(req.getDivisionId(), req.getTeamId());
'''
save_replace = '''        BasketCaptainGrant grant = requireActiveGrant(req.getDivisionId(), player.getId());
        captainGrantSupport.assertTeamBindingForRegistration(grant, team);

        Date now = new Date();
        Integer approved = playerRegistrationMapper.countApprovedByDivisionId(division.getId());
        Integer remaining = null;
        if (division.getMaxTeamCount() != null) {
            remaining = Math.max(division.getMaxTeamCount() - (approved == null ? 0 : approved), 0);
        }
        BasketRegistration existingForEligibility = findRegistration(req.getDivisionId(), req.getTeamId());
        TeamRegistrationEligibility eligibility = eligibilitySupport.evaluate(
                division, grant, team,
                existingForEligibility == null ? null : existingForEligibility.getRegistrationStatus(),
                remaining, now);
        eligibilitySupport.assertCanMutateRegistration(eligibility);

        BasketRegistration registration = existingForEligibility;
'''
if save_anchor not in text:
    raise SystemExit('save anchor missing')
text = text.replace(save_anchor, save_replace, 1)

# remove duplicate Date now = new Date() in save - there's still one later
# After our replace, registration create block has `Date now = new Date();` - need to remove second declaration
text = text.replace(
'''        List<BasketRegistrationPlayer> lineup = buildLineup(division, req.getTeamId(), player.getId(), req.getPlayerIds());
        Date now = new Date();
        if (registration == null) {
''',
'''        List<BasketRegistrationPlayer> lineup = buildLineup(division, req.getTeamId(), player.getId(), req.getPlayerIds());
        if (registration == null) {
''',
1)

# submit eligibility check
submit_anchor = '''        requireActiveGrant(registration.getDivisionId(), registration.getCaptainPlayerId());
        List<BasketRegistrationPlayer> players = registrationPlayerDao.selectList(new LambdaQueryWrapper<BasketRegistrationPlayer>()
'''
submit_replace = '''        BasketDivision division = requireDivision(registration.getDivisionId());
        rejectPersonalModeDivision(division.getRegistrationMode());
        BasketCaptainGrant grant = requireActiveGrant(registration.getDivisionId(), registration.getCaptainPlayerId());
        BasketTeam team = requireValidTeamEntity(registration.getTeamId());
        Date eligibilityNow = new Date();
        Integer approvedCount = playerRegistrationMapper.countApprovedByDivisionId(division.getId());
        Integer remainingCount = null;
        if (division.getMaxTeamCount() != null) {
            remainingCount = Math.max(division.getMaxTeamCount() - (approvedCount == null ? 0 : approvedCount), 0);
        }
        TeamRegistrationEligibility submitEligibility = eligibilitySupport.evaluate(
                division, grant, team, registration.getRegistrationStatus(), remainingCount, eligibilityNow);
        // 驳回/草稿重提：窗口关闭后不允许再 mutate（与列表 canRegister 一致）
        if (!RegistrationStatusEnum.PENDING_REVIEW.getCode().equals(registration.getRegistrationStatus())
                && !RegistrationStatusEnum.APPROVED.getCode().equals(registration.getRegistrationStatus())) {
            eligibilitySupport.assertCanMutateRegistration(submitEligibility);
        }
        List<BasketRegistrationPlayer> players = registrationPlayerDao.selectList(new LambdaQueryWrapper<BasketRegistrationPlayer>()
'''
if submit_anchor not in text:
    raise SystemExit('submit anchor missing')
text = text.replace(submit_anchor, submit_replace, 1)

# replace enrichAvailability entirely
old_enrich_start = text.find("    private void enrichAvailability(V4AvailableDivisionResp row, Long playerId, Date now) {")
old_enrich_end = text.find("    private BasketCaptainGrant findActiveGrant(Long divisionId, Long playerId) {")
if old_enrich_start < 0 or old_enrich_end < 0:
    raise SystemExit('enrich block not found')

new_enrich = '''    private void enrichAvailability(V4AvailableDivisionResp row, Long playerId, Date now) {
        BasketDivision division = new BasketDivision();
        division.setId(row.getDivisionId());
        division.setSeasonId(row.getSeasonId());
        division.setDivisionStatus(row.getDivisionStatus());
        division.setRegistrationStartTime(row.getRegistrationStartTime());
        division.setRegistrationEndTime(row.getRegistrationEndTime());
        division.setIsDelete(NOT_DELETED);

        BasketCaptainGrant grant = findActiveGrant(row.getDivisionId(), playerId);
        BasketTeam team = null;
        if (row.getGrantTeamId() != null) {
            team = teamDao.selectById(row.getGrantTeamId());
        } else if (grant != null && grant.getTeamId() != null) {
            team = teamDao.selectById(grant.getTeamId());
            row.setGrantTeamId(grant.getTeamId());
            if (team != null) {
                row.setGrantTeamName(team.getTeamName());
            }
        }
        if (grant != null && eligibilitySupport.isGrantUsable(grant, now)) {
            row.setMyGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
        } else if (grant != null) {
            row.setMyGrantStatus(grant.getGrantStatus());
        }

        TeamRegistrationEligibility eligibility = eligibilitySupport.evaluate(
                division, grant, team, row.getMyRegistrationStatus(), row.getRemainingTeamCount(), now);
        applyEligibility(row, eligibility);
    }

    private void applyEligibility(V4AvailableDivisionResp row, TeamRegistrationEligibility eligibility) {
        row.setCanRegister(eligibility.isCanRegister());
        row.setCanViewRegistration(eligibility.isCanViewRegistration());
        row.setRegistrationWindowStatus(eligibility.getWindowStatusCode());
        row.setRegistrationWindowStatusLabel(eligibility.getWindowStatusLabel());
        row.setCannotRegisterReasonCode(eligibility.getReasonCode());
        row.setCannotRegisterReason(eligibility.isCanRegister() ? null : eligibility.getReasonMessage());
    }

    private void applyEligibility(V4RegistrationDivisionContextResp resp, TeamRegistrationEligibility eligibility) {
        resp.setCanRegister(eligibility.isCanRegister());
        resp.setCanViewRegistration(eligibility.isCanViewRegistration());
        resp.setRegistrationWindowStatus(eligibility.getWindowStatusCode());
        resp.setRegistrationWindowStatusLabel(eligibility.getWindowStatusLabel());
        resp.setCannotRegisterReasonCode(eligibility.getReasonCode());
        resp.setCannotRegisterReason(eligibility.isCanRegister() ? null : eligibility.getReasonMessage());
    }

    private void fillDivisionContextBase(V4RegistrationDivisionContextResp resp, BasketDivision division) {
        resp.setSeasonId(division.getSeasonId());
        resp.setDivisionName(division.getDivisionName());
        resp.setPlatformCode(division.getPlatformCode());
        resp.setRegistrationMode(division.getRegistrationMode());
        resp.setGameMode(division.getGameMode());
        resp.setDivisionStatus(division.getDivisionStatus());
        resp.setLineupMinCount(division.getLineupMinCount());
        resp.setLineupMaxCount(division.getLineupMaxCount());
        resp.setRegistrationStartTime(division.getRegistrationStartTime());
        resp.setRegistrationEndTime(division.getRegistrationEndTime());
        resp.setMatchStartTime(division.getMatchStartTime());
        resp.setMatchEndTime(division.getMatchEndTime());
        resp.setMaxTeamCount(division.getMaxTeamCount());
        BasketSeason season = seasonDao.selectById(division.getSeasonId());
        if (season != null) {
            resp.setSeasonTitle(season.getDisplayTitle());
            BasketSeries series = seriesDao.selectById(season.getSeriesId());
            if (series != null) {
                resp.setSeriesName(series.getSeriesName());
            }
        }
    }

    private BasketRegistration findRegistrationByDivisionMember(Long divisionId, Long memberId) {
        return registrationDao.selectOne(new LambdaQueryWrapper<BasketRegistration>()
                .eq(BasketRegistration::getDivisionId, divisionId)
                .eq(BasketRegistration::getCaptainMemberId, memberId)
                .eq(BasketRegistration::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

'''

text = text[:old_enrich_start] + new_enrich + text[old_enrich_end:]

# replace isGrantUsable private method to delegate
text = text.replace(
'''    private boolean isGrantUsable(BasketCaptainGrant grant, Date now) {
        if (grant == null) {
            return false;
        }
        if (grant.getValidFrom() != null && grant.getValidFrom().after(now)) {
            return false;
        }
        return grant.getValidTo() == null || !grant.getValidTo().before(now);
    }
''',
'''    private boolean isGrantUsable(BasketCaptainGrant grant, Date now) {
        return eligibilitySupport.isGrantUsable(grant, now);
    }
'''
)

path.write_text(text, encoding='utf-8', newline='\n')
print('service patched', path.stat().st_size)
