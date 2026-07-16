import { useState, useEffect } from 'react';
import { Shield, RefreshCw, Search, Filter } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';

interface AccessEntry {
  id: string;
  principalType: string;
  principalName: string;
  application: string;
  resourceType: string;
  resourceName: string;
  permission: string;
  allow: boolean;
  source: string;
  syncedAt: string;
}

export function AccessMatrixPage() {
  const [entries, setEntries] = useState<AccessEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSyncing, setIsSyncing] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterApp, setFilterApp] = useState<string>('');
  const [filterType, setFilterType] = useState<string>('');
  const [syncResult, setSyncResult] = useState<string | null>(null);

  useEffect(() => {
    loadEntries();
  }, []);

  const loadEntries = async () => {
    setIsLoading(true);
    try {
      const response = await apiClient.get<{ entries: AccessEntry[]; count: number }>('/access-matrix');
      setEntries(response.data.entries || []);
    } catch (e) {
      console.error('Failed to load access matrix:', e);
    } finally {
      setIsLoading(false);
    }
  };

  const syncRights = async () => {
    setIsSyncing(true);
    setSyncResult(null);
    try {
      // Get stored credentials from the xwiki connector
      const connectorsRes = await apiClient.get<{ data: { id: string; connectorType: string; config: string }[] }>('/connectors');
      const xwikiConnector = connectorsRes.data.data.find((c) => c.connectorType === 'xwiki');

      if (!xwikiConnector) {
        setSyncResult('No xWiki connector found. Register one first.');
        return;
      }

      // We need credentials — for now prompt or use stored
      const baseUrl = prompt('xWiki Base URL:', 'https://xwiki.notropia.co');
      const username = prompt('Username:', 'kiro2');
      const password = prompt('Password:');

      if (!baseUrl || !username || !password) {
        setSyncResult('Cancelled');
        return;
      }

      const response = await apiClient.post<{ success: boolean; entriesCreated?: number; error?: string }>('/access-matrix/sync-rights', {
        baseUrl,
        username,
        password,
      });

      if (response.data.success) {
        setSyncResult(`Synced! ${response.data.entriesCreated} permission entries imported.`);
        loadEntries();
      } else {
        setSyncResult(`Error: ${response.data.error}`);
      }
    } catch (e) {
      setSyncResult('Sync failed');
    } finally {
      setIsSyncing(false);
    }
  };

  const filteredEntries = entries.filter((entry) => {
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      if (!entry.principalName.toLowerCase().includes(q) &&
          !entry.resourceName.toLowerCase().includes(q) &&
          !entry.permission.toLowerCase().includes(q)) {
        return false;
      }
    }
    if (filterApp && entry.application !== filterApp) return false;
    if (filterType && entry.principalType !== filterType) return false;
    return true;
  });

  // Group by principal for matrix view
  const principals = [...new Set(filteredEntries.map((e) => e.principalName))];
  const permissions = [...new Set(filteredEntries.map((e) => e.permission))].sort();

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-foreground">Access Matrix</h2>
          <p className="text-sm text-muted-foreground">
            Unified view of permissions across all connected applications
          </p>
        </div>
        <Button onClick={syncRights} disabled={isSyncing}>
          <RefreshCw className={`mr-2 h-4 w-4 ${isSyncing ? 'animate-spin' : ''}`} />
          {isSyncing ? 'Syncing...' : 'Sync Permissions'}
        </Button>
      </div>

      {syncResult && (
        <div className={`rounded-md p-3 text-sm ${syncResult.startsWith('Error') || syncResult === 'Sync failed' ? 'bg-destructive/10 text-destructive' : 'bg-emerald-50 text-emerald-700'}`}>
          {syncResult}
        </div>
      )}

      {/* Filters */}
      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search by principal, resource, permission..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="h-9 w-full rounded-md border bg-background pl-9 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
        <select
          value={filterApp}
          onChange={(e) => setFilterApp(e.target.value)}
          className="h-9 rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="">All apps</option>
          <option value="xwiki">xWiki</option>
          <option value="openproject">OpenProject</option>
          <option value="nextcloud">Nextcloud</option>
        </select>
        <select
          value={filterType}
          onChange={(e) => setFilterType(e.target.value)}
          className="h-9 rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="">Users & Groups</option>
          <option value="USER">Users only</option>
          <option value="GROUP">Groups only</option>
        </select>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
        <div className="rounded-lg border bg-card p-4">
          <p className="text-sm text-muted-foreground">Total Entries</p>
          <p className="text-2xl font-bold text-foreground">{entries.length}</p>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <p className="text-sm text-muted-foreground">Principals</p>
          <p className="text-2xl font-bold text-foreground">{principals.length}</p>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <p className="text-sm text-muted-foreground">Permission Types</p>
          <p className="text-2xl font-bold text-foreground">{permissions.length}</p>
        </div>
        <div className="rounded-lg border bg-card p-4">
          <p className="text-sm text-muted-foreground">Applications</p>
          <p className="text-2xl font-bold text-foreground">{[...new Set(entries.map((e) => e.application))].length}</p>
        </div>
      </div>

      {/* Matrix Table */}
      <div className="rounded-lg border bg-card">
        {isLoading ? (
          <div className="flex h-40 items-center justify-center">
            <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : filteredEntries.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-muted/50 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  <th className="px-4 py-3">Principal</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Application</th>
                  <th className="px-4 py-3">Resource</th>
                  <th className="px-4 py-3">Permission</th>
                  <th className="px-4 py-3">Access</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {filteredEntries.slice(0, 100).map((entry) => (
                  <tr key={entry.id} className="hover:bg-muted/30">
                    <td className="px-4 py-2">
                      <div className="flex items-center gap-2">
                        <Shield className="h-4 w-4 text-muted-foreground" />
                        <span className="text-sm font-medium text-foreground">{entry.principalName}</span>
                      </div>
                    </td>
                    <td className="px-4 py-2">
                      <Badge variant={entry.principalType === 'GROUP' ? 'default' : 'secondary'}>
                        {entry.principalType}
                      </Badge>
                    </td>
                    <td className="px-4 py-2 text-sm text-foreground">{entry.application}</td>
                    <td className="px-4 py-2">
                      <span className="text-sm text-foreground">{entry.resourceName}</span>
                      <span className="ml-1 text-xs text-muted-foreground">({entry.resourceType})</span>
                    </td>
                    <td className="px-4 py-2">
                      <Badge variant="outline">{entry.permission}</Badge>
                    </td>
                    <td className="px-4 py-2">
                      <Badge variant={entry.allow ? 'success' : 'destructive'}>
                        {entry.allow ? 'Allow' : 'Deny'}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {filteredEntries.length > 100 && (
              <p className="px-4 py-3 text-sm text-muted-foreground">
                Showing first 100 of {filteredEntries.length} entries
              </p>
            )}
          </div>
        ) : (
          <div className="flex h-40 flex-col items-center justify-center text-muted-foreground">
            <Shield className="mb-2 h-8 w-8" />
            <p>No permission entries found</p>
            <p className="text-xs">Click "Sync Permissions" to import rights from connected apps</p>
          </div>
        )}
      </div>
    </div>
  );
}
