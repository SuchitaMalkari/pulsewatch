interface SparklineProps {
  values: number[];
  threshold: number;
  /** Offsets the sweep so cards don't all pulse in lockstep. */
  seed?: number;
  width?: number;
  height?: number;
}

/**
 * Latency trend for a monitor card, animated like a monitor trace: a glowing head sweeps
 * along the line leaving a trail. The dashed line is the latency threshold.
 */
export function Sparkline({ values, threshold, seed = 0, width = 160, height = 36 }: SparklineProps) {
  if (values.length < 2) {
    return <div className="sparkline-empty">Waiting for data…</div>;
  }
  const max = Math.max(threshold * 1.2, ...values);
  const x = (i: number) => (i / (values.length - 1)) * width;
  const y = (v: number) => height - 2 - (v / max) * (height - 4);
  const points = values.map((v, i) => `${x(i).toFixed(1)},${y(v).toFixed(1)}`).join(' ');
  const overThreshold = values[values.length - 1] > threshold;
  const sweep = { animationDelay: `-${((seed * 1.37) % 4).toFixed(2)}s` };
  const tone = overThreshold ? ' warn' : '';

  return (
    <svg className="sparkline" viewBox={`0 0 ${width} ${height}`} width={width} height={height} role="img"
         aria-label={`Latency trend over the last ${values.length} checks`} preserveAspectRatio="none">
      <line x1={0} x2={width} y1={y(threshold)} y2={y(threshold)} className="sparkline-threshold" />
      <polyline points={points} className={`sparkline-line${tone}`} />
      <polyline points={points} pathLength={1000} style={sweep} className={`sparkline-trail${tone}`} />
      <polyline points={points} pathLength={1000} style={sweep} className={`sparkline-head${tone}`} />
    </svg>
  );
}
