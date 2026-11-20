import { formatDuration, timeAgo, toMillis } from '../format';
import type { Incident } from '../types';

interface IncidentListProps {
  incidents: Incident[];
  now: number;
  onSelectMonitor: (id: number) => void;
}

export function IncidentList({ incidents, now, onSelectMonitor }: IncidentListProps) {
  const open = incidents.filter((i) => i.resolvedAt === null);
  const resolved = incidents.filter((i) => i.resolvedAt !== null).slice(0, 8);

  // Open incidents keep counting up locally between server updates.
  const liveDuration = (incident: Incident) => {
    const started = toMillis(incident.startedAt);
    return started === null ? incident.durationSeconds : (now - started) / 1000;
  };

  return (
    <section className="card incidents" aria-labelledby="incidents-title">
      <h2 id="incidents-title">Incidents</h2>

      {open.length === 0 && resolved.length === 0 && (
        <p className="muted">No incidents yet. They open automatically when a service fails repeatedly.</p>
      )}

      <ul className="incident-list">
        {open.map((incident) => (
          <li key={incident.id} className="incident open">
            <button type="button" className="link" onClick={() => onSelectMonitor(incident.monitorId)}>
              {incident.monitorName}
            </button>
            <span className="badge badge-down">Ongoing · {formatDuration(liveDuration(incident))}</span>
            <p className="incident-cause">{incident.cause}</p>
            <p className="muted small">Started {timeAgo(incident.startedAt, now)}</p>
          </li>
        ))}
        {resolved.map((incident) => (
          <li key={incident.id} className="incident">
            <button type="button" className="link" onClick={() => onSelectMonitor(incident.monitorId)}>
              {incident.monitorName}
            </button>
            <span className="badge badge-resolved">Resolved after {formatDuration(incident.durationSeconds)}</span>
            <p className="incident-cause">{incident.cause}</p>
            <p className="muted small">Ended {timeAgo(incident.resolvedAt, now)}</p>
          </li>
        ))}
      </ul>
    </section>
  );
}
