import { useEffect, useState } from 'react';
import { Users, Group, Network, Shield } from 'lucide-react';
import { apiClient } from '@/lib/api-client';

interface DashboardStats {
  userCount: number;
  groupCount: number;
  connectorCount: number;
}

export function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats>({ userCount: 0, groupCount: 0, connectorCount: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const [usersRes, groupsRes, connectorsRes] = await Promise.all([
        apiClient.get<{ content: unknown[]; totalElements: number }>('/users', { params: { size: 1 } }),
        apiClient.get<{ content: unknown[]; totalElements: number }>('/groups', { params: { size: 1 } }),
        apiClient.get<{ data: unknown[]; count: number }>('/connectors'),
      ]);
      setStats({
        userCount: usersRes.data.totalElements,
        groupCount: groupsRes.data.totalElements,
        connectorCount: connectorsRes.data.count,
      });
    } catch (e) {
      console.error('Failed to load dashboard stats:', e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Stats Grid */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard title="Users" value={loading ? '...' : stats.userCount.toLocaleString()} icon={Users} />
        <StatCard title="Groups" value={loading ? '...' : stats.groupCount.toLocaleString()} icon={Group} />
        <StatCard title="Connected Apps" value={loading ? '...' : stats.connectorCount.toString()} icon={Network} />
        <StatCard title="Governance Score" value="72" change="Medium risk" icon={Shield} />
      </div>

      {/* Placeholder sections */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="rounded-lg border bg-card p-6">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-foreground">Access Overview</h3>
            <span className="text-sm text-muted-foreground">Last 30 days</span>
          </div>
          <div className="mt-4 flex h-40 items-center justify-center text-muted-foreground">
            Chart — will be implemented with Access Matrix data
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-foreground">Applications Health</h3>
            <span className="text-sm text-emerald-600">Live</span>
          </div>
          <ConnectorHealthList />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="rounded-lg border bg-card p-6">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-foreground">Unified Access Matrix</h3>
            <span className="text-sm text-amber-600">Review-ready</span>
          </div>
          <div className="mt-4 flex h-24 items-center justify-center text-muted-foreground">
            Matrix summary — coming in Milestone 2
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-foreground">Risk Alerts</h3>
            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-destructive text-xs text-destructive-foreground">
              3
            </span>
          </div>
          <div className="mt-4 space-y-2 text-sm text-muted-foreground">
            <p>Users with no group membership</p>
            <p>Connectors without recent health check</p>
            <p>Users imported but not verified</p>
          </div>
        </div>
      </div>
    </div>
  );
}

function StatCard({ title, value, change, icon: Icon }: { title: string; value: string; change?: string; icon: React.ElementType }) {
  return (
    <div className="rounded-lg border bg-card p-6">
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium text-muted-foreground">{title}</p>
        <Icon className="h-5 w-5 text-muted-foreground" />
      </div>
      <div className="mt-2">
        <p className="text-3xl font-bold text-foreground">{value}</p>
        {change && <p className="mt-1 text-sm text-emerald-600">{change}</p>}
      </div>
    </div>
  );
}

function ConnectorHealthList() {
  const [connectors, setConnectors] = useState<{ name: string; status: string; healthStatus: string | null }[]>([]);

  useEffect(() => {
    apiClient.get<{ data: { name: string; status: string; healthStatus: string | null }[] }>('/connectors')
      .then((res) => setConnectors(res.data.data))
      .catch(() => {});
  }, []);

  if (connectors.length === 0) {
    return <div className="mt-4 text-sm text-muted-foreground">No connectors registered</div>;
  }

  return (
    <div className="mt-4 space-y-3">
      {connectors.map((c) => (
        <div key={c.name} className="flex items-center justify-between">
          <span className="text-sm text-foreground">{c.name}</span>
          <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
            c.healthStatus === 'HEALTHY' ? 'bg-emerald-100 text-emerald-800' :
            c.status === 'CONNECTED' ? 'bg-amber-100 text-amber-800' :
            'bg-gray-100 text-gray-800'
          }`}>
            {c.healthStatus || c.status}
          </span>
        </div>
      ))}
    </div>
  );
}
