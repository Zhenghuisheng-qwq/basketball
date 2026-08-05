#!/usr/bin/env python3
"""CNPA V2 dynamic-format full integration runner (local execution only).

Usage:
  python scripts/cnpa_v2_full_integration_runner.py --dry-run
  python scripts/cnpa_v2_full_integration_runner.py --scenario A
  python scripts/cnpa_v2_full_integration_runner.py --all
  python scripts/cnpa_v2_full_integration_runner.py --exception-only
  python scripts/cnpa_v2_full_integration_runner.py --permission-only
  python scripts/cnpa_v2_full_integration_runner.py --concurrency-only

Environment variables (or scripts/.env.v2.local, fallback scripts/.env.r4.local):
  CNPA_ADMIN_BASE_URL, CNPA_WEB_BASE_URL
  CNPA_ADMIN_USERNAME, CNPA_ADMIN_PASSWORD
  CNPA_TEST_USER_CONFIG (JSON file path with web test users)
  CNPA_V2_BATCH_CODE, CNPA_SCENARIOS, CNPA_HTTP_TIMEOUT
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "scripts" / "output"
ENV_LOCAL = ROOT / "scripts" / ".env.v2.local"
ENV_LOCAL_FALLBACK = ROOT / "scripts" / ".env.r4.local"

RESULTS: list[dict] = []


def load_dotenv(path: Path) -> None:
    if not path.exists():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, val = line.split("=", 1)
        os.environ.setdefault(key.strip(), val.strip().strip('"').strip("'"))


def env(key: str, default: str = "") -> str:
    return os.environ.get(key, default)


def mask(value: str | None) -> str:
    if not value:
        return ""
    if len(value) <= 4:
        return "****"
    return value[:2] + "****" + value[-2:]


def record(name: str, status: str, detail: str = "", **extra) -> None:
    item = {"name": name, "status": status, "detail": detail, **extra}
    RESULTS.append(item)
    mark = {"PASS": "OK", "FAIL": "!!", "SKIP": "--", "INFO": "..", "PENDING": "??"}.get(status, "??")
    print(f"[{mark}] {name}: {detail or status}")


def http_json(base: str, method: str, path: str, body: dict | None = None,
              token: str | None = None, timeout: int = 30, allow_fail: bool = False) -> dict:
    url = base.rstrip("/") + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
            http_status = resp.status
    except urllib.error.HTTPError as exc:
        http_status = exc.code
        payload = json.loads(exc.read().decode("utf-8", errors="ignore") or "{}")
        if not allow_fail:
            raise RuntimeError(f"{method} {path} HTTP {exc.code}: {payload}") from exc
    else:
        http_status = http_status
    code = payload.get("code")
    if not allow_fail and code not in (200, None):
        raise RuntimeError(f"{method} {path} business code={code} msg={payload.get('msg')}")
    payload["_http_status"] = http_status
    return payload


def check_service(name: str, base: str, path: str, timeout: int) -> bool:
    if not base:
        record(f"check_{name}", "FAIL", f"{name} base URL 未配置")
        return False
    try:
        http_json(base, "GET", path, allow_fail=True, timeout=timeout)
        record(f"check_{name}", "PASS", f"{base}{path} 可访问")
        return True
    except Exception as exc:
        record(f"check_{name}", "FAIL", str(exc))
        return False


def admin_login(base: str, username: str, password: str, timeout: int) -> str | None:
    try:
        resp = http_json(base, "POST", "/login", {"username": username, "password": password}, timeout=timeout)
        token = resp.get("token")
        if not token:
            record("admin_login", "FAIL", f"无 token: {resp}")
            return None
        record("admin_login", "PASS", f"user={username}")
        return token
    except Exception as exc:
        record("admin_login", "FAIL", str(exc))
        return None


def web_login(base: str, email: str, password: str, timeout: int) -> str | None:
    try:
        resp = http_json(base, "POST", "/basket/auth/login", {"email": email, "password": password}, timeout=timeout)
        token = resp.get("data", {}).get("token") if isinstance(resp.get("data"), dict) else resp.get("token")
        if not token and isinstance(resp.get("data"), str):
            token = resp.get("data")
        if not token:
            record("web_login", "FAIL", f"无 token email={email}")
            return None
        record("web_login", "PASS", f"email={email}")
        return token
    except Exception as exc:
        record("web_login", "FAIL", f"email={email}: {exc}")
        return None


def scenario_a(admin_base: str, admin_token: str, timeout: int) -> None:
    record("scenario_A", "PENDING", "TEAM+PC+5V5+4队单循环 — 待本地完整执行（需 R4_TEST Division 与报名链路）")
    # Guidance assertions for local completion:
    for assertion in [
        "EventTeam=4", "Match=6", "每队3场", "重复对阵=0",
    ]:
        record("scenario_A_assert", "PENDING", assertion)


def scenario_b(admin_base: str, admin_token: str, timeout: int) -> None:
    record("scenario_B", "PENDING", "TEAM+PS_XBOX+3V3+5队单循环 — 待本地完整执行")


def scenario_c(admin_base: str, admin_token: str, timeout: int) -> None:
    record("scenario_C", "PENDING", "16队小组赛→8强BO1 — 待本地完整执行")


def scenario_d(admin_base: str, web_base: str, admin_token: str, web_token: str | None, timeout: int) -> None:
    record("scenario_D", "PENDING", "PERSONAL+PS_XBOX+3V3+两队BO1 — 待本地完整执行")


def legacy_generate_removed(admin_base: str, admin_token: str, timeout: int) -> None:
    try:
        http_json(
            admin_base,
            "POST",
            "/basket/v4/schedule-batch/generate",
            {"divisionId": 1},
            token=admin_token,
            timeout=timeout,
            allow_fail=True,
        )
        record("legacyGenerateRemoved", "FAIL", "Legacy 生成入口应已删除但返回成功")
    except RuntimeError as exc:
        if "404" in str(exc) or "Not Found" in str(exc) or "不存在" in str(exc):
            record("legacyGenerateRemoved", "PASS", str(exc))
        else:
            record("legacyGenerateRemoved", "FAIL", str(exc))


def unauthorized_admin_api(admin_base: str, timeout: int) -> None:
    try:
        http_json(admin_base, "GET", "/basket/v4/admin/stages/list?divisionId=1", timeout=timeout)
        record("unauthorizedAdminApiRejected", "FAIL", "未授权访问应失败")
    except Exception as exc:
        record("unauthorizedAdminApiRejected", "PASS", str(exc))


SCENARIO_MAP = {
    "A": scenario_a,
    "B": scenario_b,
    "C": scenario_c,
    "D": scenario_d,
}


def write_output(batch_code: str) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    out = {
        "batchCode": batch_code,
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "results": RESULTS,
    }
    json_path = OUTPUT_DIR / "cnpa-v2-integration-result.json"
    json_path.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    md_lines = ["# CNPA V2 Integration Result", "", f"- batch: `{batch_code}`", f"- time: {out['generatedAt']}", ""]
    for item in RESULTS:
        md_lines.append(f"- **{item['status']}** {item['name']}: {item.get('detail', '')}")
    (OUTPUT_DIR / "cnpa-v2-integration-result.md").write_text("\n".join(md_lines) + "\n", encoding="utf-8")
    record("output", "INFO", f"written {json_path}")


def main() -> int:
    load_dotenv(ENV_LOCAL)
    load_dotenv(ENV_LOCAL_FALLBACK)
    parser = argparse.ArgumentParser(description="CNPA V2 full HTTP integration runner")
    parser.add_argument("--dry-run", action="store_true", help="Only check services and env")
    parser.add_argument("--scenario", choices=["A", "B", "C", "D"], help="Run single scenario")
    parser.add_argument("--all", action="store_true", help="Run all scenarios")
    parser.add_argument("--exception-only", action="store_true", help="Run exception/idempotency checks only")
    parser.add_argument("--permission-only", action="store_true", help="Run permission boundary checks only")
    parser.add_argument("--core-smoke", action="store_true", help="Run CNPA V2 core smoke test")
    args = parser.parse_args()

    admin_base = env("CNPA_ADMIN_BASE_URL", "http://127.0.0.1:8080")
    web_base = env("CNPA_WEB_BASE_URL", "http://127.0.0.1:8881")
    admin_user = env("CNPA_ADMIN_USERNAME", "admin")
    admin_pass = env("CNPA_ADMIN_PASSWORD")
    batch_code = env("CNPA_V2_BATCH_CODE", env("CNPA_R4_BATCH_CODE", "CNPA_V2_TEST_BATCH"))
    timeout = int(env("CNPA_HTTP_TIMEOUT", "30"))
    legacy_test_division = env("CNPA_LEGACY_TEST_DIVISION_ID", "")

    if not admin_pass:
        record("config", "FAIL", "CNPA_ADMIN_PASSWORD 未配置（可使用 scripts/.env.r4.local）")
        write_output(batch_code)
        return 1

    record("config", "INFO", f"admin={admin_base} web={web_base} batch={batch_code} pwd={mask(admin_pass)}")

    admin_ok = check_service("admin", admin_base, "/captchaImage", timeout)
    web_ok = check_service("web", web_base, "/basket/v4/public/series/list", timeout)
    if not admin_ok or not web_ok:
        write_output(batch_code)
        return 2

    if args.core_smoke:
        import subprocess
        proc = subprocess.run([sys.executable, str(ROOT / "scripts" / "cnpa_v2_core_smoke.py")], cwd=str(ROOT))
        return proc.returncode

    if args.dry_run:
        record("dry_run", "PASS", "服务可达，环境变量已加载")
        write_output(batch_code)
        return 0

    admin_token = admin_login(admin_base, admin_user, admin_pass, timeout)
    if not admin_token:
        write_output(batch_code)
        return 3

    web_token = None
    test_user_cfg = env("CNPA_TEST_USER_CONFIG")
    if test_user_cfg and Path(test_user_cfg).exists():
        cfg = json.loads(Path(test_user_cfg).read_text(encoding="utf-8"))
        captain = cfg.get("captain") or {}
        if captain.get("email") and captain.get("password"):
            web_token = web_login(web_base, captain["email"], captain["password"], timeout)

    if args.exception_only or args.permission_only or args.concurrency_only:
        legacy_generate_removed(admin_base, admin_token, timeout)
        unauthorized_admin_api(admin_base, timeout)
        if args.permission_only:
            record("permission_only", "PENDING", "权限越权检查待本地完整执行")
        if args.concurrency_only:
            record("concurrency_only", "PENDING", "并发/幂等检查待本地完整执行")
        write_output(batch_code)
        return 0

    scenarios = []
    if args.scenario:
        scenarios = [args.scenario]
    elif args.all:
        scenarios = ["A", "B", "C", "D"]
    else:
        raw = env("CNPA_SCENARIOS", "A,B,C,D")
        scenarios = [s.strip().upper() for s in re.split(r"[,;\s]+", raw) if s.strip()]

    for code in scenarios:
        fn = SCENARIO_MAP.get(code)
        if not fn:
            continue
        if code == "D":
            fn(admin_base, web_base, admin_token, web_token, timeout)
        else:
            fn(admin_base, admin_token, timeout)

    legacy_generate_removed(admin_base, admin_token, timeout)
    unauthorized_admin_api(admin_base, timeout)

    write_output(batch_code)
    failed = sum(1 for r in RESULTS if r["status"] == "FAIL")
    pending = sum(1 for r in RESULTS if r["status"] == "PENDING")
    if failed:
        return 4
    if pending:
        record("summary", "INFO", f"存在 {pending} 项待本地完整执行项（非伪造通过）")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
