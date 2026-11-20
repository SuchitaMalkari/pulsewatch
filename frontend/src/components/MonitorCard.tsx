import { formatLatency, formatUptime, hostOf, timeAgo } from '../format';
import type { Monitor } from '../types';
import { Sparkline } from './Sparkline';
import { StatusPill } from './StatusPill';

interface MonitorCardProps {
  monitor: Monitor;
  selected: boolean;
  now: number;
  onSelect: (id: number) => void;
}

export function MonitorCard({ monitor, selected, now, onSelect }: MonitorCardProps) {
  return (
    <button
      type="button"
      className={`card monitor-card status-${monitor.status.toLowerCase()}${selected ? ' selected' : ''}`}
      onClick={() => onSelect(monitor.id)}
      aria-pressed={selected}
    >
      <div className="monitor-card-head">
        <div className="monitor-card-title">
          <h3>{monitor.name}</h3>
          <span className="muted mono">{hostOf(monitor.url)}</span>
        </div>
        <StatusPill status={monitor.status} />
      </div>

      <Sparkline values={monitor.recentLatencies} threshold={monitor.latencyThresholdMs} />

      <dl className="metrics">
        <div>
          <dt>Latency</dt>
          <dd>{formatLatency(monitor.lastLatencyMs)}</dd>
        </div>
        <div>
          <dt>p95</dt>
          <dd>{formatLatency(monitor.p95LatencyMs)}</dd>
        </div>
        <div>
          <dt>Uptime</dt>
          <dd>{formatUptime(monitor.uptime24h)}</dd>
        </div>
      </dl>
      <p className="muted small">Checked {timeAgo(monitor.lastCheckedAt, now)} · every {monitor.intervalSeconds}s · p95 &amp; uptime over 24h</p>
    </button>
  );
}
