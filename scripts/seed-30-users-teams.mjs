/**
 * @deprecated 请使用完整编排脚本：
 *   node scripts/seed-cnpa-full-pipeline.mjs
 *   node scripts/seed-cnpa-full-pipeline.mjs --skip-init --email-start=111111 --count=30
 */
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const dir = path.dirname(fileURLToPath(import.meta.url))
const target = path.join(dir, 'seed-cnpa-full-pipeline.mjs')
const extra = process.argv.slice(2)
const args = extra.length ? extra : ['--skip-init', '--email-start=111111', '--count=30']
console.log(`[redirect] node ${target} ${args.join(' ')}`)
const r = spawnSync(process.execPath, [target, ...args], { stdio: 'inherit' })
process.exit(r.status ?? 1)
