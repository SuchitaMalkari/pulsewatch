import { useCallback, useEffect, useRef, useState } from 'react';
import { api, ApiError } from '../api';
import { formatLatency, formatUptime, timeAgo } from '../format';
import type { CheckResult, Monitor } from '../types';
import { LatencyChart } from './LatencyChart';
import { StatusPill } from './StatusPill';

interface MonitorDetailProps {
  monitor: Monitor;
  now: number;
  onEdit: () => void;
  onDeleted: (id: number) => void;
  onChanged: (monitor: Monitor) => void;
  onClose: () => void;
}

export function MonitorDetail({ monitor, now, onEdit, onDeleted, onChanged, onClose }: MonitorDetailProps) {
  const [checks, setChecks] = useState<CheckResult[]>([]);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const sectionRef = useRef<HTMLElement>(null);

  // Bring the panel into view when a monitor is opened from the grid or an incident.
  useEffect(() => {
    sectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [monitor.id]);

  const loadChecks = useCallback(async () => {
    try {
      setChecks(await api.listChecks(monitor.id));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Could not load check history');
    }
  }, [monitor.id]);

  // Reload the history whenever a new check lands (lastCheckedAt changes via the live stream).
  useEffect(() => {
    void loadChecks();
  }, [loadChecks, monitor.lastCheckedAt]);

  async function run(label: string, action: () => Promise<void>) {
    setBusy(label);
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Action failed');
    } finally {
      setBusy(null);
    }
  }

  const checkNow = () => run('check', async () => {
    await api.checkNow(monitor.id);
    await loadChecks();
  });
  const togglePause = () => run('pause', async () => {
    onChanged(monitor.paused ? await api.resumeMonitor(monitor.id) : await api.pauseMonitor(monitor.id));
  });
  const remove = () => {
    if (!window.confirm(`Delete "${monitor.name}" and all of its history?`)) return;
    void run('delete', async () => {
      await api.deleteMonitor(monitor.id);
      onDeleted(monitor.id);
    });
  };

  const failures = checks.filter((c) => c.outcome === 'DOWN').length;

  return (
    <section ref={sectionRef} className="card detail" aria-labelledby="detail-title">
      <header className="detail-head">
        <div>
          <div className="detail-title-row">
            <h2 id="detail-title">{monitor.name}</h2>
            <StatusPill status={monitor.status} />
          </div>
          <a className="muted mono small" href={monitor.url} target="_blank" rel="noreferrer">{monitor.url}</a>
        </div>
        <button type="button" className="btn btn-ghost icon" onClick={onClose} aria-label="Close details">✕</button>
      </header>

      <div className="detail-actions">
        <button type="button" className="btn btn-primary" onClick={checkNow} disabled={busy !== null}>
          {busy === 'check' ? 'Checking…' : 'Check now'}
        </button>
        <button type="button" className="btn" onClick={togglePause} disabled={busy !== null}>
          {monitor.paused ? 'Resume' : 'Pause'}
        </button>
        <button type="button" className="btn" onClick={onEdit} disabled={busy !== null}>Edit</button>
        <button type="button" className="btn btn-danger" onClick={remove} disabled={busy !== null}>Delete</button>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}

      <dl className="metrics metrics-wide">
        <div><dt>Last latency</dt><dd>{formatLatency(monitor.lastLatencyMs)}</dd></div>
        <div><dt>p95 (24h)</dt><dd>{formatLatency(monitor.p95LatencyMs)}</dd></div>
        <div><dt>Uptime (24h)</dt><dd>{formatUptime(monitor.uptime24h)}</dd></div>
        <div><dt>Last check</dt><dd>{timeAgo(monitor.lastCheckedAt, now)}</dd></div>
      </dl>

      <h3 className="section-title">
        Last {checks.length} checks
        {failures > 0 && <span className="muted small"> · {failures} failed</span>}
      </h3>
      <LatencyChart checks={checks} threshold={monitor.latencyThresholdMs} />

      <p className="muted small rules">
        Alerts after {monitor.failureThreshold} failed checks in a row · expects HTTP {monitor.expectedStatus} ·
        slow above {formatLatency(monitor.latencyThresholdMs)} · checked every {monitor.intervalSeconds}s
      </p>

      <table className="checks-table">
        <thead>
          <tr><th>Time</th><th>Result</th><th>Status</th><th>Latency</th><th>Detail</th></tr>
        </thead>
        <tbody>
          {checks.slice(0, 8).map((check) => (
            <tr key={check.id}>
              <td>{timeAgo(check.checkedAt, now)}</td>
              <td><span className={`outcome outcome-${check.outcome.toLowerCase()}`}>{check.outcome}</span></td>
              <td className="mono">{check.statusCode ?? '—'}</td>
              <td className="mono">{formatLatency(check.latencyMs)}</td>
              <td className="muted truncate" title={check.error ?? ''}>{check.error ?? ''}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
