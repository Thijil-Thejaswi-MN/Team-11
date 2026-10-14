/**
 * Standard Error Codes from SAD Section 4.4
 * Project: Insurance Workflow Automation System
 */
export const ERROR_CODES = {
  VALIDATION_ERROR: 'VALID-422',
  AUTH_UNAUTHORIZED: 'AUTH-401',
  AUTH_LOCKED: 'AUTH-423',
  ACCESS_FORBIDDEN: 'ACCESS-403',
  NOT_FOUND: 'NOTFOUND-404',
  STATE_CONFLICT: 'STATE-409',
  PAYMENT_FAILED: 'PAY-402',
  RATE_LIMITED: 'RATE-429',
  SYSTEM_ERROR: 'SYS-500',
};

/**
 * Standard Human-Friendly Descriptions for SAD Error Codes
 */
export const ERROR_DESCRIPTIONS = {
  [ERROR_CODES.VALIDATION_ERROR]: 'Input failed validation or a business rule.',
  [ERROR_CODES.AUTH_UNAUTHORIZED]: 'Session expired, missing, or invalid credentials.',
  [ERROR_CODES.AUTH_LOCKED]: 'Account temporarily locked after 5 failed login attempts.',
  [ERROR_CODES.ACCESS_FORBIDDEN]: 'You do not have permission to perform this action.',
  [ERROR_CODES.NOT_FOUND]: 'The requested record does not exist or is not visible.',
  [ERROR_CODES.STATE_CONFLICT]: 'Operation not allowed in the current state of this record.',
  [ERROR_CODES.PAYMENT_FAILED]: 'Payment or payout failed; transaction can be retried.',
  [ERROR_CODES.RATE_LIMITED]: 'Too many requests; please slow down and try again shortly.',
  [ERROR_CODES.SYSTEM_ERROR]: 'An unexpected system error occurred. Please contact support.',
};
