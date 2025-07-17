export const FullSpinner = ({ ariaLabel }: { ariaLabel?: string }) => (
  <div
    aria-label={ariaLabel}
    style={{
      position: 'relative',
      left: '0',
      top: '0',
      minHeight: '150px',
      maxHeight: '80vh',
      width: '100%',
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
    }}
  >
    Ladataan...
  </div>
);
