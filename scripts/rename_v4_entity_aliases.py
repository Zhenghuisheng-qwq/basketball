# -*- coding: utf-8 -*-
from pathlib import Path

root = Path(__file__).resolve().parents[1]

# 长名优先，避免 BasketMatch 误伤 BasketMatchPeriodScore 等
renames = [
    ("BasketMatchPeriodScore", "BasketV4MatchPeriodScore"),
    ("BasketMatchPlayerStat", "BasketV4MatchPlayerStat"),
    ("BasketMatchTeamStat", "BasketV4MatchTeamStat"),
    ("BasketMatch", "BasketV4Match"),
]


def replace_type_refs(text: str, old: str, new: str) -> str:
    text = text.replace(f"com.zhs.basket.domain.v4.{old}", f"com.zhs.basket.domain.v4.{new}")
    text = text.replace(f"com.zhs.model.v4.{old}", f"com.zhs.model.v4.{new}")
    text = text.replace(f"实体：{old}", f"实体：{new}")
    text = text.replace(f"Mapper：{old}", f"Mapper：{new}")
    text = text.replace(f"Dao：{old}", f"Dao：{new}")
    text = text.replace(f"List<{old}>", f"List<{new}>")
    text = text.replace(f"BaseMapper<{old}>", f"BaseMapper<{new}>")
    text = text.replace(f"{old} selectById", f"{new} selectById")
    text = text.replace(f"selectList({old} ", f"selectList({new} ")
    text = text.replace(f"insert({old} ", f"insert({new} ")
    text = text.replace(f"updateById({old} ", f"updateById({new} ")
    text = text.replace(f'id="{old}Result"', f'id="{new}Result"')
    text = text.replace(f'resultMap="{old}Result"', f'resultMap="{new}Result"')
    text = text.replace(f'type="com.zhs.basket.domain.v4.{new}"', f'type="com.zhs.basket.domain.v4.{new}"')
    return text


for old, new in renames:
    p = root / f"basketball-admin/src/main/java/com/zhs/basket/domain/v4/{old}.java"
    t = p.read_text(encoding="utf-8")
    t = t.replace(f"实体：{old}", f"实体：{new}")
    t = t.replace(f"public class {old}", f"public class {new}")
    np = p.with_name(f"{new}.java")
    np.write_text(t, encoding="utf-8")
    p.unlink()
    print(f"admin entity {old} -> {new}")

for old, new in renames:
    p = root / f"basketball-web/src/main/java/com/zhs/model/v4/{old}.java"
    t = p.read_text(encoding="utf-8")
    t = t.replace(f"实体：{old}", f"实体：{new}")
    t = t.replace(f"public class {old}", f"public class {new}")
    np = p.with_name(f"{new}.java")
    np.write_text(t, encoding="utf-8")
    p.unlink()
    print(f"web model {old} -> {new}")

mapper_files = [
    root / "basketball-admin/src/main/java/com/zhs/basket/mapper/v4/BasketV4MatchMapper.java",
    root / "basketball-admin/src/main/java/com/zhs/basket/mapper/v4/BasketV4MatchPeriodScoreMapper.java",
    root / "basketball-admin/src/main/java/com/zhs/basket/mapper/v4/BasketV4MatchTeamStatMapper.java",
    root / "basketball-admin/src/main/java/com/zhs/basket/mapper/v4/BasketV4MatchPlayerStatMapper.java",
]
for p in mapper_files:
    t = p.read_text(encoding="utf-8")
    for old, new in renames:
        t = replace_type_refs(t, old, new)
    p.write_text(t, encoding="utf-8")
    print(f"updated {p.name}")

xml_files = [
    root / "basketball-admin/src/main/resources/mapper/basket/v4/BasketV4MatchMapper.xml",
    root / "basketball-admin/src/main/resources/mapper/basket/v4/BasketV4MatchPeriodScoreMapper.xml",
    root / "basketball-admin/src/main/resources/mapper/basket/v4/BasketV4MatchTeamStatMapper.xml",
    root / "basketball-admin/src/main/resources/mapper/basket/v4/BasketV4MatchPlayerStatMapper.xml",
]
for p in xml_files:
    t = p.read_text(encoding="utf-8")
    for old, new in renames:
        t = replace_type_refs(t, old, new)
    p.write_text(t, encoding="utf-8")
    print(f"updated {p.name}")

dao_files = [
    root / "basketball-web/src/main/java/com/zhs/dao/v4/BasketV4MatchDao.java",
    root / "basketball-web/src/main/java/com/zhs/dao/v4/BasketV4MatchPeriodScoreDao.java",
    root / "basketball-web/src/main/java/com/zhs/dao/v4/BasketV4MatchTeamStatDao.java",
    root / "basketball-web/src/main/java/com/zhs/dao/v4/BasketV4MatchPlayerStatDao.java",
]
for p in dao_files:
    t = p.read_text(encoding="utf-8")
    for old, new in renames:
        t = replace_type_refs(t, old, new)
    p.write_text(t, encoding="utf-8")
    print(f"updated {p.name}")

print("rename done")
