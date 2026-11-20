import { overallStatus } from '../format';
import type { Monitor, MonitorStatus } from '../types';

const ORDER: { status: MonitorStatus; label: string }[] = [
  { status: 'UP', label: 'Up' },
  { status: 'DEGRADED', label: 'Slow' },
  { status: 'DOWN', label: 'Down' },
  { status: 'PENDING', label: 'Pending' },
  { status: 'PAUSED', label: 'Paused' },
];

interface SummaryBarProps {
  monitors: Monitor[];
  openIncidents: number;
  connected: boolean;
  onAdd: () => void;
}

/** Sticky status strip: overall health, counts, live-connection state and the add button. */
export function SummaryBar({ monitors, openIncidents, connected, onAdd }: SummaryBarProps) {
  const overall = overallStatus(monitors);
  const count = (status: MonitorStatus) => monitors.filter((m) => m.status === status).length;

  return (
    <div className={`summary summary-${overall.level}`}>
      <div className="summary-headline" aria-live="polite">
        <span className="summary-dot" aria-hidden="true" />
        <strong>{overall.text}</strong>
        {openIncidents > 0 && (
          <span className="muted">· {openIncidents} open incident{openIncidents === 1 ? '' : 's'}</span>
        )}
      </div>
      <ul className="summary-counts">
        {ORDER.map(({ status, label }) => (
          <li key={status} className={`count count-${status.toLowerCase()}`}>
            <span className="count-value">{count(status)}</span> {label}
          </li>
        ))}
      </ul>
      <div className="summary-actions">
        <span className={`live ${connected ? 'live-on' : 'live-off'}`}
              title={connected ? 'Receiving live updates' : 'Reconnecting; polling every 15s'}>
          <span className="live-dot" aria-hidden="true" />
          {connected ? 'Live' : 'Reconnecting'}
        </span>
        <button type="button" className="btn btn-primary btn-sm" onClick={onAdd}>+ Add</button>
      </div>
    </div>
  );
}
