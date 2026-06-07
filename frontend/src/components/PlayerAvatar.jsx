import { useState, useEffect } from 'react'

function isValidPhotoUrl(url) {
  return typeof url === 'string' && (
    url.startsWith('https://') ||
    url.startsWith('http://') ||
    url.startsWith('data:image/')
  )
}

function initials(name) {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/)
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
  return parts[0].slice(0, 2).toUpperCase()
}

export default function PlayerAvatar({ name, photo, size = 'md', loading = 'lazy', className = '' }) {
  const [status, setStatus] = useState(() => (photo ? 'loading' : 'error'))

  useEffect(() => {
    if (!photo || !isValidPhotoUrl(photo)) {
      setStatus('error')
      return
    }
    setStatus('loading')
  }, [photo])

  const px = { sm: 32, md: 48, lg: 80 }[size] || 48

  return (
    <div
      className={className}
      style={{
        width: px,
        height: px,
        borderRadius: '50%',
        background: '#163522',
        border: '1px solid #1E422E',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
        overflow: 'hidden',
      }}
    >
      {status !== 'error' ? (
        <img
          src={photo}
          alt={name}
          loading={loading}
          referrerPolicy="no-referrer"
          onLoad={() => setStatus('loaded')}
          onError={() => setStatus('error')}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
        />
      ) : (
        <span style={{
          fontFamily: "'Oswald', sans-serif",
          fontSize: px * 0.4,
          fontWeight: 700,
          color: '#fff',
          lineHeight: 1,
        }}>
          {initials(name)}
        </span>
      )}
    </div>
  )
}
