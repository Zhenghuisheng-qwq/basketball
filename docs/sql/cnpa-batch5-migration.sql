-- CNPA Batch5 migration: match audit, official result, dispute, revision, settlement batch, stat sync, roster snapshot
-- Execute via docs/sql/run_batch5_migration.py

-- basket_match extensions
ALTER TABLE basket_match
    ADD COLUMN result_status tinyint NOT NULL DEFAULT 0 COMMENT '结果状态 0无 1正常 2弃赛 3管理员裁定 4重赛 5作废' AFTER data_status,
    ADD COLUMN score_confirmed tinyint NOT NULL DEFAULT 0 COMMENT '比分已确认标记' AFTER result_status,
    ADD COLUMN data_version int NOT NULL DEFAULT 1 COMMENT '当前数据版本号' AFTER score_confirmed,
    ADD COLUMN origin_match_id bigint DEFAULT NULL COMMENT '原比赛主键重赛关联' AFTER data_version,
    ADD COLUMN settlement_locked tinyint NOT NULL DEFAULT 0 COMMENT '结算锁定标记' AFTER origin_match_id;

ALTER TABLE basket_match ADD KEY idx_match_data_status (season_id, data_status, match_status);

-- basket_match_roster snapshot extensions
ALTER TABLE basket_match_roster
    ADD COLUMN season_roster_id bigint DEFAULT NULL COMMENT '正式赛事阵容主键' AFTER registration_player_id,
    ADD COLUMN event_team_id bigint DEFAULT NULL COMMENT '赛事球队主键' AFTER season_roster_id,
    ADD COLUMN player_nickname_snapshot varchar(128) DEFAULT NULL COMMENT '球员昵称快照' AFTER player_id,
    ADD COLUMN platform_snapshot varchar(16) DEFAULT NULL COMMENT '平台快照' AFTER player_nickname_snapshot,
    ADD COLUMN game_id_display_snapshot varchar(128) DEFAULT NULL COMMENT '游戏ID展示快照' AFTER platform_snapshot,
    ADD COLUMN roster_status tinyint NOT NULL DEFAULT 0 COMMENT '名单状态 0草稿 1已锁定' AFTER captain_flag,
    ADD COLUMN locked_time datetime DEFAULT NULL COMMENT '锁定时间' AFTER roster_status,
    ADD COLUMN data_version int NOT NULL DEFAULT 1 COMMENT '名单数据版本' AFTER locked_time;

ALTER TABLE basket_match_roster ADD KEY idx_roster_season_roster (season_roster_id);

-- formal official result (one current per match enforced by app + partial unique when current=1)
CREATE TABLE IF NOT EXISTS basket_match_official_result (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    match_id bigint NOT NULL COMMENT '比赛主键',
    data_version int NOT NULL DEFAULT 1 COMMENT '数据版本号',
    home_score int NOT NULL DEFAULT 0 COMMENT '主队正式比分',
    away_score int NOT NULL DEFAULT 0 COMMENT '客队正式比分',
    winner_team_id bigint DEFAULT NULL COMMENT '胜方球队主键',
    loser_team_id bigint DEFAULT NULL COMMENT '负方球队主键',
    is_draw tinyint NOT NULL DEFAULT 0 COMMENT '是否平局',
    result_type varchar(32) NOT NULL DEFAULT 'NORMAL' COMMENT '结果类型',
    effective_time datetime NOT NULL COMMENT '生效时间',
    confirmed_by bigint DEFAULT NULL COMMENT '审核人主键',
    confirmed_time datetime DEFAULT NULL COMMENT '审核时间',
    is_current tinyint NOT NULL DEFAULT 1 COMMENT '是否当前生效',
    is_public tinyint NOT NULL DEFAULT 1 COMMENT '是否公开',
    revision_no int NOT NULL DEFAULT 1 COMMENT '修订号',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_match_official_current (match_id, is_current, is_delete),
    KEY idx_official_match_version (match_id, data_version),
    KEY idx_official_effective (effective_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='正式比赛结果表';

-- audit records
CREATE TABLE IF NOT EXISTS basket_match_data_audit (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    match_id bigint NOT NULL COMMENT '比赛主键',
    data_version int NOT NULL DEFAULT 1 COMMENT '数据版本号',
    audit_type varchar(32) NOT NULL COMMENT '审核类型 SCORE STAT RESULT',
    submitter_id bigint DEFAULT NULL COMMENT '提交人主键',
    submit_time datetime DEFAULT NULL COMMENT '提交时间',
    audit_status varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
    auditor_id bigint DEFAULT NULL COMMENT '审核人主键',
    audit_time datetime DEFAULT NULL COMMENT '审核时间',
    audit_remark varchar(1000) DEFAULT NULL COMMENT '审核备注',
    reject_reason varchar(1000) DEFAULT NULL COMMENT '驳回原因',
    prev_audit_id bigint DEFAULT NULL COMMENT '上一版本审核主键',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_audit_match_status (match_id, audit_status, create_time),
    KEY idx_audit_pending (audit_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛数据审核记录表';

-- data dispute
CREATE TABLE IF NOT EXISTS basket_match_data_dispute (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    dispute_no varchar(64) NOT NULL COMMENT '争议编号',
    match_id bigint NOT NULL COMMENT '比赛主键',
    initiator_type varchar(32) NOT NULL COMMENT '发起方类型',
    initiator_id bigint NOT NULL COMMENT '发起人主键',
    dispute_type varchar(32) NOT NULL COMMENT '争议类型',
    original_value varchar(2000) DEFAULT NULL COMMENT '原值',
    expected_value varchar(2000) DEFAULT NULL COMMENT '期望值',
    dispute_reason varchar(2000) NOT NULL COMMENT '争议说明',
    evidence_file_ids varchar(500) DEFAULT NULL COMMENT '证明材料文件主键列表',
    dispute_status varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '争议状态',
    handler_id bigint DEFAULT NULL COMMENT '处理人主键',
    handle_time datetime DEFAULT NULL COMMENT '处理时间',
    handle_result varchar(32) DEFAULT NULL COMMENT '处理结果',
    handle_remark varchar(2000) DEFAULT NULL COMMENT '处理备注',
    revision_id bigint DEFAULT NULL COMMENT '关联修订主键',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dispute_no (dispute_no),
    KEY idx_dispute_match_status (match_id, dispute_status),
    KEY idx_dispute_pending (dispute_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛数据争议表';

-- data revision
CREATE TABLE IF NOT EXISTS basket_match_data_revision (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    revision_no varchar(64) NOT NULL COMMENT '修订编号',
    match_id bigint NOT NULL COMMENT '比赛主键',
    from_version int NOT NULL COMMENT '原数据版本号',
    to_version int NOT NULL COMMENT '新数据版本号',
    from_official_result_id bigint DEFAULT NULL COMMENT '原正式结果主键',
    to_official_result_id bigint DEFAULT NULL COMMENT '新正式结果主键',
    revision_reason varchar(2000) NOT NULL COMMENT '修订原因',
    revision_source varchar(32) NOT NULL COMMENT '修订来源',
    dispute_id bigint DEFAULT NULL COMMENT '关联争议主键',
    operator_id bigint NOT NULL COMMENT '操作人主键',
    auditor_id bigint DEFAULT NULL COMMENT '审核人主键',
    effective_time datetime DEFAULT NULL COMMENT '生效时间',
    revision_status varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '修订状态',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_revision_no (revision_no),
    KEY idx_revision_match (match_id, revision_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛数据修订表';

-- settlement batch
CREATE TABLE IF NOT EXISTS basket_data_settlement_batch (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_no varchar(64) NOT NULL COMMENT '批次编号',
    season_id bigint NOT NULL COMMENT '赛季主键',
    stage_id bigint DEFAULT NULL COMMENT '阶段主键',
    settlement_scope varchar(32) NOT NULL COMMENT '结算范围',
    start_time datetime NOT NULL COMMENT '开始时间',
    end_time datetime DEFAULT NULL COMMENT '结束时间',
    operator_id bigint NOT NULL COMMENT '操作人主键',
    batch_status varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '批次状态',
    data_version int DEFAULT NULL COMMENT '数据版本',
    match_count int NOT NULL DEFAULT 0 COMMENT '比赛数量',
    success_count int NOT NULL DEFAULT 0 COMMENT '成功数量',
    fail_count int NOT NULL DEFAULT 0 COMMENT '失败数量',
    error_message varchar(2000) DEFAULT NULL COMMENT '错误信息',
    is_current tinyint NOT NULL DEFAULT 1 COMMENT '是否当前有效批次',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_settlement_batch_no (batch_no),
    KEY idx_settlement_season_current (season_id, is_current, batch_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据结算批次表';

-- stat sync idempotency
CREATE TABLE IF NOT EXISTS basket_stat_sync_record (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    match_id bigint NOT NULL COMMENT '比赛主键',
    official_result_id bigint NOT NULL COMMENT '正式结果主键',
    sync_type varchar(32) NOT NULL COMMENT '同步类型',
    sync_status varchar(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '同步状态',
    idempotency_key varchar(128) NOT NULL COMMENT '幂等键',
    error_message varchar(2000) DEFAULT NULL COMMENT '错误信息',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stat_sync_idempotency (idempotency_key),
    KEY idx_stat_sync_match (match_id, sync_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统计同步记录表';

-- migrate existing confirmed matches to official results
INSERT INTO basket_match_official_result
    (match_id, data_version, home_score, away_score, winner_team_id, loser_team_id, is_draw, result_type,
     effective_time, confirmed_by, confirmed_time, is_current, is_public, revision_no, create_time, update_time, is_delete)
SELECT m.id, COALESCE(m.data_version, 1), m.home_score, m.away_score, m.winner_team_id,
       CASE WHEN m.winner_team_id = m.home_team_id THEN m.away_team_id
            WHEN m.winner_team_id = m.away_team_id THEN m.home_team_id ELSE NULL END,
       CASE WHEN m.home_score = m.away_score THEN 1 ELSE 0 END,
       CASE WHEN m.walkover_team_id IS NOT NULL THEN 'FORFEIT' ELSE 'NORMAL' END,
       COALESCE(m.actual_end_time, m.update_time), 0, m.update_time, 1, m.public_status, 1, NOW(), NOW(), 0
FROM basket_match m
WHERE m.is_delete = 0 AND m.data_status = 2 AND m.match_status = 3
  AND NOT EXISTS (
    SELECT 1 FROM basket_match_official_result r WHERE r.match_id = m.id AND r.is_current = 1 AND r.is_delete = 0
  );

UPDATE basket_match SET result_status = 1, score_confirmed = 1
WHERE is_delete = 0 AND data_status = 2 AND result_status = 0;

-- Admin menus batch5: 2230-2233
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2230, '比赛数据审核', 2205, 5, 'match-data-audit', 'cnpa/competition/match-data-audit/index', 'BasketMatchDataAudit', 1, 0, 'C', '0', '0', 'basket:matchAudit:list', 'checkbox', 'admin', NOW(), 'CNPA比赛数据审核')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2231, '数据争议处理', 2205, 6, 'match-dispute', 'cnpa/competition/match-dispute/index', 'BasketMatchDispute', 1, 0, 'C', '0', '0', 'basket:matchDispute:list', 'message', 'admin', NOW(), 'CNPA数据争议')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2232, '异常比赛处理', 2205, 7, 'match-abnormal', 'cnpa/competition/match-abnormal/index', 'BasketMatchAbnormal', 1, 0, 'C', '0', '0', 'basket:matchAbnormal:list', 'warning', 'admin', NOW(), 'CNPA异常比赛')
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), component=VALUES(component), perms=VALUES(perms);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2233, '驳回比赛数据', 2230, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'basket:matchAudit:reject', '#', 'admin', NOW(), NULL)
ON DUPLICATE KEY UPDATE perms=VALUES(perms);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2230);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2231);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2232);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2233);
