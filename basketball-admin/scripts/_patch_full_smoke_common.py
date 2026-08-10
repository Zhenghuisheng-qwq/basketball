from pathlib import Path

p = Path(__file__).resolve().parent / "full_smoke_common.py"
text = p.read_text(encoding="utf-8")
if "FORBIDDEN_DIVISION_IDS" in text:
    raise SystemExit(0)
addon = (Path(__file__).resolve().parent / "_full_smoke_common_addon.py").read_text(encoding="utf-8")
p.write_text(text.rstrip() + "\n\n" + addon, encoding="utf-8", newline="\n")
print("patched")
