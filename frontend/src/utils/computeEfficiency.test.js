import { describe, it, expect } from 'vitest'
import { computeEfficiency, efficiencyColor } from './computeEfficiency'

describe('computeEfficiency', () => {
  it('calculates (goals + assists) / matches', () => {
    expect(computeEfficiency(30, 10, 38)).toBe(1.05)
  })

  it('returns 0 when matches is 0', () => {
    expect(computeEfficiency(10, 5, 0)).toBe(0)
  })

  it('returns 0 when matches is null', () => {
    expect(computeEfficiency(10, 5, null)).toBe(0)
  })

  it('returns 0 when matches is undefined', () => {
    expect(computeEfficiency(10, 5, undefined)).toBe(0)
  })

  it('defaults goals to 0 when undefined', () => {
    expect(computeEfficiency(undefined, 5, 10)).toBe(0.5)
  })

  it('defaults assists to 0 when undefined', () => {
    expect(computeEfficiency(10, undefined, 10)).toBe(1)
  })

  it('defaults all params to 0 when empty', () => {
    expect(computeEfficiency()).toBe(0)
  })

  it('rounds to 2 decimal places', () => {
    expect(computeEfficiency(1, 2, 3)).toBe(1)
  })
})

describe('efficiencyColor', () => {
  it('returns green for value >= 1.0', () => {
    expect(efficiencyColor(1.0)).toBe('#22C55E')
    expect(efficiencyColor(2.5)).toBe('#22C55E')
  })

  it('returns amber for value between 0.5 and 0.99', () => {
    expect(efficiencyColor(0.5)).toBe('#F59E0B')
    expect(efficiencyColor(0.75)).toBe('#F59E0B')
  })

  it('returns red for value < 0.5', () => {
    expect(efficiencyColor(0.49)).toBe('#EF4444')
    expect(efficiencyColor(0)).toBe('#EF4444')
  })
})
