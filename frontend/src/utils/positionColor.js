const COLORS = {
  GOALKEEPER: { bg: 'rgba(245,158,11,0.15)', text: '#F59E0B', label: 'GK' },
  DEFENDER: { bg: 'rgba(59,130,246,0.15)', text: '#3B82F6', label: 'DEF' },
  MIDFIELDER: { bg: 'rgba(139,92,246,0.15)', text: '#8B5CF6', label: 'MID' },
  FORWARD: { bg: 'rgba(239,68,68,0.15)', text: '#EF4444', label: 'FWD' },
}

export function positionColor(position) {
  return COLORS[position] || { bg: '#333', text: '#fff', label: position?.slice(0, 3) }
}

export const POSITIONS = ['GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD']
