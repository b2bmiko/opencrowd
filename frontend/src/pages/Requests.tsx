import { useState, useEffect } from 'react';
import { ClipboardList, Plus, Clock, CheckCircle, XCircle, Filter, MessageSquare } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';

interface AccessRequest {
  id: string;
  requestorName: string;
  requestorEmail: string;
  type: string;
  resource: string;
  resourceName: string;
  application: string;
  permission: string;
  justification: string;
  customFields: string;
  status: string;
  reviewerName: string;
  reviewComment: string;
  reviewedAt: string;
  createdAt: string;
}

export function RequestsPage() {
  const [requests, setRequests] = useState<AccessRequest[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [showSubmitForm, setShowSubmitForm] = useState(false);
  const [pendingCount, setPendingCount] = useState(0);

  useEffect(() => {
    loadRequests();
  }, [statusFilter]);

  const loadRequests = async () => {
    setIsLoading(true);
    try {
      const params: Record<string, string> = { size: '50' };
      if (statusFilter !== 'all') params.status = statusFilter;
      const response = await apiClient.get<{ data: AccessRequest[]; pendingCount: number }>('/requests', { params });
      setRequests(response.data.data || []);
      setPendingCount(response.data.pendingCount || 0);
    } catch (e) {
      setRequests([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleApprove = async (id: string) => {
    try {
      await apiClient.post(`/requests/${id}/approve`);
      loadRequests();
    } catch (e) {
      console.error('Approve failed:', e);
    }
  };

  const handleReject = async (id: string) => {
    const comment = prompt('Rejection reason (optional):');
    try {
      await apiClient.post(`/requests/${id}/reject`, { comment });
      loadRequests();
    } catch (e) {
      console.error('Reject failed:', e);
    }
  };

  const statusVariant = {
    pending: 'warning',
    approved: 'success',
    rejected: 'destructive',
    expired: 'secondary',
    cancelled: 'secondary',
  } as const;

  const typeLabel: Record<string, string> = {
    access: 'Access Request',
    removal: 'Removal Request',
    elevation: 'Elevation Request',
    temporary: 'Temporary Access',
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-foreground">Access Requests</h2>
          <p className="text-sm text-muted-foreground">
            Review and manage access requests from users
          </p>
        </div>
        <div className="flex items-center gap-3">
          {pendingCount > 0 && (
            <Badge variant="warning" className="text-sm px-3 py-1">
              {pendingCount} pending
            </Badge>
          )}
          <Button onClick={() => setShowSubmitForm(true)}>
            <Plus className="mr-2 h-4 w-4" />
            New Request
          </Button>
        </div>
      </div>

      {/* Filter */}
      <div className="flex items-center gap-3">
        <Filter className="h-4 w-4 text-muted-foreground" />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="h-9 rounded-md border bg-background px-3 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="all">All requests</option>
          <option value="pending">Pending</option>
          <option value="approved">Approved</option>
          <option value="rejected">Rejected</option>
        </select>
      </div>

      {/* Requests List */}
      {isLoading ? (
        <div className="flex h-40 items-center justify-center">
          <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : requests.length > 0 ? (
        <div className="space-y-3">
          {requests.map((request) => (
            <div key={request.id} className="rounded-lg border bg-card p-5">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3">
                    <p className="font-medium text-foreground">{request.requestorName}</p>
                    <Badge variant={statusVariant[request.status as keyof typeof statusVariant] || 'secondary'}>{request.status}</Badge>
                    <Badge variant="outline">{typeLabel[request.type] || request.type}</Badge>
                  </div>
                  {request.requestorEmail && (
                    <p className="mt-1 text-sm text-muted-foreground">{request.requestorEmail}</p>
                  )}

                  <div className="mt-3 grid grid-cols-3 gap-4 text-sm">
                    <div>
                      <span className="text-muted-foreground">Permission:</span>{' '}
                      <span className="font-medium text-foreground">{request.permission}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Resource:</span>{' '}
                      <span className="font-medium text-foreground">{request.resourceName}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Application:</span>{' '}
                      <span className="font-medium text-foreground">{request.application}</span>
                    </div>
                  </div>

                  {request.justification && (
                    <div className="mt-2 text-sm">
                      <span className="text-muted-foreground">Justification:</span>{' '}
                      <span className="text-foreground italic">"{request.justification}"</span>
                    </div>
                  )}

                  {request.customFields && request.customFields !== '' && (
                    <div className="mt-2 text-sm">
                      <span className="text-muted-foreground">Additional info:</span>{' '}
                      <span className="text-foreground">{request.customFields}</span>
                    </div>
                  )}

                  <div className="mt-2 flex items-center gap-4 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      Requested: {new Date(request.createdAt).toLocaleString()}
                    </span>
                    {request.reviewerName && (
                      <span>Reviewed by {request.reviewerName}{request.reviewedAt ? ` on ${new Date(request.reviewedAt).toLocaleDateString()}` : ''}</span>
                    )}
                  </div>

                  {request.reviewComment && (
                    <div className="mt-2 flex items-center gap-1 text-xs text-muted-foreground">
                      <MessageSquare className="h-3 w-3" />
                      <span>"{request.reviewComment}"</span>
                    </div>
                  )}
                </div>

                {request.status === 'pending' && (
                  <div className="flex gap-2 ml-4">
                    <Button size="sm" onClick={() => handleApprove(request.id)}>
                      <CheckCircle className="mr-1 h-3.5 w-3.5" />
                      Approve
                    </Button>
                    <Button size="sm" variant="outline" className="text-destructive" onClick={() => handleReject(request.id)}>
                      <XCircle className="mr-1 h-3.5 w-3.5" />
                      Reject
                    </Button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="flex h-40 flex-col items-center justify-center rounded-lg border bg-card text-muted-foreground">
          <ClipboardList className="mb-2 h-8 w-8" />
          <p>No requests found</p>
          <p className="text-xs">Access requests from users will appear here</p>
        </div>
      )}

      {/* Submit Request Dialog */}
      {showSubmitForm && (
        <SubmitRequestDialog
          onClose={() => setShowSubmitForm(false)}
          onSuccess={() => { setShowSubmitForm(false); loadRequests(); }}
        />
      )}
    </div>
  );
}

function SubmitRequestDialog({ onClose, onSuccess }: { onClose: () => void; onSuccess: () => void }) {
  const [form, setForm] = useState({
    requestorName: '',
    requestorEmail: '',
    type: 'access',
    application: 'xwiki',
    resourceName: '(global)',
    permission: '',
    justification: '',
    projectCode: '',
    duration: '',
    managerName: '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);
    try {
      // Build custom fields JSON
      const customFields: Record<string, string> = {};
      if (form.projectCode) customFields['Project Code'] = form.projectCode;
      if (form.duration) customFields['Duration'] = form.duration;
      if (form.managerName) customFields['Manager'] = form.managerName;

      await apiClient.post('/requests', {
        requestorName: form.requestorName,
        requestorEmail: form.requestorEmail || null,
        type: form.type,
        application: form.application,
        resourceName: form.resourceName,
        permission: form.permission,
        justification: form.justification || null,
        customFields: Object.keys(customFields).length > 0 ? JSON.stringify(customFields) : null,
      });
      onSuccess();
    } catch (e: unknown) {
      setError(e && typeof e === 'object' && 'message' in e ? (e as { message: string }).message : 'Failed to submit request');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-lg rounded-lg border bg-card p-6 shadow-lg max-h-[90vh] overflow-y-auto">
        <h2 className="text-lg font-semibold text-foreground">Submit Access Request</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Request access to an application or resource. An admin will review and approve or reject.
        </p>

        {error && <div className="mt-3 rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</div>}

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          {/* Requestor info */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-sm font-medium text-foreground">Your Name *</label>
              <input
                type="text"
                required
                value={form.requestorName}
                onChange={(e) => setForm({ ...form, requestorName: e.target.value })}
                className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                placeholder="John Smith"
              />
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">Email</label>
              <input
                type="email"
                value={form.requestorEmail}
                onChange={(e) => setForm({ ...form, requestorEmail: e.target.value })}
                className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                placeholder="john@example.com"
              />
            </div>
          </div>

          {/* Request details */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-sm font-medium text-foreground">Request Type</label>
              <select
                value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value })}
                className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              >
                <option value="access">New Access</option>
                <option value="elevation">Elevation (more permissions)</option>
                <option value="temporary">Temporary Access</option>
                <option value="removal">Remove Access</option>
              </select>
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">Application</label>
              <select
                value={form.application}
                onChange={(e) => setForm({ ...form, application: e.target.value })}
                className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              >
                <option value="xwiki">xWiki</option>
                <option value="openproject">OpenProject</option>
                <option value="nextcloud">Nextcloud</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-sm font-medium text-foreground">Permission *</label>
              <select
                required
                value={form.permission}
                onChange={(e) => setForm({ ...form, permission: e.target.value })}
                className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              >
                <option value="">Select permission...</option>
                <option value="view">View</option>
                <option value="comment">Comment</option>
                <option value="edit">Edit</option>
                <option value="delete">Delete</option>
                <option value="admin">Admin</option>
                <option value="script">Script</option>
              </select>
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">Resource / Space</label>
              <input
                type="text"
                value={form.resourceName}
                onChange={(e) => setForm({ ...form, resourceName: e.target.value })}
                className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                placeholder="(global) or space name"
              />
            </div>
          </div>

          {/* Justification */}
          <div>
            <label className="text-sm font-medium text-foreground">Justification</label>
            <textarea
              value={form.justification}
              onChange={(e) => setForm({ ...form, justification: e.target.value })}
              className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              rows={3}
              placeholder="Why do you need this access?"
            />
          </div>

          {/* Custom Fields */}
          <div className="border-t pt-4">
            <p className="text-sm font-medium text-foreground">Additional Information</p>
            <p className="text-xs text-muted-foreground">Optional fields for your organization's approval process</p>
            <div className="mt-3 grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs text-muted-foreground">Project Code</label>
                <input
                  type="text"
                  value={form.projectCode}
                  onChange={(e) => setForm({ ...form, projectCode: e.target.value })}
                  className="mt-1 h-8 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  placeholder="PRJ-001"
                />
              </div>
              <div>
                <label className="text-xs text-muted-foreground">Manager Name</label>
                <input
                  type="text"
                  value={form.managerName}
                  onChange={(e) => setForm({ ...form, managerName: e.target.value })}
                  className="mt-1 h-8 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  placeholder="Jane Doe"
                />
              </div>
            </div>
            {form.type === 'temporary' && (
              <div className="mt-3">
                <label className="text-xs text-muted-foreground">Duration</label>
                <input
                  type="text"
                  value={form.duration}
                  onChange={(e) => setForm({ ...form, duration: e.target.value })}
                  className="mt-1 h-8 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  placeholder="e.g. 2 weeks, 30 days"
                />
              </div>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={isSubmitting || !form.requestorName || !form.permission}>
              {isSubmitting ? 'Submitting...' : 'Submit Request'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
