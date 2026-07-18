import { useState } from 'react';
import { Network, Plus, CheckCircle, AlertCircle, XCircle, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useConnectors } from '@/hooks/use-connectors';
import { apiClient } from '@/lib/api-client';
import type { Connector } from '@/types/models';

export function ApplicationsPage() {
  const { data, isLoading, error, refetch } = useConnectors();
  const [showCreateDialog, setShowCreateDialog] = useState(false);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-foreground">Applications</h2>
          <p className="text-sm text-muted-foreground">
            Manage connected applications and their sync status
          </p>
        </div>
        <Button onClick={() => setShowCreateDialog(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Connect Application
        </Button>
      </div>

      {/* Connector Cards */}
      {isLoading ? (
        <div className="flex h-40 items-center justify-center">
          <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : error ? (
        <div className="flex h-40 items-center justify-center text-destructive">
          Failed to load connectors.
        </div>
      ) : data && data.data.length > 0 ? (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {data.data.map((connector) => (
            <ConnectorCard key={connector.id} connector={connector} onRefresh={refetch} />
          ))}
        </div>
      ) : (
        <div className="flex h-40 flex-col items-center justify-center rounded-lg border bg-card text-muted-foreground">
          <Network className="mb-2 h-8 w-8" />
          <p>No applications connected</p>
          <p className="text-xs">Connect your first application to get started</p>
        </div>
      )}

      {/* Simple create dialog */}
      {showCreateDialog && (
        <CreateConnectorDialog
          onClose={() => setShowCreateDialog(false)}
          onSuccess={() => { setShowCreateDialog(false); refetch(); }}
        />
      )}
    </div>
  );
}

function ConnectorCard({ connector, onRefresh }: { connector: Connector; onRefresh: () => void }) {
  const [checking, setChecking] = useState(false);
  const [showSyncDialog, setShowSyncDialog] = useState(false);
  const [showDisconnectConfirm, setShowDisconnectConfirm] = useState(false);
  const [disconnecting, setDisconnecting] = useState(false);

  const healthIcon = {
    HEALTHY: <CheckCircle className="h-5 w-5 text-emerald-500" />,
    DEGRADED: <AlertCircle className="h-5 w-5 text-amber-500" />,
    UNHEALTHY: <XCircle className="h-5 w-5 text-red-500" />,
  };

  const runHealthCheck = async () => {
    setChecking(true);
    try {
      await apiClient.post(`/connectors/${connector.id}/health-check`);
      onRefresh();
    } catch (e) {
      console.error('Health check failed:', e);
    } finally {
      setChecking(false);
    }
  };

  const handleDisconnect = async () => {
    setDisconnecting(true);
    try {
      await apiClient.delete(`/connectors/${connector.id}`);
      onRefresh();
    } catch (e) {
      console.error('Disconnect failed:', e);
    } finally {
      setDisconnecting(false);
      setShowDisconnectConfirm(false);
    }
  };

  return (
    <>
      <div className="rounded-lg border bg-card p-5">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
              <Network className="h-5 w-5 text-primary" />
            </div>
            <div>
              <p className="font-medium text-foreground">{connector.name}</p>
              <p className="text-xs text-muted-foreground">{connector.connectorType}</p>
            </div>
          </div>
          {connector.healthStatus && healthIcon[connector.healthStatus as keyof typeof healthIcon]}
        </div>

        <div className="mt-4 space-y-2">
          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">Status</span>
            <Badge variant={connector.status === 'CONNECTED' ? 'success' : 'secondary'}>
              {connector.status}
            </Badge>
          </div>
          {connector.lastHealthAt && (
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">Last health check</span>
              <span className="text-foreground">{new Date(connector.lastHealthAt).toLocaleString()}</span>
            </div>
          )}
          {connector.lastSyncAt && (
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">Last sync</span>
              <span className="text-foreground">{new Date(connector.lastSyncAt).toLocaleString()}</span>
            </div>
          )}
        </div>

        <div className="mt-4 flex gap-2">
          <Button variant="outline" size="sm" onClick={runHealthCheck} disabled={checking}>
            {checking ? 'Checking...' : 'Health Check'}
          </Button>
          <Button variant="outline" size="sm" onClick={() => setShowSyncDialog(true)}>
            Sync
          </Button>
          <Button
            variant="outline"
            size="sm"
            className="ml-auto text-destructive hover:bg-destructive/10 hover:text-destructive"
            onClick={() => setShowDisconnectConfirm(true)}
          >
            <Trash2 className="mr-1 h-3.5 w-3.5" />
            Disconnect
          </Button>
        </div>
      </div>

      {showDisconnectConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-sm rounded-lg border bg-card p-6 shadow-lg">
            <h3 className="text-lg font-semibold text-foreground">Disconnect Application</h3>
            <p className="mt-2 text-sm text-muted-foreground">
              Are you sure you want to disconnect <strong>{connector.name}</strong>? This will remove the connector and its saved credentials. Imported users and groups will not be deleted.
            </p>
            <div className="mt-6 flex justify-end gap-3">
              <Button variant="outline" size="sm" onClick={() => setShowDisconnectConfirm(false)} disabled={disconnecting}>
                Cancel
              </Button>
              <Button variant="destructive" size="sm" onClick={handleDisconnect} disabled={disconnecting}>
                {disconnecting ? 'Disconnecting...' : 'Disconnect'}
              </Button>
            </div>
          </div>
        </div>
      )}

      {showSyncDialog && (
        <SyncDialog
          connectorId={connector.id}
          connectorName={connector.name}
          onClose={() => setShowSyncDialog(false)}
          onSuccess={() => { setShowSyncDialog(false); onRefresh(); }}
        />
      )}
    </>
  );
}

function CreateConnectorDialog({ onClose, onSuccess }: { onClose: () => void; onSuccess: () => void }) {
  const [form, setForm] = useState({ name: '', connectorType: 'xwiki' });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);
    try {
      await apiClient.post('/connectors', form);
      onSuccess();
    } catch (err: unknown) {
      setError(err && typeof err === 'object' && 'message' in err ? (err as { message: string }).message : 'Failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-md rounded-lg border bg-card p-6 shadow-lg">
        <h2 className="text-lg font-semibold text-foreground">Connect Application</h2>
        {error && <div className="mt-3 rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</div>}
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="text-sm font-medium text-foreground">Application Type</label>
            <select
              value={form.connectorType}
              onChange={(e) => setForm({ ...form, connectorType: e.target.value })}
              className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            >
              <option value="xwiki">xWiki</option>
              <option value="openproject">OpenProject</option>
              <option value="nextcloud">Nextcloud</option>
            </select>
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">Display Name</label>
            <input
              type="text"
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              placeholder="Production xWiki"
            />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Connecting...' : 'Connect'}</Button>
          </div>
        </form>
      </div>
    </div>
  );
}


function SyncDialog({ connectorId, connectorName, onClose, onSuccess }: { connectorId: string; connectorName: string; onClose: () => void; onSuccess: () => void }) {
  const [form, setForm] = useState({ baseUrl: '', username: '', password: '' });
  const [isSyncing, setIsSyncing] = useState(false);
  const [result, setResult] = useState<{
    success: boolean;
    users?: { total: number; created: number; updated: number; errors: number };
    groups?: { total: number; created: number; skipped: number };
    memberships?: { linked: number; skipped: number };
    error?: string;
  } | null>(null);

  const handleFullSync = async () => {
    setIsSyncing(true);
    setResult(null);
    try {
      const response = await apiClient.post(`/connectors/${connectorId}/sync-all`, form);
      setResult(response.data);
    } catch (e: unknown) {
      setResult({ success: false, error: 'Sync failed — check connection details' });
    } finally {
      setIsSyncing(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-md rounded-lg border bg-card p-6 shadow-lg">
        <h2 className="text-lg font-semibold text-foreground">Full Sync: {connectorName}</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Import all users, groups, and memberships from the connected application.
        </p>

        <div className="mt-4 space-y-3">
          <div>
            <label className="text-sm font-medium text-foreground">Base URL</label>
            <input
              type="text"
              value={form.baseUrl}
              onChange={(e) => setForm({ ...form, baseUrl: e.target.value })}
              className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              placeholder="https://xwiki.example.com"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">Username</label>
            <input
              type="text"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              placeholder="admin"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">Password</label>
            <input
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              placeholder="••••••••"
            />
          </div>
        </div>

        {result && (
          <div className={`mt-4 rounded-md p-3 text-sm ${result.success ? 'bg-emerald-50 text-emerald-700' : 'bg-destructive/10 text-destructive'}`}>
            {result.success ? (
              <div className="space-y-1">
                <p className="font-medium">Sync complete</p>
                {result.users && (
                  <p>Users: {result.users.total} found, {result.users.created} created, {result.users.updated} updated{result.users.errors > 0 ? `, ${result.users.errors} errors` : ''}</p>
                )}
                {result.groups && (
                  <p>Groups: {result.groups.total} found, {result.groups.created} created, {result.groups.skipped} existing</p>
                )}
                {result.memberships && (
                  <p>Memberships: {result.memberships.linked} linked</p>
                )}
              </div>
            ) : (
              <p>Error: {result.error}</p>
            )}
          </div>
        )}

        <div className="mt-6 flex justify-between">
          <Button
            size="sm"
            onClick={handleFullSync}
            disabled={isSyncing || !form.baseUrl || !form.username || !form.password}
          >
            {isSyncing ? 'Syncing...' : 'Sync All (Users + Groups + Members)'}
          </Button>
          <Button variant="ghost" size="sm" onClick={result?.success ? onSuccess : onClose}>
            {result?.success ? 'Done' : 'Cancel'}
          </Button>
        </div>
      </div>
    </div>
  );
}
