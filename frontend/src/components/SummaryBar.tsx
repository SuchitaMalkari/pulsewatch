import { overallStatus } from '../format';
import type { Monitor, MonitorStatus } from '../types';

const ORDER: { status: MonitorStatus; label: string }[] = [
  { status: 'UP', label: 'Up' },
  { status: 'DEGRADED', label: 'Slow' },
  { status: 'DOWN', label: 'Down' },
  { status: 'PENDING', label: 'Pending' },
  { status: 'PAUSED', label: 'Paused' },
];

export function SummaryBar({ monitors, openIncidents }: { monitors: Monitor[]; openIncidents: number }) {
  const overall = overallStatus(monitors);
  const count = (status: MonitorStatus) => monitors.filter((m) => m.status === status).length;

  return (
    <section className={`summary summary-${overall.level}`} aria-live="polite">
      <div className="summary-headline">
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
    </section>
  );
}
