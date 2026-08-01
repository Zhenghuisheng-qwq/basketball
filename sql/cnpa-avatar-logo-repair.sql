-- CNPA 头像 / 球队 Logo 坏引用修复
-- 1. 修正雪花 ID 精度丢失导致的 file_id 偏差
-- 2. 为仍无有效头像的会员分配已有 ACCOUNT_AVATAR 池
-- 3. 为仍无有效 Logo 的球队分配已有 TEAM_LOGO 池

-- ========== 球员头像：精确修正已知截断 ID ==========
UPDATE basket_member_profile SET avatar_file_id = 2081011196244312065 WHERE avatar_file_id = 2081011196244312000;
UPDATE basket_member_profile SET avatar_file_id = 2081013405434560513 WHERE avatar_file_id = 2081013405434560500;
UPDATE basket_member_profile SET avatar_file_id = 2081019389284573186 WHERE avatar_file_id = 2081019389284573200;
UPDATE basket_member_profile SET avatar_file_id = 2081072846158700545 WHERE avatar_file_id = 2081072846158700500;
UPDATE basket_member_profile SET avatar_file_id = 2081073199587532802 WHERE avatar_file_id = 2081073199587532800;
UPDATE basket_member_profile SET avatar_file_id = 2081073445205975042 WHERE avatar_file_id = 2081073445205975000;
UPDATE basket_member_profile SET avatar_file_id = 2081079552771129346 WHERE avatar_file_id = 2081079552771129300;
UPDATE basket_member_profile SET avatar_file_id = 2082478424710045697 WHERE avatar_file_id = 2082478424710045700;
UPDATE basket_member_profile SET avatar_file_id = 2082786739050532865 WHERE avatar_file_id = 2082786739050532900;
UPDATE basket_member_profile SET avatar_file_id = 2083398443425968129 WHERE avatar_file_id = 2083398443425968000;

-- ========== 球队 Logo：精确修正已知截断 ID ==========
UPDATE basket_team SET logo_file_id = 2081047953174171650 WHERE logo_file_id = 2081047953174171600;
UPDATE basket_team_season_profile SET logo_file_id = 2081047953174171650 WHERE logo_file_id = 2081047953174171600;

-- ========== 会员：仍无有效头像的，按 id 轮询分配已有公共头像 ==========
UPDATE basket_member_profile mp
INNER JOIN (
    SELECT
        mp2.id AS profile_id,
        ELT(1 + MOD(mp2.id, 10),
            2081011196244312065,
            2081013405434560513,
            2081019389284573186,
            2081072846158700545,
            2081073199587532802,
            2081073445205975042,
            2081079552771129346,
            2082478424710045697,
            2082786739050532865,
            2083398443425968129
        ) AS new_avatar_file_id
    FROM basket_member_profile mp2
    WHERE mp2.is_delete = 0
      AND (
          mp2.avatar_file_id IS NULL
          OR NOT EXISTS (
              SELECT 1 FROM basket_file_asset fa
              WHERE fa.id = mp2.avatar_file_id AND fa.is_delete = 0
          )
      )
) fix ON fix.profile_id = mp.id
SET mp.avatar_file_id = fix.new_avatar_file_id;

-- ========== 球队：仍无有效 Logo 的，按 id 轮询分配已有 TEAM_LOGO ==========
UPDATE basket_team bt
INNER JOIN (
    SELECT
        bt2.id AS team_id,
        ELT(1 + MOD(bt2.id, 9),
            2081040245142446081,
            2081046848142958594,
            2081047953174171650,
            2081067518675390465,
            2083607240383664129,
            2083607291533201409,
            2083607366829346818,
            2083649545031397377,
            2083654311425884161
        ) AS new_logo_file_id
    FROM basket_team bt2
    WHERE bt2.is_delete = 0
      AND (
          bt2.logo_file_id IS NULL
          OR NOT EXISTS (
              SELECT 1 FROM basket_file_asset fa
              WHERE fa.id = bt2.logo_file_id AND fa.is_delete = 0
          )
      )
) fix ON fix.team_id = bt.id
SET bt.logo_file_id = fix.new_logo_file_id;

UPDATE basket_team_season_profile tsp
INNER JOIN (
    SELECT
        tsp2.id AS event_team_id,
        ELT(1 + MOD(tsp2.id, 9),
            2081040245142446081,
            2081046848142958594,
            2081047953174171650,
            2081067518675390465,
            2083607240383664129,
            2083607291533201409,
            2083607366829346818,
            2083649545031397377,
            2083654311425884161
        ) AS new_logo_file_id
    FROM basket_team_season_profile tsp2
    WHERE tsp2.is_delete = 0
      AND (
          tsp2.logo_file_id IS NULL
          OR NOT EXISTS (
              SELECT 1 FROM basket_file_asset fa
              WHERE fa.id = tsp2.logo_file_id AND fa.is_delete = 0
          )
      )
) fix ON fix.event_team_id = tsp.id
SET tsp.logo_file_id = fix.new_logo_file_id;
