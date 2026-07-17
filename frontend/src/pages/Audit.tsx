import { useState, useEffect } from 'react';
import { ClipboardList, Search, Download } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';

interface AuditEvent {
  id: string;
  eventType: string;
  actorId: string | null;
  actorEmail: string | null;
  targetType: string | null;
  targetId: string | null;
  action: string;
  correlationId: string | null;
  createdAt: string;
}

export function AuditPage() {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    loadEvents();
  }, [page, filterType]);

  const loadEvents = async () => {
    setIsLoading(true);
    try {
      const params: Record<string, unknown> = { page, size: 20 };
      if (filterType) params.eventType = filterType;

      const response = await apiClient.get<{ content: AuditEvent[]; totalPages: number; totalElements: number }>('/audit-events', { params });
      setEvents(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
      setTotalElements(response.data.totalElements || 0);
    } catch (e) {
      console.error('Failed to load audit events:', e);
    } finally {
      setIsLoading(false);
    }
  };

  const filteredEvents = events.filter((event) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      event.eventType.toLowerCase().includes(q) ||
      event.action.toLowerCase().includes(q) ||
      event.actorEmail?.toLowerCase().includes(q) ||
      event.targetType?.toLowerCase().includes(q)
    );
  });

  const exportCSV = () => {
    const csv = [
      ['Timestamp', 'Event', 'Action', 'Target Type', 'Actor', 'Correlation ID'].join(','),
      ...filteredEvents.map((e) =>
        [
          e.createdAt,
          e.eventType,
          e.action,
          e.targetType || '',
          e.actorEmail || '',
          e.correlationId || '',
        ].join(',')
      ),
    ].join('\n');

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `opencrowd-audit-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const actionColors: Record<string, string> = {
    created: 'bg-emerald-100 text-emerald-800',
    updated: 'bg-blue-100 text-blue-800',
    deleted: 'bg-red-100 text-red-800',
    status_changed: 'bg-amber-100 text-amber-800',
    assigned: 'bg-purple-100 text-purple-800',
    revoked: 'bg-red-100 text-red-800',
    member_added: 'bg-emerald-100 text-emerald-800',
    member_removed: 'bg-red-100 text-red-800',
    sync_completed: 'bg-blue-100 text-blue-800',
    health_changed: 'bg-amber-100 text-amber-800',
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-foreground">Audit Log</h2>
          <p className="text-sm text-muted-foreground">
            Track all governance actions across the platform
          </p>
        </div>
        <Button variant="outline" onClick={exportCSV} disabled={filteredEvents.length === 0}>
          <Download className="mr-2 h-4 w-4" />
          Export CSV
        </Button>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search events..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="h-9 w-full rounded-md border bg-background pl-9 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
        <select
          value={filterType}
          onChange={(e) => { setFilterType(e.target.value); setPage(0); }}
          className="h-9 rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="">All events</option>
          <option value="UserCreated">User Created</option>
          <option value="UserUpdated">User Updated</option>
          <option value="UserStatusChanged">Status Changed</option>
          <option value="GroupCreated">Group Created</option>
          <option value="GroupMemberAdded">Member Added</option>
          <option value="GroupMemberRemoved">Member Removed</option>
          <option value="ConnectorHealthChanged">Health Changed</option>
        </select>
      </div>

      {/* Stats */}
      <div className="text-sm text-muted-foreground">
        {totalElements} events total
      </div>

      {/* Event List */}
      <div className="rounded-lg border bg-card">
        {isLoading ? (
          <div className="flex h-40 items-center justify-center">
            <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : filteredEvents.length > 0 ? (
          <table className="w-full">
            <thead>
              <tr className="border-b bg-muted/50 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                <th className="px-4 py-3">Timestamp</th>
                <th className="px-4 py-3">Event</th>
                <th className="px-4 py-3">Action</th>
                <th className="px-4 py-3">Target</th>
                <th className="px-4 py-3">Correlation ID</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {filteredEvents.map((event) => (
                <tr key={event.id} className="hover:bg-muted/30">
                  <td className="px-4 py-2 text-sm text-muted-foreground whitespace-nowrap">
                    {new Date(event.createdAt).toLocaleString()}
                  </td>
                  <td className="px-4 py-2">
                    <span className="text-sm font-medium text-foreground">{event.eventType}</span>
                  </td>
                  <td className="px-4 py-2">
                    <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${actionColors[event.action] || 'bg-gray-100 text-gray-800'}`}>
                      {event.action}
                    </span>
                  </td>
                  <td className="px-4 py-2 text-sm text-muted-foreground">
                    {event.targetType && (
                      <span>{event.targetType}</span>
                    )}
                  </td>
                  <td className="px-4 py-2 text-xs font-mono text-muted-foreground">
                    {event.correlationId?.substring(0, 8)}...
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <div className="flex h-40 flex-col items-center justify-center text-muted-foreground">
            <ClipboardList className="mb-2 h-8 w-8" />
            <p>No audit events found</p>
            <p className="text-xs">Events are generated when you manage users, groups, and permissions</p>
          </div>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </p>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Previous</Button>
            <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</Button>
          </div>
        </div>
      )}
    </div>
  );
}
