import { describe, expect, it } from 'vitest';
import { mergeLatencies, pulsePath } from './pulse';

describe('pulsePath', () => {
  it('starts at the left edge and draws one beat per sample', () => {
    const d = pulsePath([100, 200, 300], 300, 100);
    expect(d.startsWith('M0 ')).toBe(true);
    expect((d.match(/L/g) ?? []).length).toBe(3 * 7);
  });

  it('still draws a line with no data', () => {
    expect(pulsePath([], 100, 50).startsWith('M0 ')).toBe(true);
  });
});

describe('mergeLatencies', () => {
  it('interleaves samples by recency across monitors, oldest first', () => {
    expect(mergeLatencies([[1, 2, 3], [10, 20]]).join(',')).toBe('1,10,2,20,3');
  });

  it('respects the limit', () => {
    expect(mergeLatencies([[1, 2, 3, 4, 5]], 2).join(',')).toBe('4,5');
  });
});
