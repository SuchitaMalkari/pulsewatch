import type { CheckResult, Incident, Monitor, MonitorInput } from './types';

/** An error response from the API, with per-field validation messages when there are any. */
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(message: string, status: number, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

interface ProblemBody {
  title?: string;
  detail?: string;
  errors?: Record<string, string>;
  properties?: { errors?: Record<string, string> };
}

async function toApiError(response: Response): Promise<ApiError> {
  let problem: ProblemBody = {};
  try {
    problem = (await response.json()) as ProblemBody;
  } catch {
    // body was empty or not JSON; fall back to the status code
  }
  const fieldErrors = problem.errors ?? problem.properties?.errors ?? {};
  const message = problem.detail ?? problem.title ?? `Request failed with status ${response.status}`;
  return new ApiError(message, response.status, fieldErrors);
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      method,
      headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError('Cannot reach the PulseWatch API. Is the backend running?', 0);
  }
  if (!response.ok) {
    throw await toApiError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  listMonitors: () => request<Monitor[]>('GET', '/api/monitors'),
  createMonitor: (input: MonitorInput) => request<Monitor>('POST', '/api/monitors', input),
  updateMonitor: (id: number, input: MonitorInput) => request<Monitor>('PUT', `/api/monitors/${id}`, input),
  deleteMonitor: (id: number) => request<void>('DELETE', `/api/monitors/${id}`),
  pauseMonitor: (id: number) => request<Monitor>('POST', `/api/monitors/${id}/pause`),
  resumeMonitor: (id: number) => request<Monitor>('POST', `/api/monitors/${id}/resume`),
  checkNow: (id: number) => request<CheckResult>('POST', `/api/monitors/${id}/check`),
  listChecks: (id: number, limit = 60) => request<CheckResult[]>('GET', `/api/monitors/${id}/checks?limit=${limit}`),
  listIncidents: () => request<Incident[]>('GET', '/api/incidents'),
};
