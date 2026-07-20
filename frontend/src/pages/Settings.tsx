import { useState } from 'react';
import { Settings, Save, RefreshCw, AlertTriangle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';

export function SettingsPage() {
  const [activeSection, setActiveSection] = useState<'general' | 'sync' | 'defaults' | 'danger'>('general');
  const [isSaving, setIsSaving] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);

  const [settings, setSettings] = useState({
    tenantName: 'Notropia',
    tenantSlug: 'acme',
    adminEmail: 'admin@notropia.co',
    syncInterval: '30',
    syncEnabled: true,
    defaultGroupsForJoiners: ['XWikiAllGroup'],
    autoProvisionToApps: true,
    notifyOnJoiner: true,
    notifyOnLeaver: true,
    notifyOnAccessRequest: true,
    auditRetentionDays: '365',
  });

  const handleSave = async () => {
    setIsSaving(true);
    setSuccess(null);
    // TODO: POST to /api/v1/settings
    await new Promise(resolve => setTimeout(resolve, 500));
    setIsSaving(false);
    setSuccess('Settings saved successfully');
    setTimeout(() => setSuccess(null), 3000);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-foreground">Settings</h2>
          <p className="text-sm text-muted-foreground">
            Configure your OpenCrowd tenant
          </p>
        </div>
        <Button onClick={handleSave} disabled={isSaving}>
          <Save className="mr-2 h-4 w-4" />
          {isSaving ? 'Saving...' : 'Save Changes'}
        </Button>
      </div>

      {success && (
        <div className="rounded-md bg-emerald-50 p-3 text-sm text-emerald-700">{success}</div>
      )}

      <div className="flex gap-6">
        {/* Sidebar nav */}
        <nav className="w-48 space-y-1">
          {([
            { id: 'general', label: 'General' },
            { id: 'sync', label: 'Sync & Automation' },
            { id: 'defaults', label: 'Defaults & Policies' },
            { id: 'danger', label: 'Danger Zone' },
          ] as { id: typeof activeSection; label: string }[]).map((item) => (
            <button
              key={item.id}
              onClick={() => setActiveSection(item.id)}
              className={`w-full rounded-md px-3 py-2 text-left text-sm font-medium transition-colors ${
                activeSection === item.id
                  ? 'bg-primary/10 text-primary'
                  : 'text-muted-foreground hover:bg-muted hover:text-foreground'
              }`}
            >
              {item.label}
            </button>
          ))}
        </nav>

        {/* Content */}
        <div className="flex-1">
          {activeSection === 'general' && (
            <div className="space-y-6 rounded-lg border bg-card p-6">
              <h3 className="font-semibold text-foreground">General Settings</h3>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-foreground">Organization Name</label>
                  <input
                    type="text"
                    value={settings.tenantName}
                    onChange={(e) => setSettings({ ...settings, tenantName: e.target.value })}
                    className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  />
                </div>
                <div>
                  <label className="text-sm font-medium text-foreground">Tenant Slug</label>
                  <input
                    type="text"
                    value={settings.tenantSlug}
                    disabled
                    className="mt-1 h-9 w-full rounded-md border bg-muted px-3 text-sm text-muted-foreground"
                  />
                  <p className="mt-1 text-xs text-muted-foreground">Cannot be changed after creation</p>
                </div>
              </div>

              <div>
                <label className="text-sm font-medium text-foreground">Admin Email</label>
                <input
                  type="email"
                  value={settings.adminEmail}
                  onChange={(e) => setSettings({ ...settings, adminEmail: e.target.value })}
                  className="mt-1 h-9 w-full max-w-sm rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                />
                <p className="mt-1 text-xs text-muted-foreground">Notifications and alerts will be sent here</p>
              </div>

              <div>
                <label className="text-sm font-medium text-foreground">Audit Log Retention</label>
                <div className="mt-1 flex items-center gap-2">
                  <input
                    type="number"
                    value={settings.auditRetentionDays}
                    onChange={(e) => setSettings({ ...settings, auditRetentionDays: e.target.value })}
                    className="h-9 w-24 rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  />
                  <span className="text-sm text-muted-foreground">days</span>
                </div>
              </div>
            </div>
          )}

          {activeSection === 'sync' && (
            <div className="space-y-6 rounded-lg border bg-card p-6">
              <h3 className="font-semibold text-foreground">Sync & Automation</h3>

              <div className="flex items-center justify-between rounded-md border p-4">
                <div>
                  <p className="font-medium text-foreground">Automatic Sync</p>
                  <p className="text-sm text-muted-foreground">Automatically sync users, groups, and permissions with connected apps</p>
                </div>
                <label className="relative inline-flex cursor-pointer items-center">
                  <input
                    type="checkbox"
                    checked={settings.syncEnabled}
                    onChange={(e) => setSettings({ ...settings, syncEnabled: e.target.checked })}
                    className="peer sr-only"
                  />
                  <div className="peer h-6 w-11 rounded-full bg-muted after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:bg-white after:transition-all peer-checked:bg-primary peer-checked:after:translate-x-full" />
                </label>
              </div>

              <div>
                <label className="text-sm font-medium text-foreground">Sync Interval</label>
                <div className="mt-1 flex items-center gap-2">
                  <select
                    value={settings.syncInterval}
                    onChange={(e) => setSettings({ ...settings, syncInterval: e.target.value })}
                    className="h-9 rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                    disabled={!settings.syncEnabled}
                  >
                    <option value="5">Every 5 minutes</option>
                    <option value="15">Every 15 minutes</option>
                    <option value="30">Every 30 minutes</option>
                    <option value="60">Every hour</option>
                    <option value="360">Every 6 hours</option>
                    <option value="1440">Once a day</option>
                  </select>
                  {settings.syncEnabled && (
                    <Badge variant="success">Active</Badge>
                  )}
                </div>
              </div>

              <div className="flex items-center justify-between rounded-md border p-4">
                <div>
                  <p className="font-medium text-foreground">Auto-provision to connected apps</p>
                  <p className="text-sm text-muted-foreground">When a user is onboarded, automatically create their account in connected apps</p>
                </div>
                <label className="relative inline-flex cursor-pointer items-center">
                  <input
                    type="checkbox"
                    checked={settings.autoProvisionToApps}
                    onChange={(e) => setSettings({ ...settings, autoProvisionToApps: e.target.checked })}
                    className="peer sr-only"
                  />
                  <div className="peer h-6 w-11 rounded-full bg-muted after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:bg-white after:transition-all peer-checked:bg-primary peer-checked:after:translate-x-full" />
                </label>
              </div>
            </div>
          )}

          {activeSection === 'defaults' && (
            <div className="space-y-6 rounded-lg border bg-card p-6">
              <h3 className="font-semibold text-foreground">Defaults & Policies</h3>

              <div>
                <label className="text-sm font-medium text-foreground">Default Groups for New Users (Joiner)</label>
                <p className="text-xs text-muted-foreground">Users onboarded through the Joiner flow will automatically be added to these groups</p>
                <input
                  type="text"
                  value={settings.defaultGroupsForJoiners.join(', ')}
                  onChange={(e) => setSettings({ ...settings, defaultGroupsForJoiners: e.target.value.split(',').map(s => s.trim()).filter(Boolean) })}
                  className="mt-2 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  placeholder="XWikiAllGroup, Viewers"
                />
              </div>

              <div className="space-y-3">
                <p className="text-sm font-medium text-foreground">Notifications</p>
                <label className="flex items-center gap-3 text-sm text-foreground">
                  <input type="checkbox" checked={settings.notifyOnJoiner} onChange={(e) => setSettings({ ...settings, notifyOnJoiner: e.target.checked })} className="rounded border" />
                  Notify admin when a new user is onboarded
                </label>
                <label className="flex items-center gap-3 text-sm text-foreground">
                  <input type="checkbox" checked={settings.notifyOnLeaver} onChange={(e) => setSettings({ ...settings, notifyOnLeaver: e.target.checked })} className="rounded border" />
                  Notify admin when a user is offboarded
                </label>
                <label className="flex items-center gap-3 text-sm text-foreground">
                  <input type="checkbox" checked={settings.notifyOnAccessRequest} onChange={(e) => setSettings({ ...settings, notifyOnAccessRequest: e.target.checked })} className="rounded border" />
                  Notify admin on new access requests
                </label>
              </div>
            </div>
          )}

          {activeSection === 'danger' && (
            <div className="space-y-6 rounded-lg border border-destructive/30 bg-card p-6">
              <h3 className="font-semibold text-destructive">Danger Zone</h3>
              <p className="text-sm text-muted-foreground">These actions are irreversible. Proceed with caution.</p>

              <div className="space-y-4">
                <div className="flex items-center justify-between rounded-md border border-destructive/20 p-4">
                  <div>
                    <p className="font-medium text-foreground">Clear Access Matrix</p>
                    <p className="text-sm text-muted-foreground">Remove all permission entries. Does not affect connected apps.</p>
                  </div>
                  <Button variant="outline" size="sm" className="text-destructive border-destructive/30 hover:bg-destructive/10"
                    onClick={() => window.confirm('Clear all permission entries? This cannot be undone.') && alert('Would clear access matrix')}
                  >
                    Clear
                  </Button>
                </div>

                <div className="flex items-center justify-between rounded-md border border-destructive/20 p-4">
                  <div>
                    <p className="font-medium text-foreground">Reset All Data</p>
                    <p className="text-sm text-muted-foreground">Delete all users, groups, connectors, and audit logs. Start fresh.</p>
                  </div>
                  <Button variant="destructive" size="sm"
                    onClick={() => window.confirm('DELETE ALL DATA? This is completely irreversible!') && alert('Would reset all data')}
                  >
                    <AlertTriangle className="mr-1 h-3.5 w-3.5" />
                    Reset
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
