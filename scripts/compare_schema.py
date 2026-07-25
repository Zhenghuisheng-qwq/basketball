import json
from collections import defaultdict

EXPECTED = json.load(open(r"d:\code\dz\basketball\scaffolding-v2\scripts\expected_tables.json", encoding="utf-8"))
DB_ROWS = json.load(open(r"C:\Users\Administrator\.cursor\projects\d-code-dz-basketball-scaffolding-v2\agent-tools\a16a3377-3718-4999-89cc-2f3664d0897b.txt", encoding="utf-8"))

db_tables = defaultdict(set)
for row in DB_ROWS:
    db_tables[row["TABLE_NAME"]].add(row["COLUMN_NAME"])

expected_tables = set(EXPECTED.keys())
db_table_names = set(db_tables.keys())

missing_tables = sorted(expected_tables - db_table_names)
extra_tables = sorted(db_table_names - expected_tables)

missing_columns = {}
extra_columns = {}

for table in sorted(expected_tables & db_table_names):
    exp_cols = set(EXPECTED[table]["fields"])
    act_cols = db_tables[table]
    miss = sorted(exp_cols - act_cols)
    extra = sorted(act_cols - exp_cols)
    if miss:
        missing_columns[table] = miss
    if extra:
        extra_columns[table] = extra

print("=== MISSING TABLES (code expects, DB lacks) ===")
for t in missing_tables:
    print(f"  {t} ({EXPECTED[t]['class']})")

print("\n=== EXTRA TABLES (DB has, no entity) ===")
for t in extra_tables:
    print(f"  {t}")

print("\n=== MISSING COLUMNS (entity fields, DB lacks) ===")
for t, cols in missing_columns.items():
    print(f"  {t}: {', '.join(cols)}")

print("\n=== SUMMARY ===")
print(f"expected_tables={len(expected_tables)} db_tables={len(db_table_names)}")
print(f"missing_tables={len(missing_tables)} extra_tables={len(extra_tables)}")
print(f"tables_with_missing_columns={len(missing_columns)}")
