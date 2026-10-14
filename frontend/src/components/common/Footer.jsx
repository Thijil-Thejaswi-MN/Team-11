import React from 'react';

export default function Footer() {
  return (
    <footer className="app-footer mt-auto" id="app-main-footer">
      <div className="container px-3">
        <div className="row align-items-center gy-2">
          <div className="col-12 col-md-6 text-center text-md-start">
            <span className="fw-semibold">Insurance Workflow Automation System</span>
            <span className="text-muted ms-2">| SE Mini-Project (Team 11)</span>
          </div>
          <div className="col-12 col-md-6 text-center text-md-end text-muted small">
            <span>SRS v1.2</span>
            <span className="mx-2">•</span>
            <span>SAD v1.1</span>
            <span className="mx-2">•</span>
            <span>STP v1.0</span>
            <span className="mx-2">•</span>
            <span className="badge bg-secondary">P4-01 Shell</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
