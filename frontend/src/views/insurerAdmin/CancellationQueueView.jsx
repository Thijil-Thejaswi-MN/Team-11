import React, { useState, useEffect } from 'react';
import PortalLayout from '../../components/layout/PortalLayout';
import apiClient from '../../api/apiClient';

/**
 * Insurer Admin Cancellation Queue Screen (P3-10 / INS-F-020).
 * Lists pending policy cancellation requests for the insurer and allows Approve / Reject decisions.
 */
export default function CancellationQueueView() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionMessage, setActionMessage] = useState(null);
  const [processingId, setProcessingId] = useState(null);

  useEffect(() => {
    fetchCancellations();
  }, []);

  const fetchCancellations = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.get('/cancellations');
      setRequests(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load cancellation requests.');
    } finally {
      setLoading(false);
    }
  };

  const handleDecision = async (requestId, decision) => {
    setProcessingId(requestId);
    setActionMessage(null);
    setError(null);
    try {
      const res = await apiClient.post(`/cancellations/${requestId}/decision`, {
        decision,
        reason: decision === 'Approve' ? 'Approved by Insurer Admin' : 'Rejected by Insurer Admin',
      });
      const refund = res.data?.refundAmount != null ? ` (Refund: ₹${res.data.refundAmount})` : '';
      setActionMessage(`Request #${requestId} marked as ${res.data?.status || decision}${refund}.`);
      await fetchCancellations();
    } catch (err) {
      setError(err.response?.data?.message || err.message || `Failed to ${decision.toLowerCase()} request.`);
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <PortalLayout
      title="Policy Cancellation Queue"
      subtitle="Review customer cancellation requests and process pro-rata / free-look refunds (P3-10 / INS-F-020)"
      badgeText="Insurer Admin"
      badgeClass="bg-warning text-dark"
    >
      <div className="dashboard-card">
        <div className="dashboard-card-header d-flex justify-content-between align-items-center">
          <span className="fw-bold">Cancellation Requests</span>
          <button type="button" className="btn btn-outline-secondary btn-sm" onClick={fetchCancellations}>
            Refresh Queue
          </button>
        </div>
        <div className="dashboard-card-body">
          {actionMessage && (
            <div className="alert alert-success py-2 mb-3" role="alert">
              {actionMessage}
            </div>
          )}
          {error && (
            <div className="alert alert-danger py-2 mb-3" role="alert">
              {error}
            </div>
          )}

          {loading ? (
            <p className="text-muted mb-0">Loading cancellation requests...</p>
          ) : requests.length === 0 ? (
            <p className="text-muted mb-0">No cancellation requests found for your insurer.</p>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Request ID</th>
                    <th>Policy ID</th>
                    <th>Customer ID</th>
                    <th>Reason</th>
                    <th>Status</th>
                    <th>Refund Amount</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map((req) => (
                    <tr key={req.id}>
                      <td className="fw-semibold">#{req.id}</td>
                      <td>Policy #{req.policyId}</td>
                      <td>User #{req.requestedBy}</td>
                      <td>{req.reason || 'Customer requested cancellation'}</td>
                      <td>
                        <span className="badge bg-secondary">{req.status}</span>
                      </td>
                      <td>{req.refundAmount != null ? `₹${req.refundAmount}` : '—'}</td>
                      <td>
                        {req.status === 'REQUESTED' ? (
                          <div className="d-flex gap-2">
                            <button
                              type="button"
                              className="btn btn-success btn-sm"
                              disabled={processingId === req.id}
                              onClick={() => handleDecision(req.id, 'Approve')}
                            >
                              Approve & Refund
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-danger btn-sm"
                              disabled={processingId === req.id}
                              onClick={() => handleDecision(req.id, 'Reject')}
                            >
                              Reject
                            </button>
                          </div>
                        ) : (
                          <span className="text-muted small">Decided</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </PortalLayout>
  );
}
