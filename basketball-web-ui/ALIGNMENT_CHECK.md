# 版心对齐检查报告

## 📐 当前结构

### Hero 区域
```vue
<section class="hero">                    <!-- 全屏容器 -->
  <div class="hero-image-wrapper">        <!-- 背景图全屏 -->
  <div class="site-shell hero-container"> <!-- ✅ 版心内容 -->
    <div class="hero-content">...</div>
  </div>
</section>
```

### Stats 区域
```vue
<section class="stats-bar">               <!-- 全屏容器 -->
  <div class="site-shell stats-container"><!-- ✅ 版心内容 -->
    ...
  </div>
</section>
```

### Events 区域
```vue
<section class="site-shell section">      <!-- ✅ 版心容器 -->
  <div class="section-header">...</div>
  <div class="events-grid">...</div>
</section>
```

### Rankings 区域
```vue
<section class="site-shell section">      <!-- ✅ 版心容器 -->
  <div class="section-header">...</div>
  <div class="rankings-grid">...</div>
</section>
```

### Schedule 区域
```vue
<section class="site-shell section">      <!-- ✅ 版心容器 -->
  <div class="section-header">...</div>
  <div class="schedule-grid">...</div>
</section>
```

### Footer 区域
```vue
<footer class="footer">                   <!-- 全屏容器 -->
  <div class="site-shell footer-container"><!-- ✅ 版心内容 -->
    ...
  </div>
</footer>
```

## ✅ 对齐检查

所有区域都已正确使用 `.site-shell`：

| 区域 | 外层 | 内层 | 状态 |
|------|------|------|------|
| Hero | 全屏 | `.site-shell` | ✅ |
| Stats | 全屏 | `.site-shell` | ✅ |
| Events | `.site-shell` | - | ✅ |
| Rankings | `.site-shell` | - | ✅ |
| Schedule | `.site-shell` | - | ✅ |
| Footer | 全屏 | `.site-shell` | ✅ |

## 🎯 版心效果

`.site-shell` 类会自动应用：
- `width: var(--site-content-width)`
- `margin-inline: auto`
- `padding-inline: var(--site-inner-padding)`

其中：
- `--site-content-width`: `min(calc(100% - var(--site-gutter) * 2), var(--site-width))`
- `--site-width`: `clamp(1600px, 70vw, 2400px)`
- `--site-gutter`: `clamp(16px, 3.2vw, 72px)`

## 🔍 如何验证对齐

### 方法1: 浏览器开发者工具
1. 打开 `http://localhost:5173/home`
2. 按 F12 打开开发者工具
3. 选择任意 `.site-shell` 元素
4. 查看计算后的宽度和边距

### 方法2: 添加调试边框
临时在浏览器控制台执行：
```javascript
document.querySelectorAll('.site-shell').forEach(el => {
  el.style.outline = '2px solid red';
});
```

所有红框应该完全对齐。

### 方法3: 对比原页面
访问 `/home` 和其他页面（如 `/tournament`），比较内容区域是否对齐。

## 💡 可能的视觉差异原因

如果看起来不对齐，可能是：

1. **浏览器缓存** - 强制刷新 (Ctrl+Shift+R)
2. **CSS 未生效** - 检查是否有样式覆盖
3. **其他页面版心不同** - 确认对比的页面也使用相同版心

## 📝 验证步骤

1. 启动开发服务器：
   ```bash
   cd basketball-web-ui
   npm run dev
   ```

2. 访问首页：`http://localhost:5173/home`

3. 使用浏览器开发者工具检查 `.site-shell` 元素的计算样式

4. 对比其他页面（如赛事页面）的版心宽度

---

**如果仍然不对齐，请提供具体的截图或描述哪些区域看起来不一致，我会进一步调整。**
