export type MonitorStatus = 'PENDING' | 'UP' | 'DEGRADED' | 'DOWN' | 'PAUSED';
export type CheckOutcome = 'UP' | 'DEGRADED' | 'DOWN';

/** Timestamps arrive as ISO-8601 strings from the API. */
export type Timestamp = string;

export interface Monitor {
  id: number;
  name: string;
  url: string;
  intervalSeconds: number;
  expectedStatus: number;
  latencyThresholdMs: number;
  failureThreshold: number;
  paused: boolean;
  status: MonitorStatus;
  lastCheckedAt: Timestamp | null;
  lastLatencyMs: number | null;
  uptime24h: number | null;
  p95LatencyMs: number | null;
  recentLatencies: number[];
  openIncidentId: number | null;
  createdAt: Timestamp;
}

export interface CheckResult {
  id: number;
  checkedAt: Timestamp;
  outcome: CheckOutcome;
  statusCode: number | null;
  latencyMs: number;
  error: string | null;
}

export interface Incident {
  id: number;
  monitorId: number;
  monitorName: string;
  startedAt: Timestamp;
  resolvedAt: Timestamp | null;
  cause: string;
  durationSeconds: number;
}

export interface MonitorInput {
  name: string;
  url: string;
  intervalSeconds: number;
  expectedStatus: number;
  latencyThresholdMs: number;
  failureThreshold: number;
}
