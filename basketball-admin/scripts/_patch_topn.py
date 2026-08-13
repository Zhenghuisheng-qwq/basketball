from pathlib import Path
p = Path(r"d:/code/dz/basketball/scaffolding-v2/basketball-admin/scripts/playoff_v1_e2e_smoke.py")
text = p.read_text(encoding="utf-8")
text = text.replace(
'''    st4 = _create_stage(token, division_id, stage_payload("FINAL", "总决赛", 4, "SINGLE_ELIMINATION", 3, "NONE", pairing="PLAYOFF_BRACKET"))
    chain_update(token, division_id); put_tie_break(token, division_id, st1)
    return {"rr": st1, "s8": st2, "s4": st3, "final": st4}''',
'''    st4 = _create_stage(token, division_id, stage_payload("FINAL", "总决赛", 4, "SINGLE_ELIMINATION", 3, "NONE", pairing="PLAYOFF_BRACKET"))
    chain_update(token, division_id, top_n=8); put_tie_break(token, division_id, st1)
    return {"rr": st1, "s8": st2, "s4": st3, "final": st4}''')
p.write_text(text, encoding="utf-8")
import py_compile; py_compile.compile(str(p), doraise=True)
print("fixed top_n=8")
