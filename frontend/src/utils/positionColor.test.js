import { describe, it, expect } from 'vitest'
import { positionColor, POSITIONS } from './positionColor'

describe('positionColor', () => {
  it('returns GK colors for GOALKEEPER', () => {
    const result = positionColor('GOALKEEPER')
    expect(result.bg).toContain('245')
    expect(result.text).toBe('#F59E0B')
    expect(result.label).toBe('GK')
  })

  it('returns DEF colors for DEFENDER', () => {
    const result = positionColor('DEFENDER')
    expect(result.bg).toContain('59')
    expect(result.text).toBe('#3B82F6')
    expect(result.label).toBe('DEF')
  })

  it('returns MID colors for MIDFIELDER', () => {
    const result = positionColor('MIDFIELDER')
    expect(result.bg).toContain('139')
    expect(result.text).toBe('#8B5CF6')
    expect(result.label).toBe('MID')
  })

  it('returns FWD colors for FORWARD', () => {
    const result = positionColor('FORWARD')
    expect(result.bg).toContain('239')
    expect(result.text).toBe('#EF4444')
    expect(result.label).toBe('FWD')
  })

  it('returns fallback for unknown position', () => {
    const result = positionColor('COACH')
    expect(result).toEqual({ bg: '#333', text: '#fff', label: 'COA' })
  })

  it('returns fallback for null position (optional chaining yields undefined)', () => {
    const result = positionColor(null)
    expect(result.label).toBeUndefined()
  })

  it('returns fallback for undefined position (optional chaining yields undefined)', () => {
    const result = positionColor(undefined)
    expect(result.label).toBeUndefined()
  })
})

describe('POSITIONS', () => {
  it('includes all 4 positions', () => {
    expect(POSITIONS).toEqual(['GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD'])
  })
})
