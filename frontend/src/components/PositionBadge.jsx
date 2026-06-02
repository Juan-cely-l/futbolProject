import { positionColor } from '../utils/positionColor'

export default function PositionBadge({ position, size = 'sm' }) {
  const colors = positionColor(position)
  const fontSize = size === 'lg' ? 12 : 10
  const px = size === 'lg' ? 10 : 6
  const py = size === 'lg' ? 4 : 2

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: colors.bg,
        color: colors.text,
        fontSize,
        fontWeight: 700,
        textTransform: 'uppercase',
        letterSpacing: '0.05em',
        padding: `${py}px ${px}px`,
        borderRadius: 9999,
        lineHeight: 1,
      }}
    >
      {colors.label}
    </span>
  )
}
