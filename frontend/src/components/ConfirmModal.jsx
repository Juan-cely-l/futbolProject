import { useEffect, useRef, useCallback } from 'react'

export default function ConfirmModal({ open, title, message, confirmLabel = 'Delete', onConfirm, onCancel, isLoading }) {
  const overlayRef = useRef(null)
  const cancelRef = useRef(null)
  const confirmRef = useRef(null)

  const handleKeyDown = useCallback((e) => {
    if (e.key === 'Escape') {
      onCancel()
    }
    if (e.key === 'Tab') {
      if (!overlayRef.current) return
      const focusable = overlayRef.current.querySelectorAll('button:not([disabled])')
      if (focusable.length === 0) return
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (e.shiftKey) {
        if (document.activeElement === first) {
          e.preventDefault()
          last.focus()
        }
      } else {
        if (document.activeElement === last) {
          e.preventDefault()
          first.focus()
        }
      }
    }
  }, [onCancel])

  useEffect(() => {
    if (!open) return
    cancelRef.current?.focus()
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [open, handleKeyDown])

  if (!open) return null

  return (
    <div
      ref={overlayRef}
      style={{
        position: 'fixed', inset: 0, zIndex: 1000,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}
    >
      <div
        style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.6)' }}
        onClick={onCancel}
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        style={{
          position: 'relative', background: '#11291B', border: '1px solid #1E422E',
          borderRadius: 16, padding: 28, width: 400, maxWidth: '90vw',
          animation: 'fadeSlideUp 200ms ease-out both',
        }}
      >
        <h2 id="modal-title" style={{ fontSize: 18, fontWeight: 600, margin: '0 0 8px', color: '#fff' }}>
          {title}
        </h2>
        <p style={{ fontSize: 14, color: '#94A3B8', margin: '0 0 24px', lineHeight: 1.5 }}>
          {message}
        </p>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button
            ref={cancelRef}
            onClick={onCancel}
            disabled={isLoading}
            style={{
              padding: '8px 20px', borderRadius: 8, border: '1px solid #1E422E',
              background: 'transparent', color: '#fff', fontSize: 14, fontWeight: 500,
              cursor: isLoading ? 'not-allowed' : 'pointer',
              transition: 'background 200ms',
            }}
          >
            Cancel
          </button>
          <button
            ref={confirmRef}
            onClick={onConfirm}
            disabled={isLoading}
            style={{
              padding: '8px 20px', borderRadius: 8, border: 'none',
              background: '#EF4444', color: '#fff', fontSize: 14, fontWeight: 600,
              cursor: isLoading ? 'not-allowed' : 'pointer',
              opacity: isLoading ? 0.6 : 1,
              transition: 'opacity 200ms',
            }}
          >
            {isLoading ? 'Deleting…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
