# -*- coding: utf-8 -*-
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
req = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']')
cls = re.compile(r"public\s+class\s+(\w+)")

for label, base in [
    ("admin", root / "basketball-admin/src/main/java/com/zhs/web/controller/basket"),
    ("web", root / "basketball-web/src/main/java/com/zhs/controller"),
]:
    print("##", label)
    for p in sorted(base.rglob("*Controller.java")):
        t = p.read_text(encoding="utf-8", errors="ignore")
        m = cls.search(t)
        c = m.group(1) if m else p.stem
        rm = req.search(t)
        path = "/" + rm.group(1).lstrip("/") if rm else "(no class RequestMapping)"
        print(f"{c}\t{path}")
