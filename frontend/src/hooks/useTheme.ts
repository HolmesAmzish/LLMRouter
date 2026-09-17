import { useCallback, useEffect, useState } from 'react'

export type Theme = 'light' | 'dark' | 'system'
export type ResolvedTheme = Exclude<Theme, 'system'>

const THEME_STORAGE_KEY = 'llm-router.theme'

function isTheme(value: string | null): value is Theme {
  return value === 'light' || value === 'dark' || value === 'system'
}

function getSystemTheme(): ResolvedTheme {
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function readTheme(): Theme {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY)
    return isTheme(stored) ? stored : 'system'
  } catch {
    return 'system'
  }
}

function resolveTheme(theme: Theme): ResolvedTheme {
  return theme === 'system' ? getSystemTheme() : theme
}

function applyTheme(theme: Theme): ResolvedTheme {
  const resolved = resolveTheme(theme)
  const root = document.documentElement
  root.classList.toggle('dark', resolved === 'dark')
  root.dataset.theme = resolved

  const background = getComputedStyle(root).getPropertyValue('--background').trim()
  const themeColor = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')
  if (background && themeColor) themeColor.content = background

  return resolved
}

export function useTheme() {
  const [theme, setThemeState] = useState<Theme>(readTheme)
  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme>(() => resolveTheme(readTheme()))

  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const sync = () => setResolvedTheme(applyTheme(theme))

    sync()
    if (theme === 'system') media.addEventListener('change', sync)
    return () => media.removeEventListener('change', sync)
  }, [theme])

  useEffect(() => {
    const sync = (event: StorageEvent) => {
      if (event.key === THEME_STORAGE_KEY) setThemeState(readTheme())
    }
    window.addEventListener('storage', sync)
    return () => window.removeEventListener('storage', sync)
  }, [])

  const setTheme = useCallback((nextTheme: Theme) => {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, nextTheme)
    } catch {
      // Keep the in-memory theme when storage is unavailable.
    }
    setThemeState(nextTheme)
  }, [])

  return { theme, resolvedTheme, setTheme }
}
