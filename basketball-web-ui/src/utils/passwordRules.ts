/** 官网密码强度前端校验（与注册/改密表单共用，非 Mock） */
export function validatePasswordRules(password: string): string {
  if (password.length < 8 || password.length > 20) {
    return '密码长度应为 8 至 20 位'
  }
  if (!/[a-zA-Z]/.test(password)) {
    return '密码至少包含一个字母'
  }
  if (!/\d/.test(password)) {
    return '密码至少包含一个数字'
  }
  return ''
}
