import pymysql

SQL = """
CREATE TABLE IF NOT EXISTS basket_competition_season (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    season_no VARCHAR(32) NOT NULL COMMENT '赛季编号',
    season_name VARCHAR(128) NOT NULL COMMENT '赛季名称',
    season_year INT NOT NULL COMMENT '赛季年份',
    start_time DATETIME NOT NULL COMMENT '赛季开始时间',
    end_time DATETIME NOT NULL COMMENT '赛季结束时间',
    season_status TINYINT NOT NULL DEFAULT 0 COMMENT '赛季状态',
    public_status TINYINT NOT NULL DEFAULT 0 COMMENT '公开状态',
    description LONGTEXT NULL COMMENT '赛季说明',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_competition_season_no (season_no),
    UNIQUE KEY uk_competition_season_year_name (season_year, season_name, is_delete),
    KEY idx_competition_season_status (season_status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='顶层赛季表'
"""

ALTER_COLUMNS = [
    "ALTER TABLE basket_tournament ADD COLUMN competition_season_id BIGINT DEFAULT NULL COMMENT '所属顶层赛季主键' AFTER tournament_no",
    "ALTER TABLE basket_tournament ADD COLUMN registration_start_time DATETIME DEFAULT NULL COMMENT '报名开始时间' AFTER description",
    "ALTER TABLE basket_tournament ADD COLUMN registration_end_time DATETIME DEFAULT NULL COMMENT '报名结束时间' AFTER registration_start_time",
    "ALTER TABLE basket_tournament ADD COLUMN competition_start_time DATETIME DEFAULT NULL COMMENT '比赛开始时间' AFTER registration_end_time",
    "ALTER TABLE basket_tournament ADD COLUMN competition_end_time DATETIME DEFAULT NULL COMMENT '比赛结束时间' AFTER competition_start_time",
    "ALTER TABLE basket_tournament ADD KEY idx_tournament_competition_season (competition_season_id, tournament_status)",
]


def main() -> None:
    conn = pymysql.connect(
        host="192.168.31.48",
        port=3306,
        user="root",
        password="123456",
        database="basketball",
        charset="utf8mb4",
    )
    cur = conn.cursor()
    cur.execute(SQL)
    print("created basket_competition_season")
    for stmt in ALTER_COLUMNS:
        try:
            cur.execute(stmt)
            print("ok:", stmt.split("ADD")[1].split()[1] if "ADD COLUMN" in stmt else "index")
        except pymysql.err.OperationalError as exc:
            if exc.args[0] == 1060 or exc.args[0] == 1061:
                print("skip exists:", stmt[:80])
            else:
                raise
    conn.commit()
    cur.execute("SHOW TABLES LIKE 'basket_competition_season'")
    print("tables:", cur.fetchall())
    cur.close()
    conn.close()


if __name__ == "__main__":
    main()
