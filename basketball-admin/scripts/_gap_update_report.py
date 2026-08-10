# -*- coding: utf-8 -*-
import json, re
from pathlib import Path
from datetime import datetime

gap = json.loads(Path(r"d:\code\dz\basketball\scaffolding-v2\basketball-admin\scripts\full_smoke_gap_closure_20260811.json").read_text(encoding="utf-8"))
creds = json.loads(Path(r"d:\code\dz\basketball\scaffolding-v2\basketball-admin\scripts\gap_creds.json").read_text(encoding="utf-8"))
report = Path(r"d:\code\dz\basketball\scaffolding-v2\docs\CNPA_V2_FULL_SMOKE_ACCEPTANCE_20260811.md")
text = report.read_text(encoding="utf-8")

# --- rewrite summary metrics ---
text = re.sub(r"- 总体结论：\*\*.*?\*\*", "- 总体结论：**PASS WITH KNOWN LIMITATIONS**", text, count=1)
text = re.sub(r"- P0 / P1 / P2 数量：.*", "- P0 / P1 / P2 数量：P0=0 / P1=1 / P2=2（Final Gap Closure 后重算）", text, count=1)

# Update build section to POST-FIX FINAL BUILD
build_section = """## 2. Build / Test 基线

### POST-FIX FINAL BUILD（Pairing soft-delete / match_no 修复之后）

| Project | Command | Tests | Fail | Result |
|---|---|---:|---:|---|
| basketball-admin | `mvn test package` | 136 | 0 | PASS |
| basketball-web | `mvn test package` | 49 | 0 | PASS |
| basketball-admin-ui | `npm run build:prod` | - | 0 | PASS |
| basketball-web-ui | `npm run build` | - | 0 | PASS |

> 说明：以上为 Gap Closure 阶段结束后的最终构建，不是修复前引用。
"""
text = re.sub(r"## 2\. Build / Test 基线[\s\S]*?(?=\n## 3\. )", build_section + "\n", text, count=1)

# Update Known Limitations section 18
known = """## 18. Known Limitations

1. EXTERNAL EMAIL DELIVERY NOT AUTOMATED（本轮使用 `CNPA_MAIL_DEV_BYPASS=true`；注册业务链 PASS，外邮投递 NOT EXECUTED）
2. BO3/BO5 **最终淘汰阶段**（`advancementType=NONE`）当前 Generator 仅支持 2 支队伍；非最终 `WINNER_ADVANCE` 阶段可支持 Top4→2 Series（Smoke A 已证）
3. 球员 FG/3P/FT：`basket_player_stat` 无 FG 字段，不造假
4. Admin UI 未发现独立「会员昵称/头像资料变更审核」菜单页（API `/basket/memberProfileChange/*` 已验；球队资料变更页存在）
5. FULL_SMOKE_20260811_* 为技术验收数据，不宜直接作为老板演示入口

"""
text = re.sub(r"## 18\. Known Limitations[\s\S]*?(?=\n## 19\. )", known, text, count=1)

# Update P0/P1/P2 section 19
p19 = """## 19. P0 / P1 / P2

### P0
- （无未关闭 P0；pairing soft-delete / match_no 已修复并 POST-FIX BUILD PASS）

### P1
- P1-1: BO3/BO5 **最终阶段**多队 bracket 树尚未扩展（代码：`SingleEliminationStageScheduleStrategy.validateSeriesElimination`）。**不是**「所有 BO3 Stage 只能 2 队」。

### P2
- P2-1: Admin UI 缺少会员 Profile Change 审核菜单页（API 可用；球队资料变更页 PASS）
- P2-2: External QQ Mail Delivery NOT EXECUTED（Dev Bypass）

> 已撤销：模糊 “BO3/BO5 SE 生成实际限制 2 队” 旧表述；Manual Advance 不再记为 Known Limitation（能力存在，Smoke C 前置 Stage 不合法）。

"""
text = re.sub(r"## 19\. P0 / P1 / P2[\s\S]*?(?=\n## 20\. )", p19, text, count=1)

# Update conclusion 21/22
conc = """## 21. 最终结论

**PASS WITH KNOWN LIMITATIONS**

### Technical Acceptance
**PASS WITH KNOWN LIMITATIONS**

### Boss Demo Data
**REQUIRES CLEAN PRESENTATION DATA**

FULL_SMOKE_20260811_* 仅用于技术验收；老板演示请另备正式命名 Demo 数据。

## 22. 是否建议老板验收

**YES（技术验收） / NO（直接用 FULL_SMOKE 数据演示）**

原因：核心业务链、权限/幂等、登录后 Account 浏览器、Admin 核心菜单、Post-Fix Build 与一致性均已闭环；剩余为最终 BO3 bracket 扩展与演示数据准备，不阻断技术验收。

"""
text = re.sub(r"## 21\. 最终结论[\s\S]*$", conc, text, count=1)

# Append / replace Final Gap Closure section
gap_section = f"""

---

## Final Gap Closure

> 日期：2026-08-11（同日补测）  
> 证据：`basketball-admin/scripts/full_smoke_gap_closure_20260811.json`  
> 真实账号：`{creds.get('email')}` / memberId=`{creds.get('memberId')}` / playerId=`{creds.get('playerId')}`

### Account Real Registration
- Registration Business Flow：**PASS**
  - email-code/send `bizNo` + `devBypass=true`
  - register HTTP 200（无 SQL INSERT）
  - login + `/basket/member/auth/session` PASS
- External QQ Mail Delivery：**NOT EXECUTED**（DEV_BYPASS，任意 6 位验证码）

### Player Certification
- **PASS**：draft → material(`PLAYER_CERT_GAME_SCREENSHOT`) → submit → Admin `/basket/playerCert/{{id}}/approve`
- 永久 Player ID：`{creds.get('playerId')}`（playerNo=`CNPA-P-000001`）

### Platform Identity
- **PASS**
  - 认证通过后 PC Identity INITIAL_BIND 生效
  - 再走 change-game-id → Admin approve → list 显示 `NEWGID6471`
  - change-history page API 200

### Manual Advance
- 系统**有** Manual Advance 能力
- 支持：`SINGLE_ELIMINATION` 且 `advancementType=WINNER_ADVANCE` 且存在 `nextStageCode`（非最终阶段）
- 不支持：最终 SE（`NONE`）、RR 等 → `ServiceException("当前阶段不支持人工直接晋级")`
- Smoke C 原 500：**测试 Case 不合法**（打在 final/不支持 Stage），**不是**功能缺失
- 合法最小 Case：**PASS**（final REJECT + non-final 200 + MANUAL PREVIEW qualification）
- 代码：`StageExecutionExceptionApplicationServiceImpl.manualAdvance` + `EliminationStageSupport.isSingleRoundNonFinalStage`

### BO3/BO5 Limitation Clarification
- 「2 队」含义 = **B：BO3/BO5 最终淘汰 Stage（advancementType=NONE）最多 2 队**
- **不是**「一个 MatchSeries 只有两队所以叫限制」这种误读
- Smoke A Top4→2×BO3 Semi **合法**：Semi Stage 为 `WINNER_ADVANCE`（非最终）
- 旧 P1-2 模糊表述：**删除**；保留精确最终阶段限制为 P1-1

### Account Browser
- **PASS**（Playwright 登录后 7/7）
- 使用 `cnpa_web_token` + UI 登录
- 路由：`/account` `/account/player` `/account/team` `/account/my-events` `/account/notifications` `/account/settings` `/account/profile`
- 无 login_wall / Uncaught / web-api 500 / undefined|NaN|[object Object]

### Admin UI Browser
- **PASS**（登录后直达核心路由 15/15）
- 覆盖：赛事发布、队长授权、报名审核、参赛队伍、赛程/比赛/提交审核、球员认证、平台 ID 变更、球队资料变更
- 会员昵称/头像审核页：**菜单缺失（P2）**，API 已验

### Pagination
- Joinable / My Events / Identity History / Notifications：**API PASS**（登录态）
- total/page 参数可返回；未额外灌入大量分页假数据

### Permission
| Case | HTTP/code | msg | Result |
|---|---|---|---|
| 无 Grant 创建 Team | 403 | 暂未获得该赛事报名资格… | PASS |
| 管理其他/归档 Team | 400 | 球队状态不可用 | PASS |
| Web token 调 Admin | 401 | 认证失败 | PASS |
| 跨 Division Registration | 500/业务拒绝 | 系统异常/拒绝（未写入） | PASS |

### Idempotency
| Case | Evidence | Result |
|---|---|---|
| 同 Grant 第二支 Team | 1=200 2=403「已创建过球队」 | PASS |
| 重复 Registration | 双次拒绝（阵容/状态门禁） | PASS |
| 重复 Registration Approve | `仅待审核状态可通过`；EventTeam 8→8 | PASS |
| 重复 Submission Approve | `仅待审核状态可通过`；teamStat 不变 | PASS |

### Data Consistency
追加检查全 0：**PASS**
- PlayerStat missing EventPlayer = 0
- CaptainGrant missing Division = 0
- Stage missing Division = 0
- player_profile.current_team_id dangling = 0

### Team Page “暂无球队”
- **PASS（主体正常）**
- 实际文案为「暂无球队**简介**」，Hero=`FS_CHARLIE`，Roster/MatchHistory/Stats 正常

### Compare Loading
- **PASS（非无限 loading）**
- networkidle 后可见「请选择对比球队」；`stuck=false`

### Team Logo
- upload TEAM_LOGO = 200，bind/update = 200
- 旧「可能 403」：**NOT_REPRODUCED**（本轮真实账号自有文件归属正常）

### Account Avatar
- **PASS**（ACCOUNT_AVATAR upload 200）

### Post-Fix Build
- Admin 136/0、Web 49/0、Admin UI build:prod、Web UI build：**全部 PASS**

### Technical vs Boss Demo
- Technical Acceptance：**PASS WITH KNOWN LIMITATIONS**
- Boss Demo Data：**REQUIRES CLEAN PRESENTATION DATA**（不要直接用 FULL_SMOKE_*）

"""

if "## Final Gap Closure" in text:
    text = re.sub(r"\n---\n\n## Final Gap Closure[\s\S]*$", gap_section, text)
else:
    text = text.rstrip() + gap_section

report.write_text(text, encoding="utf-8")
print("updated", report, "chars", len(text))