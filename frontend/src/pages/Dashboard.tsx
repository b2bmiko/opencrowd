import { Users, Group, Network, Shield } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string;
  change?: string;
  icon: React.ElementType;
}

function StatCard({ title, value, change, icon: Icon }: StatCardProps) {
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

export function DashboardPage() {
  return (
    <div className="space-y-6">
      {/* Stats Grid */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard title="Users" value="4,782" change="+12.5%" icon={Users} />
        <StatCard title="Groups" value="286" change="+8.1%" icon={Group} />
        <StatCard title="Connected Apps" value="14" change="+2" icon={Network} />
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
            Chart placeholder — will be implemented in Milestone 1
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-foreground">Applications Health</h3>
            <span className="text-sm text-emerald-600">Live</span>
          </div>
          <div className="mt-4 space-y-3">
            <HealthRow name="xWiki" status="healthy" />
            <HealthRow name="OpenProject" status="warning" />
            <HealthRow name="Nextcloud" status="healthy" />
            <HealthRow name="GitLab" status="healthy" />
            <HealthRow name="Mattermost" status="review" />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="rounded-lg border bg-card p-6">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-foreground">Unified Access Matrix</h3>
            <span className="text-sm text-amber-600">Review-ready</span>
          </div>
          <div className="mt-4 flex h-24 items-center justify-center text-muted-foreground">
            Matrix summary — will be implemented in Milestone 2
          </div>
        </div>

        <div className="rounded-lg border bg-card p-6">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-foreground">Risk Alerts</h3>
            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-destructive text-xs text-destructive-foreground">
              8
            </span>
          </div>
          <div className="mt-4 space-y-2 text-sm text-muted-foreground">
            <p>12 Dormant admin accounts</p>
            <p>5 Users with excessive permissions</p>
            <p>3 Orphaned groups</p>
          </div>
        </div>
      </div>
    </div>
  );
}

interface HealthRowProps {
  name: string;
  status: 'healthy' | 'warning' | 'review';
}

function HealthRow({ name, status }: HealthRowProps) {
  const statusColors = {
    healthy: 'bg-emerald-100 text-emerald-800',
    warning: 'bg-amber-100 text-amber-800',
    review: 'bg-purple-100 text-purple-800',
  };

  return (
    <div className="flex items-center justify-between">
      <span className="text-sm text-foreground">{name}</span>
      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusColors[status]}`}>
        {status.charAt(0).toUpperCase() + status.slice(1)}
      </span>
    </div>
  );
}
