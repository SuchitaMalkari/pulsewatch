import { describe, expect, it } from 'vitest';
import { formatDuration, formatLatency, formatUptime, hostOf, overallStatus, timeAgo } from './format';
import type { Monitor } from './types';

function monitor(overrides: Partial<Monitor>): Monitor {
  return {
    id: 1, name: 'API', url: 'https://api.example.com', intervalSeconds: 30, expectedStatus: 200,
    latencyThresholdMs: 1000, failureThreshold: 3, paused: false, status: 'UP', lastCheckedAt: null,
    lastLatencyMs: null, uptime24h: null, p95LatencyMs: null, recentLatencies: [], openIncidentId: null,
    createdAt: '2026-11-19T12:00:00Z', ...overrides,
  };
}

describe('formatDuration', () => {
  it.each([
    [0, '0s'], [45, '45s'], [60, '1m'], [192, '3m 12s'], [3600, '1h'], [7500, '2h 5m'], [86400, '1d'], [97200, '1d 3h'],
  ])('formats %i seconds as %s', (seconds, expected) => {
    expect(formatDuration(seconds)).toBe(expected);
  });

  it('treats negative values as zero', () => {
    expect(formatDuration(-5)).toBe('0s');
  });
});

describe('formatLatency and formatUptime', () => {
  it('shows a dash when there is no data', () => {
    expect(formatLatency(null)).toBe('—');
    expect(formatUptime(null)).toBe('—');
  });

  it('switches to seconds above one second', () => {
    expect(formatLatency(245)).toBe('245 ms');
    expect(formatLatency(1530)).toBe('1.53 s');
  });

  it('keeps two decimals except for a perfect score', () => {
    expect(formatUptime(99.5)).toBe('99.50%');
    expect(formatUptime(100)).toBe('100%');
  });
});

describe('timeAgo', () => {
  const now = Date.parse('2026-11-19T12:10:00Z');

  it('handles missing and recent timestamps', () => {
    expect(timeAgo(null, now)).toBe('never');
    expect(timeAgo('2026-11-19T12:09:58Z', now)).toBe('just now');
  });

  it('describes older timestamps', () => {
    expect(timeAgo('2026-11-19T12:07:00Z', now)).toBe('3m ago');
  });
});

describe('hostOf', () => {
  it('shows host and path without the scheme', () => {
    expect(hostOf('https://api.example.com/health')).toBe('api.example.com/health');
    expect(hostOf('http://localhost:8080/demo/slow')).toBe('localhost:8080/demo/slow');
  });

  it('returns the input when it is not a URL', () => {
    expect(hostOf('not a url')).toBe('not a url');
  });
});

describe('overallStatus', () => {
  it('reports the worst state', () => {
    expect(overallStatus([monitor({ status: 'UP' }), monitor({ status: 'DEGRADED' })]).level).toBe('degraded');
    expect(overallStatus([monitor({ status: 'DEGRADED' }), monitor({ status: 'DOWN' })]).text).toBe('1 service is down');
  });

  it('ignores paused monitors', () => {
    expect(overallStatus([monitor({ status: 'DOWN', paused: true }), monitor({ status: 'UP' })]).level).toBe('ok');
    expect(overallStatus([monitor({ paused: true })]).level).toBe('empty');
  });
});
