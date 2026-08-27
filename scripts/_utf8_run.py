from pathlib import Path
import sys

path = Path(sys.argv[1])
raw = path.read_bytes()
if len(raw) > 3 and raw[1] == 0:
    text = raw.decode("utf-16")
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_bytes(text.encode("utf-8"))
    path.write_bytes(tmp.read_bytes())
    tmp.unlink()
    print("converted", path)
else:
    print("utf8", path)
