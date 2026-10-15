-- 手动执行，不随应用启动。
-- 只失效「赛区未冻结，且球员已不是该队当前成员」的报名行。
-- 不修改已结束、已归档、已开赛或已有球员统计的报名。
-- 已重新入队、当前仍是队员的历史报名行无法判断是否曾经离队，本脚本不处理，需人工核实。
-- 可重复执行。先看预览；确认后再执行事务。整段事务失败会回滚。

-- 1. 预览
SELECT r.id AS registration_id,
       r.registration_status,
       r.division_id,
       r.team_id,
       r.lineup_count,
       r.lineup_version,
       rp.id AS registration_player_id,
       rp.player_id,
       d.division_status
FROM basket_registration_player rp
INNER JOIN basket_registration r ON r.id = rp.registration_id AND r.is_delete = 0
INNER JOIN basket_division d ON d.id = r.division_id AND d.is_delete = 0
LEFT JOIN basket_team_member tm
    ON tm.team_id = r.team_id
   AND tm.player_id = rp.player_id
   AND tm.is_current = 1
   AND tm.membership_status = 1
   AND tm.is_delete = 0
WHERE rp.is_delete = 0
  AND r.registration_status IN ('PENDING_REVIEW', 'APPROVED')
  AND tm.id IS NULL
  AND d.division_status NOT IN ('FINISHED', 'ARCHIVED')
  AND NOT EXISTS (
      SELECT 1 FROM basket_match m
      WHERE m.division_id = r.division_id AND m.is_delete = 0
        AND m.match_status IN ('PLAYING', 'FINISHED', 'FORFEITED')
  )
  AND NOT EXISTS (
      SELECT 1 FROM basket_match_player_stat s
      WHERE s.division_id = r.division_id AND s.is_delete = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM basket_match_submission_player_stat ss
      WHERE ss.division_id = r.division_id AND ss.is_delete = 0
  );

-- 2. 确认预览后执行
START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_stale_roster_player;
CREATE TEMPORARY TABLE tmp_stale_roster_player AS
SELECT rp.id AS player_row_id,
       r.id AS registration_id,
       r.division_id,
       r.season_id,
       r.registration_status,
       rp.player_id
FROM basket_registration_player rp
INNER JOIN basket_registration r ON r.id = rp.registration_id AND r.is_delete = 0
INNER JOIN basket_division d ON d.id = r.division_id AND d.is_delete = 0
LEFT JOIN basket_team_member tm
    ON tm.team_id = r.team_id
   AND tm.player_id = rp.player_id
   AND tm.is_current = 1
   AND tm.membership_status = 1
   AND tm.is_delete = 0
WHERE rp.is_delete = 0
  AND r.registration_status IN ('PENDING_REVIEW', 'APPROVED')
  AND tm.id IS NULL
  AND d.division_status NOT IN ('FINISHED', 'ARCHIVED')
  AND NOT EXISTS (
      SELECT 1 FROM basket_match m
      WHERE m.division_id = r.division_id AND m.is_delete = 0
        AND m.match_status IN ('PLAYING', 'FINISHED', 'FORFEITED')
  )
  AND NOT EXISTS (
      SELECT 1 FROM basket_match_player_stat s
      WHERE s.division_id = r.division_id AND s.is_delete = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM basket_match_submission_player_stat ss
      WHERE ss.division_id = r.division_id AND ss.is_delete = 0
  );

DROP TEMPORARY TABLE IF EXISTS tmp_stale_roster_reg;
CREATE TEMPORARY TABLE tmp_stale_roster_reg AS
SELECT DISTINCT registration_id, division_id, season_id, registration_status
FROM tmp_stale_roster_player;

UPDATE basket_registration_player rp
INNER JOIN tmp_stale_roster_player t ON t.player_row_id = rp.id
SET rp.is_delete = 1,
    rp.update_time = NOW()
WHERE rp.is_delete = 0;

UPDATE basket_registration r
INNER JOIN tmp_stale_roster_reg t ON t.registration_id = r.id
SET r.lineup_count = (
        SELECT COUNT(1)
        FROM basket_registration_player rp
        WHERE rp.registration_id = r.id AND rp.is_delete = 0
    ),
    r.lineup_version = r.lineup_version + 1,
    r.update_time = NOW();

UPDATE basket_registration_roster_change c
INNER JOIN tmp_stale_roster_reg t ON t.registration_id = c.registration_id
SET c.change_status = 'REJECTED',
    c.audit_remark = '历史离队残留修正后，原名单变更申请失效',
    c.audit_time = NOW(),
    c.update_time = NOW()
WHERE c.is_delete = 0
  AND c.change_status = 'PENDING';

INSERT INTO basket_registration_audit
    (registration_id, division_id, season_id, audit_action, before_status, after_status,
     audit_time, audit_remark, create_time, update_time, is_delete)
SELECT t.registration_id,
       t.division_id,
       t.season_id,
       'MEMBER_LEFT',
       t.registration_status,
       t.registration_status,
       NOW(),
       CONCAT('历史离队残留脚本失效，当前报名人数 ', r.lineup_count),
       NOW(),
       NOW(),
       0
FROM tmp_stale_roster_reg t
INNER JOIN basket_registration r ON r.id = t.registration_id;

COMMIT;
