import { useSyncExternalStore } from 'react'

/** Minimal History API routing: three pages do not justify a routing library. */
const NAVIGATION_EVENT = 'quinquennat:navigate'

function subscribe(onChange: () => void) {
  window.addEventListener('popstate', onChange)
  window.addEventListener(NAVIGATION_EVENT, onChange)
  return () => {
    window.removeEventListener('popstate', onChange)
    window.removeEventListener(NAVIGATION_EVENT, onChange)
  }
}

export function usePath(): string {
  return useSyncExternalStore(subscribe, () => window.location.pathname)
}

export function navigate(path: string) {
  window.history.pushState(null, '', path)
  window.dispatchEvent(new Event(NAVIGATION_EVENT))
  window.scrollTo(0, 0)
}
