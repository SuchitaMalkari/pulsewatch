import { useEffect, useRef, useState } from 'react';
import type { Incident, Monitor } from './types';

interface LiveHandlers {
  onMonitor: (monitor: Monitor) => void;
  onMonitorDeleted: (id: number) => void;
  onIncident: (incident: Incident) => void;
}

/**
 * Subscribes to the server's event stream. The browser's EventSource reconnects by itself
 * after a network drop; this hook only reports whether the stream is currently connected.
 */
export function useLiveUpdates(handlers: LiveHandlers): boolean {
  const [connected, setConnected] = useState(false);
  const handlersRef = useRef(handlers);
  handlersRef.current = handlers;

  useEffect(() => {
    const source = new EventSource('/api/stream');

    const parse = <T,>(event: Event): T => JSON.parse((event as MessageEvent<string>).data) as T;

    source.onopen = () => setConnected(true);
    source.onerror = () => setConnected(false);
    source.addEventListener('monitor', (e) => handlersRef.current.onMonitor(parse<Monitor>(e)));
    source.addEventListener('monitor-deleted', (e) => handlersRef.current.onMonitorDeleted(parse<{ id: number }>(e).id));
    source.addEventListener('incident', (e) => handlersRef.current.onIncident(parse<Incident>(e)));

    return () => source.close();
  }, []);

  return connected;
}
