import fs from 'node:fs'

const f = 'src/components/account/AccountPageHeader.vue'
let t = fs.readFileSync(f, 'utf8')
const label = '\u9762\u5305\u5c51'
t = t.replace('aria-label="breadcrumb"', `aria-label="${label}"`)
fs.writeFileSync(f, t, 'utf8')
console.log('ok', t.includes(label))
