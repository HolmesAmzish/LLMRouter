import type { ReactNode } from 'react'

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`rounded-lg border border-border bg-card text-card-foreground shadow-sm ${className}`}>{children}</section>
}

export function CardHeader({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <header className="flex flex-wrap items-start justify-between gap-3 border-b border-border px-4 py-3">
      <div className="min-w-0">
        <h2 className="text-sm font-medium">{title}</h2>
        {description && <p className="mt-0.5 text-xs text-muted-foreground">{description}</p>}
      </div>
      {action}
    </header>
  )
}

export function Button({
  children, onClick, type = 'button', variant = 'secondary', disabled = false, className = '',
}: {
  children: ReactNode
  onClick?: () => void
  type?: 'button' | 'submit'
  variant?: 'primary' | 'secondary' | 'danger'
  disabled?: boolean
  className?: string
}) {
  const styles = variant === 'primary'
    ? 'bg-primary text-primary-foreground hover:opacity-90'
    : variant === 'danger'
      ? 'border border-danger/40 text-danger hover:bg-danger/8'
      : 'border border-border bg-card hover:bg-muted'
  return (
    <button type={type} onClick={onClick} disabled={disabled}
      className={`inline-flex h-8 items-center gap-1.5 rounded-md px-3 text-xs font-medium transition disabled:cursor-not-allowed disabled:opacity-55 ${styles} ${className}`}>
      {children}
    </button>
  )
}

export function Field({ label, children, className = '' }: { label: string; children: ReactNode; className?: string }) {
  return (
    <label className={`grid gap-1.5 ${className}`}>
      <span className="text-xs font-medium text-muted-foreground">{label}</span>
      {children}
    </label>
  )
}

export const inputClass = 'h-8 w-full rounded-md border border-input bg-card px-2.5 text-xs outline-none focus:border-primary'

export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'success' | 'warning' | 'danger' | 'primary' }) {
  const tones = {
    neutral: 'border-border text-muted-foreground',
    success: 'border-success/30 text-success',
    warning: 'border-warning/30 text-warning',
    danger: 'border-danger/30 text-danger',
    primary: 'border-primary/20 text-primary',
  } as const
  return <span className={`inline-flex h-5 items-center rounded-full border px-2 text-[11px] ${tones[tone]}`}>{children}</span>
}

export function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="grid place-items-center px-6 py-12 text-center">
      <p className="text-sm font-medium">{title}</p>
      <p className="mt-1 max-w-md text-xs text-muted-foreground">{description}</p>
    </div>
  )
}

export function Alert({ children }: { children: ReactNode }) {
  return <div className="rounded-md border border-danger/30 bg-danger/8 px-3 py-2 text-xs text-danger">{children}</div>
}
