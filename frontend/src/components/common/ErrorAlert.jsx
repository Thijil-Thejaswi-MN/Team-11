import React, { useState, useEffect } from 'react';
import { errorBus } from '../../api/errorBus';

export default function ErrorAlert({ error: propError, onDismiss }) {
  const [activeError, setActiveError] = useState(propError || null);

  useEffect(() => {
    if (propError) {
      setActiveError(propError);
    }
  }, [propError]);

  useEffect(() => {
    // Subscribe to global error bus when no direct prop is given
    const unsubscribe = errorBus.subscribe((err) => {
      setActiveError(err);
    });
    return () => unsubscribe();
  }, []);

  const handleDismiss = () => {
    setActiveError(null);
    if (onDismiss) onDismiss();
  };

  if (!activeError) return null;

  return (
    <div
      className="alert alert-danger alert-dismissible fade show error-banner shadow-sm my-3"
      role="alert"
      id="global-error-banner"
    >
      <div className="d-flex align-items-start justify-content-between">
        <div className="d-flex align-items-start gap-2">
          <i className="bi bi-exclamation-triangle-fill text-danger fs-5 mt-1"></i>
          <div>
            <div className="d-flex align-items-center gap-2 flex-wrap mb-1">
              <span className="badge bg-danger">{activeError.errorCode || 'SYS-500'}</span>
              <span className="fw-semibold">{activeError.message || 'An error occurred'}</span>
            </div>

            {/* Field-level error details list */}
            {activeError.details && activeError.details.length > 0 && (
              <ul className="mb-2 ps-3 small">
                {activeError.details.map((detail, index) => (
                  <li key={index}>{typeof detail === 'string' ? detail : JSON.stringify(detail)}</li>
                ))}
              </ul>
            )}

            {/* Correlation ID display per SAD Section 4.4 */}
            <div className="small text-muted mt-1">
              <span>Correlation ID: </span>
              <code className="error-correlation-id">{activeError.correlationId}</code>
            </div>
          </div>
        </div>

        <button
          type="button"
          className="btn-close"
          aria-label="Close"
          onClick={handleDismiss}
          id="dismiss-error-btn"
        ></button>
      </div>
    </div>
  );
}
