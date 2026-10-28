import React, { useState, useEffect } from 'react';
import PortalLayout from '../../components/layout/PortalLayout';
import apiClient from '../../api/apiClient';

export default function CancellationQueueView() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionMessage, setActionMessage] = useState(null);
  const [processingId, setProcessingId] = useState(null);

  const fetchRequests = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get('/cancellations?status=REQUESTED');
      setRequests(response.data || []);
    } catch (err) {
      console.error('Failed to fetch cancellation requests:', err);
      setError(err.response?.data?.message || 'Failed to load cancellation requests.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const handleDecision = async (id, status) => {
    setProcessingId(id);
    setActionMessage(null);
    try {
      await apiClient.post(`/cancellations/${id}/decision`, { status });
      setActionMessage(`Cancellation request ${status.toLowerCase()} successfully.`);
      fetchRequests();
    } catch (err) {
      console.error(`Failed to ${status} request:`, err);
      setError(err.response?.data?.message || `Failed to ${status.toLowerCase()} request.`);
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <PortalLayout title="Cancellation Queue">
      <div className="container-fluid py-4">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <div>
            <h2 className="fw-bold mb-1">Cancellation Requests</h2>
            <p className="text-muted mb-0">Review and process pending policy cancellation requests.</p>
          </div>
          <button className="btn btn-outline-primary btn-sm" onClick={fetchRequests} disabled={loading}>
            Refresh Queue
          </button>
        </div>

        {actionMessage && (
          <div className="alert alert-success alert-dismissible fade show" role="alert">
            {actionMessage}
          </div>
        )}
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}

        {loading ? (
          <div className="text-center py-5">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
          </div>
        ) : requests.length === 0 ? (
          <div className="card shadow-sm border-0 text-center py-5">
            <div className="card-body">
              <p className="text-muted mb-0">No pending cancellation requests found.</p>
            </div>
          </div>
        ) : (
          <div className="card shadow-sm border-0">
            <div className="card-body p-0">
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light">
                    <tr>
                      <th>Request ID</th>
                      <th>Policy ID</th>
                      <th>Reason</th>
                      <th>Status</th>
                      <th>Date</th>
                      <th className="text-end">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {requests.map((req) => (
                      <tr key={req.id}>
                        <td>{req.id}</td>
                        <td>{req.policyId || req.policy_id || req.policyNo || 'N/A'}</td>
                        <td>{req.reason || 'No reason provided'}</td>
                        <td>
                          <span className="badge bg-warning text-dark">{req.status || 'REQUESTED'}</span>
                        </td>
                        <td>{req.createdAt ? new Date(req.createdAt).toLocaleDateString() : 'N/A'}</td>
                        <td className="text-end">
                          <button
                            className="btn btn-success btn-sm me-2"
                            onClick={() => handleDecision(req.id, 'APPROVED')}
                            disabled={processingId === req.id}
                          >
                            Approve
                          </button>
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => handleDecision(req.id, 'REJECTED')}
                            disabled={processingId === req.id}
                          >
                            Reject
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </div>
    </PortalLayout>
  );
}
