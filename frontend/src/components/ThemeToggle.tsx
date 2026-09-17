import { useEffect, useRef, useState } from 'react'
import { Check, Monitor, Moon, Sun, type LucideIcon } from 'lucide-react'
import { useTheme, type Theme } from '../hooks/useTheme'

const options: { value: Theme; label: string; icon: LucideIcon }[] = [
  { value: 'light', label: 'Light', icon: Sun },
  { value: 'dark', label: 'Dark', icon: Moon },
  { value: 'system', label: 'System', icon: Monitor },
]

export function ThemeToggle() {
  const { theme, resolvedTheme, setTheme } = useTheme()
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const current = options.find((option) => option.value === theme) ?? options[2]
  const TriggerIcon = current.icon

  useEffect(() => {
    if (!open) return

    const closeOnPointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false)
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }

    window.addEventListener('pointerdown', closeOnPointerDown)
    window.addEventListener('keydown', closeOnEscape)
    return () => {
      window.removeEventListener('pointerdown', closeOnPointerDown)
      window.removeEventListener('keydown', closeOnEscape)
    }
  }, [open])

  const label = current.label === 'System' ? `System (${resolvedTheme})` : current.label

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        aria-label={`Theme: ${label}`}
        aria-haspopup="menu"
        aria-expanded={open}
        title={`Theme: ${label}`}
        onClick={() => setOpen((value) => !value)}
        className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-card text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
      >
        <TriggerIcon size={15} strokeWidth={1.8} />
      </button>

      {open && (
        <div
          role="menu"
          aria-label="Color theme"
          className="absolute right-0 top-full z-30 mt-2 w-36 rounded-xl border border-border bg-card p-1 shadow-lg dark:shadow-none"
        >
          {options.map((option) => {
            const OptionIcon = option.icon
            const selected = option.value === theme
            return (
              <button
                key={option.value}
                type="button"
                role="menuitemradio"
                aria-checked={selected}
                onClick={() => {
                  setTheme(option.value)
                  setOpen(false)
                }}
                className={`flex h-8 w-full items-center gap-2 rounded-lg px-2 text-left text-xs transition-colors ${
                  selected ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                }`}
              >
                <OptionIcon size={14} strokeWidth={1.8} />
                <span>{option.label}</span>
                {selected && <Check size={14} className="ml-auto" />}
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
