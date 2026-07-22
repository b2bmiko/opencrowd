import { useEffect, useState } from 'react';
import { Users, Group, Network, Shield, Activity, AlertTriangle, CheckCircle, ArrowUpRight, TrendingUp } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';

interface DashboardData {
  userCount: number;
  activeUsers: number;
  inactiveUsers: number;
  offboardedUsers: number;
  groupCount: number;
  connectorCount: number;
  connectedApps: number;
  totalPermissions: number;
  usersWithNoPermissions: number;
  usersWithExcessivePermissions: number;
  governanceScore: number;
  pendingRequests: number;
  recentEvents: { eventType: string; action: string; createdAt: string; details: string }[];
  connectors: { name: string; status: string; healthStatus: string | null; lastSyncAt: string | null }[];
  permissionsByType: { name: string; count: number }[];
}

export function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    try {
      const [usersRes, groupsRes, connectorsRes, matrixRes, auditRes, requestsRes] = await Promise.all([
        apiClient.get<{ content: { status: string }[]; totalElements: number }>('/users', { params: { size: 200 } }),
        apiClient.get<{ content: unknown[]; totalElements: number }>('/groups', { params: { size: 1 } }),
        apiClient.get<{ data: { name: string; status: string; healthStatus: string | null; lastSyncAt: string | null }[]; count: number }>('/connectors'),
        apiClient.get<{ entries: { principalType: string; permission: string; principalName: string }[]; count: number }>('/access-matrix'),
        apiClient.get<{ content: { eventType: string; action: string; createdAt: string; details: string }[] }>('/audit-events', { params: { size: 8 } }).catch(() => ({ data: { content: [] } })),
        apiClient.get<{ pendingCount: number }>('/requests/pending-count').catch(() => ({ data: { pendingCount: 0 } })),
      ]);

      const users = usersRes.data.content || [];
      const entries = (matrixRes.data.entries || []).filter(e => e.permission !== '(none)');
      const connectors = connectorsRes.data.data || [];

      const activeUsers = users.filter(u => u.status === 'ACTIVE').length;
      const inactiveUsers = users.filter(u => u.status === 'DISABLED' || u.status === 'LOCKED').length;
      const offboardedUsers = users.filter(u => u.status === 'OFFBOARDED').length;

      const usersWithPerms = new Set(entries.filter(e => e.principalType === 'USER').map(e => e.principalName));
      const permCountByUser = new Map<string, number>();
      entries.filter(e => e.principalType === 'USER').forEach(e => {
        permCountByUser.set(e.principalName, (permCountByUser.get(e.principalName) || 0) + 1);
      });

      // Permission breakdown by type
      const permCounts = new Map<string, number>();
      entries.forEach(e => { permCounts.set(e.permission, (permCounts.get(e.permission) || 0) + 1); });
      const permissionsByType = [...permCounts.entries()].map(([name, count]) => ({ name, count })).sort((a, b) => b.count - a.count).slice(0, 8);

      const userCoverage = users.length > 0 ? (usersWithPerms.size / users.length) * 100 : 0;
      const activeRatio = users.length > 0 ? (activeUsers / users.length) * 100 : 0;
      const score = Math.round((userCoverage * 0.4 + activeRatio * 0.3 + (connectors.length > 0 ? 80 : 0) * 0.3));

      setData({
        userCount: users.length,
        activeUsers,
        inactiveUsers,
        offboardedUsers,
        groupCount: groupsRes.data.totalElements,
        connectorCount: connectors.length,
        connectedApps: connectors.filter(c => c.status === 'CONNECTED').length,
        totalPermissions: entries.length,
        usersWithNoPermissions: users.length - usersWithPerms.size,
        usersWithExcessivePermissions: [...permCountByUser.values()].filter(c => c >= 5).length,
        governanceScore: score,
        pendingRequests: requestsRes.data.pendingCount || 0,
        recentEvents: auditRes.data.content || [],
        connectors,
        permissionsByType,
      });
    } catch (e) {
      console.error('Failed to load dashboard:', e);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-60 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!data) return null;

  const scoreColor = data.governanceScore >= 80 ? 'text-emerald-500' : data.governanceScore >= 50 ? 'text-amber-500' : 'text-red-500';
  const scoreBg = data.governanceScore >= 80 ? 'stroke-emerald-500' : data.governanceScore >= 50 ? 'stroke-amber-500' : 'stroke-red-500';

  return (
    <div className="space-y-6">
      {/* Top Stats Row */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-5">
        <StatCard icon={Users} label="Total Users" value={data.userCount} subtitle={`${data.activeUsers} active`} color="text-blue-500" bg="bg-blue-50" />
        <StatCard icon={Group} label="Groups" value={data.groupCount} subtitle="Managing access" color="text-purple-500" bg="bg-purple-50" />
        <StatCard icon={Network} label="Connected Apps" value={data.connectedApps} subtitle={`of ${data.connectorCount} registered`} color="text-emerald-500" bg="bg-emerald-50" />
        <StatCard icon={Shield} label="Permissions" value={data.totalPermissions} subtitle="Across all apps" color="text-indigo-500" bg="bg-indigo-50" />
        <StatCard icon={Activity} label="Pending Requests" value={data.pendingRequests} subtitle={data.pendingRequests > 0 ? 'Needs review' : 'All clear'} color={data.pendingRequests > 0 ? 'text-amber-500' : 'text-emerald-500'} bg={data.pendingRequests > 0 ? 'bg-amber-50' : 'bg-emerald-50'} />
      </div>

      {/* Middle Row: Governance Score + User Status + Permission Breakdown */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {/* Governance Score Ring */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-sm font-medium text-muted-foreground">Governance Score</h3>
          <div className="mt-4 flex items-center justify-center">
            <div className="relative h-36 w-36">
              <svg className="h-36 w-36 -rotate-90" viewBox="0 0 120 120">
                <circle cx="60" cy="60" r="50" fill="none" stroke="currentColor" strokeWidth="10" className="text-muted/30" />
                <circle cx="60" cy="60" r="50" fill="none" strokeWidth="10" strokeLinecap="round"
                  className={scoreBg}
                  strokeDasharray={`${data.governanceScore * 3.14} 314`}
                />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center">
                <span className={`text-3xl font-bold ${scoreColor}`}>{data.governanceScore}%</span>
                <span className="text-xs text-muted-foreground">{data.governanceScore >= 80 ? 'Healthy' : data.governanceScore >= 50 ? 'Needs work' : 'At risk'}</span>
              </div>
            </div>
          </div>
          <a href="/reports" className="mt-3 flex items-center justify-center gap-1 text-xs text-primary hover:underline">
            View full report <ArrowUpRight className="h-3 w-3" />
          </a>
        </div>

        {/* User Status Donut */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-sm font-medium text-muted-foreground">User Status</h3>
          <div className="mt-4 flex items-center gap-6">
            <div className="relative h-28 w-28 shrink-0">
              <svg className="h-28 w-28 -rotate-90" viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="40" fill="none" stroke="#10b981" strokeWidth="12"
                  strokeDasharray={`${(data.activeUsers / data.userCount) * 251} 251`} />
                <circle cx="50" cy="50" r="40" fill="none" stroke="#f59e0b" strokeWidth="12"
                  strokeDasharray={`${(data.inactiveUsers / data.userCount) * 251} 251`}
                  strokeDashoffset={`-${(data.activeUsers / data.userCount) * 251}`} />
                <circle cx="50" cy="50" r="40" fill="none" stroke="#ef4444" strokeWidth="12"
                  strokeDasharray={`${(data.offboardedUsers / data.userCount) * 251} 251`}
                  strokeDashoffset={`-${((data.activeUsers + data.inactiveUsers) / data.userCount) * 251}`} />
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-lg font-bold text-foreground">{data.userCount}</span>
              </div>
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex items-center gap-2">
                <div className="h-3 w-3 rounded-full bg-emerald-500" />
                <span className="text-foreground">Active ({data.activeUsers})</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="h-3 w-3 rounded-full bg-amber-500" />
                <span className="text-foreground">Inactive ({data.inactiveUsers})</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="h-3 w-3 rounded-full bg-red-500" />
                <span className="text-foreground">Offboarded ({data.offboardedUsers})</span>
              </div>
            </div>
          </div>
        </div>

        {/* Permission Distribution Bars */}
        <div className="rounded-lg border bg-card p-6">
          <h3 className="text-sm font-medium text-muted-foreground">Permission Distribution</h3>
          <div className="mt-4 space-y-2">
            {data.permissionsByType.map((perm) => {
              const maxCount = data.permissionsByType[0]?.count || 1;
              const pct = (perm.count / maxCount) * 100;
              const colors = ['bg-indigo-500', 'bg-blue-500', 'bg-cyan-500', 'bg-emerald-500', 'bg-amber-500', 'bg-orange-500', 'bg-rose-500', 'bg-purple-500'];
              const idx = data.permissionsByType.indexOf(perm);
              return (
                <div key={perm.name} className="flex items-center gap-2">
                  <span className="w-20 text-xs text-muted-foreground truncate">{perm.name}</span>
                  <div className="flex-1 h-4 rounded-full bg-muted/50 overflow-hidden">
                    <div className={`h-full rounded-full ${colors[idx % colors.length]} transition-all`} style={{ width: `${pct}%` }} />
                  </div>
                  <span className="text-xs font-medium text-foreground w-6 text-right">{perm.count}</span>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Bottom Row: Risk Alerts + Activity + Connector Health */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {/* Risk Alerts Summary */}
        <div className="rounded-lg border bg-card p-6">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-medium text-muted-foreground">Risk Alerts</h3>
            <a href="/reports" className="text-xs text-primary hover:underline">View all</a>
          </div>
          <div className="mt-4 space-y-3">
            {data.usersWithNoPermissions > 0 && (
              <div className="flex items-center gap-2 rounded-md bg-amber-50 p-2.5">
                <AlertTriangle className="h-4 w-4 text-amber-500 shrink-0" />
                <span className="text-xs text-amber-800">{data.usersWithNoPermissions} users without permissions</span>
              </div>
            )}
            {data.usersWithExcessivePermissions > 0 && (
              <div className="flex items-center gap-2 rounded-md bg-red-50 p-2.5">
                <AlertTriangle className="h-4 w-4 text-red-500 shrink-0" />
                <span className="text-xs text-red-800">{data.usersWithExcessivePermissions} over-privileged users</span>
              </div>
            )}
            {data.pendingRequests > 0 && (
              <div className="flex items-center gap-2 rounded-md bg-blue-50 p-2.5">
                <Activity className="h-4 w-4 text-blue-500 shrink-0" />
                <span className="text-xs text-blue-800">{data.pendingRequests} pending access requests</span>
              </div>
            )}
            {data.usersWithNoPermissions === 0 && data.usersWithExcessivePermissions === 0 && data.pendingRequests === 0 && (
              <div className="flex items-center gap-2 rounded-md bg-emerald-50 p-2.5">
                <CheckCircle className="h-4 w-4 text-emerald-500 shrink-0" />
                <span className="text-xs text-emerald-800">No critical alerts</span>
              </div>
            )}
          </div>
        </div>

        {/* Recent Activity */}
        <div className="rounded-lg border bg-card p-6">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-medium text-muted-foreground">Recent Activity</h3>
            <a href="/audit" className="text-xs text-primary hover:underline">View audit log</a>
          </div>
          <div className="mt-4 space-y-2">
            {data.recentEvents.length > 0 ? data.recentEvents.slice(0, 5).map((event, i) => (
              <div key={i} className="flex items-center gap-2 text-xs">
                <div className={`h-1.5 w-1.5 rounded-full shrink-0 ${
                  event.action === 'granted' || event.action === 'created' || event.action === 'added' ? 'bg-emerald-500' :
                  event.action === 'revoked' || event.action === 'removed' ? 'bg-red-500' :
                  'bg-blue-500'
                }`} />
                <span className="text-muted-foreground truncate flex-1">
                  {event.eventType.replace(/([A-Z])/g, ' $1').trim()} — {event.action}
                </span>
                <span className="text-muted-foreground/60 shrink-0">
                  {new Date(event.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </span>
              </div>
            )) : (
              <p className="text-xs text-muted-foreground">No recent activity</p>
            )}
          </div>
        </div>

        {/* Connector Health */}
        <div className="rounded-lg border bg-card p-6">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-medium text-muted-foreground">Applications</h3>
            <a href="/applications" className="text-xs text-primary hover:underline">Manage</a>
          </div>
          <div className="mt-4 space-y-3">
            {data.connectors.length > 0 ? data.connectors.map((c) => (
              <div key={c.name} className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className={`h-2.5 w-2.5 rounded-full ${c.healthStatus === 'HEALTHY' ? 'bg-emerald-500' : c.status === 'CONNECTED' ? 'bg-amber-500' : 'bg-gray-300'}`} />
                  <span className="text-sm text-foreground">{c.name}</span>
                </div>
                <Badge variant={c.status === 'CONNECTED' ? 'success' : 'secondary'} className="text-xs">
                  {c.status === 'CONNECTED' ? 'Connected' : c.status}
                </Badge>
              </div>
            )) : (
              <p className="text-xs text-muted-foreground">No applications connected</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function StatCard({ icon: Icon, label, value, subtitle, color, bg }: { icon: React.ElementType; label: string; value: number; subtitle: string; color: string; bg: string }) {
  return (
    <div className="rounded-lg border bg-card p-5">
      <div className="flex items-center justify-between">
        <div className={`flex h-10 w-10 items-center justify-center rounded-lg ${bg}`}>
          <Icon className={`h-5 w-5 ${color}`} />
        </div>
        <TrendingUp className="h-4 w-4 text-muted-foreground/40" />
      </div>
      <p className="mt-3 text-2xl font-bold text-foreground">{value}</p>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-0.5 text-xs text-muted-foreground/70">{subtitle}</p>
    </div>
  );
}
