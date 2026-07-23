import fs from 'node:fs'

const files = [
  'src/views/account/security/index.vue',
  'src/views/account/profile/index.vue',
  'src/views/account/player-verification/index.vue',
  'src/views/account/team-relations/index.vue',
  'src/views/account/notifications/index.vue',
]

for (const file of files) {
  if (!fs.existsSync(file)) {
    console.log('missing', file)
    continue
  }
  let text = fs.readFileSync(file, 'utf8')
  const before = text
  text = text.replaceAll('class="account-page account-center-vars"', 'class="account-view"')
  text = text.replaceAll('class="account-page"', 'class="account-view"')
  text = text.replaceAll('.account-page {', '.account-view {')
  if (text !== before) {
    fs.writeFileSync(file, text, 'utf8')
    console.log('patched', file)
  } else {
    console.log('unchanged', file)
  }
}
