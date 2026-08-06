
import { chromium } from 'playwright';
const adminUi = process.env.CNPA_ADMIN_UI_URL || 'http://127.0.0.1:80';
const webUi = process.env.CNPA_WEB_UI_URL || 'http://localhost:5173';
const adminUser = process.env.CNPA_ADMIN_USERNAME || 'admin';
const adminPass = process.env.CNPA_ADMIN_PASSWORD || 'admin123';
const out = { pages: [], consoleErrors: [], network500: [], tournamentHits: [] };
const browser = await chromium.launch({ headless: true });
try {
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  page.on('console', msg => { if (msg.type() === 'error') out.consoleErrors.push(msg.text()); });
  page.on('response', res => {
    const u = res.url();
    if (res.status() >= 500) out.network500.push({ url: u, status: res.status() });
    if (/tournament/i.test(u) && !/format|documentation/i.test(u)) out.tournamentHits.push(u);
  });
  await page.goto(adminUi + '/login', { waitUntil: 'domcontentloaded', timeout: 30000 });
  out.pages.push({ name: 'admin_login', url: page.url(), title: await page.title(), ok: !!(await page.content()).length });
  try {
    await page.fill('input[placeholder*="账号"], input[name="username"], input[type="text"]', adminUser, { timeout: 3000 }).catch(()=>{});
    await page.fill('input[type="password"]', adminPass, { timeout: 3000 }).catch(()=>{});
    await page.click('button[type="submit"], button:has-text("登录")', { timeout: 3000 }).catch(()=>{});
    await page.waitForTimeout(2000);
  } catch (e) {}
  for (const p of ['/cnpa/event-v4/season','/cnpa/event-v4/registration-audit','/cnpa/event-v4/event-team','/cnpa/event-v4/schedule','/cnpa/event-v4/match','/cnpa/event-v4/match-submission-audit','/cnpa/event-v4/standing']) {
    try {
      const resp = await page.goto(adminUi + p, { waitUntil: 'domcontentloaded', timeout: 20000 });
      await page.waitForTimeout(800);
      const body = await page.content();
      out.pages.push({ name: 'admin'+p, url: page.url(), http: resp ? resp.status() : 0, hasContent: body.length > 100,
        hasTournament: /Tournament|tournamentId/i.test(body) && !/赛制/.test(body) });
    } catch (e) { out.pages.push({ name: 'admin'+p, error: String(e) }); }
  }
  const web = await ctx.newPage();
  web.on('console', msg => { if (msg.type() === 'error') out.consoleErrors.push('web:'+msg.text()); });
  web.on('response', res => { if (res.status() >= 500) out.network500.push({ url: res.url(), status: res.status() }); });
  for (const p of ['/', '/events', '/account/my-events', '/account/my-schedule']) {
    try {
      const resp = await web.goto(webUi + p, { waitUntil: 'domcontentloaded', timeout: 20000 });
      await web.waitForTimeout(800);
      const body = await web.content();
      out.pages.push({ name: 'web'+p, url: web.url(), http: resp ? resp.status() : 0, hasContent: body.length > 100 });
    } catch (e) { out.pages.push({ name: 'web'+p, error: String(e) }); }
  }
} finally { await browser.close(); }
import fs from 'fs';
fs.writeFileSync("D:\\code\\dz\\basketball\\scaffolding-v2\\scripts\\output\\cnpa-v2-release-gate-rr4-browser.json", JSON.stringify(out, null, 2));
console.log(JSON.stringify({ pages: out.pages.length, consoleErrors: out.consoleErrors.length, network500: out.network500.length, tournamentHits: out.tournamentHits.length }));
