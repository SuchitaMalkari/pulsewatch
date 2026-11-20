import { useState } from 'react';
import type { FormEvent } from 'react';
import { ApiError } from '../api';
import type { Monitor, MonitorInput } from '../types';

interface MonitorFormProps {
  initial?: Monitor;
  onSubmit: (input: MonitorInput) => Promise<void>;
  onCancel: () => void;
}

const DEFAULTS: MonitorInput = {
  name: '',
  url: 'https://',
  intervalSeconds: 30,
  expectedStatus: 200,
  latencyThresholdMs: 1000,
  failureThreshold: 3,
};

type NumberField = 'intervalSeconds' | 'expectedStatus' | 'latencyThresholdMs' | 'failureThreshold';

const NUMBER_FIELDS: { key: NumberField; label: string; hint: string; min: number; max: number }[] = [
  { key: 'intervalSeconds', label: 'Check every (seconds)', hint: '10 to 3600', min: 10, max: 3600 },
  { key: 'expectedStatus', label: 'Expected HTTP status', hint: 'Usually 200', min: 100, max: 599 },
  { key: 'latencyThresholdMs', label: 'Slow above (ms)', hint: 'Slower responses count as degraded', min: 50, max: 60000 },
  { key: 'failureThreshold', label: 'Failures before alerting', hint: 'Avoids alerts on one-off blips', min: 1, max: 10 },
];

/** Add or edit a monitor. Validation messages come from the API so client and server never disagree. */
export function MonitorForm({ initial, onSubmit, onCancel }: MonitorFormProps) {
  const [values, setValues] = useState<MonitorInput>(() =>
    initial
      ? {
          name: initial.name,
          url: initial.url,
          intervalSeconds: initial.intervalSeconds,
          expectedStatus: initial.expectedStatus,
          latencyThresholdMs: initial.latencyThresholdMs,
          failureThreshold: initial.failureThreshold,
        }
      : DEFAULTS,
  );
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setFieldErrors({});
    setFormError(null);
    try {
      await onSubmit({ ...values, name: values.name.trim(), url: values.url.trim() });
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(error.fieldErrors);
        if (Object.keys(error.fieldErrors).length === 0) setFormError(error.message);
      } else {
        setFormError('Something went wrong. Please try again.');
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(e) => e.target === e.currentTarget && onCancel()}>
      <form className="modal card" onSubmit={handleSubmit} aria-labelledby="monitor-form-title" noValidate>
        <h2 id="monitor-form-title">{initial ? `Edit ${initial.name}` : 'Add a monitor'}</h2>

        <label className="field">
          <span>Name</span>
          <input
            autoFocus
            value={values.name}
            placeholder="Payments API"
            onChange={(e) => setValues({ ...values, name: e.target.value })}
            aria-invalid={Boolean(fieldErrors.name)}
          />
          {fieldErrors.name && <small className="field-error">Name {fieldErrors.name}</small>}
        </label>

        <label className="field">
          <span>URL</span>
          <input
            className="mono"
            value={values.url}
            placeholder="https://api.example.com/health"
            onChange={(e) => setValues({ ...values, url: e.target.value })}
            aria-invalid={Boolean(fieldErrors.url)}
          />
          {fieldErrors.url && <small className="field-error">URL {fieldErrors.url}</small>}
        </label>

        <div className="field-grid">
          {NUMBER_FIELDS.map((field) => (
            <label className="field" key={field.key}>
              <span>{field.label}</span>
              <input
                type="number"
                min={field.min}
                max={field.max}
                value={Number.isNaN(values[field.key]) ? '' : values[field.key]}
                onChange={(e) => setValues({ ...values, [field.key]: e.target.valueAsNumber })}
                aria-invalid={Boolean(fieldErrors[field.key])}
              />
              {fieldErrors[field.key]
                ? <small className="field-error">{fieldErrors[field.key]}</small>
                : <small className="muted">{field.hint}</small>}
            </label>
          ))}
        </div>

        {formError && <p className="form-error" role="alert">{formError}</p>}

        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onCancel}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving…' : initial ? 'Save changes' : 'Start monitoring'}
          </button>
        </div>
      </form>
    </div>
  );
}
