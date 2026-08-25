# 当前赛事版心检查

## 📐 当前结构

```vue
<section class="site-section home-current-events-section">
  <div class="site-shell">  <!-- ✅ 使用了全局版心 -->
    <div class="section-head section-head-light">
      <div>
        <span class="home-kicker">CURRENT EVENTS</span>
        <h2>当前赛事</h2>
      </div>
    </div>

    <div class="current-events-grid">
      <!-- 4个赛事卡片 -->
    </div>
  </div>
</section>
```

## ✅ 版心正确应用

- ✅ 外层：`.home-current-events-section` - 全屏容器
- ✅ 内层：`.site-shell` - 版心容器（自动应用全局宽度）
- ✅ 标题和卡片都在 `.site-shell` 内

## 🔍 对比其他区域

### 近期比赛
```vue
<section class="home-match-section">
  <div class="site-shell home-match-shell">
    <!-- 内容 -->
  </div>
</section>
```

### 本周焦点战队
```vue
<section class="home-focus-section">
  <div class="site-shell">
    <!-- 内容 -->
  </div>
</section>
```

### 当前赛事（一致）
```vue
<section class="home-current-events-section">
  <div class="site-shell">
    <!-- 内容 -->
  </div>
</section>
```

## 🎨 样式检查

```scss
.home-current-events-section {
  background: #f5f5f5;
  padding: clamp(40px, 5vw, 60px) 0;
  // ✅ 没有覆盖 width 或 margin
}

.current-events-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-top: 24px;
  // ✅ 没有覆盖版心设置
}
```

## 🐛 如何验证版心是否对齐

在浏览器控制台运行：

```javascript
// 检查所有 .site-shell 的左边距
document.querySelectorAll('.site-shell').forEach((el, i) => {
  const rect = el.getBoundingClientRect();
  const section = el.closest('section')?.className || 'unknown';
  console.log(`${i + 1}. ${section}: left=${rect.left}px, width=${rect.width}px`);
});
```

如果所有 `.site-shell` 的 `left` 值相同，说明版心对齐。

## 💡 可能的问题

如果看起来不对齐，可能是：

1. **浏览器缓存** - 强制刷新 (Ctrl+Shift+R)
2. **其他样式覆盖** - 检查开发者工具中的计算样式
3. **视觉误差** - 灰色背景可能让版心看起来不同

## 🔧 强制调试

临时添加红色边框查看版心：

```scss
.home-current-events-section .site-shell {
  outline: 2px solid red !important;
}
```

---

如果版心确实有问题，请告诉我具体是什么问题（比如：内容超出了、内容太窄、左右不对齐等），我会进一步修复。
