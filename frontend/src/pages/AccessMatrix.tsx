import { useState, useEffect } from 'react';
import { Shield, RefreshCw, Search, CheckCircle, XCircle } from 'lucide-react';
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

type Tab = 'groups' | 'users' | 'spaces' | 'inspect';

export function AccessMatrixPage() {
  const [entries, setEntries] = useState<AccessEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSyncing, setIsSyncing] = useState(false);
  const [activeTab, setActiveTab] = useState<Tab>('groups');
  const [searchQuery, setSearchQuery] = useState('');
  const [syncResult, setSyncResult] = useState<string | null>(null);
  const [selectedResource, setSelectedResource] = useState<string>('');

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
      const baseUrl = prompt('xWiki Base URL:', 'https://xwiki.notropia.co');
      const username = prompt('Username:', 'kiro2');
      const password = prompt('Password:');

      if (!baseUrl || !username || !password) { setSyncResult('Cancelled'); setIsSyncing(false); return; }

      const response = await apiClient.post<{ success: boolean; entriesCreated?: number; error?: string }>('/access-matrix/sync-rights', { baseUrl, username, password });

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

  // Get unique permissions (columns)
  const allPermissions = [...new Set(entries.map((e) => e.permission))].sort();

  const togglePermission = async (principalName: string, permission: string, currentlyGranted: boolean, entry?: AccessEntry) => {
    try {
      const principalType = entry?.principalType || (activeTab === 'groups' ? 'GROUP' : 'USER');
      const application = entry?.application || 'xwiki';
      const resourceName = entry?.resourceName || '(global)';
      const resourceType = entry?.resourceType || 'wiki';

      const response = await apiClient.post<{ success: boolean; message: string }>('/access-matrix/toggle', {
        principalName,
        principalType,
        permission,
        application,
        resourceName,
        resourceType,
        action: currentlyGranted ? 'revoke' : 'grant',
      });

      if (response.data.success) {
        // Refresh the matrix
        loadEntries();
      }
    } catch (e) {
      console.error('Failed to toggle permission:', e);
    }
  };

  // Get unique resources for filter
  const allResources = [...new Set(entries.map((e) => e.resourceName))].sort();

  // Filter entries based on tab and search
  const filteredEntries = entries.filter((entry) => {
    if (activeTab === 'groups' && entry.principalType !== 'GROUP') return false;
    if (activeTab === 'users' && entry.principalType !== 'USER') return false;
    if (selectedResource && entry.resourceName !== selectedResource) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      if (!entry.principalName.toLowerCase().includes(q)) return false;
    }
    return true;
  });

  // Build matrix: rows = unique principals, columns = permissions
  const principalMap = new Map<string, Map<string, boolean>>();
  filteredEntries.forEach((entry) => {
    if (!principalMap.has(entry.principalName)) {
      principalMap.set(entry.principalName, new Map());
    }
    principalMap.get(entry.principalName)!.set(entry.permission, entry.allow);
  });

  const principals = [...principalMap.keys()].sort();

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

      {/* Tabs */}
      <div className="border-b">
        <nav className="flex gap-6">
          {([
            { id: 'groups', label: 'Groups' },
            { id: 'users', label: 'Users' },
            { id: 'spaces', label: 'By Resource' },
            { id: 'inspect', label: 'Inspect Permissions' },
          ] as { id: Tab; label: string }[]).map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`border-b-2 pb-3 text-sm font-medium transition-colors ${
                activeTab === tab.id
                  ? 'border-primary text-primary'
                  : 'border-transparent text-muted-foreground hover:text-foreground'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder={`Search ${activeTab === 'groups' ? 'groups' : 'users'}...`}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="h-9 w-full rounded-md border bg-background pl-9 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
        {(activeTab === 'spaces' || allResources.length > 1) && (
          <select
            value={selectedResource}
            onChange={(e) => setSelectedResource(e.target.value)}
            className="h-9 rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          >
            <option value="">All resources</option>
            {allResources.map((r) => (
              <option key={r} value={r}>{r}</option>
            ))}
          </select>
        )}
      </div>

      {/* Matrix */}
      <div className="rounded-lg border bg-card">
        {isLoading ? (
          <div className="flex h-40 items-center justify-center">
            <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : principals.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-muted/50">
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground sticky left-0 bg-muted/50 min-w-[200px]">
                    {activeTab === 'groups' ? 'Group' : 'User'}
                  </th>
                  {allPermissions.map((perm) => (
                    <th key={perm} className="px-3 py-3 text-center text-xs font-medium uppercase tracking-wider text-muted-foreground min-w-[80px]">
                      {perm}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y">
                {principals.map((principal) => {
                  const perms = principalMap.get(principal)!;
                  return (
                    <tr key={principal} className="hover:bg-muted/30">
                      <td className="px-4 py-2 sticky left-0 bg-card">
                        <div className="flex items-center gap-2">
                          <Shield className="h-4 w-4 text-muted-foreground" />
                          <span className="text-sm font-medium text-foreground">{principal}</span>
                        </div>
                      </td>
                      {allPermissions.map((perm) => {
                        const hasPermission = perms.get(perm);
                        const entry = filteredEntries.find(e => e.principalName === principal && e.permission === perm);
                        return (
                          <td key={perm} className="px-3 py-2 text-center">
                            <button
                              onClick={() => togglePermission(principal, perm, hasPermission === true, entry)}
                              className="rounded p-1 transition-colors hover:bg-muted"
                              title={hasPermission === true ? `Revoke '${perm}' from '${principal}'` : `Grant '${perm}' to '${principal}'`}
                            >
                              {hasPermission === true && (
                                <CheckCircle className="mx-auto h-5 w-5 text-emerald-500" />
                              )}
                              {hasPermission === false && (
                                <XCircle className="mx-auto h-5 w-5 text-red-500" />
                              )}
                              {hasPermission === undefined && (
                                <span className="inline-block h-5 w-5 rounded-full border-2 border-dashed border-muted-foreground/30" />
                              )}
                            </button>
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="flex h-40 flex-col items-center justify-center text-muted-foreground">
            <Shield className="mb-2 h-8 w-8" />
            <p>No permission entries found</p>
            <p className="text-xs">Click "Sync Permissions" to import rights from connected apps</p>
          </div>
        )}
      </div>

      {/* Summary */}
      {entries.length > 0 && (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
          <div className="rounded-lg border bg-card p-4">
            <p className="text-sm text-muted-foreground">Total Entries</p>
            <p className="text-2xl font-bold text-foreground">{entries.length}</p>
          </div>
          <div className="rounded-lg border bg-card p-4">
            <p className="text-sm text-muted-foreground">Groups</p>
            <p className="text-2xl font-bold text-foreground">{[...new Set(entries.filter(e => e.principalType === 'GROUP').map(e => e.principalName))].length}</p>
          </div>
          <div className="rounded-lg border bg-card p-4">
            <p className="text-sm text-muted-foreground">Users</p>
            <p className="text-2xl font-bold text-foreground">{[...new Set(entries.filter(e => e.principalType === 'USER').map(e => e.principalName))].length}</p>
          </div>
          <div className="rounded-lg border bg-card p-4">
            <p className="text-sm text-muted-foreground">Resources</p>
            <p className="text-2xl font-bold text-foreground">{allResources.length}</p>
          </div>
        </div>
      )}
    </div>
  );
}
