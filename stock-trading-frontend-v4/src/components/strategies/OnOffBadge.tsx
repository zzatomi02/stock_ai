export function OnOffBadge({ on }: { on: boolean }) {
  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 10px',
        borderRadius: 999,
        fontSize: 12,
        fontWeight: 600,
        background: on ? '#ECFDF3' : '#FEF3F2',
        color: on ? '#067647' : '#B42318',
        border: `1px solid ${on ? '#ABEFC6' : '#FECDCA'}`,
      }}
    >
      {on ? 'ON' : 'OFF'}
    </span>
  )
}
