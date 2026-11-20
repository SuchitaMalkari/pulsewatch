interface SparklineProps {
  values: number[];
  threshold: number;
  width?: number;
  height?: number;
}

/** Tiny latency trend for a monitor card. The dashed line is the latency threshold. */
export function Sparkline({ values, threshold, width = 160, height = 36 }: SparklineProps) {
  if (values.length < 2) {
    return <div className="sparkline-empty">Waiting for data…</div>;
  }
  const max = Math.max(threshold * 1.2, ...values);
  const x = (i: number) => (i / (values.length - 1)) * width;
  const y = (v: number) => height - 2 - (v / max) * (height - 4);
  const points = values.map((v, i) => `${x(i).toFixed(1)},${y(v).toFixed(1)}`).join(' ');
  const overThreshold = values[values.length - 1] > threshold;

  return (
    <svg className="sparkline" viewBox={`0 0 ${width} ${height}`} width={width} height={height} role="img"
         aria-label={`Latency trend over the last ${values.length} checks`}>
      <line x1={0} x2={width} y1={y(threshold)} y2={y(threshold)} className="sparkline-threshold" />
      <polyline points={points} className={overThreshold ? 'sparkline-line warn' : 'sparkline-line'} />
    </svg>
  );
}
