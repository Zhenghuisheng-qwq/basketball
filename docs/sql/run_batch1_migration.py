import pymysql

statements = [
    "ALTER TABLE basket_captain_qualification ADD COLUMN season_id BIGINT NULL COMMENT 'season' AFTER player_id",
    "ALTER TABLE basket_captain_qualification ADD COLUMN team_id BIGINT NULL COMMENT 'team' AFTER season_id",
    "ALTER TABLE basket_captain_qualification ADD COLUMN remark VARCHAR(500) NULL COMMENT 'remark' AFTER revoke_reason",
    """CREATE TABLE IF NOT EXISTS basket_tournament_season_platform (
        id BIGINT NOT NULL AUTO_INCREMENT,
        season_id BIGINT NOT NULL,
        platform_code VARCHAR(16) NOT NULL,
        platform_status TINYINT NOT NULL DEFAULT 1,
        create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        is_delete TINYINT NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        UNIQUE KEY uk_season_platform (season_id, platform_code, is_delete)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    "ALTER TABLE basket_tournament_registration ADD COLUMN platform_code VARCHAR(16) NULL COMMENT 'platform' AFTER team_id",
    "ALTER TABLE basket_team_member ADD COLUMN active_member_slot VARCHAR(64) NULL COMMENT 'member slot' AFTER active_platform_slot",
]

conn = pymysql.connect(host='192.168.31.49', user='root', password='123456', database='basketball', charset='utf8mb4')
cur = conn.cursor()

for stmt in statements:
    try:
        cur.execute(stmt)
        print('OK', stmt.split('\n')[0][:80])
    except pymysql.err.OperationalError as e:
        if e.args[0] == 1060:
            print('SKIP column exists')
        else:
            raise

cur.execute(
    "UPDATE basket_team_member SET active_member_slot=CAST(member_id AS CHAR) "
    "WHERE is_delete=0 AND is_current=1 AND membership_status=1 AND active_member_slot IS NULL"
)
print('backfill active_member_slot', cur.rowcount)

cur.execute(
    "UPDATE basket_tournament_registration r INNER JOIN basket_team tm ON tm.id=r.team_id "
    "SET r.platform_code=CASE WHEN UPPER(TRIM(tm.game_terminal)) IN ('PS5','PSN') THEN 'PS' "
    "WHEN UPPER(TRIM(tm.game_terminal)) IN ('PC','PS','XBOX') THEN UPPER(TRIM(tm.game_terminal)) ELSE 'PC' END "
    "WHERE r.platform_code IS NULL AND r.is_delete=0"
)
print('backfill registration platform', cur.rowcount)

for stmt in [
    "ALTER TABLE basket_team_member ADD UNIQUE KEY uk_team_member_active_member_slot (active_member_slot)",
    "ALTER TABLE basket_tournament_registration DROP INDEX uk_registration_season_team",
    "ALTER TABLE basket_tournament_registration ADD UNIQUE KEY uk_registration_season_team_platform (season_id, team_id, platform_code, is_delete)",
    "ALTER TABLE basket_team MODIFY COLUMN game_terminal VARCHAR(16) NOT NULL DEFAULT 'GENERAL' COMMENT 'deprecated team platform'",
]:
    try:
        cur.execute(stmt)
        print('OK', stmt[:70])
    except pymysql.err.OperationalError as e:
        if e.args[0] in (1061, 1091):
            print('SKIP index', stmt[:50])
        else:
            raise

conn.commit()
cur.close()
conn.close()
print('migration complete')
