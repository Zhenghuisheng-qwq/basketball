-- R7: system notification template code. Historical rows stay null and keep original title/content.
ALTER TABLE basket_message_task
    ADD COLUMN template_code varchar(64) NULL COMMENT '系统通知模板编码，历史消息为空' AFTER content_text,
    ADD COLUMN template_params text NULL COMMENT '模板参数 JSON' AFTER template_code;
