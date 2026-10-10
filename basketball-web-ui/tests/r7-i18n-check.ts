import assert from 'node:assert/strict'
import { displayNoticeText, renderNotice } from '../src/i18n/noticeTemplates.ts'
import { resolveBusinessMessage } from '../src/utils/apiError.ts'

const zh = {
  INVALID_CREDENTIALS: '邮箱或密码错误',
  TEAM_FULL: '球队成员已满',
  APPLICATION_PENDING: '已存在待处理的申请',
  REGISTRATION_CLOSED: '报名已结束',
  PLAYER_NOT_ELIGIBLE: '当前账号未关联认证球员',
  VERIFY_CODE_EXPIRED: '验证码已过期，请重新获取',
  VERIFY_CODE_MISMATCH: '验证码错误',
}
const en = {
  INVALID_CREDENTIALS: 'The email or password is incorrect.',
  TEAM_FULL: 'This team is full.',
  APPLICATION_PENDING: 'A request is already pending.',
  REGISTRATION_CLOSED: 'Registration is closed.',
  PLAYER_NOT_ELIGIBLE: 'This account is not linked to a verified player.',
  VERIFY_CODE_EXPIRED: 'The code has expired. Request a new one.',
  VERIFY_CODE_MISMATCH: 'The code is incorrect.',
}

function translate(locale: 'zh-CN' | 'en-US', code: string) {
  return (locale === 'en-US' ? en : zh)[code as keyof typeof zh]
}

for (const code of ['INVALID_CREDENTIALS', 'TEAM_FULL', 'APPLICATION_PENDING', 'REGISTRATION_CLOSED', 'PLAYER_NOT_ELIGIBLE', 'VERIFY_CODE_EXPIRED', 'VERIFY_CODE_MISMATCH'] as const) {
  const message = resolveBusinessMessage({
    reasonCode: code,
    serverMessage: '邮箱或密码错误',
    locale: 'en-US',
    generic: 'Something went wrong. Please try again.',
    translate: (reason) => translate('en-US', reason),
  })
  assert.equal(message, en[code])
  assert.equal(message.includes('不存在'), false)
}

assert.equal(resolveBusinessMessage({
  reasonCode: 'NOT_A_REAL_CODE',
  serverMessage: '内部细节',
  locale: 'en-US',
  generic: 'Something went wrong. Please try again.',
  translate: (reason) => translate('en-US', reason),
}), 'Something went wrong. Please try again.')

assert.equal(resolveBusinessMessage({
  reasonCode: undefined,
  serverMessage: '请先填写球队名称',
  locale: 'zh-CN',
  generic: '操作失败，请稍后重试',
  translate: (reason) => translate('zh-CN', reason),
}), '请先填写球队名称')

const rendered = renderNotice('TEAM_INVITATION_RECEIVED', JSON.stringify({ teamName: '<Red>' }), 'en-US')
assert.ok(rendered)
assert.equal(rendered.title, 'Team invitation')
assert.equal(rendered.body.includes('<Red>'), true)
assert.equal(displayNoticeText(null, '历史中文通知', rendered.body), '历史中文通知')
assert.equal(displayNoticeText('TEAM_INVITATION_RECEIVED', '历史中文通知', rendered.body), rendered.body)

const rejected = renderNotice('PLAYER_CERT_REJECTED', JSON.stringify({ remark: '照片不清晰' }), 'en-US')
assert.ok(rejected)
assert.equal(rejected.body.includes('照片不清晰'), true)
assert.equal(rejected.body.toLowerCase().includes('photo is unclear'), false)

console.log('r7 i18n checks passed')
