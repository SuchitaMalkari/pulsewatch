import type { Monitor } from './types';

export function toMillis(timestamp: string | null): number | null {
  if (timestamp === null) return null;
  const ms = Date.parse(timestamp);
  return Number.isNaN(ms) ? null : ms;
}

export function formatLatency(ms: number | null): string {
  if (ms === null) return '—';
  if (ms < 1000) return `${Math.round(ms)} ms`;
  return `${(ms / 1000).toFixed(2)} s`;
}

export function formatUptime(percent: number | null): string {
  if (percent === null) return '—';
  if (percent === 100) return '100%';
  return `${percent.toFixed(2)}%`;
}

/** 45 -> "45s", 192 -> "3m 12s", 7500 -> "2h 5m", 97200 -> "1d 3h". */
export function formatDuration(totalSeconds: number): string {
  const s = Math.max(0, Math.floor(totalSeconds));
  if (s < 60) return `${s}s`;
  const minutes = Math.floor(s / 60);
  if (minutes < 60) return s % 60 === 0 ? `${minutes}m` : `${minutes}m ${s % 60}s`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return minutes % 60 === 0 ? `${hours}h` : `${hours}h ${minutes % 60}m`;
  const days = Math.floor(hours / 24);
  return hours % 24 === 0 ? `${days}d` : `${days}d ${hours % 24}h`;
}

export function timeAgo(timestamp: string | null, now: number): string {
  const then = toMillis(timestamp);
  if (then === null) return 'never';
  const seconds = Math.round((now - then) / 1000);
  if (seconds < 5) return 'just now';
  return `${formatDuration(seconds)} ago`;
}

export function hostOf(url: string): string {
  try {
    const parsed = new URL(url);
    return parsed.port ? `${parsed.hostname}:${parsed.port}${parsed.pathname}` : `${parsed.hostname}${parsed.pathname}`;
  } catch {
    return url;
  }
}

export type OverallLevel = 'empty' | 'ok' | 'degraded' | 'down';

/** One-line summary for the status banner, worst state first. */
export function overallStatus(monitors: Monitor[]): { level: OverallLevel; text: string } {
  const active = monitors.filter((m) => !m.paused);
  if (active.length === 0) return { level: 'empty', text: 'No active monitors' };
  const down = active.filter((m) => m.status === 'DOWN').length;
  if (down > 0) return { level: 'down', text: `${down} ${down === 1 ? 'service is' : 'services are'} down` };
  const degraded = active.filter((m) => m.status === 'DEGRADED').length;
  if (degraded > 0) {
    return { level: 'degraded', text: `${degraded} ${degraded === 1 ? 'service is' : 'services are'} slow` };
  }
  return { level: 'ok', text: 'All systems operational' };
}
