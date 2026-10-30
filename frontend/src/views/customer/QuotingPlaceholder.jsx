import React, { useState } from 'react';
import PortalLayout from '../../components/layout/PortalLayout';

export default function QuotingPlaceholder({ mode = 'MOTOR' }) {
  const [vehicleType, setVehicleType] = useState('4W');
  const [idv, setIdv] = useState(650000);
  const [calculatedPremium, setCalculatedPremium] = useState(null);

  const handleSimulateQuote = (e) => {
    e.preventDefault();
    // Demo calculation for shell testing
    const base = vehicleType === '4W' ? idv * 0.025 : idv * 0.03;
    setCalculatedPremium(Math.round(base));
  };

  return (
    <PortalLayout
      title={mode === 'MOTOR' ? 'Motor Insurance Quote' : 'Health Insurance Quote'}
      subtitle="Quoting & Underwriting Engine (SAD Section 4.3.3)"
      badgeText="Instant Quote"
      badgeClass="bg-success"
    >
      {import.meta.env.DEV && (
        <div className="dev-hook-box mb-4">
          <span className="dev-hook-badge">Person 1 Ownership (Quote & Underwriting C5/C6)</span>
          <p className="mb-0">
            This screen connects to <code>POST /api/v1/quotes/motor</code> or <code>POST /api/v1/quotes/health</code>.
            Person 1 owns the rating rule calculations (SRS 4.4.1) and add-on pricing logic.
          </p>
        </div>
      )}

      <div className="row g-4">
        <div className="col-12 col-lg-7">
          <div className="dashboard-card">
            <div className="dashboard-card-header">
              <span className="fw-bold">Quote Parameters Form</span>
              <span className="badge bg-secondary">{mode}</span>
            </div>
            <div className="dashboard-card-body">
              <form onSubmit={handleSimulateQuote}>
                {mode === 'MOTOR' ? (
                  <>
                    <div className="row g-2 mb-3">
                      <div className="col-6">
                        <label htmlFor="vehicle-type" className="form-label small fw-semibold">
                          Vehicle Type
                        </label>
                        <select
                          id="vehicle-type"
                          className="form-select form-select-sm"
                          value={vehicleType}
                          onChange={(e) => setVehicleType(e.target.value)}
                        >
                          <option value="4W">4-Wheeler (Car)</option>
                          <option value="2W">2-Wheeler (Bike/Scooter)</option>
                        </select>
                      </div>
                      <div className="col-6">
                        <label htmlFor="reg-year" className="form-label small fw-semibold">
                          Registration Year
                        </label>
                        <input
                          type="number"
                          id="reg-year"
                          className="form-control form-control-sm"
                          defaultValue={2022}
                        />
                      </div>
                    </div>

                    <div className="mb-3">
                      <label htmlFor="idv-input" className="form-label small fw-semibold">
                        Insured Declared Value (IDV in ₹)
                      </label>
                      <input
                        type="number"
                        id="idv-input"
                        className="form-control form-control-sm"
                        value={idv}
                        onChange={(e) => setIdv(Number(e.target.value))}
                      />
                    </div>
                  </>
                ) : (
                  <>
                    <div className="mb-3">
                      <label htmlFor="sum-insured" className="form-label small fw-semibold">
                        Sum Insured (₹)
                      </label>
                      <select id="sum-insured" className="form-select form-select-sm" defaultValue="500000">
                        <option value="300000">₹3,00,000</option>
                        <option value="500000">₹5,00,000</option>
                        <option value="1000000">₹10,00,000</option>
                      </select>
                    </div>
                  </>
                )}

                <button type="submit" className="btn btn-primary btn-sm w-100" id="calculate-quote-btn">
                  <i className="bi bi-calculator me-1"></i>Calculate Premium Quote
                </button>
              </form>
            </div>
          </div>
        </div>

        <div className="col-12 col-lg-5">
          <div className="dashboard-card">
            <div className="dashboard-card-header">
              <span className="fw-bold">Quote Breakdown</span>
              <span className="badge bg-info text-dark">Live Estimate</span>
            </div>
            <div className="dashboard-card-body">
              {calculatedPremium ? (
                <div>
                  <div className="text-center p-3 bg-light rounded-3 mb-3">
                    <span className="small text-muted d-block">Estimated Annual Premium</span>
                    <span className="fs-3 fw-bold text-primary">₹{calculatedPremium.toLocaleString()}</span>
                  </div>
                  <ul className="list-group list-group-flush small mb-3">
                    <li className="list-group-item d-flex justify-content-between px-0">
                      <span>Base Own Damage:</span>
                      <span className="fw-semibold">₹{(calculatedPremium * 0.7).toFixed(0)}</span>
                    </li>
                    <li className="list-group-item d-flex justify-content-between px-0">
                      <span>Third Party Cover:</span>
                      <span className="fw-semibold">₹{(calculatedPremium * 0.18).toFixed(0)}</span>
                    </li>
                    <li className="list-group-item d-flex justify-content-between px-0">
                      <span>GST (18%):</span>
                      <span className="fw-semibold">₹{(calculatedPremium * 0.12).toFixed(0)}</span>
                    </li>
                  </ul>
                  <button className="btn btn-success btn-sm w-100">
                    <i className="bi bi-arrow-right-circle me-1"></i>Proceed to Application
                  </button>
                </div>
              ) : (
                <div className="text-center text-muted py-4 small">
                  Fill in the vehicle parameters and click Calculate to view the rating breakdown.
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </PortalLayout>
  );
}
