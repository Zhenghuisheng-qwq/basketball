from pathlib import Path

for path in [
    Path(r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\main\java\com\zhs\response\v4\V4AvailableDivisionResp.java"),
    Path(r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\main\java\com\zhs\response\v4\V4RegistrationDivisionContextResp.java"),
]:
    text = path.read_text(encoding="utf-8")
    if "JsonFormat" not in text:
        text = text.replace(
            "import com.fasterxml.jackson.databind.annotation.JsonSerialize;\n",
            "import com.fasterxml.jackson.annotation.JsonFormat;\nimport com.fasterxml.jackson.databind.annotation.JsonSerialize;\n",
        )
    for field in [
        "private Date registrationStartTime;",
        "private Date registrationEndTime;",
        "private Date matchStartTime;",
        "private Date matchEndTime;",
    ]:
        annotated = '@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")\n    ' + field
        if annotated in text:
            continue
        if field not in text:
            raise SystemExit(f"missing {field} in {path}")
        text = text.replace(field, annotated)
    path.write_text(text, encoding="utf-8", newline="\n")
    print(path.name, "ok")
