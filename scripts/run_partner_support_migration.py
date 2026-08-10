"""执行合作伙伴支持记录表 migration（幂等）。"""
import pymysql

HOST = "192.168.31.48"
PORT = 3306
USER = "root"
PASSWORD = "123456"
DATABASE = "basketball"

CREATE_SUPPORT_TABLE = """
CREATE TABLE IF NOT EXISTS basket_partner_support_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    partner_id BIGINT NOT NULL COMMENT '合作伙伴ID',
    support_type VARCHAR(32) NOT NULL COMMENT '支持方式',
    support_amount DECIMAL(12, 2) NULL COMMENT '支持金额',
    support_date DATE NOT NULL COMMENT '支持日期',
    public_description VARCHAR(1000) NULL COMMENT '公开说明',
    publish_status TINYINT NOT NULL DEFAULT 1 COMMENT '公开状态 0停用 1公开',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    create_by VARCHAR(64) NULL COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) NULL COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_partner_support_partner (partner_id),
    KEY idx_partner_support_public_sort (is_delete, publish_status, sort_order, support_date),
    KEY idx_partner_support_partner_public (partner_id, is_delete, publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合作伙伴支持记录表'
"""

PARTNER_ALTER_STATEMENTS = [
    "ALTER TABLE basket_partner ADD COLUMN partner_code VARCHAR(32) NULL COMMENT '合作伙伴编号' AFTER id",
    "ALTER TABLE basket_partner ADD COLUMN public_description VARCHAR(1000) NULL COMMENT '公开简介' AFTER website_url",
    "ALTER TABLE basket_partner ADD COLUMN create_by VARCHAR(64) NULL COMMENT '创建者' AFTER remark",
    "ALTER TABLE basket_partner ADD COLUMN update_by VARCHAR(64) NULL COMMENT '更新者' AFTER create_by",
]

PARTNER_INDEX_STATEMENTS = [
    "ALTER TABLE basket_partner ADD UNIQUE KEY uk_partner_code (partner_code)",
    "ALTER TABLE basket_partner ADD KEY idx_partner_delete_publish_sort (is_delete, partner_status, sort_no)",
]

BACKFILL_PARTNER_CODE = """
UPDATE basket_partner
SET partner_code = CONCAT('CNPA-PT-', LPAD(id, 6, '0'))
WHERE partner_code IS NULL OR partner_code = ''
"""


def column_exists(cursor, table, column):
    cursor.execute(
        """
        SELECT COUNT(1) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s AND COLUMN_NAME = %s
        """,
        (DATABASE, table, column),
    )
    return cursor.fetchone()[0] > 0


def index_exists(cursor, table, index):
    cursor.execute(
        """
        SELECT COUNT(1) FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s AND INDEX_NAME = %s
        """,
        (DATABASE, table, index),
    )
    return cursor.fetchone()[0] > 0


def table_exists(cursor, table):
    cursor.execute(
        """
        SELECT COUNT(1) FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s
        """,
        (DATABASE, table),
    )
    return cursor.fetchone()[0] > 0


def main():
    conn = pymysql.connect(
        host=HOST,
        port=PORT,
        user=USER,
        password=PASSWORD,
        database=DATABASE,
        charset="utf8mb4",
        autocommit=False,
    )
    try:
        with conn.cursor() as cursor:
            print("Creating basket_partner_support_record ...")
            cursor.execute(CREATE_SUPPORT_TABLE)
            print("  OK" if table_exists(cursor, "basket_partner_support_record") else "  FAILED")

            for sql in PARTNER_ALTER_STATEMENTS:
                col = sql.split("ADD COLUMN ")[1].split(" ")[0]
                if column_exists(cursor, "basket_partner", col):
                    print(f"Skip column {col} (exists)")
                    continue
                print(f"Adding column {col} ...")
                cursor.execute(sql)

            cursor.execute(BACKFILL_PARTNER_CODE)

            for sql in PARTNER_INDEX_STATEMENTS:
                idx = sql.split("ADD ")[1].split(" ")[-1]
                idx = idx.replace("KEY ", "").replace("UNIQUE ", "").strip()
                if "uk_partner_code" in sql:
                    idx = "uk_partner_code"
                elif "idx_partner_delete_publish_sort" in sql:
                    idx = "idx_partner_delete_publish_sort"
                if index_exists(cursor, "basket_partner", idx):
                    print(f"Skip index {idx} (exists)")
                    continue
                print(f"Adding index {idx} ...")
                try:
                    cursor.execute(sql)
                except pymysql.err.OperationalError as exc:
                    if exc.args[0] == 1061:
                        print(f"  Skip duplicate index {idx}")
                    else:
                        raise

        conn.commit()
        print("Migration completed.")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
