const EDITABLE_SELECTOR = 'input, textarea, select, [contenteditable=""], [contenteditable="true"]'

function isEditableTarget(target: EventTarget | null): boolean {
  if (!(target instanceof Element)) {
    return false
  }
  return Boolean(target.closest(EDITABLE_SELECTOR))
}

function isDeterrenceShortcut(event: KeyboardEvent): boolean {
  const key = event.key
  if (key === 'F12') {
    return true
  }
  const ctrl = event.ctrlKey || event.metaKey
  if (!ctrl) {
    return false
  }
  if (event.shiftKey && (key === 'I' || key === 'i' || key === 'J' || key === 'j' || key === 'C' || key === 'c')) {
    return true
  }
  if (!event.shiftKey && !event.altKey && (key === 'U' || key === 'u' || key === 'S' || key === 's')) {
    return true
  }
  return false
}

export function installBrowserDeterrence(): void {
  if (!import.meta.env.PROD) {
    return
  }
  document.addEventListener(
    'keydown',
    (event) => {
      if (!isDeterrenceShortcut(event)) {
        return
      }
      event.preventDefault()
      event.stopPropagation()
    },
    true,
  )
  document.addEventListener(
    'contextmenu',
    (event) => {
      if (isEditableTarget(event.target)) {
        return
      }
      event.preventDefault()
    },
    true,
  )
}