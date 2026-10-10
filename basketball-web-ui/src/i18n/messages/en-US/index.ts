import type { MessageSchema } from '../zh-CN'
import account from './account'
import auth from './auth'
import common from './common'
import events from './events'
import match from './match'
import nav from './nav'
import registration from './registration'
import validation from './validation'

const messages = {
  common,
  nav,
  auth,
  account,
  events,
  match,
  registration,
  validation,
} satisfies MessageSchema

export default messages
