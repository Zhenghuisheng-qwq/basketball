from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
t=t.replace("from datetime import datetime", "from datetime import datetime, timedelta")
p.write_text(t, encoding="utf-8", newline="\n")
print("imported timedelta")
