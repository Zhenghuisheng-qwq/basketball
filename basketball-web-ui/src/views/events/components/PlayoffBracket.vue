<script setup lang="ts">
// 公开接口暂无季后赛对阵图，本轮展示空状态，不回退 Mock
</script>

<template>
  <section class="playoff-bracket">
    <div class="playoff-bracket__heading">
      <span class="playoff-bracket__league">CNPA LEAGUE</span>
      <h3 class="playoff-bracket__title">
        <i aria-hidden="true" />
        季后赛对阵图
        <i aria-hidden="true" />
      </h3>
    </div>
    <div class="playoff-bracket__empty">暂无季后赛对阵数据</div>
  </section>
</template>

<style scoped lang="scss">
.playoff-bracket {
  padding: 12px 0 28px;
}

.playoff-bracket__heading {
  text-align: center;
  margin-bottom: 24px;
}

.playoff-bracket__league {
  display: block;
  margin-bottom: 8px;
  color: rgba(17, 17, 17, 0.45);
  font-size: 12px;
  letter-spacing: 0.12em;
  font-weight: 800;
}

.playoff-bracket__title {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-size: 22px;

  i {
    width: 28px;
    height: 2px;
    background: rgba(17, 17, 17, 0.2);
  }
}

.playoff-bracket__empty {
  min-height: 220px;
  display: grid;
  place-items: center;
  border: 1px dashed rgba(17, 17, 17, 0.18);
  background: rgba(255, 255, 255, 0.35);
  color: rgba(17, 17, 17, 0.5);
  font-weight: 700;
}
</style>
