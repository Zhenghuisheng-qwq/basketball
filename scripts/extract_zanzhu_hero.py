import base64
import os
import re

html_path = r"D:\code\dz\basketball\scaffolding-v2\ui\html\zanzhu\zanzhu.html"
out_dir = r"D:\code\dz\basketball\scaffolding-v2\basketball-web-ui\src\assets\partners"

with open(html_path, "r", encoding="utf-8") as f:
    html = f.read()

os.makedirs(out_dir, exist_ok=True)
match = re.search(r"data:image/jpeg;base64,([A-Za-z0-9+/=]+)", html)
if match:
    data = base64.b64decode(match.group(1))
    out_path = os.path.join(out_dir, "partners-hero.jpg")
    with open(out_path, "wb") as f:
        f.write(data)
    print(f"saved {out_path} ({len(data)} bytes)")
else:
    raise SystemExit("hero image not found")
