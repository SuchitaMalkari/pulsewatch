import type { MonitorStatus } from '../types';

const LABELS: Record<MonitorStatus, string> = {
  UP: 'Up',
  DEGRADED: 'Slow',
  DOWN: 'Down',
  PENDING: 'Pending',
  PAUSED: 'Paused',
};

export function StatusPill({ status }: { status: MonitorStatus }) {
  return (
    <span className={`pill pill-${status.toLowerCase()}`}>
      <span className="pill-dot" aria-hidden="true" />
      {LABELS[status]}
    </span>
  );
}
