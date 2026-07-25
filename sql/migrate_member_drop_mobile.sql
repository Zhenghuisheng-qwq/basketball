 -- 官网账号以邮箱为唯一凭证，删除会员手机号相关字段
ALTER TABLE basket_member_account
  DROP INDEX uk_member_mobile,
  DROP COLUMN mobile_country_code,
  DROP COLUMN mobile,
  DROP COLUMN mobile_verified;
