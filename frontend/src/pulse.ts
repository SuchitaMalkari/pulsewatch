/**
 * Builds an ECG-style SVG path from latency samples: a flat baseline with one heartbeat
 * per sample, where taller spikes mean slower responses. Used by the hero animation.
 */
export function pulsePath(values: number[], width: number, height: number): string {
  const samples = values.length > 0 ? values : [0];
  const max = Math.max(...samples, 1);
  const baseline = height * 0.62;
  const step = width / samples.length;
  let d = `M0 ${baseline.toFixed(1)}`;

  samples.forEach((value, i) => {
    const x = i * step;
    const amplitude = 0.25 + 0.75 * (value / max); // every beat is visible; slow ones are taller
    const peak = baseline - amplitude * height * 0.55;
    const dip = baseline + amplitude * height * 0.18;
    d +=
      ` L${(x + step * 0.30).toFixed(1)} ${baseline.toFixed(1)}` +
      ` L${(x + step * 0.38).toFixed(1)} ${(baseline - height * 0.05).toFixed(1)}` +
      ` L${(x + step * 0.44).toFixed(1)} ${baseline.toFixed(1)}` +
      ` L${(x + step * 0.50).toFixed(1)} ${peak.toFixed(1)}` +
      ` L${(x + step * 0.56).toFixed(1)} ${dip.toFixed(1)}` +
      ` L${(x + step * 0.62).toFixed(1)} ${baseline.toFixed(1)}` +
      ` L${(x + step).toFixed(1)} ${baseline.toFixed(1)}`;
  });
  return d;
}

/** Interleaves the recent latencies of several monitors into one stream of beats. */
export function mergeLatencies(series: number[][], limit = 24): number[] {
  const merged: number[] = [];
  const longest = Math.max(0, ...series.map((s) => s.length));
  for (let i = 0; i < longest && merged.length < limit; i++) {
    for (const s of series) {
      const value = s[s.length - 1 - i];
      if (value !== undefined && merged.length < limit) merged.push(value);
    }
  }
  return merged.reverse();
}
