<script setup lang="ts">
import logoImg from '@/assets/icon/logo.png'
</script>

<template>
  <footer class="web-footer">
    <div class="footer-shell site-shell">
      <div class="footer-grid">
        <div class="footer-brand">
          <img :src="logoImg" alt="CNPA" class="footer-logo" />
          <p>CNPA 中国职业电竞篮球联赛，汇聚全国顶尖战队，打造专业、公平、开放的赛事平台。</p>
          <div class="social-icons">
            <span aria-hidden="true">W</span>
            <span aria-hidden="true">B</span>
            <span aria-hidden="true">D</span>
          </div>
        </div>

        <div class="footer-col">
          <h4>赛事</h4>
          <a href="#">CNPA League</a>
          <a href="#">CNPA Arena</a>
          <a href="#">CNPA Cup</a>
          <a href="#">CNPA Open</a>
        </div>

        <div class="footer-col">
          <h4>数据</h4>
          <a href="#">球员数据</a>
          <a href="#">战队数据</a>
          <a href="#">赛事赛程</a>
          <a href="#">数据记录</a>
        </div>

        <div class="footer-col">
          <h4>名人堂</h4>
          <a href="#">传奇球员</a>
          <a href="#">荣耀战队</a>
          <a href="#">年度时刻</a>
          <a href="#">最佳阵容</a>
        </div>

        <div class="footer-col">
          <h4>关于 CNPA</h4>
          <a href="#">关于我们</a>
          <a href="#">公告中心</a>
          <a href="#">合作伙伴</a>
          <a href="#">联系我们</a>
        </div>

        <div class="footer-qrcodes">
          <div class="qrcode-box">
            <div class="qrcode-placeholder">公众号</div>
            <span>官方公众号</span>
          </div>
          <div class="qrcode-box">
            <div class="qrcode-placeholder">微博</div>
            <span>官方微博</span>
          </div>
        </div>
      </div>

      <div class="footer-bottom">
        <span>© 2026 CNPA 中国职业电竞篮球联赛，保留所有权利。</span>
        <div class="footer-links">
          <a href="#">隐私政策</a>
          <a href="#">服务条款</a>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped lang="scss">
.web-footer {
  width: 100%;
  background: #070707;
  border-top: 1px solid var(--cnpa-border-color);
}

.footer-shell {
  padding: 48px 0 28px;
}

.footer-grid {
  display: grid;
  grid-template-columns: 1.4fr repeat(4, 1fr) 1.2fr;
  gap: 24px;
}

.footer-brand {
  p {
    margin: 16px 0;
    font-size: 13px;
    line-height: 1.7;
    color: var(--cnpa-text-secondary);
  }
}

.footer-logo {
  width: 120px;
  height: auto;
}

.social-icons {
  display: flex;
  gap: 10px;

  span {
    width: 32px;
    height: 32px;
    display: grid;
    place-items: center;
    border: 1px solid rgba(255, 255, 255, 0.12);
    font-size: 12px;
    font-weight: 900;
  }
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 10px;

  h4 {
    margin: 0 0 6px;
    font-size: 14px;
    color: #fff;
  }

  a {
    font-size: 13px;
    color: var(--cnpa-text-secondary);
    transition: color 0.2s ease;

    &:hover {
      color: var(--cnpa-red);
    }
  }
}

.footer-qrcodes {
  display: flex;
  gap: 16px;
}

.qrcode-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--cnpa-text-secondary);
}

.qrcode-placeholder {
  width: 88px;
  height: 88px;
  display: grid;
  place-items: center;
  border: 1px dashed rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.04);
  font-size: 12px;
}

.footer-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 12px;
  color: var(--cnpa-text-secondary);
}

.footer-links {
  display: flex;
  gap: 18px;

  a:hover {
    color: #fff;
  }
}

@media (max-width: 1200px) {
  .footer-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .footer-qrcodes {
    grid-column: span 3;
  }
}

@media (max-width: 768px) {
  .footer-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .footer-bottom {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
