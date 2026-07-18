<template>
  <footer class="web-footer">
    <div class="footer-inner">
      <p class="web-footer__title">CNPA 线上篮球赛事平台</p>
      <p class="web-footer__copyright">Copyright © 2026 CNPA</p>
    </div>
  </footer>
</template>

<style scoped lang="scss">
.web-footer {
  width: 100%;
  background: #070707;
  border-top: 1px solid var(--cnpa-border-color);
}

.footer-inner {
  width: 100%;
  padding: 34px var(--site-inner-padding);
  box-sizing: border-box;
  text-align: center;
}

.web-footer__title {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--cnpa-text-primary);
}

.web-footer__copyright {
  margin: 0;
  font-size: 12px;
  color: var(--cnpa-text-secondary);
}
</style>
