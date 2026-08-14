<template>
  <el-alert type="warning" :closable="false" show-icon class="mb8" title="预留 / Demo，当前版本暂不用于正式赛事" />
  <el-card shadow="never">
    <p>双败淘汰仅作产品预览。当前版本不会生成系列赛、比赛、赛程结构或发布记录。</p>
    <el-descriptions :column="1" border size="small" class="mt8">
      <el-descriptions-item label="Winner Bracket">胜者组：按单败树向下推进</el-descriptions-item>
      <el-descriptions-item label="Loser Bracket">败者组：从胜者组掉入后继续比赛</el-descriptions-item>
      <el-descriptions-item label="Grand Final">总决赛：胜者组冠军对败者组冠军（本版本不实现 Reset）</el-descriptions-item>
    </el-descriptions>
    <el-alert class="mt8" type="error" :closable="false" title="当前版本暂未开放双败淘汰正式执行。" />
  </el-card>
</template>
