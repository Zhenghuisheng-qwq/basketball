import sys, json
sys.path.insert(0, r"d:/code/dz/basketball/scaffolding-v2/basketball-admin/scripts")
from full_smoke_common import *
divs = db_query(
    "SELECT d.id, d.division_name, d.division_status FROM basket_division d WHERE d.division_name LIKE %s ORDER BY d.id DESC LIMIT 5",
    ("%PLAYOFF_V1_E2E_DIV_A16%",),
)
print(json.dumps(divs, default=str, ensure_ascii=False, indent=2))
for d in divs:
    st = db_query(
        "SELECT id, stage_code, execution_status, participant_count, best_of FROM basket_division_stage WHERE division_id=%s AND is_delete=0 ORDER BY stage_order",
        (d["id"],),
    )
    print("div", d["id"], st)
