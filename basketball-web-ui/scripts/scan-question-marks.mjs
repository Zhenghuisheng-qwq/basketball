import fs from 'node:fs'
import path from 'node:path'

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) walk(full, out)
    else if (/\.(vue|ts|scss|js|mjs)$/.test(entry.name)) out.push(full)
  }
  return out
}

const roots = ['src/views/account', 'src/components/account', 'src/mock', 'src/layouts']
const bad = []

for (const root of roots) {
  if (!fs.existsSync(root)) continue
  for (const file of walk(root)) {
    const text = fs.readFileSync(file, 'utf8')
    const lines = text.split(/\r?\n/)
    lines.forEach((line, index) => {
      if (line.includes('????') || /[\u4e00-\u9fff]/.test(line) === false && /['"`][^'"`]*\?\?\?/.test(line)) {
        if (line.includes('????') || (/\?\?\?/.test(line) && !/\?\?/.test(line.replace(/\?\?\?/g, '')) && !line.includes('?? ') && !line.includes('?.') && !line.includes('??='))) {
          // keep only clear corruption markers
        }
      }
      if (line.includes('????')) {
        bad.push(`${file}:${index + 1}:${line.trim().slice(0, 120)}`)
      }
    })
  }
}

console.log(bad.length ? bad.join('\n') : 'CLEAN')
