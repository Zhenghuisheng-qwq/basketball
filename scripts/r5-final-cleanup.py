#!/usr/bin/env python3
"""R5 final cleanup: delete legacy Tournament domain files."""
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

WEB_GLOBS = [
    "basketball-web/src/main/java/com/zhs/model/BasketTournament*.java",
    "basketball-web/src/main/java/com/zhs/dao/BasketTournament*.java",
    "basketball-web/src/main/java/com/zhs/mapper/BasketTournament*.xml",
    "basketball-web/src/main/java/com/zhs/dto/BasketTournament*.java",
    "basketball-web/src/main/java/com/zhs/request/*BasketTournament*.java",
    "basketball-web/src/main/java/com/zhs/response/BasketTournament*.java",
]

WEB_FILES = [
    "basketball-web/src/main/java/com/zhs/controller/BasketTournamentController.java",
    "basketball-web/src/main/java/com/zhs/controller/BasketTournamentAwardConfigController.java",
    "basketball-web/src/main/java/com/zhs/service/IBasketTournamentService.java",
    "basketball-web/src/main/java/com/zhs/service/IBasketTournamentAwardConfigService.java",
    "basketball-web/src/main/java/com/zhs/service/impl/BasketTournamentServiceImpl.java",
    "basketball-web/src/main/java/com/zhs/service/impl/BasketTournamentAwardConfigServiceImpl.java",
    "basketball-web/src/main/java/com/zhs/service/IBasketPublicTournamentService.java",
    "basketball-web/src/main/java/com/zhs/service/impl/BasketPublicTournamentServiceImpl.java",
    "basketball-web/src/main/java/com/zhs/controller/publicapi/BasketPublicTournamentController.java",
    "basketball-web/src/test/java/com/zhs/service/impl/RosterChangeBusinessServiceImplTest.java",
    "basketball-web/src/test/java/com/zhs/service/impl/OpenRegistrationBusinessServiceImplTest.java",
    "basketball-web/src/main/java/com/zhs/dao/BasketCompetitionSeasonDao.java",
    "basketball-web/src/main/java/com/zhs/model/BasketCompetitionSeason.java",
    "basketball-web/src/main/java/com/zhs/dao/BasketTeamSeasonProfileDao.java",
    "basketball-web/src/main/java/com/zhs/model/BasketTeamSeasonProfile.java",
    "basketball-web/src/main/java/com/zhs/dao/BasketTeamSeasonStatDao.java",
    "basketball-web/src/main/java/com/zhs/model/BasketTeamSeasonStat.java",
    "basketball-web/src/main/java/com/zhs/dao/BasketPlayerSeasonStatDao.java",
    "basketball-web/src/main/java/com/zhs/model/BasketPlayerSeasonStat.java",
    "basketball-web/src/main/java/com/zhs/dao/BasketStageStandingDao.java",
    "basketball-web/src/main/java/com/zhs/model/BasketStageStanding.java",
    "basketball-web/src/main/java/com/zhs/service/IBasketPublicSeasonStatService.java",
    "basketball-web/src/main/java/com/zhs/service/impl/BasketPublicSeasonStatServiceImpl.java",
]

ADMIN_FILES = [
    "basketball-admin/src/main/java/com/zhs/basket/domain/BasketTournament.java",
    "basketball-admin/src/main/java/com/zhs/basket/domain/BasketTournamentSeason.java",
    "basketball-admin/src/main/java/com/zhs/basket/domain/BasketTournamentAdminScope.java",
    "basketball-admin/src/main/java/com/zhs/basket/mapper/BasketTournamentMapper.java",
    "basketball-admin/src/main/java/com/zhs/basket/mapper/BasketTournamentSeasonMapper.java",
    "basketball-admin/src/main/java/com/zhs/basket/mapper/BasketTournamentAdminScopeMapper.java",
    "basketball-admin/src/main/resources/mapper/basket/BasketTournamentMapper.xml",
    "basketball-admin/src/main/resources/mapper/basket/BasketTournamentSeasonMapper.xml",
    "basketball-admin/src/main/resources/mapper/basket/BasketTournamentAdminScopeMapper.xml",
    "basketball-admin/src/main/java/com/zhs/basket/service/IBasketTournamentService.java",
    "basketball-admin/src/main/java/com/zhs/basket/service/impl/BasketTournamentServiceImpl.java",
    "basketball-admin/src/main/java/com/zhs/web/controller/basket/BasketTournamentController.java",
    "basketball-admin/src/main/java/com/zhs/basket/service/IBasketTournamentScopeService.java",
    "basketball-admin/src/main/java/com/zhs/basket/service/impl/BasketTournamentScopeServiceImpl.java",
    "basketball-admin/src/main/java/com/zhs/web/controller/basket/BasketTournamentScopeController.java",
    "basketball-admin/src/main/java/com/zhs/basket/support/BasketTournamentScopeSupport.java",
    "basketball-admin/src/main/resources/mapper/basket/BasketCompetitionSeasonMapper.xml",
    "basketball-admin/src/main/resources/mapper/basket/BasketTeamSeasonProfileMapper.xml",
    "basketball-admin-ui/src/api/basket/tournament.ts",
    "basketball-admin-ui/src/api/basket/tournamentScope.ts",
    "basketball-admin-ui/src/views/cnpa/discipline/tournament-scope/index.vue",
]


def main() -> None:
    deleted: list[str] = []
    for pattern in WEB_GLOBS:
        for path in ROOT.glob(pattern):
            if path.is_file():
                path.unlink()
                deleted.append(str(path.relative_to(ROOT)))
    for rel in WEB_FILES + ADMIN_FILES:
        path = ROOT / rel
        if path.is_file():
            path.unlink()
            deleted.append(rel)
    out = ROOT / "docs" / "r5-final-deleted-files.txt"
    out.write_text("\n".join(sorted(deleted)) + "\n", encoding="utf-8")
    print(f"Deleted {len(deleted)} files -> {out}")


if __name__ == "__main__":
    main()
