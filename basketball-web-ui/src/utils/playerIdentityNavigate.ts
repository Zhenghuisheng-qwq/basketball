import type { useAccountStore } from '@/stores/account'
import { isIdentityRestrictedQuery } from '@/types/account'

type AccountStore = ReturnType<typeof useAccountStore>

export function resolvePlayerIdentityRoute(accountStore: AccountStore) {
  if (accountStore.eligibilityLimited) {
    return { name: 'AccountPlayerEligibility' }
  }
  const status = accountStore.verification?.status
  if (status === 'PENDING') {
    return { name: 'AccountPlayerIdentityStatusPending' }
  }
  if (status === 'REJECTED') {
    return { name: 'AccountPlayerIdentityStatusRejected' }
  }
  if (status === 'APPROVED' || status === 'CHANGE_PENDING') {
    return { name: 'AccountPlayerIdentityStatusApproved' }
  }
  return { name: 'AccountPlayerIdentity' }
}

export function resolvePlayerIdentityRouteFromQuery(
  query: string | null | undefined,
  accountStore: AccountStore,
) {
  if (isIdentityRestrictedQuery(query)) {
    return { name: 'AccountPlayerEligibility' }
  }
  const normalized = query?.replace(/_/g, '-')
  if (normalized === 'not-submitted') {
    return { name: 'AccountPlayerIdentity' }
  }
  if (normalized === 'pending') {
    return { name: 'AccountPlayerIdentityStatusPending' }
  }
  if (normalized === 'rejected') {
    return { name: 'AccountPlayerIdentityStatusRejected' }
  }
  if (normalized === 'approved') {
    return { name: 'AccountPlayerIdentityStatusApproved' }
  }
  return resolvePlayerIdentityRoute(accountStore)
}

export function resolvePlayerIdentityStatusRedirect(accountStore: AccountStore) {
  return resolvePlayerIdentityRoute(accountStore)
}
