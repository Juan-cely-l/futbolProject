import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import PlayerAvatar from './PlayerAvatar'

describe('PlayerAvatar', () => {
  it('renders img element for valid https photo URL', () => {
    render(<PlayerAvatar name="Lionel Messi" photo="https://example.com/photo.jpg" />)
    const img = screen.getByRole('img')
    expect(img).toHaveAttribute('src', 'https://example.com/photo.jpg')
    expect(img).toHaveAttribute('alt', 'Lionel Messi')
  })

  it('renders img element for valid http photo URL', () => {
    render(<PlayerAvatar name="Player" photo="http://example.com/pic.png" />)
    expect(screen.getByRole('img')).toBeInTheDocument()
  })

  it('renders initials when photo is null', () => {
    render(<PlayerAvatar name="Lionel Messi" photo={null} />)
    expect(screen.getByText('LM')).toBeInTheDocument()
  })

  it('renders initials when photo is empty string', () => {
    render(<PlayerAvatar name="Cristiano Ronaldo" photo="" />)
    expect(screen.getByText('CR')).toBeInTheDocument()
  })

  it('renders initials when photo is undefined', () => {
    render(<PlayerAvatar name="Neymar Jr" />)
    expect(screen.getByText('NJ')).toBeInTheDocument()
  })

  it('handles image onError by switching to initials', async () => {
    render(<PlayerAvatar name="Test Player" photo="https://example.com/broken.jpg" />)
    const img = screen.getByRole('img')
    img.dispatchEvent(new Event('error'))
    await waitFor(() => {
      expect(screen.getByText('TP')).toBeInTheDocument()
    })
  })
})

describe('initials extraction', () => {
  it('shows ? for null name', () => {
    render(<PlayerAvatar name={null} photo={null} />)
    expect(screen.getByText('?')).toBeInTheDocument()
  })

  it('shows ? for empty name', () => {
    render(<PlayerAvatar name="" photo={null} />)
    expect(screen.getByText('?')).toBeInTheDocument()
  })

  it('shows first two chars for single-word name', () => {
    render(<PlayerAvatar name="Pelé" photo={null} />)
    expect(screen.getByText('PE')).toBeInTheDocument()
  })
})

describe('size prop', () => {
  it('renders sm size (32px)', () => {
    const { container } = render(<PlayerAvatar name="Test" photo={null} size="sm" />)
    const div = container.firstChild
    expect(div).toHaveStyle({ width: '32px', height: '32px' })
  })

  it('renders md size (48px) as default', () => {
    const { container } = render(<PlayerAvatar name="Test" photo={null} />)
    const div = container.firstChild
    expect(div).toHaveStyle({ width: '48px', height: '48px' })
  })

  it('renders lg size (80px)', () => {
    const { container } = render(<PlayerAvatar name="Test" photo={null} size="lg" />)
    const div = container.firstChild
    expect(div).toHaveStyle({ width: '80px', height: '80px' })
  })
})
