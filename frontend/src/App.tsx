import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, ApiError } from './api';
import { Hero } from './components/Hero';
import { IncidentList } from './components/IncidentList';
import { MonitorCard } from './components/MonitorCard';
import { MonitorDetail } from './components/MonitorDetail';
import { MonitorForm } from './components/MonitorForm';
import { PullUp } from './components/PullUp';
import { SummaryBar } from './components/SummaryBar';
import type { Incident, Monitor, MonitorInput } from './types';
import { useLiveUpdates } from './useLiveUpdates';
import { useNow } from './useNow';

type FormState = { mode: 'closed' } | { mode: 'create' } | { mode: 'edit'; monitor: Monitor };

const STATUS_RANK: Record<Monitor['status'], number> = { DOWN: 0, DEGRADED: 1, PENDING: 2, UP: 3, PAUSED: 4 };

function upsertById<T extends { id: number }>(items: T[], item: T): T[] {
  const index = items.findIndex((existing) => existing.id === item.id);
  if (index === -1) return [...items, item];
  const copy = items.slice();
  copy[index] = item;
  return copy;
}

export default function App() {
  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>({ mode: 'closed' });
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const now = useNow();

  const refresh = useCallback(async () => {
    try {
      const [monitorList, incidentList] = await Promise.all([api.listMonitors(), api.listIncidents()]);
      setMonitors(monitorList);
      setIncidents(incidentList);
      setLoadError(null);
    } catch (e) {
      setLoadError(e instanceof ApiError ? e.message : 'Could not load monitors');
    } finally {
      setLoading(false);
    }
  }, []);

  const connected = useLiveUpdates({
    onMonitor: (monitor) => setMonitors((current) => upsertById(current, monitor)),
    onMonitorDeleted: (id) => {
      setMonitors((current) => current.filter((m) => m.id !== id));
      setIncidents((current) => current.filter((i) => i.monitorId !== id));
      setSelectedId((current) => (current === id ? null : current));
    },
    onIncident: (incident) =>
      setIncidents((current) =>
        upsertById(current, incident).sort((a, b) => b.startedAt.localeCompare(a.startedAt))),
  });

  useEffect(() => {
    void refresh();
  }, [refresh]);

  // Fallback: if the live stream is down, poll so the dashboard never goes stale.
  useEffect(() => {
    if (connected) return;
    const timer = window.setInterval(() => void refresh(), 15000);
    return () => window.clearInterval(timer);
  }, [connected, refresh]);

  // Worst first, so problems are at the top of the grid.
  const sorted = useMemo(
    () => [...monitors].sort((a, b) => STATUS_RANK[a.status] - STATUS_RANK[b.status] || a.name.localeCompare(b.name)),
    [monitors],
  );
  const selected = monitors.find((m) => m.id === selectedId) ?? null;
  const openIncidents = incidents.filter((i) => i.resolvedAt === null).length;

  async function saveMonitor(input: MonitorInput) {
    if (form.mode === 'edit') {
      const updated = await api.updateMonitor(form.monitor.id, input);
      setMonitors((current) => upsertById(current, updated));
    } else {
      const created = await api.createMonitor(input);
      setMonitors((current) => upsertById(current, created));
      setSelectedId(created.id);
    }
    setForm({ mode: 'closed' });
  }

  const openForm = () => setForm({ mode: 'create' });

  return (
    <div className="app">
      <Hero monitors={monitors} incidents={incidents} now={now} onAdd={openForm} />

      <SummaryBar monitors={monitors} openIncidents={openIncidents} connected={connected} onAdd={openForm} />

      <main className="content" id="monitors">
        <header className="section-head">
          <p className="eyebrow">Live status</p>
          <h2 className="section-heading">
            <PullUp segments={[{ text: 'Every service,' }, { text: 'in real time.', className: 'serif' }]} delay={200} />
          </h2>
        </header>

        {loadError && (
          <div className="banner-error" role="alert">
            {loadError} <button type="button" className="link" onClick={() => void refresh()}>Retry</button>
          </div>
        )}

        <div className={`layout${selected ? ' with-detail' : ''}`}>
          <div className="main-column">
            {loading ? (
              <div className="grid">
                {[0, 1, 2].map((i) => <div key={i} className="card skeleton" />)}
              </div>
            ) : sorted.length === 0 ? (
              <div className="card empty-state">
                <h2>No monitors yet</h2>
                <p className="muted">Add the URL of any API or website. PulseWatch checks it on a schedule, charts its latency and opens an incident when it goes down.</p>
                <button type="button" className="btn btn-primary" onClick={openForm}>Add your first monitor</button>
              </div>
            ) : (
              <div className="grid">
                {sorted.map((monitor) => (
                  <MonitorCard
                    key={monitor.id}
                    monitor={monitor}
                    now={now}
                    selected={monitor.id === selectedId}
                    onSelect={(id) => setSelectedId((current) => (current === id ? null : id))}
                  />
                ))}
              </div>
            )}

            {selected && (
              <MonitorDetail
                key={selected.id}
                monitor={selected}
                now={now}
                onClose={() => setSelectedId(null)}
                onEdit={() => setForm({ mode: 'edit', monitor: selected })}
                onChanged={(monitor) => setMonitors((current) => upsertById(current, monitor))}
                onDeleted={(id) => {
                  setMonitors((current) => current.filter((m) => m.id !== id));
                  setSelectedId(null);
                }}
              />
            )}
          </div>

          <aside className="side-column" id="incidents">
            <IncidentList incidents={incidents} now={now} onSelectMonitor={setSelectedId} />
          </aside>
        </div>
      </main>

      {form.mode !== 'closed' && (
        <MonitorForm
          initial={form.mode === 'edit' ? form.monitor : undefined}
          onSubmit={saveMonitor}
          onCancel={() => setForm({ mode: 'closed' })}
        />
      )}

      <footer className="footer">
        <span className="footer-brand">Pulse<span className="serif">watch</span></span>
        <span className="muted small">Spring Boot · PostgreSQL · React · checks run server-side and stream here over Server-Sent Events</span>
      </footer>
    </div>
  );
}
