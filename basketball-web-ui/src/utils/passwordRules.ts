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
