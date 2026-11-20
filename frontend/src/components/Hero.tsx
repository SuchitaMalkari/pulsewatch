import { formatDuration, timeAgo, toMillis } from '../format';
import { mergeLatencies, pulsePath } from '../pulse';
import type { Incident, Monitor } from '../types';
import { PullUp } from './PullUp';

interface HeroProps {
  monitors: Monitor[];
  incidents: Incident[];
  now: number;
  onAdd: () => void;
}

const WIDTH = 1200;
const HEIGHT = 220;
const REPO_URL = 'https://github.com/SuchitaMalkari/pulsewatch';

/** Opening section. Its heartbeat line is drawn from the real latencies of every monitor. */
export function Hero({ monitors, incidents, now, onAdd }: HeroProps) {
  const active = monitors.filter((m) => !m.paused);
  const anyDown = active.some((m) => m.status === 'DOWN');

  const path = pulsePath(mergeLatencies(active.map((m) => m.recentLatencies), 24), WIDTH, HEIGHT);

  const uptimes = active.map((m) => m.uptime24h).filter((u): u is number => u !== null);
  const averageUptime = uptimes.length > 0 ? uptimes.reduce((a, b) => a + b, 0) / uptimes.length : null;
  const open = incidents.find((i) => i.resolvedAt === null);
  const lastResolved = incidents.find((i) => i.resolvedAt !== null);

  let incidentLine = 'No incidents recorded';
  if (open) {
    const started = toMillis(open.startedAt);
    const seconds = started === null ? open.durationSeconds : (now - started) / 1000;
    incidentLine = `${open.monitorName} is down · ${formatDuration(seconds)}`;
  } else if (lastResolved) {
    incidentLine = `Last incident resolved ${timeAgo(lastResolved.resolvedAt, now)}`;
  }

  return (
    <section className={`hero${anyDown ? ' hero-alert' : ''}`} aria-labelledby="hero-title">
      <div className="hero-frame">
        <div className="noise" aria-hidden="true" />

        <nav className="hero-nav" aria-label="Sections">
          <a href="#monitors">Monitors</a>
          <a href="#incidents">Incidents</a>
          <a href={`${REPO_URL}#api`} target="_blank" rel="noreferrer">API</a>
          <a href={REPO_URL} target="_blank" rel="noreferrer">Source</a>
        </nav>

        <div className="hero-pulse" aria-hidden="true">
          <svg viewBox={`0 0 ${WIDTH * 2} ${HEIGHT}`} preserveAspectRatio="none">
            <g className="hero-pulse-track">
              <path d={path} />
              <path d={path} transform={`translate(${WIDTH} 0)`} />
            </g>
          </svg>
        </div>

        <div className="hero-content">
          <h1 id="hero-title" className="hero-title">
            <PullUp segments={[{ text: 'Pulse' }, { text: 'watch', className: 'serif', attached: true }]} stagger={120} />
          </h1>

          <div className="hero-side">
            <p className="hero-copy fade-up" style={{ animationDelay: '450ms' }}>
              Watches your APIs around the clock, notices the moment one breaks, and tells the story of every
              outage as it happens.
            </p>

            <ul className="hero-stats fade-up" style={{ animationDelay: '600ms' }} aria-live="polite">
              <li><strong>{active.length}</strong> services watched</li>
              <li><strong>{averageUptime === null ? '—' : `${averageUptime.toFixed(2)}%`}</strong> uptime, 24h</li>
              <li className={open ? 'stat-alert' : undefined}>{incidentLine}</li>
            </ul>

            <div className="hero-actions fade-up" style={{ animationDelay: '750ms' }}>
              <button type="button" className="cta" onClick={onAdd}>
                Add a monitor
                <span className="cta-circle" aria-hidden="true">
                  <svg viewBox="0 0 24 24" width="16" height="16"><path d="M5 12h14M13 6l6 6-6 6" /></svg>
                </span>
              </button>
              <a className="cta-ghost" href="#monitors">See live status ↓</a>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
