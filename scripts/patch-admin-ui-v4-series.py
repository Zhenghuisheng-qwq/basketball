#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FILES = [
    "basketball-admin-ui/src/views/cnpa/content/announcement/index.vue",
    "basketball-admin-ui/src/views/cnpa/content/player-honor/index.vue",
    "basketball-admin-ui/src/views/cnpa/content/team-honor/index.vue",
    "basketball-admin-ui/src/views/cnpa/content/hall-of-fame/HallOfFameDisplayPanel.vue",
]

IMPORT_BLOCK = """import { listV4Series } from '@/api/cnpa/event-v4/series'
import { listV4Season } from '@/api/cnpa/event-v4/season'
"""

LOAD_TOURNAMENTS = """function loadTournaments() {
  listV4Series().then(res => {
    tournamentOptions.value = (res.data || []).map((s: any) => ({ id: s.id, tournamentName: s.seriesName }))
    if (tournamentOptions.value.length > 0) {
      selectedTournamentId.value = tournamentOptions.value[0].id
      queryParams.value.tournamentId = selectedTournamentId.value
      loadSeasons().then(() => getList())
    } else {
      getList()
    }
  })
}
"""

LOAD_SEASONS = """function loadSeasons() {
  seasonOptions.value = []
  queryParams.value.seasonId = undefined
  if (!selectedTournamentId.value) return Promise.resolve()
  return listV4Season({ seriesId: selectedTournamentId.value, pageNum: 1, pageSize: 500 }).then(res => {
    seasonOptions.value = res.rows || []
    queryParams.value.seasonId = seasonOptions.value[0]?.id
    if (typeof candidateQuery !== 'undefined') candidateQuery.value.seasonId = queryParams.value.seasonId
  })
}
"""

for rel in FILES:
    path = ROOT / rel
    if not path.exists():
        print("skip", rel)
        continue
    text = path.read_text(encoding="utf-8")
    text = text.replace("import { listSeasonByTournament } from '@/api/basket/season'\n", "")
    text = text.replace("import { listTournament } from '@/api/basket/tournament'\n", "")
    if "listV4Series" not in text:
        text = text.replace("from '@/types'", IMPORT_BLOCK + "from '@/types'", 1)
    import re
    text = re.sub(
        r"function loadTournaments\(\) \{[\s\S]*?\n\}\n\nfunction loadSeasons\(\) \{[\s\S]*?\n\}",
        LOAD_TOURNAMENTS + "\n" + LOAD_SEASONS,
        text,
        count=1,
    )
    path.write_text(text, encoding="utf-8")
    print("patched", rel)
