import React from 'react';

export default function LoadingSpinner({ message = 'Loading...' }) {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center p-5 text-muted" role="status">
      <div className="spinner-border text-primary mb-3" role="status">
        <span className="visually-hidden">{message}</span>
      </div>
      <span className="small fw-medium">{message}</span>
    </div>
  );
}
