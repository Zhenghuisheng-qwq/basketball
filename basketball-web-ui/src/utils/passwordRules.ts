/** 官网密码强度前端校验（与注册/改密表单共用，非 Mock） */
export function validatePasswordRules(password: string): string {
  if (!password) return '请输入新密码'
  if (password.length < 8 || password.length > 20) {
    return '密码长度应为 8 至 20 位'
  }
  if (!/[a-zA-Z]/.test(password)) {
    return '密码需同时包含字母和数字'
  }
  if (!/\d/.test(password)) {
    return '密码需同时包含字母和数字'
  }
  return ''
}

export type PasswordChangeErrorField = 'currentPassword' | 'newPassword' | 'confirmPassword' | 'general'

/** 将改密接口/校验错误映射到对应表单字段 */
export function resolvePasswordChangeError(message: string): { field: PasswordChangeErrorField; message: string } {
  const text = (message || '').trim()
  if (!text) return { field: 'general', message: '密码修改失败' }
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
