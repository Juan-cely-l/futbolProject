import { describe, it, expect } from 'vitest'
import { formatCurrency } from './formatCurrency'

describe('formatCurrency', () => {
  it('formats millions', () => {
    expect(formatCurrency(50_000_000)).toBe('€50.0M')
  })

  it('formats thousands', () => {
    expect(formatCurrency(800_000)).toBe('€800K')
  })

  it('formats values under 1000', () => {
    expect(formatCurrency(999)).toBe('€999')
  })

  it('formats zero', () => {
    expect(formatCurrency(0)).toBe('€0')
  })

  it('rounds millions to 1 decimal', () => {
    expect(formatCurrency(1_234_567)).toBe('€1.2M')
  })

  it('rounds thousands up', () => {
    expect(formatCurrency(1_500)).toBe('€2K')
  })

  it('returns em dash for null', () => {
    expect(formatCurrency(null)).toBe('—')
  })

  it('returns em dash for undefined', () => {
    expect(formatCurrency(undefined)).toBe('—')
  })

  it('returns em dash for Infinity', () => {
    expect(formatCurrency(Infinity)).toBe('—')
  })

  it('returns em dash for NaN', () => {
    expect(formatCurrency(NaN)).toBe('—')
  })

  it('returns em dash for string input', () => {
    expect(formatCurrency('not a number')).toBe('—')
  })

  it('formats values at the million boundary', () => {
    expect(formatCurrency(1_000_000)).toBe('€1.0M')
    expect(formatCurrency(999_999)).toBe('€1000K')
  })
})
