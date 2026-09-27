"use client";
import { useEffect, useRef, type ReactNode } from "react";
import { X } from "lucide-react";
export function Modal({ title, subtitle, children, onClose, busy = false }: {
  title: string; subtitle?: string; children: ReactNode; onClose: () => void; busy?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current; dialog?.showModal();
    const previous = document.body.style.overflow; document.body.style.overflow = "hidden";
    return () => { dialog?.close(); document.body.style.overflow = previous; };
  }, []);
  return <dialog ref={ref} className="modal" aria-labelledby="dialog-title"
    onCancel={e => { e.preventDefault(); if (!busy) onClose(); }}
    onClick={e => { if (e.target === e.currentTarget && !busy) onClose(); }}>
    <div className="modal-inner">
      <header className="modal-heading"><div><span className="eyebrow">MAKE SPACE FOR YOUR TEAM</span><h2 id="dialog-title">{title}</h2>{subtitle && <p>{subtitle}</p>}</div>
        <button className="icon-button" aria-label="Close dialog" disabled={busy} onClick={onClose}><X size={20}/></button>
      </header>{children}
    </div>
  </dialog>;
}
