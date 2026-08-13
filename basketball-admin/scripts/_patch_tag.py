from pathlib import Path
p = Path(r"d:/code/dz/basketball/scaffolding-v2/basketball-admin/scripts/playoff_v1_e2e_smoke.py")
text = p.read_text(encoding="utf-8")
# uniquify division names with run stamp
if "RUN_TAG" not in text:
    text = text.replace(
        'MARKER = "PLAYOFF_V1_E2E"',
        'MARKER = "PLAYOFF_V1_E2E"\nRUN_TAG = datetime.now().strftime("%H%M%S")',
    )
    text = text.replace('f"{MARKER}_DIV_A16"', 'f"{MARKER}_DIV_A16_{RUN_TAG}"')
    text = text.replace('f"{MARKER}_DIV_B8"', 'f"{MARKER}_DIV_B8_{RUN_TAG}"')
    text = text.replace('f"{MARKER}_DIV_RAND8"', 'f"{MARKER}_DIV_RAND8_{RUN_TAG}"')
    text = text.replace('f"{MARKER}_SEASON_A"', 'f"{MARKER}_SEASON_A_{RUN_TAG}"')
    text = text.replace('f"{MARKER}_SEASON_B"', 'f"{MARKER}_SEASON_B_{RUN_TAG}"')
    text = text.replace('f"{MARKER}_SEASON_R"', 'f"{MARKER}_SEASON_R_{RUN_TAG}"')
    # also uniquify template season divisions
    text = text.replace('f"{MARKER}_SEASON_T"', 'f"{MARKER}_SEASON_T_{RUN_TAG}"')
    text = text.replace('f"{MARKER}_DIV_T4"', 'f"{MARKER}_DIV_T4_{RUN_TAG}"')
    text = text.replace('f"{MARKER}_DIV_T8"', 'f"{MARKER}_DIV_T8_{RUN_TAG}"')
    text = text.replace('f"{MARKER}_DIV_T16"', 'f"{MARKER}_DIV_T16_{RUN_TAG}"')
    text = text.replace('f"{MARKER}_DIV_RR"', 'f"{MARKER}_DIV_RR_{RUN_TAG}"')
    text = text.replace('f"{MARKER}_DIV_G"', 'f"{MARKER}_DIV_G_{RUN_TAG}"')
    text = text.replace('f"{MARKER}_DIV_BO1"', 'f"{MARKER}_DIV_BO1_{RUN_TAG}"')
    p.write_text(text, encoding="utf-8")
    print("tagged")
else:
    print("already tagged")
import py_compile; py_compile.compile(str(p), doraise=True)
