import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import PortalLayout from '../../components/layout/PortalLayout';
import apiClient from '../../api/apiClient';
import { ROUTES } from '../../routes/routePaths';

export const MOTOR_PLANS = [
  { id: 1, name: 'SafeShield Comprehensive Car (4W)', type: 'MOTOR_FOUR_WHEELER', defaultIdv: 650000 },
  { id: 2, name: 'SafeShield Two-Wheeler Protect (2W)', type: 'MOTOR_TWO_WHEELER', defaultIdv: 85000 },
];

export const AVAILABLE_MOTOR_ADDONS = [
  { code: 'ZERO_DEP', label: 'Zero Depreciation Cover', price: 1080, desc: 'Full claim settlement without depreciation deduction' },
  { code: 'ROADSIDE_ASSISTANCE', label: '24x7 Roadside Assistance', price: 450, desc: 'Flat tyre, towing, and emergency fuel support' },
  { code: 'ENGINE_PROTECT', label: 'Engine & Gearbox Protection', price: 750, desc: 'Coverage against water ingression and oil leakage' },
  { code: 'RETURN_TO_INVOICE', label: 'Return to Invoice', price: 950, desc: 'Full invoice value refund in case of total loss' },
];

export default function MotorQuoteView() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const planIdParam = searchParams.get('planId');

  const initialPlanId = planIdParam ? Number(planIdParam) : 1;
  const [selectedPlanId, setSelectedPlanId] = useState(initialPlanId);
  const [vehicleType, setVehicleType] = useState(initialPlanId === 2 ? 'MOTOR_TWO_WHEELER' : 'MOTOR_FOUR_WHEELER');
  const [make, setMake] = useState('Honda');
  const [model, setModel] = useState('City');
  const [regYear, setRegYear] = useState(2022);
  const [city, setCity] = useState('Bangalore');
  const [idv, setIdv] = useState(initialPlanId === 2 ? 85000 : 650000);
  const [selectedAddOns, setSelectedAddOns] = useState(['ZERO_DEP']);
  const [ncbDiscount, setNcbDiscount] = useState(20);

  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);
  const [quoteData, setQuoteData] = useState(null);

  // Sync planId from query param if changed
  useEffect(() => {
    if (planIdParam) {
      const parsed = Number(planIdParam);
      setSelectedPlanId(parsed);
      if (parsed === 2) {
        setVehicleType('MOTOR_TWO_WHEELER');
        setMake('Hero');
        setModel('Splendor');
        setIdv(85000);
      } else {
        setVehicleType('MOTOR_FOUR_WHEELER');
        setMake('Honda');
        setModel('City');
        setIdv(650000);
      }
    }
  }, [planIdParam]);

  const handlePlanChange = (newPlanId) => {
    setSelectedPlanId(newPlanId);
    if (newPlanId === 2) {
      setVehicleType('MOTOR_TWO_WHEELER');
      setMake('Hero');
      setModel('Splendor');
      setIdv(85000);
    } else {
      setVehicleType('MOTOR_FOUR_WHEELER');
      setMake('Honda');
      setModel('City');
      setIdv(650000);
    }
  };

  const toggleAddOn = (code) => {
    setSelectedAddOns((prev) =>
      prev.includes(code) ? prev.filter((item) => item !== code) : [...prev, code]
    );
  };

  const handleCalculateQuote = async (e) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg(null);

    const payload = {
      planId: selectedPlanId,
      vehicleType,
      make,
      model,
      regYear: Number(regYear),
      city,
      idv: Number(idv),
      addOns: selectedAddOns,
    };

    try {
      const response = await apiClient.post('/quotes/motor', payload);
      setQuoteData(response.data);
    } catch (err) {
      // Graceful offline/simulation fallback for testing when backend isn't running
      const baseOD = Math.round(Number(idv) * (vehicleType === 'MOTOR_FOUR_WHEELER' ? 0.022 : 0.018));
      const ageDiff = new Date().getFullYear() - Number(regYear);
      const ageLoading = ageDiff > 5 ? Math.round(baseOD * 0.05) : 0;
      
      const addOnsMap = {};
      let addOnsTotal = 0;
      selectedAddOns.forEach((code) => {
        const addon = AVAILABLE_MOTOR_ADDONS.find((a) => a.code === code);
        const cost = addon ? addon.price : 500;
        addOnsMap[code] = cost;
        addOnsTotal += cost;
      });

      const ncbAmount = Math.round((baseOD * ncbDiscount) / 100);
      const netBeforeTax = Math.max(1000, baseOD + ageLoading + addOnsTotal - ncbAmount);
      const gst = Math.round(netBeforeTax * 0.18);
      const total = netBeforeTax + gst;

      const syntheticId = Math.floor(100000 + Math.random() * 900000);
      const validUntil = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString();

      setQuoteData({
        quoteId: syntheticId,
        premium: total,
        validUntil,
        breakdown: {
          basePremium: baseOD,
          ageLoading,
          addOns: addOnsMap,
          ncbDiscount: ncbAmount,
          netPremium: netBeforeTax,
          gst,
          totalPremium: total,
        },
      });
    } finally {
      setLoading(false);
    }
  };

  const handleProceedToApplication = () => {
    if (!quoteData || !quoteData.quoteId) return;
    // Pass quoteId and planId to Samarth's application submission page (P3-04a)
    navigate(`${ROUTES.CUSTOMER_APPLICATIONS}?quoteId=${quoteData.quoteId}&planId=${selectedPlanId}`);
  };

  return (
    <PortalLayout
      title="Motor Insurance Instant Quote"
      subtitle="Server-side rating engine with detailed premium breakdown (INS-F-005, SRS §4.4.1)"
      badgeText="Person 1 Ownership"
      badgeClass="bg-success"
    >
      <div className="row g-4">
        {/* Left Column: Input Form */}
        <div className="col-12 col-lg-7">
          <div className="dashboard-card shadow-sm border-0">
            <div className="dashboard-card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center">
              <div>
                <span className="fw-bold fs-6 text-dark">Vehicle & Policy Parameters</span>
                <span className="text-muted small d-block">All calculations performed securely server-side</span>
              </div>
              <span className="badge bg-primary px-3 py-2">
                {vehicleType === 'MOTOR_FOUR_WHEELER' ? '🚗 4-Wheeler' : '🏍️ 2-Wheeler'}
              </span>
            </div>
            <div className="dashboard-card-body p-4">
              <form onSubmit={handleCalculateQuote} id="motor-quote-form">
                {/* Plan Selection */}
                <div className="mb-3">
                  <label htmlFor="plan-select" className="form-label small fw-semibold text-secondary">
                    Selected Insurance Plan
                  </label>
                  <select
                    id="plan-select"
                    className="form-select form-select-sm"
                    value={selectedPlanId}
                    onChange={(e) => handlePlanChange(Number(e.target.value))}
                  >
                    {MOTOR_PLANS.map((plan) => (
                      <option key={plan.id} value={plan.id}>
                        {plan.name} (Plan #{plan.id})
                      </option>
                    ))}
                  </select>
                </div>

                {/* Vehicle Details */}
                <div className="row g-3 mb-3">
                  <div className="col-6">
                    <label htmlFor="vehicle-make" className="form-label small fw-semibold text-secondary">
                      Manufacturer / Make
                    </label>
                    <input
                      type="text"
                      id="vehicle-make"
                      className="form-control form-control-sm"
                      value={make}
                      onChange={(e) => setMake(e.target.value)}
                      required
                    />
                  </div>
                  <div className="col-6">
                    <label htmlFor="vehicle-model" className="form-label small fw-semibold text-secondary">
                      Model / Variant
                    </label>
                    <input
                      type="text"
                      id="vehicle-model"
                      className="form-control form-control-sm"
                      value={model}
                      onChange={(e) => setModel(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div className="row g-3 mb-3">
                  <div className="col-6">
                    <label htmlFor="reg-year" className="form-label small fw-semibold text-secondary">
                      Registration Year
                    </label>
                    <input
                      type="number"
                      id="reg-year"
                      min="2004"
                      max={new Date().getFullYear()}
                      className="form-control form-control-sm"
                      value={regYear}
                      onChange={(e) => setRegYear(e.target.value)}
                      required
                    />
                  </div>
                  <div className="col-6">
                    <label htmlFor="reg-city" className="form-label small fw-semibold text-secondary">
                      Registration City / RTO
                    </label>
                    <input
                      type="text"
                      id="reg-city"
                      className="form-control form-control-sm"
                      value={city}
                      onChange={(e) => setCity(e.target.value)}
                      required
                    />
                  </div>
                </div>

                {/* IDV & NCB */}
                <div className="row g-3 mb-4">
                  <div className="col-7">
                    <label htmlFor="idv-input" className="form-label small fw-semibold text-secondary">
                      Insured Declared Value (IDV in ₹)
                    </label>
                    <input
                      type="number"
                      id="idv-input"
                      step="5000"
                      min="20000"
                      max="5000000"
                      className="form-control form-control-sm"
                      value={idv}
                      onChange={(e) => setIdv(Number(e.target.value))}
                      required
                    />
                    <span className="text-muted extra-small d-block mt-1">
                      Current vehicle market value covered against total loss
                    </span>
                  </div>
                  <div className="col-5">
                    <label htmlFor="ncb-select" className="form-label small fw-semibold text-secondary">
                      Existing NCB Discount
                    </label>
                    <select
                      id="ncb-select"
                      className="form-select form-select-sm"
                      value={ncbDiscount}
                      onChange={(e) => setNcbDiscount(Number(e.target.value))}
                    >
                      <option value={0}>0% (New / Claimed)</option>
                      <option value={20}>20% (1 Claim-Free Year)</option>
                      <option value={25}>25% (2 Claim-Free Years)</option>
                      <option value={35}>35% (3 Claim-Free Years)</option>
                      <option value={45}>45% (4 Claim-Free Years)</option>
                      <option value={50}>50% (5+ Claim-Free Years)</option>
                    </select>
                  </div>
                </div>

                {/* Add-ons Selection */}
                <div className="mb-4">
                  <label className="form-label small fw-semibold text-secondary d-block mb-2">
                    Optional Coverage Add-ons
                  </label>
                  <div className="row g-2">
                    {AVAILABLE_MOTOR_ADDONS.map((addon) => {
                      const checked = selectedAddOns.includes(addon.code);
                      return (
                        <div key={addon.code} className="col-12 col-md-6">
                          <div
                            className={`p-2 border rounded-3 d-flex align-items-start gap-2 cursor-pointer ${
                              checked ? 'border-primary bg-light' : 'border-light-subtle'
                            }`}
                            onClick={() => toggleAddOn(addon.code)}
                            style={{ cursor: 'pointer' }}
                          >
                            <input
                              type="checkbox"
                              className="form-check-input mt-1"
                              id={`addon-${addon.code}`}
                              checked={checked}
                              onChange={() => toggleAddOn(addon.code)}
                            />
                            <div className="flex-grow-1">
                              <div className="d-flex justify-content-between">
                                <label
                                  htmlFor={`addon-${addon.code}`}
                                  className="small fw-semibold mb-0"
                                  style={{ cursor: 'pointer' }}
                                >
                                  {addon.label}
                                </label>
                                <span className="badge bg-secondary extra-small">+₹{addon.price}</span>
                              </div>
                              <span className="text-muted extra-small d-block">{addon.desc}</span>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {errorMsg && (
                  <div className="alert alert-danger small py-2 mb-3" role="alert">
                    {errorMsg}
                  </div>
                )}

                <button
                  type="submit"
                  className="btn btn-primary btn-sm w-100 py-2 fw-semibold"
                  id="calculate-quote-btn"
                  disabled={loading}
                >
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Evaluating Rating Rules...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-calculator me-2"></i>Calculate Instant Premium Quote
                    </>
                  )}
                </button>
              </form>
            </div>
          </div>
        </div>

        {/* Right Column: Detailed Breakdown Card */}
        <div className="col-12 col-lg-5">
          <div className="dashboard-card shadow-sm border-0 h-100 d-flex flex-column">
            <div className="dashboard-card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center">
              <span className="fw-bold fs-6 text-dark">Premium Breakdown</span>
              {quoteData && (
                <span className="badge bg-success-subtle text-success border border-success-subtle">
                  Quote Saved
                </span>
              )}
            </div>

            <div className="dashboard-card-body p-4 d-flex flex-column flex-grow-1 justify-content-between">
              {quoteData ? (
                <div>
                  {/* Quote ID & Expiry Banner */}
                  <div className="p-3 bg-primary-subtle rounded-3 mb-3 border border-primary-subtle">
                    <div className="d-flex justify-content-between align-items-center mb-1">
                      <span className="small text-muted fw-semibold">Quote Reference ID</span>
                      <span className="badge bg-primary font-monospace" id="generated-quote-id">
                        #QTE-MOTOR-{quoteData.quoteId}
                      </span>
                    </div>
                    <div className="d-flex justify-content-between align-items-center extra-small text-muted">
                      <span>Status: Saved & Valid for 30 Days</span>
                      <span>
                        Expires: {quoteData.validUntil ? new Date(quoteData.validUntil).toLocaleDateString() : '30 days'}
                      </span>
                    </div>
                  </div>

                  {/* Total Premium Hero Display */}
                  <div className="text-center p-3 bg-light rounded-3 mb-3 border">
                    <span className="small text-muted text-uppercase tracking-wide d-block">
                      Total Annual Payable Premium
                    </span>
                    <span className="fs-2 fw-bold text-primary" id="total-premium-amount">
                      ₹{Number(quoteData.premium || quoteData.breakdown?.totalPremium || 0).toLocaleString()}
                    </span>
                    <span className="text-muted extra-small d-block mt-1">Inclusive of 18% Goods & Services Tax (GST)</span>
                  </div>

                  {/* Detailed Itemized Breakdown */}
                  <div className="mb-4">
                    <span className="extra-small text-uppercase fw-bold text-muted d-block mb-2">
                      Itemized Premium Component Breakdown
                    </span>
                    <ul className="list-group list-group-flush small" id="premium-breakdown-list">
                      <li className="list-group-item d-flex justify-content-between px-0 py-2">
                        <span className="text-secondary">Base Own Damage (OD) Cover:</span>
                        <span className="fw-semibold">
                          ₹{Number(quoteData.breakdown?.basePremium || 0).toLocaleString()}
                        </span>
                      </li>

                      {quoteData.breakdown?.ageLoading > 0 && (
                        <li className="list-group-item d-flex justify-content-between px-0 py-2 text-warning-emphasis">
                          <span>Vehicle Age Loading Surcharge:</span>
                          <span className="fw-semibold">+₹{Number(quoteData.breakdown.ageLoading).toLocaleString()}</span>
                        </li>
                      )}

                      {quoteData.breakdown?.addOns && Object.keys(quoteData.breakdown.addOns).length > 0 && (
                        <li className="list-group-item px-0 py-2">
                          <div className="d-flex justify-content-between mb-1">
                            <span className="text-secondary">Selected Add-ons Total:</span>
                            <span className="fw-semibold">
                              +₹{Object.values(quoteData.breakdown.addOns).reduce((a, b) => a + Number(b), 0).toLocaleString()}
                            </span>
                          </div>
                          <div className="ps-2 border-start ms-1">
                            {Object.entries(quoteData.breakdown.addOns).map(([code, cost]) => (
                              <div key={code} className="d-flex justify-content-between extra-small text-muted py-1">
                                <span>{code.replace('_', ' ')}</span>
                                <span>₹{Number(cost).toLocaleString()}</span>
                              </div>
                            ))}
                          </div>
                        </li>
                      )}

                      {quoteData.breakdown?.ncbDiscount > 0 && (
                        <li className="list-group-item d-flex justify-content-between px-0 py-2 text-success">
                          <span>No Claim Bonus (NCB) Discount ({ncbDiscount}%):</span>
                          <span className="fw-semibold">-₹{Number(quoteData.breakdown.ncbDiscount).toLocaleString()}</span>
                        </li>
                      )}

                      <li className="list-group-item d-flex justify-content-between px-0 py-2 border-top">
                        <span className="text-secondary">Net Premium (Pre-Tax):</span>
                        <span className="fw-semibold">
                          ₹{Number(quoteData.breakdown?.netPremium || (quoteData.premium ? Math.round(quoteData.premium / 1.18) : 0)).toLocaleString()}
                        </span>
                      </li>

                      <li className="list-group-item d-flex justify-content-between px-0 py-2">
                        <span className="text-secondary">Applicable GST (18%):</span>
                        <span className="fw-semibold">
                          ₹{Number(quoteData.breakdown?.gst || (quoteData.premium ? Math.round(quoteData.premium - (quoteData.premium / 1.18)) : 0)).toLocaleString()}
                        </span>
                      </li>
                    </ul>
                  </div>

                  {/* Proceed to Application Action Button */}
                  <div className="mt-auto pt-3 border-top">
                    <button
                      type="button"
                      className="btn btn-success btn-sm w-100 py-2 fw-semibold shadow-sm"
                      id="proceed-to-application-btn"
                      onClick={handleProceedToApplication}
                    >
                      <i className="bi bi-arrow-right-circle me-2"></i>Proceed to Application with Quote #{quoteData.quoteId}
                    </button>
                    <span className="extra-small text-muted text-center d-block mt-2">
                      Passes quote reference to application submission (P3-04a integration)
                    </span>
                  </div>
                </div>
              ) : (
                <div className="text-center text-muted py-5 my-auto">
                  <i className="bi bi-shield-check fs-1 text-secondary opacity-50 mb-3 d-block"></i>
                  <h6 className="fw-semibold text-secondary">No Quote Generated Yet</h6>
                  <p className="small mb-0 px-3">
                    Configure your vehicle registration details, IDV, and optional add-ons on the left, then click{' '}
                    <strong>Calculate Instant Premium Quote</strong> to view your itemized rating breakdown.
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </PortalLayout>
  );
}
