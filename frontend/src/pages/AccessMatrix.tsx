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

interface GroupModel {
  id: string;
  name: string;
  description?: string;
  type?: string;
}

interface UserModel {
  id: string;
  username: string;
  displayName?: string;
  email?: string;
}

type Tab = 'groups' | 'users' | 'spaces' | 'inspect';

export function AccessMatrixPage() {
  const [entries, setEntries] = useState<AccessEntry[]>([]);
  const [allGroups, setAllGroups] = useState<GroupModel[]>([]);
  const [allUsers, setAllUsers] = useState<UserModel[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSyncing, setIsSyncing] = useState(false);
  const [activeTab, setActiveTab] = useState<Tab>('groups');
  const [searchQuery, setSearchQuery] = useState('');
  const [syncResult, setSyncResult] = useState<string | null>(null);
  const [selectedResource, setSelectedResource] = useState<string>('');
  const [selectedApp, setSelectedApp] = useState<string>('');

  useEffect(() => {
    loadEntries();
    loadGroups();
    loadUsers();
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

  const loadGroups = async () => {
    try {
      const response = await apiClient.get<{ content: GroupModel[]; totalElements: number }>('/groups', { params: { size: 200 } });
      setAllGroups(response.data.content || []);
    } catch (e) {
      console.error('Failed to load groups:', e);
    }
  };

  const loadUsers = async () => {
    try {
      const response = await apiClient.get<{ content: UserModel[]; totalElements: number }>('/users', { params: { size: 200 } });
      setAllUsers(response.data.content || []);
    } catch (e) {
      console.error('Failed to load users:', e);
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

  const pushToApps = async () => {
    setIsSyncing(true);
    setSyncResult(null);
    try {
      const response = await apiClient.post<{ success: boolean; pushed?: number; failed?: number; error?: string; details?: string[] }>('/access-matrix/push-to-apps');
      if (response.data.success) {
        setSyncResult(`Pushed ${response.data.pushed} permission(s) to xWiki.${response.data.failed ? ` ${response.data.failed} failed.` : ''}`);
      } else {
        setSyncResult(`Error: ${response.data.error}`);
      }
    } catch (e) {
      setSyncResult('Push failed');
    } finally {
      setIsSyncing(false);
    }
  };

  // Get unique permissions (columns) — filtered by selected app so only relevant permissions show
  const permissionSource = selectedApp ? entries.filter(e => e.application === selectedApp) : entries;
  const allPermissions = [...new Set(permissionSource.map((e) => e.permission))].filter(p => p !== '(none)').sort();

  const togglePermission = async (principalName: string, permission: string, currentlyGranted: boolean, entry?: AccessEntry) => {
    try {
      const principalType = entry?.principalType || (activeTab === 'groups' ? 'GROUP' : 'USER');
      const application = entry?.application || selectedApp || 'xwiki';
      const resourceName = entry?.resourceName || (application === 'openproject' ? '(global)' : '(global)');
      const resourceType = entry?.resourceType || (application === 'openproject' ? 'project' : 'wiki');

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

  // Get unique resources and applications for filters
  const allResources = [...new Set(entries.map((e) => e.resourceName))].sort();
  const allApps = [...new Set(entries.map((e) => e.application))].sort();

  // Filter entries based on tab and search
  const filteredEntries = entries.filter((entry) => {
    if (activeTab === 'groups' && entry.principalType !== 'GROUP') return false;
    if (activeTab === 'users' && entry.principalType !== 'USER') return false;
    if (activeTab === 'inspect') return true; // Inspect shows all, filtered by search
    if (selectedApp && entry.application !== selectedApp) return false;
    if (selectedResource && entry.resourceName !== selectedResource) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      if (!entry.principalName.toLowerCase().includes(q)) return false;
    }
    return true;
  });

  // For inspect tab — filter by search matching principal name exactly
  const inspectEntries = activeTab === 'inspect' && searchQuery
    ? entries.filter(e => e.principalName.toLowerCase().includes(searchQuery.toLowerCase()))
    : [];

  // Build matrix: rows = unique principals, columns = permissions
  const principalMap = new Map<string, Map<string, boolean>>();
  filteredEntries.forEach((entry) => {
    if (entry.permission === '(none)') return; // Skip placeholder entries
    if (!principalMap.has(entry.principalName)) {
      principalMap.set(entry.principalName, new Map());
    }
    principalMap.get(entry.principalName)!.set(entry.permission, entry.allow);
  });

  // On groups tab, merge all OpenCrowd groups so they all appear as rows
  if (activeTab === 'groups') {
    allGroups.forEach((group) => {
      const name = group.name;
      if (searchQuery && !name.toLowerCase().includes(searchQuery.toLowerCase())) return;
      if (!principalMap.has(name)) {
        principalMap.set(name, new Map());
      }
    });
  }

  // On users tab, merge all OpenCrowd users so they all appear as rows
  if (activeTab === 'users') {
    allUsers.forEach((user) => {
      const name = user.username;
      if (searchQuery && !name.toLowerCase().includes(searchQuery.toLowerCase())) return;
      if (!principalMap.has(name)) {
        principalMap.set(name, new Map());
      }
    });
  }

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
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => { loadEntries(); loadGroups(); loadUsers(); }}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Refresh
          </Button>
        </div>
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
            { id: 'spaces', label: 'By Application' },
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
        {allApps.length > 1 && (
          <select
            value={selectedApp}
            onChange={(e) => setSelectedApp(e.target.value)}
            className="h-9 rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          >
            <option value="">All applications</option>
            {allApps.map((app) => (
              <option key={app} value={app}>{app}</option>
            ))}
          </select>
        )}
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
                        <div className="flex items-center gap-2 group relative">
                          <Shield className="h-4 w-4 text-muted-foreground" />
                          <span className="text-sm font-medium text-foreground cursor-default">{principal}</span>
                          {activeTab === 'users' && (() => {
                            const userInfo = allUsers.find(u => u.username === principal);
                            if (userInfo && (userInfo.displayName || userInfo.email)) {
                              return (
                                <div className="absolute left-0 top-full z-10 mt-1 hidden group-hover:block rounded-md border bg-popover p-2 shadow-md text-xs min-w-[180px]">
                                  {userInfo.displayName && <p className="font-medium text-foreground">{userInfo.displayName}</p>}
                                  {userInfo.email && <p className="text-muted-foreground">{userInfo.email}</p>}
                                </div>
                              );
                            }
                            return null;
                          })()}
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

      {/* Inspect Permissions View */}
      {activeTab === 'inspect' && (
        <div className="rounded-lg border bg-card p-6">
          <h3 className="font-semibold text-foreground">Inspect: {searchQuery || 'Type a name to inspect'}</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Shows all effective permissions for a user or group across all applications
          </p>
          {inspectEntries.length > 0 ? (
            <div className="mt-4 space-y-4">
              {/* Group by resource */}
              {[...new Set(inspectEntries.map(e => `${e.application}:${e.resourceName}`))].map(key => {
                const [app, resource] = key.split(':');
                const resourceEntries = inspectEntries.filter(e => e.application === app && e.resourceName === resource);
                return (
                  <div key={key} className="rounded-md border p-4">
                    <div className="flex items-center gap-2">
                      <Badge variant="outline">{app}</Badge>
                      <span className="font-medium text-foreground">{resource}</span>
                    </div>
                    <div className="mt-2 flex flex-wrap gap-2">
                      {resourceEntries.map(e => (
                        <Badge key={e.id} variant={e.allow ? 'success' : 'destructive'}>
                          {e.permission}
                        </Badge>
                      ))}
                    </div>
                    <p className="mt-1 text-xs text-muted-foreground">
                      Source: {resourceEntries[0]?.source} • Synced: {new Date(resourceEntries[0]?.syncedAt).toLocaleDateString()}
                    </p>
                  </div>
                );
              })}
            </div>
          ) : searchQuery ? (
            <p className="mt-4 text-sm text-muted-foreground">No permissions found for "{searchQuery}"</p>
          ) : null}
        </div>
      )}

      {/* Summary */}
      {(entries.length > 0 || allUsers.length > 0 || allGroups.length > 0) && (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
          <div className="rounded-lg border bg-card p-4">
            <p className="text-sm text-muted-foreground">Total Entries</p>
            <p className="text-2xl font-bold text-foreground">{entries.filter(e => e.permission !== '(none)').length}</p>
          </div>
          <div className="rounded-lg border bg-card p-4">
            <p className="text-sm text-muted-foreground">Groups</p>
            <p className="text-2xl font-bold text-foreground">{allGroups.length || [...new Set(entries.filter(e => e.principalType === 'GROUP').map(e => e.principalName))].length}</p>
          </div>
          <div className="rounded-lg border bg-card p-4">
            <p className="text-sm text-muted-foreground">Users</p>
            <p className="text-2xl font-bold text-foreground">{allUsers.length || [...new Set(entries.filter(e => e.principalType === 'USER').map(e => e.principalName))].length}</p>
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
