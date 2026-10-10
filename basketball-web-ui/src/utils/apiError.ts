const KNOWN_REASON_CODES = new Set([
  'INVALID_CREDENTIALS',
  'ACCOUNT_DISABLED',
  'ACCOUNT_LOCKED',
  'LOGIN_TEMPORARILY_LOCKED',
  'EMAIL_NOT_VERIFIED',
  'PASSWORD_REQUIRED',
  'CURRENT_PASSWORD_REQUIRED',
  'PASSWORD_INCORRECT',
  'PASSWORD_UNCHANGED',
  'PASSWORD_LENGTH_INVALID',
  'PASSWORD_COMPLEXITY',
  'PASSWORD_MISMATCH',
  'EMAIL_REQUIRED',
  'EMAIL_INVALID',
  'EMAIL_ALREADY_REGISTERED',
  'EMAIL_TAKEN',
  'EMAIL_UNCHANGED',
  'AGREEMENT_REQUIRED',
  'VERIFY_CODE_TOO_FREQUENT',
  'VERIFY_CODE_DAILY_LIMIT',
  'VERIFY_CODE_IP_LIMIT',
  'VERIFY_CODE_SEND_FAILED',
  'VERIFY_CODE_REQUIRED',
  'VERIFY_CODE_INVALID',
  'VERIFY_CODE_FORMAT_INVALID',
  'VERIFY_CODE_EXPIRED',
  'VERIFY_CODE_TOO_MANY_FAILURES',
  'VERIFY_CODE_MISMATCH',
  'VERIFY_CODE_USED',
  'SESSION_EXPIRED',
  'UNAUTHORIZED',
  'ACCOUNT_NOT_FOUND',
  'INVALID_REQUEST',
  'TEAM_FULL',
  'TEAM_FULL_APPLY',
  'APPLICATION_PENDING',
  'INVITATION_PENDING',
  'INVITATION_EXPIRED',
  'NO_CAPTAIN_GRANT',
  'REGISTRATION_NOT_STARTED',
  'REGISTRATION_CLOSED',
  'REGISTRATION_NOT_OPEN',
  'QUOTA_FULL',
  'PERSONAL_QUOTA_FULL',
  'PLAYER_NOT_ELIGIBLE',
  'PLAYER_NOT_CERTIFIED',
  'PERSONAL_REGISTRATION_EXISTS',
  'ALREADY_TEAM_REGISTERED',
  'PERSONAL_REVIEW_PENDING',
  'PERSONAL_ALREADY_DONE',
  'DIVISION_NOT_PERSONAL',
  'DIVISION_PERSONAL_ONLY',
  'TEAM_NOT_APPROVED',
  'ALREADY_ON_TEAM',
  'CERT_APPLICATION_PENDING',
  'ALREADY_CERTIFIED',
  'APPLICATION_STATE_CONFLICT',
  'SUBMISSION_NOT_EDITABLE',
  'SUBMISSION_IN_REVIEW',
  'SUBMISSION_NOT_ALLOWED',
  'MATCH_NOT_PUBLISHED',
  'PLAYER_MISSING',
  'PLAYER_NOT_FOUND',
  'PLATFORM_IDENTITY_MISSING',
  'GAME_ID_MISSING',
  'NOT_STARTED',
  'TERMINAL_MISMATCH',
])

export function isKnownReasonCode(reasonCode?: string | null): boolean {
  return Boolean(reasonCode && KNOWN_REASON_CODES.has(reasonCode))
}

export function resolveBusinessMessage(options: {
  reasonCode?: string | null
  serverMessage?: string | null
  locale: string
  translate: (reasonCode: string) => string
  generic: string
}): string {
  const code = options.reasonCode || ''
  if (isKnownReasonCode(code)) {
    const translated = options.translate(code)
    if (translated && translated !== code && !translated.startsWith('errors.')) {
      return translated
    }
  }
  const chineseUi = !options.locale.toLowerCase().startsWith('en')
  if (!code && chineseUi && options.serverMessage) {
    return options.serverMessage
  }
  return options.generic
}
