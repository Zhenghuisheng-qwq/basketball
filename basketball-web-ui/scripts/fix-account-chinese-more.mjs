import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const projectRoot = path.resolve(__dirname, '..')
const accountRoot = path.join(projectRoot, 'src/views/account')
const ps1Path = path.join(projectRoot, 'fix-account-utf8.ps1')

function decode(source) {
  return source.replace(/\\u([0-9a-fA-F]{4})/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)))
}

function toEscapedSource(content) {
  let out = ''
  for (const ch of content) {
    const code = ch.charCodeAt(0)
    if (code > 127) {
      out += '\\u' + code.toString(16).padStart(4, '0')
    } else {
      out += ch
    }
  }
  return out
}

function extractPs1HereDoc(ps1Text, varName) {
  const marker = `$content_${varName} = @'`
  const start = ps1Text.indexOf(marker)
  if (start < 0) throw new Error(`Missing heredoc: ${varName}`)
  let bodyStart = start + marker.length
  if (ps1Text[bodyStart] === '\r') bodyStart += 2
  else if (ps1Text[bodyStart] === '\n') bodyStart += 1
  const closeMatch = ps1Text.slice(bodyStart).match(/\r?\n'@/)
  if (!closeMatch || closeMatch.index == null) throw new Error(`Unclosed heredoc: ${varName}`)
  const end = bodyStart + closeMatch.index
  return ps1Text.slice(bodyStart, end).replace(/\r\n/g, '\n')
}

function writeVue(rel, escapedSource) {
  const file = path.join(accountRoot, rel)
  const content = decode(escapedSource)
  fs.writeFileSync(file, content, 'utf8')
  const hasCn = /[\u4e00-\u9fff]/.test(content)
  const hasCorrupt = content.includes('????')
  console.log(rel, hasCn ? 'has-CN' : 'NO-CN', hasCorrupt ? 'HAS-????' : 'ok', 'len=' + content.length)
  return content
}

function patchPlayerVerification(source) {
  return source.replace(
    /\.account-page \{\r?\n  padding: clamp\(24px, 3vw, 40px\) clamp\(20px, 3vw, 48px\) 48px;\r?\n\}/,
    `.account-page {
  width: 100%;
  min-width: 0;
}`,
  )
}

function patchTeamRelations(source) {
  return source.replace(
    /\.account-page \{\r?\n  padding: clamp\(24px, 3vw, 40px\) clamp\(20px, 3vw, 48px\) 48px;\r?\n  display: flex;\r?\n  flex-direction: column;\r?\n  gap: 18px;\r?\n\}/,
    `.account-page {
  width: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}`,
  )
}

const ps1Text = fs.readFileSync(ps1Path, 'utf8')

const playerVerification = patchPlayerVerification(
  extractPs1HereDoc(ps1Text, 'player_verification_index_vue'),
)
const teamRelations = patchTeamRelations(extractPs1HereDoc(ps1Text, 'team_relations_index_vue'))

writeVue('player-verification/index.vue', toEscapedSource(playerVerification))
writeVue('team-relations/index.vue', toEscapedSource(teamRelations))

const verifyFiles = [
  'overview/index.vue',
  'security/index.vue',
  'my-tournaments/index.vue',
  'profile/index.vue',
  'player-verification/index.vue',
  'team-relations/index.vue',
]

for (const rel of verifyFiles) {
  const text = fs.readFileSync(path.join(accountRoot, rel), 'utf8')
  console.log(
    'verify',
    rel,
    'CN=' + /[\u4e00-\u9fff]/.test(text),
    '????=' + text.includes('????'),
  )
}

const corruptFiles = []

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      walk(full)
    } else if (/\.(vue|ts|scss)$/.test(entry.name)) {
      const text = fs.readFileSync(full, 'utf8')
      if (text.includes('????')) corruptFiles.push(path.relative(projectRoot, full))
    }
  }
}

walk(accountRoot)
console.log('any ???? left under src/views/account:', corruptFiles.length ? corruptFiles : 'none')
