<script setup lang="ts">
import { leagueInfo } from '../mock'
</script>

<template>
  <section class="league-about events-section events-section-dark">
    <div class="events-inner about-grid">
      <div class="about-copy">
        <h2 class="section-head-title">关于 CNPA LEAGUE</h2>
        <span class="section-head-sub">About The League</span>
        <p>
          CNPA League 是 CNPA 体系中最高级别的 5V5 职业联赛，代表着国内电竞篮球的最高竞技水平。我们致力于打造专业、
          公平、开放的赛事环境，推动电竞篮球文化的发展与普及，为所有热爱电竞篮球的玩家提供一个实现梦想的舞台。
        </p>
        <button type="button" class="btn-outline">了解 CNPA →</button>
      </div>

      <div class="about-visual" aria-hidden="true">
        <div class="visual-frame">
          <div class="visual-accent" />
          <div class="visual-content">
            <span class="visual-tag">{{ leagueInfo.tag }}</span>
            <strong>CHAMPIONSHIP GLORY</strong>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.league-about {
  padding: 56px 0;
}

.about-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  align-items: center;
}

.about-copy {
  p {
    max-width: 520px;
    margin: 20px 0 28px;
    font-size: 14px;
    line-height: 1.9;
    color: var(--cnpa-text-secondary);
  }
}

.about-visual {
  position: relative;
  min-height: 280px;
}

.visual-frame {
  position: relative;
  height: 100%;
  min-height: 280px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background:
    linear-gradient(135deg, rgba(225, 6, 19, 0.18) 0%, transparent 42%),
    radial-gradient(circle at 70% 40%, rgba(225, 6, 19, 0.24) 0%, transparent 38%),
    linear-gradient(180deg, #151515 0%, #080808 100%);
  overflow: hidden;
}

.visual-accent {
  position: absolute;
  left: 0;
  top: 18%;
  width: 4px;
  height: 120px;
  background: var(--cnpa-red);
  box-shadow: 0 0 18px rgba(225, 6, 19, 0.45);
}

.visual-content {
  position: absolute;
  inset: auto 24px 24px 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;

  strong {
    font-family: Impact, 'Arial Narrow', sans-serif;
    font-size: clamp(28px, 3vw, 40px);
    letter-spacing: 0.06em;
  }
}

.visual-tag {
  align-self: flex-start;
  padding: 4px 10px;
  background: rgba(225, 6, 19, 0.85);
  font-size: 11px;
  font-weight: 800;
}

@media (max-width: 992px) {
  .about-grid {
    grid-template-columns: 1fr;
  }
}
</style>
