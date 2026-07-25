import json
import os
import re

def camel_to_snake(name):
    s = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", s).lower()

def class_to_table(cls):
    tm = None
    if cls.startswith("Basket"):
        return "basket_" + camel_to_snake(cls[6:])
    return camel_to_snake(cls)

entity_dirs = [
    r"d:\code\dz\basketball\scaffolding-v2\basketball-admin\src\main\java\com\zhs\basket\domain",
    r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\main\java\com\zhs\model",
]
tables = {}
field_pat = re.compile(r"private\s+[\w<>,\s]+\s+(\w+)\s*;")
table_pat = re.compile(r'@TableName\s*\(\s*"([^"]+)"\s*\)')
class_pat = re.compile(r"public\s+class\s+(\w+)")

for d in entity_dirs:
    for root, _, files in os.walk(d):
        for f in files:
            if not f.endswith(".java"):
                continue
            path = os.path.join(root, f)
            text = open(path, encoding="utf-8").read()
            cm = class_pat.search(text)
            if not cm:
                continue
            cls = cm.group(1)
            tm = table_pat.search(text)
            table = tm.group(1) if tm else class_to_table(cls)
            fields = field_pat.findall(text)
            cols = {camel_to_snake(fld) for fld in fields}
            if table not in tables:
                tables[table] = {"class": cls, "fields": cols}
            else:
                tables[table]["fields"].update(cols)

# output for diff
with open(r"d:\code\dz\basketball\scaffolding-v2\scripts\expected_tables.json", "w", encoding="utf-8") as out:
    json.dump({k: {"class": v["class"], "fields": sorted(v["fields"])} for k, v in sorted(tables.items())}, out, ensure_ascii=False, indent=2)

print(f"tables={len(tables)}")
