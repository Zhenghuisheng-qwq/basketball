-- CNPA 球员认证去 KYC：删除实名/证件/手机字段
-- 执行前请备份。已在本地 basketball 库执行。

ALTER TABLE basket_player_cert_application
  DROP COLUMN real_name,
  DROP COLUMN id_type,
  DROP COLUMN id_no_cipher,
  DROP COLUMN id_no_masked,
  DROP COLUMN contact_mobile,
  MODIFY COLUMN game_nickname VARCHAR(128) NULL COMMENT '游戏昵称',
  MODIFY COLUMN position_code VARCHAR(16) NOT NULL COMMENT '场上位置';

ALTER TABLE basket_player_profile
  DROP COLUMN real_name,
  DROP COLUMN id_type,
  DROP COLUMN id_no_cipher,
  DROP COLUMN id_no_masked;
