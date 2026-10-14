import React from 'react';
import AppLayout from './AppLayout';
import Sidebar from '../common/Sidebar';

export default function PortalLayout({ title, subtitle, badgeText, badgeClass = 'bg-primary', children }) {
  return (
    <AppLayout>
      <div className="portal-container" id="portal-container-wrapper">
        <Sidebar />
        <section className="portal-content" aria-labelledby="portal-page-title">
          {/* Standard Portal Header Bar */}
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-4 pb-2 border-bottom">
            <div>
              <div className="d-flex align-items-center gap-2">
                <h1 className="h3 mb-0 fw-bold" id="portal-page-title">
                  {title}
                </h1>
                {badgeText && <span className={`badge ${badgeClass} small`}>{badgeText}</span>}
              </div>
              {subtitle && <p className="text-muted small mb-0 mt-1">{subtitle}</p>}
            </div>
          </div>

          {/* Child content rendered within the portal */}
          {children}
        </section>
      </div>
    </AppLayout>
  );
}
