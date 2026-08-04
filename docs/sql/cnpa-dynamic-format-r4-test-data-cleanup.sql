-- =============================================================================
-- CNPA 动态赛制 R4 测试数据清理
-- 按 test_batch_code 清理标记的测试数据
-- 警告：仅清理 basket_r4_test_marker 标记的数据，不删除非测试 Division
-- =============================================================================

SET NAMES utf8mb4;

SET @test_batch_code = 'R4_TEST_BATCH_20260805';

-- 1. 清理 R4 测试 Division（按名称前缀，仅无生产关联的测试数据）
-- 实际执行前请确认 division_name LIKE 'R4_TEST_%'

-- 示例清理顺序（需按外键依赖逆序，此处仅标记待清理 ID）：
SELECT entity_type, entity_id, remark
FROM basket_r4_test_marker
WHERE test_batch_code = @test_batch_code
ORDER BY entity_type, entity_id;

-- 2. 删除 marker 记录
-- DELETE FROM basket_r4_test_marker WHERE test_batch_code = @test_batch_code;

-- 3. 按 Division 名称前缀查找待清理测试赛区
SELECT id, division_name, division_status
FROM basket_division
WHERE is_delete = 0 AND division_name LIKE 'R4_TEST_%';

SELECT @test_batch_code AS test_batch_code, 'Review IDs above before executing DELETE' AS status;
