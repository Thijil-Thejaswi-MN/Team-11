import { describe, it, expect } from 'vitest';

describe('CI Failure Demonstration (Deliberate)', () => {
  it('deliberately fails to demonstrate that CI check reports failure on broken changes', () => {
    // Deliberate failure assertion for Phase 3 Step D demonstration
    expect('ci-check-failing-demo').toBe('ci-check-passed');
  });
});
