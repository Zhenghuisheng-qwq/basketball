# -*- coding: utf-8 -*-
from pathlib import Path

root = Path(__file__).resolve().parents[1]
renames = [
    "BasketMatch",
    "BasketMatchPeriodScore",
    "BasketMatchTeamStat",
    "BasketMatchPlayerStat",
]

for name in renames:
    new = "BasketV4" + name[len("Basket") :]
    old_java = root / f"basketball-admin/src/main/java/com/zhs/basket/mapper/v4/{name}Mapper.java"
    new_java = root / f"basketball-admin/src/main/java/com/zhs/basket/mapper/v4/{new}Mapper.java"
    text = old_java.read_text(encoding="utf-8")
    text = text.replace(f"public interface {name}Mapper", f"public interface {new}Mapper")
    new_java.write_text(text, encoding="utf-8")
    old_java.unlink()

    old_xml = root / f"basketball-admin/src/main/resources/mapper/basket/v4/{name}Mapper.xml"
    new_xml = root / f"basketball-admin/src/main/resources/mapper/basket/v4/{new}Mapper.xml"
    xt = old_xml.read_text(encoding="utf-8")
    xt = xt.replace(f"com.zhs.basket.mapper.v4.{name}Mapper", f"com.zhs.basket.mapper.v4.{new}Mapper")
    new_xml.write_text(xt, encoding="utf-8")
    old_xml.unlink()

    old_dao = root / f"basketball-web/src/main/java/com/zhs/dao/v4/{name}Dao.java"
    new_dao = root / f"basketball-web/src/main/java/com/zhs/dao/v4/{new}Dao.java"
    dt = old_dao.read_text(encoding="utf-8")
    dt = dt.replace(f"public interface {name}Dao", f"public interface {new}Dao")
    new_dao.write_text(dt, encoding="utf-8")
    old_dao.unlink()
    print(f"renamed {name} -> {new}")

print("done")
