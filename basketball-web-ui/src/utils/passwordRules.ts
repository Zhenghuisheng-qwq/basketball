import { translate } from '@/i18n'

/** 官网密码强度前端校验（与注册/改密表单共用，非 Mock） */
export function validatePasswordRules(password: string): string {
  if (!password) return translate('accountShell.profile.enterNewPassword')
  if (password.length < 8 || password.length > 20) {
    return translate('accountShell.profile.passwordLength')
  }
  if (!/[a-zA-Z]/.test(password)) {
    return translate('accountShell.profile.passwordNeedBoth')
  }
  if (!/\d/.test(password)) {
    return translate('accountShell.profile.passwordNeedBoth')
  }
  return ''
}

export type PasswordChangeErrorField = 'currentPassword' | 'newPassword' | 'confirmPassword' | 'general'

const PASSWORD_ERROR_FIELDS: Record<Exclude<PasswordChangeErrorField, 'general'>, string[]> = {
  currentPassword: [
    'accountShell.settings.currentPasswordRequired',
    'accountShell.security.currentPasswordRequired',
  ],
  newPassword: [
    'accountShell.profile.enterNewPassword',
    'accountShell.profile.passwordLength',
    'accountShell.profile.passwordNeedBoth',
    'accountShell.profile.samePassword',
  ],
  confirmPassword: [
    'accountShell.settings.confirmPasswordRequired',
    'accountShell.security.confirmPasswordRequired',
    'accountShell.settings.passwordMismatch',
    'accountShell.security.passwordMismatch',
  ],
}

function matchesOwnMessage(text: string, key: string): boolean {
  const localized = translate(key)
  return localized !== key && text === localized
}

/** 将改密接口/校验错误映射到对应表单字段。后端中文错误仍原样展示。 */
export function resolvePasswordChangeError(message: string): { field: PasswordChangeErrorField; message: string } {
  const text = (message || '').trim()
  if (!text) return { field: 'general', message: translate('accountShell.profile.passwordChangeFailed') }

  for (const field of ['currentPassword', 'newPassword', 'confirmPassword'] as const) {
    if (PASSWORD_ERROR_FIELDS[field].some((key) => matchesOwnMessage(text, key))) {
      return { field, message: text }
    }
  }

  if (text.includes('当前密码不正确') || text.includes('请输入当前密码')) {
    return { field: 'currentPassword', message: text }
  }
  if (text.includes('新密码不能与')) {
    return { field: 'newPassword', message: text }
  }
  if (text.includes('两次') && text.includes('不一致')) {
    return { field: 'confirmPassword', message: text }
  }
  if (text.includes('密码长度') || text.includes('密码需') || text.includes('密码至少') || text.includes('密码不能为空')) {
    return { field: 'newPassword', message: text }
  }
  return { field: 'general', message: text }
}
