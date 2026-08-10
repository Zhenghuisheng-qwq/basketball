#!/usr/bin/env python3
"""Run CNPA batch3 migration on local dev DB."""
import pymysql

HOST = "192.168.31.48"
USER = "root"
PASSWORD = "123456"
DATABASE = "basketball"

STATEMENTS = [
    """ALTER TABLE basket_player_cert_application
       ADD COLUMN normalized_game_account varchar(128) NULL AFTER game_account""",
    """UPDATE basket_player_cert_application
       SET normalized_game_account = LOWER(TRIM(game_account))
       WHERE normalized_game_account IS NULL AND game_account IS NOT NULL""",
    """ALTER TABLE basket_player_cert_application
       ADD KEY idx_cert_platform_normalized (game_terminal, normalized_game_account)""",
    """CREATE TABLE IF NOT EXISTS basket_player_platform_account (
        id bigint NOT NULL AUTO_INCREMENT,
        player_id bigint NOT NULL,
        platform varchar(16) NOT NULL,
        display_game_id varchar(128) NOT NULL,
        normalized_game_id varchar(128) NOT NULL,
        account_status tinyint NOT NULL DEFAULT 1,
        is_primary tinyint NOT NULL DEFAULT 0,
        source_application_id bigint NULL,
        approved_by bigint NULL,
        approved_time datetime NULL,
        effective_time datetime NULL,
        invalid_time datetime NULL,
        create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
        update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        is_delete tinyint NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        UNIQUE KEY uk_platform_normalized_active (platform, normalized_game_id, is_delete),
        KEY idx_platform_player (player_id, platform, account_status, is_delete),
        KEY idx_platform_primary (player_id, is_primary, is_delete)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""",
    """CREATE TABLE IF NOT EXISTS basket_player_platform_change_request (
        id bigint NOT NULL AUTO_INCREMENT,
        request_no varchar(64) NOT NULL,
        player_id bigint NOT NULL,
        member_id bigint NOT NULL,
        change_type varchar(32) NOT NULL,
        platform varchar(16) NOT NULL,
        platform_account_id bigint NULL,
        old_display_game_id varchar(128) NULL,
        new_display_game_id varchar(128) NOT NULL,
        new_normalized_game_id varchar(128) NOT NULL,
        proof_file_id bigint NULL,
        request_status tinyint NOT NULL DEFAULT 0,
        submit_time datetime NULL,
        reviewed_time datetime NULL,
        reviewer_id bigint NULL,
        review_remark varchar(1000) NULL,
        result_platform_account_id bigint NULL,
        create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
        update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        is_delete tinyint NOT NULL DEFAULT 0,
        PRIMARY KEY (id),
        UNIQUE KEY uk_platform_change_request_no (request_no),
        KEY idx_platform_change_player (player_id, request_status, is_delete),
        KEY idx_platform_change_member (member_id, request_status, is_delete)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci""",
    """INSERT INTO basket_player_platform_account (
        player_id, platform, display_game_id, normalized_game_id, account_status, is_primary,
        approved_time, effective_time, create_time, update_time, is_delete
    )
    SELECT p.id, p.game_terminal, p.game_account, LOWER(TRIM(p.game_account)), 1, 1,
           p.certified_time, p.certified_time, NOW(), NOW(), 0
    FROM basket_player_profile p
    WHERE p.is_delete = 0
      AND p.certification_status = 3
      AND p.game_terminal IS NOT NULL AND p.game_account IS NOT NULL
      AND NOT EXISTS (
          SELECT 1 FROM basket_player_platform_account pa
          WHERE pa.player_id = p.id AND pa.is_delete = 0 AND pa.is_primary = 1
      )""",
]

MENU_STATEMENTS = [
    """INSERT INTO sys_menu (
        menu_id, menu_name, parent_id, order_num, path, component,
        query, route_name, is_frame, is_cache, menu_type, visible, status,
        perms, icon, create_by, create_time, remark
    ) VALUES
    (2210, '账号与球员管理', 2151, 4, 'account-player', 'cnpa/identity/account-player/index', '', 'BasketAccountPlayer', 1, 0, 'C', '0', '0', 'basket:accountPlayer:list', 'user', 'admin', NOW(), 'CNPA账号与球员管理')
    ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), parent_id=VALUES(parent_id), path=VALUES(path), component=VALUES(component), perms=VALUES(perms)""",
    """INSERT INTO sys_menu (
        menu_id, menu_name, parent_id, order_num, path, component,
        query, route_name, is_frame, is_cache, menu_type, visible, status,
        perms, icon, create_by, create_time, remark
    ) VALUES
    (2211, '平台游戏ID变更', 2151, 5, 'platform-change', 'cnpa/identity/platform-change/index', '', 'BasketPlayerPlatformChange', 1, 0, 'C', '0', '0', 'basket:playerPlatformChange:list', 'edit', 'admin', NOW(), 'CNPA平台游戏ID变更审核')
    ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), parent_id=VALUES(parent_id), path=VALUES(path), component=VALUES(component), perms=VALUES(perms)""",
    """INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2210)""",
    """INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2211)""",
    """INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
    VALUES (2212, '账号球员查询', 2210, 1, '#', '', 'F', '0', '0', 'basket:accountPlayer:query', 'admin', NOW())
    ON DUPLICATE KEY UPDATE perms=VALUES(perms)""",
    """INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
    VALUES (2213, '平台变更查询', 2211, 1, '#', '', 'F', '0', '0', 'basket:playerPlatformChange:query', 'admin', NOW())
    ON DUPLICATE KEY UPDATE perms=VALUES(perms)""",
    """INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
    VALUES (2214, '平台变更审核', 2211, 2, '#', '', 'F', '0', '0', 'basket:playerPlatformChange:audit', 'admin', NOW())
    ON DUPLICATE KEY UPDATE perms=VALUES(perms)""",
    """INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2212)""",
    """INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2213)""",
    """INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2214)""",
]


def main():
    conn = pymysql.connect(host=HOST, user=USER, password=PASSWORD, database=DATABASE, charset="utf8mb4")
    cur = conn.cursor()
    skip_msgs = ("Duplicate column", "Duplicate key", "already exists")
    for sql in STATEMENTS + MENU_STATEMENTS:
        try:
            cur.execute(sql)
            print("OK:", sql[:60].replace("\n", " "))
        except Exception as e:
            if any(s in str(e) for s in skip_msgs):
                print("SKIP:", str(e)[:100])
            else:
                print("ERR:", e)
                raise
    conn.commit()
    cur.close()
    conn.close()
    print("DONE")


if __name__ == "__main__":
    main()
