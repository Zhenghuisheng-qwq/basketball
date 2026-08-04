import re
from pathlib import Path

ROOT = Path(r"D:/code/dz/basketball/scaffolding-v2")

# --- router/index.ts ---
router_path = ROOT / "basketball-web-ui/src/router/index.ts"
text = router_path.read_text(encoding="utf-8")

def remove_route_blocks(src: str, path_substrings: list[str]) -> str:
    out = []
    i = 0
    n = len(src)
    while i < n:
        if src.startswith("{", i):
            # find matching closing brace at same nesting for route object
            depth = 0
            j = i
            while j < n:
                c = src[j]
                if c == "{":
                    depth += 1
                elif c == "}":
                    depth -= 1
                    if depth == 0:
                        j += 1
                        break
                j += 1
            block = src[i:j]
            if any(s in block for s in path_substrings):
                # skip trailing comma/newlines
                while j < n and src[j] in " \t\r\n,":
                    j += 1
                i = j
                continue
            out.append(block)
            i = j
            continue
        out.append(src[i])
        i += 1
    return "".join(out)

# Remove top-level legacy routes
for sub in [
    "path: '/tournament/legacy'",
    "path: '/data/legacy'",
]:
    # naive: regex remove whole route object
    pattern = rf"\{{\s*\n\s*path: '{sub.split("'")[1]}',[\s\S]*?\n\s*\}},\s*\n"
    text = re.sub(pattern, "", text)

# Remove account legacy redirect blocks
legacy_paths = [
    "path: 'events/team/manage'",
    "path: 'events/team/roster'",
    "path: 'events/team/free-agent'",
    "path: 'events/team/transfer'",
    "path: 'events/team/transfer/create'",
    "path: 'events/team/transfer/detail'",
    "path: 'events/team/captain-transfer'",
    "path: 'my-tournaments'",
    "path: 'my-tournaments/list'",
    "path: 'my-tournaments/:tournamentId/team'",
    "path: 'my-tournaments/:tournamentId/team/match-data'",
    "path: 'my-tournaments/:tournamentId/team/match-data/:matchId/submit'",
    "path: 'my-tournaments/:tournamentId/team/match-data/:matchId/resubmit'",
    "path: 'my-tournaments/:tournamentId/team/match-data/:matchId/status'",
    "path: 'my-tournaments/:tournamentId/team/match-data/:matchId/dispute'",
    "path: 'my-tournaments/:tournamentId/team/match-data/:matchId/dispute-status'",
    "path: 'my-tournaments/:tournamentId/team/match-data/:matchId/exception'",
    "path: 'my-tournaments/:tournamentId/team/match-data/:matchId/exception-status'",
    "path: 'my-tournaments/:tournamentId/team/roster'",
    "path: 'my-tournaments/:tournamentId/team/roster-history'",
    "path: 'my-tournaments/:tournamentId/team/profile/edit'",
    "path: 'my-tournaments/:tournamentId/team/free-agents'",
    "path: 'my-tournaments/:tournamentId/team/transfers'",
    "path: 'my-tournaments/:tournamentId/team/transfers/initiate'",
    "path: 'my-tournaments/:tournamentId/team/transfers/:transferId'",
    "path: 'my-tournaments/:tournamentId/team/captain-transfer'",
    "path: 'my-tournaments/open'",
    "path: 'my-tournaments/open/apply'",
    "path: 'my-tournaments/open/status'",
    "path: 'my-tournaments/open/status/pending'",
    "path: 'my-tournaments/open/status/rejected'",
    "path: 'my-tournaments/open/status/approved'",
]
for p in legacy_paths:
    esc = re.escape(p)
    pattern = rf"\{{\s*\n\s*{esc},[\s\S]*?\n\s*\}},\s*\n"
    text, n = re.subn(pattern, "", text, count=1)
router_path.write_text(text, encoding="utf-8")
print("router updated")

# --- schedule index.vue ---
sched_path = ROOT / "basketball-admin-ui/src/views/cnpa/event-v4/schedule/index.vue"
sched = sched_path.read_text(encoding="utf-8")
# Remove batch tab pane
sched = re.sub(
    r"\n\s*<el-tab-pane label=\"历史赛程（只读）\" name=\"batch\">[\s\S]*?</el-tab-pane>\s*",
    "\n",
    sched,
    count=1,
)
# Unwrap tabs if only stage pane remains
sched = sched.replace("<el-tabs v-model=\"mainTab\">\n      <el-tab-pane label=\"阶段执行\" name=\"stage\">", "<div class=\"schedule-stage-panel\">")
sched = sched.replace("      </el-tab-pane>\n\n    </el-tabs>", "    </div>")
# Remove legacyGenerateAllowed button condition -> remove button entirely
sched = re.sub(r"\n\s*<el-button type=\"success\" icon=\"Plus\" v-if=\"legacyGenerateAllowed\" @click=\"openGenerate\">生成草稿</el-button>", "", sched)
# Remove batch imports partially
for imp in [
    "  cancelV4ScheduleBatch, generateV4ScheduleBatch, getV4ScheduleBatch,\n  listV4ScheduleBatch, publishV4ScheduleBatch,\n  type BasketScheduleBatchDetailVo, type BasketScheduleBatchQuery\n",
    "  calcRoundRobinMatchCount,\n",
]:
    sched = sched.replace(imp, "")
sched = re.sub(r"import \{\s*\} from '@/api/cnpa/event-v4/schedule-batch'\n", "", sched)
# Remove batch state and functions
blocks_to_remove = [
    r"\nconst loading = ref\(false\)[\s\S]*?const PUBLISH_OPTIONS = V4_PUBLISH_STATUS_FILTER_OPTIONS\n",
    r"\nconst selectedDivision = computed[\s\S]*?const generateDisabledReason = computed[\s\S]*?\}\)\n",
    r"\nfunction onQuerySeasonChange[\s\S]*?function onGenerateDivisionChange\(\) \{\n  // counts derived from selectedDivision\n\}\n",
    r"\nfunction loadList\(\) \{[\s\S]*?\}\n\nfunction openGenerate\(\) \{[\s\S]*?\}\n\nfunction submitGenerate\(\) \{[\s\S]*?\}\n\nfunction viewDetail\(row: any\) \{[\s\S]*?\}\n\nfunction handlePublish\(row: any\) \{[\s\S]*?\}\n\nfunction handleCancel\(row: any\) \{[\s\S]*?\}\n",
]
for pat in blocks_to_remove:
    sched = re.sub(pat, "\n", sched, count=1)
sched = re.sub(r"\nconst mainTab = ref\('stage'\)\n", "\n", sched)
sched = re.sub(
    r"onMounted\(async \(\) => \{\n  await loadSeasonOptions\(\)\n  loadList\(\)\n\}\)",
    "onMounted(async () => {\n  await loadSeasonOptions()\n})",
    sched,
)
# cleanup unused imports from v4AdminDisplay
sched = sched.replace("  V4_PUBLISH_STATUS_FILTER_OPTIONS,\n", "")
for fn in [
    "  v4FormatTypeLabel,\n",
    "  v4GenerateStatusLabel,\n",
    "  v4GenerateStatusTagType,\n",
    "  v4PublishStatusLabel,\n",
    "  v4PublishStatusTagType,\n",
]:
    sched = sched.replace(fn, "")
sched = re.sub(r"\.generate-tip \{[^\}]+\}\n", "", sched)
sched_path.write_text(sched, encoding="utf-8")
print("schedule updated")

# --- types/api/basket/index.ts ---
types_path = ROOT / "basketball-admin-ui/src/types/api/basket/index.ts"
lines = types_path.read_text(encoding="utf-8").splitlines(keepends=True)

def drop_line_range(start: int, end: int):
    global lines
    lines = lines[: start - 1] + lines[end:]

# 1-based inclusive ranges to remove (legacy stage/group/round)
drop_line_range(1063, 1204)
# TournamentRule
drop_line_range(920, 942)
# standing / settlement rank block
drop_line_range(1586, 1704)
# schedule v1 block to EOF (adjust after prior drops)
for i, ln in enumerate(lines):
    if ln.startswith("/** 赛程规则 V1 */"):
        lines = lines[:i]
        break
# remove TournamentRuleListResult if present
lines = [ln for ln in lines if "TournamentRuleListResult" not in ln]
types_path.write_text("".join(lines), encoding="utf-8")
print("types updated")
