# CNPA 赛制规则专项测试报告

- 批次: `FORMAT_RULE_UI_20260806`
- 时间: 2026-08-05T18:32:55.267Z
- Admin 后端: http://127.0.0.1:8080
- Admin UI: http://127.0.0.1:80
- seasonId: **15**
- divisionAId: **41**
- divisionBId: **42**
- 结论: **存在未通过项，暂不可标记验收通过** (38 PASS / 1 FAIL)

## 测试结果明细

| 项 | 状态 | 详情 |
|---|---|---|
| env_admin_backend | PASS | http://127.0.0.1:8080 |
| env_admin_ui | PASS | http://127.0.0.1:80 |
| env_mysql | PASS | 192.168.31.48:3306/basketball |
| env_redis | PASS | PONG |
| api_admin_login | PASS | HTTP 200 token=eyJhbGciOiJI... |
| p0_season_soft_delete_uk | PASS | delete=200 recreate=200 |
| ui_login | PASS | url=http://127.0.0.1/index |
| ui_publish_page | PASS | http://127.0.0.1/event/event-v4-publish |
| setup_fresh_divisions | PASS | A=41 B=42 |
| ui_continue_publish_step4 | PASS | seasonId=15 |
| shared_step4_load | PASS | step4 visible |
| create_season | PASS | seasonId=15 |
| create_divisions | PASS | A=41 B=42 |
| shared_division_a_selected | PASS | Division A context |
| scene_a_round_robin_ui | PASS | has120=true stages=true |
| scene_a_round_robin_db | PASS | snapshotId=81 stages=1 |
| scene_b_group_round_ui | FAIL | preview has 24 matches |
| scene_b_group_round_db | PASS | group_count=4 stageCount=1 |
| shared_division_b_isolated | PASS | switched to B |
| scene_c_no_bo35 | PASS | no BO3/BO5 |
| scene_c_elim_ui | PASS | 8 teams 7 matches |
| scene_c_elim_db | PASS | snapshotId=83 |
| scene_d_group_to_elim_ui | PASS | flow+matches preview |
| scene_d_group_to_elim_db | PASS | s1=96 order=1 adv=GROUP_TOP_N/2 next=KNOCKOUT s2=97 fmt=SINGLE_ELIMINATION |
| scene_d_lock | PASS | locked=true |
| scene_d_lock_db | PASS | status=LOCKED |
| scene_e_rr_to_elim | PASS | stages=2 adv=OVERALL_TOP_N/4 |
| scene_f_illegal_6_advance | PASS | error shown=true applyDisabled=true lockDisabled=true |
| scene_f_api_reject | PASS | POST /basket/v4/divisions/41/format-plan/apply code=500 msg=当前将有 6 支球队晋级，但系统暂不支持 6 支球队自动生成单败淘汰赛。请调整为 4 支或 8 支晋级。 |
| scene_f_api_reject_b | PASS | POST /basket/v4/divisions/42/format-plan/apply code=500 msg=当前将有 6 支球队晋级，但系统暂不支持 6 支球队自动生成单败淘汰赛。请调整为 4 支或 8 支晋级。 |
| scene_f_lock_reject | PASS | POST /basket/v4/divisions/42/format-snapshot/lock code=500 msg=当前将有 6 支球队晋级，但系统暂不支持 6 支球队自动生成单败淘汰赛。请调整为 4 支或 8 支晋级。 |
| custom_mode_keep_stages | SKIP | 自定义赛制按钮不可用（可能已锁定） |
| isolation_a_after_b | PASS | A still group+elim |
| isolation_snapshot_ids | PASS | A snap=84 B snap=85 |
| step5_summary | PASS | step5 division summary visible |
| drawer_consistency | PASS | drawer shows locked group+elim |
| idempotent_lock | PASS | lock1=200 lock2=200 currentSnapshots=1 |
| network_no_500 | PASS | none |
| network_no_legacy_tournament | PASS | none |
| console_no_vue_warn | PASS | none |

## 数据库核对摘要
- Division A snapshotId=84 status=LOCKED stages=2
  - Stage 96: order=1 小组赛 ROUND_ROBIN bestOf=1 adv=GROUP_TOP_N/2 next=KNOCKOUT
  - Stage 97: order=2 淘汰赛 SINGLE_ELIMINATION bestOf=1 adv=NONE/null next=-
- Division B snapshotId=85 status=DRAFT stages=2

## P0 / P1
- P0: 无
- P1: scene_b_group_round_ui
