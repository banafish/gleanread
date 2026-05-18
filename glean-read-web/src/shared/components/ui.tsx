import { forwardRef, type ButtonHTMLAttributes, type InputHTMLAttributes, type TextareaHTMLAttributes, type ReactNode } from "react";
import { cx } from "@/shared/utils";

type Variant = "primary" | "secondary" | "ghost" | "danger";

const variantClassName: Record<Variant, string> = {
  primary:
    "bg-app-accent text-white shadow-panel hover:opacity-90 disabled:opacity-50 disabled:hover:opacity-50",
  secondary:
    "bg-app-surface2 text-app-text border border-app-border hover:bg-app-surface disabled:opacity-50",
  ghost:
    "bg-transparent text-app-text hover:bg-white/5 border border-transparent disabled:opacity-50",
  danger:
    "bg-app-danger text-white hover:opacity-90 disabled:opacity-50",
};

export function Button({
  className,
  variant = "primary",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  return (
    <button
      className={cx(
        "inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-medium transition",
        "focus:outline-none focus:ring-2 focus:ring-app-accent/50 focus:ring-offset-2 focus:ring-offset-transparent",
        variantClassName[variant],
        className
      )}
      {...props}
    />
  );
}

export function IconButton({
  className,
  variant = "ghost",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  return (
    <button
      className={cx(
        "inline-flex h-10 w-10 items-center justify-center rounded-xl transition",
        "focus:outline-none focus:ring-2 focus:ring-app-accent/50 focus:ring-offset-2 focus:ring-offset-transparent",
        variantClassName[variant],
        className
      )}
      {...props}
    />
  );
}

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(function Input({ className, ...props }, ref) {
  return (
    <input
      ref={ref}
      className={cx(
        "w-full rounded-xl border border-app-border bg-app-surface px-3 py-2 text-sm text-app-text outline-none transition",
        "placeholder:text-app-muted focus:border-app-accent focus:ring-2 focus:ring-app-accent/20",
        className
      )}
      {...props}
    />
  );
});

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(function Textarea(
  { className, ...props },
  ref
) {
  return (
    <textarea
      ref={ref}
      className={cx(
        "w-full rounded-xl border border-app-border bg-app-surface px-3 py-2 text-sm text-app-text outline-none transition",
        "placeholder:text-app-muted focus:border-app-accent focus:ring-2 focus:ring-app-accent/20",
        className
      )}
      {...props}
    />
  );
});

export function Card({ className, children }: { className?: string; children: ReactNode }) {
  return <div className={cx("rounded-panel border border-app-border bg-app-surface shadow-panel", className)}>{children}</div>;
}

export function Badge({ className, children }: { className?: string; children: ReactNode }) {
  return (
    <span
      className={cx(
        "inline-flex items-center rounded-full border border-app-border bg-app-surface2 px-2 py-0.5 text-[11px] font-medium text-app-muted",
        className
      )}
    >
      {children}
    </span>
  );
}

export function Dialog({
  open,
  title,
  onClose,
  children,
}: {
  open: boolean;
  title?: string;
  onClose: () => void;
  children: ReactNode;
}) {
  if (!open) {
    return null;
  }
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-sm">
      <div className="w-full max-w-3xl overflow-hidden rounded-2xl border border-app-border bg-app-bg shadow-2xl">
        <div className="flex items-center justify-between border-b border-app-border px-5 py-4">
          <div className="text-base font-semibold">{title}</div>
          <IconButton onClick={onClose} aria-label="关闭">
            ×
          </IconButton>
        </div>
        <div className="max-h-[80vh] overflow-auto p-5">{children}</div>
      </div>
    </div>
  );
}

export function SectionTitle({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="space-y-1">
      <h2 className="text-sm font-semibold text-app-text">{title}</h2>
      {subtitle ? <p className="text-xs text-app-muted">{subtitle}</p> : null}
    </div>
  );
}
