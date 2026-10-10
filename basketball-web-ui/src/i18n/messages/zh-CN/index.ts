import account from './account'
import auth from './auth'
import common from './common'
import events from './events'
import eventsPage from './eventsPage'
import home from './home'
import match from './match'
import nav from './nav'
import publicPages from './publicPages'
import registration from './registration'
import validation from './validation'

const messages = {
  common,
  nav,
  home,
  auth,
  account,
  events,
  eventsPage,
  match,
  publicPages,
  registration,
  validation,
}

export default messages

export type MessageSchema = typeof messages
