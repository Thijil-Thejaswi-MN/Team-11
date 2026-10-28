import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import PortalLayout from '../../components/layout/PortalLayout';
import { ROUTES } from '../../routes/routePaths';

export default function InsurerAdminDashboardView() {
  const [activeTab, setActiveTab] = useState('OVERVIEW');

  return (
    <PortalLayout
      title="Insurer Administration Desk"
      subtitle="SafeShield General management portal (SAD Section 4.3.2, 4.3.4, 4.5)"
      badgeText="Insurer Admin"
      badgeClass="bg-warning text-dark"
    >
      {import.meta.env.DEV && (
        <div className="dev-hook-box mb-4">
          <span className="dev-hook-badge">Person 1 & Person 3 Ownership</span>
          <p className="mb-0">
            Person 1 owns Plan Management (<code>POST/PUT /api/v1/plans</code>) and underwriting parameters.
            Person 3 owns Cancellation Requests (<code>POST /api/v1/cancellations/&#123;id&#125;/decision</code>).
          </p>
        </div>
      )}

      {/* Tabs */}
      <ul className="nav nav-tabs mb-4">
        <li className="nav-item">
          <button
            className={`nav-link ${activeTab === 'OVERVIEW' ? 'active fw-bold' : ''}`}
            onClick={() => setActiveTab('OVERVIEW')}
          >
            <i className="bi bi-speedometer2 me-1"></i>Metrics Overview
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link ${activeTab === 'PLANS' ? 'active fw-bold' : ''}`}
            onClick={() => setActiveTab('PLANS')}
          >
            <i className="bi bi-card-checklist me-1"></i>Plan Management
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link ${activeTab === 'CANCELLATIONS' ? 'active fw-bold' : ''}`}
            onClick={() => setActiveTab('CANCELLATIONS')}
          >
            <i className="bi bi-x-circle me-1"></i>Cancellations
          </button>
        </li>
      </ul>

      {activeTab === 'OVERVIEW' && (
        <div className="row g-3 mb-4">
          <div className="col-12 col-sm-6 col-lg-3">
            <div className="dashboard-card">
              <div className="dashboard-card-body">
                <span className="text-muted small text-uppercase">Policies Issued</span>
                <h3 className="h3 fw-bold text-primary mb-0 mt-1">1,420</h3>
                <span className="text-success small">+8% this month</span>
              </div>
            </div>
          </div>
          <div className="col-12 col-sm-6 col-lg-3">
            <div className="dashboard-card">
              <div className="dashboard-card-body">
                <span className="text-muted small text-uppercase">Total Premium</span>
                <h3 className="h3 fw-bold text-success mb-0 mt-1">₹1.18 Cr</h3>
                <span className="text-muted small">SafeShield Portfolio</span>
              </div>
            </div>
          </div>
          <div className="col-12 col-sm-6 col-lg-3">
            <div className="dashboard-card">
              <div className="dashboard-card-body">
                <span className="text-muted small text-uppercase">Claims Settled</span>
                <h3 className="h3 fw-bold text-info mb-0 mt-1">142</h3>
                <span className="text-muted small">₹18.4 Lakhs Payout</span>
              </div>
            </div>
          </div>
          <div className="col-12 col-sm-6 col-lg-3">
            <div className="dashboard-card">
              <div className="dashboard-card-body">
                <span className="text-muted small text-uppercase">Loss Ratio</span>
                <h3 className="h3 fw-bold text-dark mb-0 mt-1">62.4%</h3>
                <span className="text-success small">Within target (&lt;70%)</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'PLANS' && (
        <div className="dashboard-card">
          <div className="dashboard-card-header d-flex justify-content-between align-items-center">
            <span className="fw-bold">Active Insurance Plans</span>
            <Link to={ROUTES.INSURER_ADMIN_PLANS} className="btn btn-primary btn-sm">
              <i className="bi bi-pencil-square me-1"></i>Open Plan Editor & Management
            </Link>
          </div>
          <div className="dashboard-card-body p-0">
            <div className="table-responsive">
              <table className="table table-hover mb-0 align-middle">
                <thead className="table-light">
                  <tr>
                    <th>Plan ID</th>
                    <th>Plan Name</th>
                    <th>Category</th>
                    <th>Base Premium Formula</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="fw-semibold">PLAN-01</td>
                    <td>SafeShield Motor Comprehensive (4W)</td>
                    <td>Motor (4W)</td>
                    <td>2.5% of IDV + AddOns</td>
                    <td><span className="status-badge status-active">Active</span></td>
                    <td>
                      <Link to={ROUTES.INSURER_ADMIN_PLANS} className="btn btn-outline-primary btn-sm">
                        Edit in Plan Editor
                      </Link>
                    </td>
                  </tr>
                  <tr>
                    <td className="fw-semibold">PLAN-03</td>
                    <td>SafeShield Two-Wheeler Protect (2W)</td>
                    <td>Motor (2W)</td>
                    <td>3.0% of IDV</td>
                    <td><span className="status-badge status-active">Active</span></td>
                    <td>
                      <Link to={ROUTES.INSURER_ADMIN_PLANS} className="btn btn-outline-primary btn-sm">
                        Edit in Plan Editor
                      </Link>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'CANCELLATIONS' && (
        <div className="dashboard-card">
          <div className="dashboard-card-header">
            <span className="fw-bold">Policy Cancellation Requests</span>
            <span className="badge bg-secondary">Free-Look Period Review</span>
          </div>
          <div className="dashboard-card-body p-0">
            <div className="table-responsive">
              <table className="table table-hover mb-0 align-middle">
                <thead className="table-light">
                  <tr>
                    <th>Request ID</th>
                    <th>Policy No</th>
                    <th>Requested At</th>
                    <th>Refund Estimate</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="fw-semibold">CAN-2026-001</td>
                    <td>POL-2026-4W-0088</td>
                    <td>11 Oct 2026 (Day 6 of Free-look)</td>
                    <td>₹8,200 (Near-full refund per SAD 4.1.6)</td>
                    <td>
                      <button className="btn btn-success btn-sm me-1">Approve Refund</button>
                      <button className="btn btn-outline-danger btn-sm">Reject</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </PortalLayout>
  );
}
