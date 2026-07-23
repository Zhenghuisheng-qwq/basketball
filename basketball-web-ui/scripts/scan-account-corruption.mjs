import fs from 'node:fs'
import path from 'node:path'

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) walk(full, out)
    else if (/\.(vue|ts|scss)$/.test(entry.name)) out.push(full)
  }
  return out
}

const roots = ['src/views/account', 'src/components/account', 'src/mock']
const bad = []

for (const root of roots) {
  if (!fs.existsSync(root)) continue
  for (const file of walk(root)) {
    const text = fs.readFileSync(file, 'utf8')
    if (text.includes('????') || /zh-title="\?/.test(text) || />\?\?\?</.test(text)) {
      bad.push(file)
    }
  }
}

if (bad.length) {
  console.log('CORRUPTED:')
  bad.forEach((f) => console.log(f))
  process.exit(1)
}

console.log('NO_CORRUPTED_FILES')
