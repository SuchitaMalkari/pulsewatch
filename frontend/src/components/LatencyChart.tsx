import { formatLatency, toMillis } from '../format';
import type { CheckResult } from '../types';

interface LatencyChartProps {
  checks: CheckResult[]; // newest first, as returned by the API
  threshold: number;
}

const WIDTH = 640;
const HEIGHT = 180;
const PAD = { top: 12, right: 12, bottom: 22, left: 52 };

/** Bar chart of recent checks: height is latency, colour is the outcome. */
export function LatencyChart({ checks, threshold }: LatencyChartProps) {
  if (checks.length === 0) {
    return <div className="chart-empty">No checks yet. Use “Check now” or wait for the next scheduled check.</div>;
  }

  const ordered = [...checks].reverse();
  const max = Math.max(threshold * 1.3, ...ordered.map((c) => c.latencyMs));
  const plotW = WIDTH - PAD.left - PAD.right;
  const plotH = HEIGHT - PAD.top - PAD.bottom;
  const slot = plotW / ordered.length;
  const barW = Math.max(2, slot * 0.7);
  const y = (v: number) => PAD.top + plotH - (v / max) * plotH;
  const ticks = [0, max / 2, max];

  const first = toMillis(ordered[0].checkedAt);
  const last = toMillis(ordered[ordered.length - 1].checkedAt);
  const timeLabel = (ms: number | null) =>
    ms === null ? '' : new Date(ms).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  return (
    <svg className="latency-chart" viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img"
         aria-label={`Latency of the last ${ordered.length} checks`}>
      {ticks.map((t) => (
        <g key={t}>
          <line x1={PAD.left} x2={WIDTH - PAD.right} y1={y(t)} y2={y(t)} className="grid-line" />
          <text x={PAD.left - 8} y={y(t) + 4} className="axis-label" textAnchor="end">{formatLatency(t)}</text>
        </g>
      ))}

      {ordered.map((check, i) => {
        // A failed check is often fast (an instant 503), so its bar would be tiny. Shade the
        // whole column red so outages stand out at a glance.
        const failed = check.outcome === 'DOWN';
        const top = failed ? PAD.top : y(Math.max(check.latencyMs, max * 0.01));
        return (
          <rect
            key={check.id}
            x={PAD.left + i * slot + (slot - barW) / 2}
            y={top}
            width={barW}
            height={PAD.top + plotH - top}
            rx={1.5}
            className={`bar bar-${check.outcome.toLowerCase()}`}
          >
            <title>
              {`${new Date(toMillis(check.checkedAt) ?? 0).toLocaleTimeString()} · ${check.outcome} · ` +
                `${check.statusCode ?? 'no response'} · ${formatLatency(check.latencyMs)}` +
                (check.error ? ` · ${check.error}` : '')}
            </title>
          </rect>
        );
      })}

      <line x1={PAD.left} x2={WIDTH - PAD.right} y1={y(threshold)} y2={y(threshold)} className="threshold-line" />
      <text x={WIDTH - PAD.right} y={y(threshold) - 5} className="axis-label threshold-label" textAnchor="end">
        threshold {formatLatency(threshold)}
      </text>

      <text x={PAD.left} y={HEIGHT - 4} className="axis-label">{timeLabel(first)}</text>
      <text x={WIDTH - PAD.right} y={HEIGHT - 4} className="axis-label" textAnchor="end">{timeLabel(last)}</text>
    </svg>
  );
}
