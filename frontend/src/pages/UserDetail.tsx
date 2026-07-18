import { useState, useEffect } from 'react';
import { ArrowLeft, Save, UserCog, Users, Network, MapPin, UserMinus, UserPlus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';
import type { User, Group } from '@/types/models';

interface UserDetailPageProps {
  userId: string;
  onBack: () => void;
}

export function UserDetailPage({ userId, onBack }: UserDetailPageProps) {
  const [user, setUser] = useState<User | null>(null);
  const [groups, setGroups] = useState<Group[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    displayName: '',
    department: '',
    title: '',
    phone: '',
  });

  useEffect(() => {
    loadUser();
    loadGroups();
  }, [userId]);

  const loadUser = async () => {
    setIsLoading(true);
    try {
      const response = await apiClient.get<{ data: User }>(`/users/${userId}`);
      const u = response.data.data;
      setUser(u);
      setForm({
        firstName: u.firstName || '',
        lastName: u.lastName || '',
        displayName: u.displayName || '',
        department: u.department || '',
        title: u.title || '',
        phone: u.phone || '',
      });
    } catch (e) {
      setError('Failed to load user');
    } finally {
      setIsLoading(false);
    }
  };

  const loadGroups = async () => {
    try {
      const response = await apiClient.get<{ content: Group[] }>('/groups', { params: { size: 100 } });
      setGroups(response.data.content || []);
    } catch (e) {
      // Groups loading is non-critical
    }
  };

  const handleSave = async () => {
    setIsSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await apiClient.patch<{ data: User }>(`/users/${userId}`, {
        firstName: form.firstName || null,
        lastName: form.lastName || null,
        displayName: form.displayName || null,
        department: form.department || null,
        title: form.title || null,
        phone: form.phone || null,
      });
      setUser(response.data.data);
      setSuccess('User updated successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (e: unknown) {
      setError(e && typeof e === 'object' && 'message' in e ? (e as { message: string }).message : 'Failed to save');
    } finally {
      setIsSaving(false);
    }
  };

  const handleStatusChange = async (newStatus: string) => {
    try {
      const response = await apiClient.put<{ data: User }>(`/users/${userId}/status`, { status: newStatus });
      setUser(response.data.data);
      setSuccess(`Status changed to ${newStatus}`);
      setTimeout(() => setSuccess(null), 3000);
    } catch (e: unknown) {
      setError(e && typeof e === 'object' && 'message' in e ? (e as { message: string }).message : 'Failed to change status');
    }
  };

  if (isLoading) {
    return (
      <div className="flex h-40 items-center justify-center">
        <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!user) {
    return <div className="text-center text-destructive">User not found</div>;
  }

  const statusVariant = {
    ACTIVE: 'success',
    DISABLED: 'secondary',
    LOCKED: 'destructive',
    PENDING: 'warning',
    OFFBOARDED: 'outline',
  } as const;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={onBack}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-lg font-medium text-primary">
              {(user.displayName || user.username).charAt(0).toUpperCase()}
            </div>
            <div>
              <h2 className="text-xl font-bold text-foreground">
                {user.displayName || user.username}
              </h2>
              <p className="text-sm text-muted-foreground">@{user.username} · {user.email}</p>
            </div>
            <Badge variant={statusVariant[user.status as keyof typeof statusVariant] || 'outline'}>
              {user.status}
            </Badge>
          </div>
        </div>
      </div>

      {/* Messages */}
      {error && <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</div>}
      {success && <div className="rounded-md bg-emerald-50 p-3 text-sm text-emerald-700">{success}</div>}

      {/* Main Content */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Profile Form - 2 columns */}
        <div className="space-y-6 lg:col-span-2">
          {/* Personal Information */}
          <div className="rounded-lg border bg-card p-6">
            <h3 className="flex items-center gap-2 font-semibold text-foreground">
              <UserCog className="h-5 w-5" />
              Personal Information
            </h3>

            <div className="mt-4 grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-foreground">First Name</label>
                <input
                  type="text"
                  value={form.firstName}
                  onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                  className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-foreground">Last Name</label>
                <input
                  type="text"
                  value={form.lastName}
                  onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                  className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
            </div>

            <div className="mt-4">
              <label className="text-sm font-medium text-foreground">Display Name</label>
              <input
                type="text"
                value={form.displayName}
                onChange={(e) => setForm({ ...form, displayName: e.target.value })}
                className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              />
            </div>

            <div className="mt-4">
              <label className="text-sm font-medium text-foreground">Phone</label>
              <input
                type="text"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              />
            </div>
          </div>

          {/* Organization */}
          <div className="rounded-lg border bg-card p-6">
            <h3 className="flex items-center gap-2 font-semibold text-foreground">
              <MapPin className="h-5 w-5" />
              Organization & Location
            </h3>

            <div className="mt-4 grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-foreground">Department</label>
                <input
                  type="text"
                  value={form.department}
                  onChange={(e) => setForm({ ...form, department: e.target.value })}
                  className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-foreground">Job Title</label>
                <input
                  type="text"
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
            </div>

            <p className="mt-4 text-xs text-muted-foreground italic">
              Additional fields (country, address, postal code) will be available via custom field configuration in Settings.
            </p>
          </div>

          {/* Save Button */}
          <div className="flex justify-end">
            <Button onClick={handleSave} disabled={isSaving}>
              <Save className="mr-2 h-4 w-4" />
              {isSaving ? 'Saving...' : 'Save Changes'}
            </Button>
          </div>
        </div>

        {/* Right sidebar */}
        <div className="space-y-6">
          {/* Lifecycle */}
          <div className="rounded-lg border bg-card p-6">
            <h3 className="font-semibold text-foreground">Lifecycle Status</h3>
            <p className="mt-1 text-sm text-muted-foreground">
              Current: <strong>{user.status}</strong>
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              {user.status === 'ACTIVE' && (
                <>
                  <Button variant="outline" size="sm" onClick={() => handleStatusChange('DISABLED')}>Disable</Button>
                  <Button variant="outline" size="sm" onClick={() => handleStatusChange('LOCKED')}>Lock</Button>
                  <OffboardButton userId={userId} username={user.username} onSuccess={loadUser} setError={setError} />
                </>
              )}
              {user.status === 'DISABLED' && (
                <>
                  <Button variant="outline" size="sm" onClick={() => handleStatusChange('ACTIVE')}>Re-enable</Button>
                  <OffboardButton userId={userId} username={user.username} onSuccess={loadUser} setError={setError} />
                </>
              )}
              {user.status === 'LOCKED' && (
                <>
                  <Button variant="outline" size="sm" onClick={() => handleStatusChange('ACTIVE')}>Unlock</Button>
                  <Button variant="outline" size="sm" onClick={() => handleStatusChange('DISABLED')}>Disable</Button>
                </>
              )}
              {user.status === 'PENDING' && (
                <Button variant="outline" size="sm" onClick={() => handleStatusChange('ACTIVE')}>Activate</Button>
              )}
              {user.status === 'OFFBOARDED' && (
                <ReactivateButton userId={userId} username={user.username} groups={groups} onSuccess={loadUser} setError={setError} />
              )}
            </div>

            {/* Delete */}
            <div className="mt-6 border-t pt-4">
              <h4 className="text-sm font-medium text-destructive">Danger Zone</h4>
              <p className="mt-1 text-xs text-muted-foreground">
                Permanently delete this user. This cannot be undone.
              </p>
              <Button
                variant="destructive"
                size="sm"
                className="mt-2"
                onClick={async () => {
                  if (window.confirm(`Are you sure you want to delete "${user.username}"? This action cannot be undone.`)) {
                    try {
                      await apiClient.delete(`/users/${userId}`);
                      window.location.href = '/identity';
                    } catch (e: unknown) {
                      setError('Failed to delete user');
                    }
                  }
                }}
              >
                Delete User
              </Button>
            </div>
          </div>

          {/* Group Memberships */}
          <div className="rounded-lg border bg-card p-6">
            <h3 className="flex items-center gap-2 font-semibold text-foreground">
              <Users className="h-5 w-5" />
              Group Memberships
            </h3>
            {groups.length > 0 ? (
              <ul className="mt-3 space-y-2">
                {groups.slice(0, 10).map((group) => (
                  <li key={group.id} className="flex items-center justify-between text-sm">
                    <span className="text-foreground">{group.name}</span>
                    <Badge variant="secondary">{group.type}</Badge>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-3 text-sm text-muted-foreground">Not a member of any groups</p>
            )}
            <p className="mt-3 text-xs text-muted-foreground italic">
              Group membership management coming soon
            </p>
          </div>

          {/* Connected Accounts */}
          <div className="rounded-lg border bg-card p-6">
            <h3 className="flex items-center gap-2 font-semibold text-foreground">
              <Network className="h-5 w-5" />
              Connected Accounts
            </h3>
            <div className="mt-3 space-y-2 text-sm">
              {user.externalId ? (
                <div className="flex items-center justify-between">
                  <span className="text-muted-foreground">xWiki</span>
                  <Badge variant="success">Linked</Badge>
                </div>
              ) : (
                <div className="flex items-center justify-between">
                  <span className="text-muted-foreground">xWiki</span>
                  <Badge variant="outline">Not linked</Badge>
                </div>
              )}
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">OpenProject</span>
                <Badge variant="outline">Not linked</Badge>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Nextcloud</span>
                <Badge variant="outline">Not linked</Badge>
              </div>
            </div>
          </div>

          {/* Account Details */}
          <div className="rounded-lg border bg-card p-6">
            <h3 className="font-semibold text-foreground">Account Details</h3>
            <dl className="mt-3 space-y-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Username</dt>
                <dd className="font-mono text-foreground">{user.username}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Email</dt>
                <dd className="text-foreground">{user.email}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Created</dt>
                <dd className="text-foreground">{new Date(user.createdAt).toLocaleDateString()}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Last Login</dt>
                <dd className="text-foreground">{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleDateString() : 'Never'}</dd>
              </div>
              {user.externalId && (
                <div className="flex justify-between">
                  <dt className="text-muted-foreground">External ID</dt>
                  <dd className="font-mono text-xs text-foreground">{user.externalId}</dd>
                </div>
              )}
            </dl>
          </div>
        </div>
      </div>
    </div>
  );
}


// --- JML Components ---

function OffboardButton({ userId, username, onSuccess, setError }: { userId: string; username: string; onSuccess: () => void; setError: (e: string) => void }) {
  const [showDialog, setShowDialog] = useState(false);
  const [preview, setPreview] = useState<{ groupsToRemove: string[]; connectorsToDeprovision: string[] } | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<{ success: boolean; groupsRemoved: string[]; deprovisioning: { connector: string; action: string; message: string }[]; errors: string[] } | null>(null);

  const loadPreview = async () => {
    setShowDialog(true);
    try {
      const response = await apiClient.post<{ groupsToRemove: string[]; connectorsToDeprovision: string[] }>(`/lifecycle/leaver/${userId}/preview`);
      setPreview(response.data);
    } catch (e) {
      setPreview({ groupsToRemove: [], connectorsToDeprovision: [] });
    }
  };

  const executeLeaver = async () => {
    setIsLoading(true);
    try {
      const response = await apiClient.post(`/lifecycle/leaver/${userId}`);
      setResult(response.data as typeof result);
    } catch (e: unknown) {
      setError('Offboarding failed');
      setShowDialog(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    setShowDialog(false);
    setPreview(null);
    setResult(null);
    if (result?.success) onSuccess();
  };

  return (
    <>
      <Button variant="destructive" size="sm" onClick={loadPreview}>
        <UserMinus className="mr-1 h-3.5 w-3.5" />
        Offboard
      </Button>

      {showDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-md rounded-lg border bg-card p-6 shadow-lg">
            {!result ? (
              <>
                <h3 className="text-lg font-semibold text-foreground">Offboard User</h3>
                <p className="mt-2 text-sm text-muted-foreground">
                  This will fully offboard <strong>{username}</strong> by:
                </p>

                {preview ? (
                  <div className="mt-4 space-y-3">
                    <div>
                      <p className="text-sm font-medium text-foreground">Groups to remove from:</p>
                      {preview.groupsToRemove.length > 0 ? (
                        <ul className="mt-1 list-inside list-disc text-sm text-muted-foreground">
                          {preview.groupsToRemove.map((g) => <li key={g}>{g}</li>)}
                        </ul>
                      ) : (
                        <p className="mt-1 text-sm text-muted-foreground">No group memberships</p>
                      )}
                    </div>
                    <div>
                      <p className="text-sm font-medium text-foreground">Applications to deprovision from:</p>
                      {preview.connectorsToDeprovision.length > 0 ? (
                        <ul className="mt-1 list-inside list-disc text-sm text-muted-foreground">
                          {preview.connectorsToDeprovision.map((c) => <li key={c}>{c}</li>)}
                        </ul>
                      ) : (
                        <p className="mt-1 text-sm text-muted-foreground">No connected applications</p>
                      )}
                    </div>
                  </div>
                ) : (
                  <div className="mt-4 flex justify-center">
                    <div className="h-5 w-5 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                  </div>
                )}

                <div className="mt-6 flex justify-end gap-3">
                  <Button variant="outline" size="sm" onClick={handleClose}>Cancel</Button>
                  <Button variant="destructive" size="sm" onClick={executeLeaver} disabled={isLoading || !preview}>
                    {isLoading ? 'Offboarding...' : 'Confirm Offboarding'}
                  </Button>
                </div>
              </>
            ) : (
              <>
                <h3 className="text-lg font-semibold text-foreground">
                  {result.success ? 'Offboarding Complete' : 'Offboarding Completed with Errors'}
                </h3>
                <div className="mt-4 space-y-3">
                  {result.groupsRemoved.length > 0 && (
                    <div>
                      <p className="text-sm font-medium text-foreground">Removed from groups:</p>
                      <p className="text-sm text-muted-foreground">{result.groupsRemoved.join(', ')}</p>
                    </div>
                  )}
                  {result.deprovisioning.length > 0 && (
                    <div>
                      <p className="text-sm font-medium text-foreground">Connector results:</p>
                      <ul className="mt-1 space-y-1 text-sm">
                        {result.deprovisioning.map((d, i) => (
                          <li key={i} className={d.action === 'deprovisioned' ? 'text-emerald-600' : 'text-amber-600'}>
                            {d.connector}: {d.action} — {d.message}
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                  {result.errors.length > 0 && (
                    <div className="rounded-md bg-destructive/10 p-3">
                      <p className="text-sm font-medium text-destructive">Errors:</p>
                      <ul className="mt-1 list-inside list-disc text-sm text-destructive">
                        {result.errors.map((e, i) => <li key={i}>{e}</li>)}
                      </ul>
                    </div>
                  )}
                </div>
                <div className="mt-6 flex justify-end">
                  <Button size="sm" onClick={handleClose}>Done</Button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </>
  );
}

function ReactivateButton({ userId, username, groups, onSuccess, setError }: { userId: string; username: string; groups: Group[]; onSuccess: () => void; setError: (e: string) => void }) {
  const [showDialog, setShowDialog] = useState(false);
  const [selectedGroups, setSelectedGroups] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const handleReactivate = async () => {
    setIsLoading(true);
    try {
      await apiClient.post(`/lifecycle/reactivate/${userId}`, { groupIds: selectedGroups });
      setShowDialog(false);
      onSuccess();
    } catch (e: unknown) {
      setError('Reactivation failed');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <Button variant="outline" size="sm" onClick={() => setShowDialog(true)}>
        <UserPlus className="mr-1 h-3.5 w-3.5" />
        Reactivate
      </Button>

      {showDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-md rounded-lg border bg-card p-6 shadow-lg">
            <h3 className="text-lg font-semibold text-foreground">Reactivate User</h3>
            <p className="mt-2 text-sm text-muted-foreground">
              Re-enable <strong>{username}</strong> and re-provision to connected applications.
            </p>

            {groups.length > 0 && (
              <div className="mt-4">
                <p className="text-sm font-medium text-foreground">Assign to groups (optional):</p>
                <div className="mt-2 max-h-40 overflow-y-auto space-y-1">
                  {groups.map((group) => (
                    <label key={group.id} className="flex items-center gap-2 text-sm text-foreground">
                      <input
                        type="checkbox"
                        checked={selectedGroups.includes(group.id)}
                        onChange={(e) => {
                          if (e.target.checked) setSelectedGroups([...selectedGroups, group.id]);
                          else setSelectedGroups(selectedGroups.filter((id) => id !== group.id));
                        }}
                        className="rounded border"
                      />
                      {group.name}
                    </label>
                  ))}
                </div>
              </div>
            )}

            <div className="mt-6 flex justify-end gap-3">
              <Button variant="outline" size="sm" onClick={() => setShowDialog(false)}>Cancel</Button>
              <Button size="sm" onClick={handleReactivate} disabled={isLoading}>
                {isLoading ? 'Reactivating...' : 'Reactivate'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
