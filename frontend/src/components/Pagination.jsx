export default function Pagination({ page, totalPages, totalElements, size, onPageChange }) {
  if (totalPages <= 1) return null

  const start = page * size + 1
  const end = Math.min((page + 1) * size, totalElements)

  const pages = []
  const maxVisible = 5
  let startPage = Math.max(0, page - Math.floor(maxVisible / 2))
  let endPage = Math.min(totalPages - 1, startPage + maxVisible - 1)
  if (endPage - startPage < maxVisible - 1) {
    startPage = Math.max(0, endPage - maxVisible + 1)
  }

  for (let i = startPage; i <= endPage; i++) {
    pages.push(i)
  }

  const btnBase = {
    background: 'transparent',
    border: '1px solid #1E422E',
    color: '#94A3B8',
    borderRadius: 6,
    padding: '6px 10px',
    fontSize: 13,
    fontWeight: 500,
    cursor: 'pointer',
    transition: 'all 200ms',
    minWidth: 32,
    textAlign: 'center',
  }

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '16px 0',
      flexWrap: 'wrap',
      gap: 12,
    }}>
      <span style={{ fontSize: 13, color: '#64748B' }}>
        Showing {start}–{end} of {totalElements} results
      </span>
      <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
        <button
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
          style={{ ...btnBase, opacity: page === 0 ? 0.3 : 1, cursor: page === 0 ? 'default' : 'pointer' }}
          aria-label="Previous page"
        >
          ‹ Prev
        </button>
        {startPage > 0 && (
          <>
            <button onClick={() => onPageChange(0)} style={btnBase}>1</button>
            {startPage > 1 && <span style={{ color: '#64748B', fontSize: 13 }}>…</span>}
          </>
        )}
        {pages.map((p) => (
          <button
            key={p}
            onClick={() => onPageChange(p)}
            style={{
              ...btnBase,
              background: p === page ? '#B8FF47' : 'transparent',
              color: p === page ? '#0A1A12' : '#94A3B8',
              fontWeight: p === page ? 700 : 500,
              borderColor: p === page ? '#B8FF47' : '#1E422E',
            }}
            aria-current={p === page ? 'page' : undefined}
          >
            {p + 1}
          </button>
        ))}
        {endPage < totalPages - 1 && (
          <>
            {endPage < totalPages - 2 && <span style={{ color: '#64748B', fontSize: 13 }}>…</span>}
            <button onClick={() => onPageChange(totalPages - 1)} style={btnBase}>{totalPages}</button>
          </>
        )}
        <button
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
          style={{ ...btnBase, opacity: page >= totalPages - 1 ? 0.3 : 1, cursor: page >= totalPages - 1 ? 'default' : 'pointer' }}
          aria-label="Next page"
        >
          Next ›
        </button>
      </div>
    </div>
  )
}
