import { Fragment } from 'react';

interface Segment {
  text: string;
  className?: string;
  /** Join to the previous segment without a space, e.g. "Pulse" + "watch". */
  attached?: boolean;
}

interface PullUpProps {
  segments: Segment[];
  /** Delay before the first word, in ms. */
  delay?: number;
  /** Delay between words, in ms. */
  stagger?: number;
}

/** Text whose words slide up into place one after another (pure CSS, no animation library). */
export function PullUp({ segments, delay = 0, stagger = 80 }: PullUpProps) {
  const words = segments.flatMap((segment) =>
    segment.text.split(' ').filter(Boolean).map((word, i) => ({
      word,
      className: segment.className,
      spaceBefore: !(i === 0 && segment.attached),
    })),
  );

  return (
    <>
      {words.map(({ word, className, spaceBefore }, order) => (
        // The space sits between the clipping wrappers; inside an inline-block it would collapse.
        <Fragment key={order}>
          {order > 0 && spaceBefore ? ' ' : null}
          <span className="pullup">
            <span
              className={`pullup-word${className ? ` ${className}` : ''}`}
              style={{ animationDelay: `${delay + order * stagger}ms` }}
            >
              {word}
            </span>
          </span>
        </Fragment>
      ))}
    </>
  );
}
