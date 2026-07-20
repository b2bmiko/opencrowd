import { useState, useEffect } from 'react';
import { ClipboardList, Clock, CheckCircle, XCircle, Filter } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';

interface AccessRequest {
  id: string;
  requestor: string;
  requestorEmail: string;
  type: 'access' | 'removal' | 'elevation';
  resource: string;
  application: string;
  permission: string;
  justification: string;
  status: 'pending' | 'approved' | 'rejected' | 'expired';
  reviewer?: string;
  reviewedAt?: string;
  createdAt: string;
}

export function RequestsPage() {
  const [requests, setRequests] = useState<AccessRequest[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('all');

  useEffect(() => {
    loadRequests();
  }, []);

  const loadRequests = async () => {
    setIsLoading(true);
    try {
      const response = await apiClient.get<{ data: AccessRequest[] }>('/requests');
      setRequests(response.data.data || []);
    } catch (e) {
      // Demo data until backend endpoint exists
      setRequests([
        { id: '1', requestor: 'MikeSawaya', requestorEmail: 'mike@notropia.co', type: 'access', resource: '(global)', application: 'xwiki', permission: 'admin', justification: 'Need admin access to manage team spaces', status: 'pending', createdAt: '2026-07-19T10:30:00Z' },
        { id: '2', requestor: 'aanderson', requestorEmail: 'amanda@example.com', type: 'access', resource: 'UAT_Project', application: 'xwiki', permission: 'edit', justification: 'Joining the UAT team, need write access', status: 'pending', createdAt: '2026-07-19T09:15:00Z' },
        { id: '3', requestor: 'crodriguez', requestorEmail: 'carlos@example.com', type: 'elevation', resource: '(global)', application: 'xwiki', permission: 'script', justification: 'Need script rights for automation task', status: 'pending', createdAt: '2026-07-18T16:00:00Z' },
        { id: '4', requestor: 'jsmith', requestorEmail: 'john@example.com', type: 'access', resource: 'CustomsProject1', application: 'xwiki', permission: 'view', justification: 'Cross-team collaboration', status: 'approved', reviewer: 'kiro2', reviewedAt: '2026-07-18T11:00:00Z', createdAt: '2026-07-17T14:30:00Z' },
        { id: '5', requestor: 'abrown', requestorEmail: 'alice@example.com', type: 'removal', resource: '(global)', application: 'xwiki', permission: 'delete', justification: 'No longer need delete access', status: 'approved', reviewer: 'kiro2', reviewedAt: '2026-07-17T09:00:00Z', createdAt: '2026-07-16T15:00:00Z' },
        { id: '6', requestor: 'mwilson', requestorEmail: 'michael@example.com', type: 'access', resource: '(global)', application: 'xwiki', permission: 'programming', justification: 'Want programming rights', status: 'rejected', reviewer: 'kiro2', reviewedAt: '2026-07-16T10:00:00Z', createdAt: '2026-07-15T11:00:00Z' },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleApprove = (id: string) => {
    setRequests(requests.map(r => r.id === id ? { ...r, status: 'approved' as const, reviewer: 'Platform Admin', reviewedAt: new Date().toISOString() } : r));
  };

  const handleReject = (id: string) => {
    setRequests(requests.map(r => r.id === id ? { ...r, status: 'rejected' as const, reviewer: 'Platform Admin', reviewedAt: new Date().toISOString() } : r));
  };

  const filteredRequests = requests.filter(r => statusFilter === 'all' || r.status === statusFilter);
  const pendingCount = requests.filter(r => r.status === 'pending').length;

  const statusVariant = {
    pending: 'warning',
    approved: 'success',
    rejected: 'destructive',
    expired: 'secondary',
  } as const;

  const typeLabel = {
    access: 'Access Request',
    removal: 'Removal Request',
    elevation: 'Elevation Request',
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
        {pendingCount > 0 && (
          <Badge variant="warning" className="text-sm px-3 py-1">
            {pendingCount} pending
          </Badge>
        )}
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
      ) : filteredRequests.length > 0 ? (
        <div className="space-y-3">
          {filteredRequests.map((request) => (
            <div key={request.id} className="rounded-lg border bg-card p-5">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3">
                    <p className="font-medium text-foreground">{request.requestor}</p>
                    <Badge variant={statusVariant[request.status]}>{request.status}</Badge>
                    <Badge variant="outline">{typeLabel[request.type]}</Badge>
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground">{request.requestorEmail}</p>

                  <div className="mt-3 grid grid-cols-3 gap-4 text-sm">
                    <div>
                      <span className="text-muted-foreground">Permission:</span>{' '}
                      <span className="font-medium text-foreground">{request.permission}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Resource:</span>{' '}
                      <span className="font-medium text-foreground">{request.resource}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Application:</span>{' '}
                      <span className="font-medium text-foreground">{request.application}</span>
                    </div>
                  </div>

                  <div className="mt-2 text-sm">
                    <span className="text-muted-foreground">Justification:</span>{' '}
                    <span className="text-foreground italic">"{request.justification}"</span>
                  </div>

                  <div className="mt-2 flex items-center gap-4 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      Requested: {new Date(request.createdAt).toLocaleString()}
                    </span>
                    {request.reviewer && (
                      <span>Reviewed by {request.reviewer} on {new Date(request.reviewedAt!).toLocaleDateString()}</span>
                    )}
                  </div>
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
    </div>
  );
}
