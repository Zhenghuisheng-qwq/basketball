-- CNPA 邮箱登录体系迁移脚本（本地/测试环境）
-- 用途：
-- 1. 标准化历史邮箱（trim + lower）
-- 2. 为缺少内部 username 的账号生成 cnpa{memberId}
-- 3. 输出重复邮箱清单供人工处理
-- 执行前请先备份 basket_member_account 表

-- ========== 第一步：检查重复邮箱（执行后人工确认，勿直接删用户） ==========
SELECT
    LOWER(TRIM(email)) AS normalized_email,
    COUNT(*) AS total,
    GROUP_CONCAT(id ORDER BY create_time) AS member_ids,
    GROUP_CONCAT(username ORDER BY create_time) AS usernames,
    GROUP_CONCAT(IFNULL(last_login_time, 'NULL') ORDER BY create_time) AS last_login_times,
    GROUP_CONCAT(create_time ORDER BY create_time) AS create_times
FROM basket_member_account
WHERE is_delete = 0
  AND email IS NOT NULL
  AND TRIM(email) <> ''
GROUP BY LOWER(TRIM(email))
HAVING COUNT(*) > 1;

-- ========== 第二步：标准化邮箱 ==========
UPDATE basket_member_account
SET email = LOWER(TRIM(email)),
    update_time = NOW()
WHERE is_delete = 0
  AND email IS NOT NULL
  AND TRIM(email) <> ''
  AND email <> LOWER(TRIM(email));

-- ========== 第三步：为空邮箱的历史测试账号补充占位（生产勿用，需人工补真实邮箱） ==========
-- 仅标记 email_verified=0，不伪造可登录邮箱
-- UPDATE basket_member_account SET email_verified = 0 WHERE is_delete = 0 AND (email IS NULL OR TRIM(email) = '');

-- ========== 第四步：为历史用户生成内部兼容 username（不用于登录） ==========
UPDATE basket_member_account
SET username = CONCAT('cnpa', id),
    update_time = NOW()
WHERE is_delete = 0
  AND (username IS NULL OR TRIM(username) = '' OR username NOT LIKE 'cnpa%');

-- ========== 第五步：处理 username 冲突（若第四步与其他 username 冲突，需人工调整） ==========
-- SELECT id, username FROM basket_member_account WHERE is_delete = 0 GROUP BY username HAVING COUNT(*) > 1;

-- 说明：
-- - uk_member_email 已存在于 basketball.sql，标准化后若仍有重复需先合并/禁用重复账号再加索引
-- - 已有唯一且已验证邮箱的历史用户：迁移后使用「原邮箱 + 原密码」登录
-- - 无邮箱账号：需后台补充或首次登录补邮箱（生产方案，本次未实现补邮箱页面）
-- - 旧 username 字段保留，仅作内部兼容标识
