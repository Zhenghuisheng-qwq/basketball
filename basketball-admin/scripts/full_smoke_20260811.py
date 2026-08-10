#!/usr/bin/env python3
"""FULL_SMOKE_20260811 orchestrator"""
from __future__ import annotations

import subprocess
import sys
from datetime import datetime
from pathlib import Path

from full_smoke_common import MARKER, OUT_DIR, baseline_counts, env_health, load_context, save_context

SCRIPTS = Path(__file__).resolve().parent
DOC = Path(__file__).resolve().parents[2] / "docs" / "CNPA_V2_FULL_SMOKE_ACCEPTANCE_20260811.md"


def _run(mod: str) -> int:
    print(f"\n===== RUN {mod} =====")
    p = subprocess.run([sys.executable, str(SCRIPTS / mod)], cwd=str(SCRIPTS))
    return int(p.returncode)


def _write_report(ctx: dict, results: dict[str, int]) -> None:
    smokes = ctx.get("smokes") or {}
    lines = [
        "# CNPA V2 Full Smoke Acceptance (20260811)",
        "",
        f"Marker: `{MARKER}`",
        f"Generated: {datetime.now().isoformat(timespec='seconds')}",
        "",
        "## Environment",
        "",
    ]
    for k, v in (ctx.get("env") or {}).items():
        lines.append(f"- {k}: {v}")
    lines.extend(["", "## Baseline (before)", ""])
    for k, v in (ctx.get("baseline") or {}).items():
        lines.append(f"- {k}: {v}")
    lines.extend(["", "## Smoke results", ""])
    for name, code in results.items():
        bag = smokes.get(name.replace("full_smoke_", "").upper().replace(".PY", ""), {})
        # map module to smoke key
        key_map = {
            "full_smoke_main.py": "A_MAIN",
            "full_smoke_group.py": "B_GROUP",
            "full_smoke_advanced.py": "C_ADVANCED",
            "full_smoke_lifecycle.py": "D_LIFECYCLE",
            "full_smoke_extra.py": "E_EXTRA",
        }
        sk = key_map.get(name, name)
        bag = smokes.get(sk, {})
        lines.append(f"### {sk} (exit={code})")
        lines.append(f"- pass: {bag.get('pass', 0)} fail: {bag.get('fail', 0)} skip: {bag.get('skip', 0)}")
        if bag.get("ids"):
            lines.append(f"- ids: `{bag.get('ids')}`")
        fails = [c for c in (bag.get("cases") or []) if c.get("result") == "FAIL"]
        if fails:
            lines.append("- failures:")
            for c in fails[:20]:
                lines.append(f"  - {c.get('name')}: {c.get('actual')}")
        lines.append("")
    lines.extend(
        [
            "## Browser verification",
            "",
            (
        "EXECUTED ? see full_smoke_20260811_browser.json"
        if (SCRIPTS / "full_smoke_20260811_browser.json").exists()
        else "NOT_EXECUTED (no browser result file)"
    ),
            "",
            "## External email",
            "",
            "EXTERNAL EMAIL DELIVERY NOT AUTOMATED / NOT_EXECUTED",
            "",
            "## P0/P1/P2",
            "",
            "See case tables in `basketball-admin/scripts/full_smoke_20260811_context.json`.",
            "",
        ]
    )
    DOC.parent.mkdir(parents=True, exist_ok=True)
    DOC.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {DOC}")


def main() -> int:
    ctx = load_context()
    ctx["env"] = env_health()
    ctx["baseline"] = baseline_counts()
    ctx.setdefault("build", {})["orchestrator_at"] = datetime.now().isoformat(timespec="seconds")
    save_context(ctx)

    down = [k for k, v in ctx["env"].items() if str(v).startswith("DOWN")]
    if down:
        print("WARN env down:", down)

    results: dict[str, int] = {}
    rc_a = _run("full_smoke_main.py")
    results["full_smoke_main.py"] = rc_a
    ctx = load_context()

    for mod in ("full_smoke_group.py", "full_smoke_advanced.py"):
        results[mod] = _run(mod)
        ctx = load_context()

    if rc_a == 0:
        results["full_smoke_lifecycle.py"] = _run("full_smoke_lifecycle.py")
    else:
        print("SKIP full_smoke_lifecycle.py (A_MAIN failed)")
        results["full_smoke_lifecycle.py"] = 0

    results["full_smoke_extra.py"] = _run("full_smoke_extra.py")
    ctx = load_context()
    _write_report(ctx, results)

    fail_any = any(v != 0 for k, v in results.items() if k != "full_smoke_lifecycle.py" or rc_a == 0)
    return 1 if (rc_a != 0 or fail_any) else 0


if __name__ == "__main__":
    sys.exit(main())
