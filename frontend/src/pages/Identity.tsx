import { useState } from 'react';
import { Users, Plus, Search, Filter, UserPlus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useUsers } from '@/hooks/use-users';
import { CreateUserDialog } from '@/components/users/CreateUserDialog';
import { apiClient } from '@/lib/api-client';
import type { User } from '@/types/models';

export function IdentityPage() {
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string | undefined>();
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showOnboardDialog, setShowOnboardDialog] = useState(false);

  const { data, isLoading, error, refetch } = useUsers({
    page,
    size: 20,
    status: statusFilter,
  });

  const filteredContent = data?.content.filter((user) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      user.username.toLowerCase().includes(q) ||
      user.email.toLowerCase().includes(q) ||
      user.firstName?.toLowerCase().includes(q) ||
      user.lastName?.toLowerCase().includes(q) ||
      user.department?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-foreground">Identity Management</h2>
          <p className="text-sm text-muted-foreground">
            Manage user identities across your organization
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setShowCreateDialog(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Create User
          </Button>
          <Button onClick={() => setShowOnboardDialog(true)}>
            <UserPlus className="mr-2 h-4 w-4" />
            Onboard User
          </Button>
        </div>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search by name, email, department..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="h-9 w-full rounded-md border bg-background pl-9 pr-4 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4 text-muted-foreground" />
          <select
            value={statusFilter || ''}
            onChange={(e) => setStatusFilter(e.target.value || undefined)}
            className="h-9 rounded-md border bg-background px-3 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="DISABLED">Disabled</option>
            <option value="LOCKED">Locked</option>
            <option value="PENDING">Pending</option>
            <option value="OFFBOARDED">Offboarded</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="rounded-lg border bg-card">
        {isLoading ? (
          <div className="flex h-40 items-center justify-center">
            <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : error ? (
          <div className="flex h-40 items-center justify-center text-destructive">
            Failed to load users. Please try again.
          </div>
        ) : filteredContent && filteredContent.length > 0 ? (
          <table className="w-full">
            <thead>
              <tr className="border-b bg-muted/50 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                <th className="px-4 py-3">User</th>
                <th className="px-4 py-3">Email</th>
                <th className="px-4 py-3">Source</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {filteredContent.map((user) => (
                <UserRow key={user.id} user={user} />
              ))}
            </tbody>
          </table>
        ) : (
          <div className="flex h-40 flex-col items-center justify-center text-muted-foreground">
            <Users className="mb-2 h-8 w-8" />
            <p>No users found</p>
            <p className="text-xs">Create your first user to get started</p>
          </div>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Showing page {data.page + 1} of {data.totalPages} ({data.totalElements} total users)
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={!data.hasNext}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      {/* Create User Dialog */}
      <CreateUserDialog
        open={showCreateDialog}
        onClose={() => setShowCreateDialog(false)}
        onSuccess={() => {
          setShowCreateDialog(false);
          refetch();
        }}
      />

      {/* Onboard User Dialog */}
      {showOnboardDialog && (
        <OnboardUserDialog
          onClose={() => setShowOnboardDialog(false)}
          onSuccess={() => { setShowOnboardDialog(false); refetch(); }}
        />
      )}
    </div>
  );
}

function UserRow({ user }: { user: User }) {
  const statusVariant = {
    ACTIVE: 'success',
    DISABLED: 'secondary',
    LOCKED: 'destructive',
    PENDING: 'warning',
    OFFBOARDED: 'outline',
  } as const;

  const displayName = user.displayName || `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username;

  return (
    <tr className="cursor-pointer hover:bg-muted/30" onClick={() => window.location.href = `/identity/${user.id}`}>
      <td className="px-4 py-3">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-xs font-medium text-primary">
            {displayName.charAt(0).toUpperCase()}
          </div>
          <div>
            <p className="text-sm font-medium text-foreground">{displayName}</p>
            <p className="text-xs text-muted-foreground">@{user.username}</p>
          </div>
        </div>
      </td>
      <td className="px-4 py-3 text-sm text-foreground">{user.email}</td>
      <td className="px-4 py-3">
        {user.externalId ? (
          <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
            user.externalId.startsWith('xwiki') ? 'bg-blue-100 text-blue-800' :
            user.externalId.startsWith('openproject') ? 'bg-emerald-100 text-emerald-800' :
            'bg-gray-100 text-gray-800'
          }`}>
            {user.externalId.split(':')[0]}
          </span>
        ) : (
          <span className="inline-flex rounded-full px-2 py-0.5 text-xs font-medium bg-purple-100 text-purple-800">local</span>
        )}
      </td>
      <td className="px-4 py-3">
        <Badge variant={statusVariant[user.status as keyof typeof statusVariant] || 'outline'}>
          {user.status}
        </Badge>
      </td>
      <td className="px-4 py-3 text-sm text-muted-foreground">
        {new Date(user.createdAt).toLocaleDateString()}
      </td>
    </tr>
  );
}


interface GroupOption {
  id: string;
  name: string;
}

function OnboardUserDialog({ onClose, onSuccess }: { onClose: () => void; onSuccess: () => void }) {
  const [step, setStep] = useState<'details' | 'profile' | 'result'>('details');
  const [form, setForm] = useState({ username: '', email: '', firstName: '', lastName: '', department: '', title: '' });
  const [groups, setGroups] = useState<GroupOption[]>([]);
  const [selectedGroups, setSelectedGroups] = useState<string[]>([]);
  const [selectedProfile, setSelectedProfile] = useState<string | null>(null);
  const [profiles, setProfiles] = useState<{ id: string; name: string; description: string; groups: string[]; permissions: string[] }[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [result, setResult] = useState<{ success: boolean; username: string; groupsAssigned: string[]; provisioning: { connector: string; action: string; message: string }[]; errors: string[] } | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Load groups and profiles when moving to step 2
  const loadProfilesAndGroups = async () => {
    try {
      const response = await apiClient.get<{ content: GroupOption[] }>('/groups', { params: { size: 200 } });
      setGroups(response.data.content || []);
    } catch (e) {
      setGroups([]);
    }
    // Load profiles from localStorage
    try {
      const stored = localStorage.getItem('opencrowd_access_profiles');
      if (stored) setProfiles(JSON.parse(stored));
    } catch {}
    setStep('profile');
  };

  const applyProfile = (profileId: string) => {
    const profile = profiles.find(p => p.id === profileId);
    if (profile) {
      setSelectedProfile(profileId);
      // Map profile group names to group IDs
      const groupIds = groups.filter(g => profile.groups.includes(g.name)).map(g => g.id);
      setSelectedGroups(groupIds);
    }
  };

  const handleSubmit = async () => {
    setIsSubmitting(true);
    setError(null);
    try {
      const response = await apiClient.post('/lifecycle/joiner', {
        username: form.username,
        email: form.email,
        firstName: form.firstName || null,
        lastName: form.lastName || null,
        department: form.department || null,
        title: form.title || null,
        groupIds: selectedGroups,
      });
      setResult(response.data as typeof result);
      setStep('result');
    } catch (e: unknown) {
      setError(e && typeof e === 'object' && 'message' in e ? (e as { message: string }).message : 'Onboarding failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-lg rounded-lg border bg-card p-6 shadow-lg max-h-[90vh] overflow-y-auto">
        {step === 'details' && (
          <>
            <h2 className="text-lg font-semibold text-foreground">Onboard New User</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Create user, assign an access profile, and provision across connected applications.
            </p>

            {error && <div className="mt-3 rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</div>}

            <div className="mt-4 space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-sm font-medium text-foreground">Username *</label>
                  <input
                    type="text"
                    required
                    value={form.username}
                    onChange={(e) => setForm({ ...form, username: e.target.value })}
                    className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                    placeholder="jdoe"
                  />
                </div>
                <div>
                  <label className="text-sm font-medium text-foreground">Email *</label>
                  <input
                    type="email"
                    required
                    value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                    className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                    placeholder="jane@example.com"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
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
              <div className="grid grid-cols-2 gap-3">
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
            </div>

            <div className="mt-6 flex justify-end gap-3">
              <Button variant="outline" size="sm" onClick={onClose}>Cancel</Button>
              <Button size="sm" onClick={loadProfilesAndGroups} disabled={!form.username || !form.email}>
                Next: Select Profile
              </Button>
            </div>
          </>
        )}

        {step === 'profile' && (
          <>
            <h2 className="text-lg font-semibold text-foreground">Select Access Profile</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Choose a profile for <strong>{form.firstName || form.username}</strong> to auto-assign groups and permissions. Or skip to assign manually.
            </p>

            {/* Profile Cards */}
            {profiles.length > 0 && (
              <div className="mt-4 space-y-2 max-h-48 overflow-y-auto">
                {profiles.map((profile) => (
                  <label
                    key={profile.id}
                    className={`flex cursor-pointer items-start gap-3 rounded-md border p-3 transition-colors ${
                      selectedProfile === profile.id ? 'border-primary bg-primary/5' : 'hover:bg-muted/30'
                    }`}
                  >
                    <input
                      type="radio"
                      name="profile"
                      checked={selectedProfile === profile.id}
                      onChange={() => applyProfile(profile.id)}
                      className="mt-1"
                    />
                    <div className="flex-1">
                      <p className="text-sm font-medium text-foreground">{profile.name}</p>
                      <p className="text-xs text-muted-foreground">{profile.description}</p>
                      <div className="mt-1 flex flex-wrap gap-1">
                        {profile.groups.slice(0, 3).map(g => (
                          <span key={g} className="inline-block rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">{g}</span>
                        ))}
                        {profile.groups.length > 3 && <span className="text-xs text-muted-foreground">+{profile.groups.length - 3} more</span>}
                      </div>
                    </div>
                  </label>
                ))}
              </div>
            )}

            {/* Manual group selection */}
            <div className="mt-4">
              <p className="text-sm font-medium text-foreground">
                {selectedProfile ? 'Groups (from profile):' : 'Or select groups manually:'}
              </p>
              <div className="mt-2 max-h-40 overflow-y-auto space-y-1 rounded-md border p-2">
                {groups.length > 0 ? groups.map((group) => (
                  <label key={group.id} className="flex items-center gap-2 py-0.5 text-sm text-foreground hover:bg-muted/30 rounded px-2">
                    <input
                      type="checkbox"
                      checked={selectedGroups.includes(group.id)}
                      onChange={(e) => {
                        if (e.target.checked) setSelectedGroups([...selectedGroups, group.id]);
                        else { setSelectedGroups(selectedGroups.filter((id) => id !== group.id)); setSelectedProfile(null); }
                      }}
                      className="rounded border"
                    />
                    {group.name}
                  </label>
                )) : (
                  <p className="text-sm text-muted-foreground p-2">No groups available</p>
                )}
              </div>
              <p className="mt-1 text-xs text-muted-foreground">{selectedGroups.length} group(s) selected</p>
            </div>

            <div className="mt-6 flex justify-between">
              <Button variant="outline" size="sm" onClick={() => setStep('details')}>Back</Button>
              <div className="flex gap-3">
                <Button variant="outline" size="sm" onClick={onClose}>Cancel</Button>
                <Button size="sm" onClick={handleSubmit} disabled={isSubmitting}>
                  {isSubmitting ? 'Onboarding...' : 'Onboard User'}
                </Button>
              </div>
            </div>
          </>
        )}

        {step === 'result' && result && (
          <>
            <h2 className="text-lg font-semibold text-foreground">
              {result.success ? 'Onboarding Complete' : 'Onboarding Completed with Errors'}
            </h2>

            <div className="mt-4 space-y-3">
              <div className="rounded-md bg-emerald-50 p-3">
                <p className="text-sm text-emerald-700">User <strong>{result.username}</strong> created successfully.</p>
              </div>

              {result.groupsAssigned.length > 0 && (
                <div>
                  <p className="text-sm font-medium text-foreground">Groups assigned:</p>
                  <p className="text-sm text-muted-foreground">{result.groupsAssigned.join(', ')}</p>
                </div>
              )}

              {result.provisioning.length > 0 && (
                <div>
                  <p className="text-sm font-medium text-foreground">Provisioning:</p>
                  <ul className="mt-1 space-y-1 text-sm">
                    {result.provisioning.map((p, i) => (
                      <li key={i} className={p.action === 'provisioned' ? 'text-emerald-600' : p.action === 'failed' ? 'text-red-600' : 'text-amber-600'}>
                        {p.connector}: {p.action} {p.message && `— ${p.message}`}
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
              <Button size="sm" onClick={onSuccess}>Done</Button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
