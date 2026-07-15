import { useState, useEffect } from 'react';
import { ArrowLeft, Save, UserCog } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';
import type { User } from '@/types/models';

interface UserDetailPageProps {
  userId: string;
  onBack: () => void;
}

export function UserDetailPage({ userId, onBack }: UserDetailPageProps) {
  const [user, setUser] = useState<User | null>(null);
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
    return (
      <div className="text-center text-destructive">User not found</div>
    );
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
      {error && (
        <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</div>
      )}
      {success && (
        <div className="rounded-md bg-emerald-50 p-3 text-sm text-emerald-700">{success}</div>
      )}

      {/* Edit Form */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="space-y-4 rounded-lg border bg-card p-6">
          <h3 className="flex items-center gap-2 font-semibold text-foreground">
            <UserCog className="h-5 w-5" />
            Profile Information
          </h3>

          <div className="grid grid-cols-2 gap-4">
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

          <div>
            <label className="text-sm font-medium text-foreground">Display Name</label>
            <input
              type="text"
              value={form.displayName}
              onChange={(e) => setForm({ ...form, displayName: e.target.value })}
              className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
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

          <div>
            <label className="text-sm font-medium text-foreground">Phone</label>
            <input
              type="text"
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
              className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>

          <div className="flex justify-end pt-2">
            <Button onClick={handleSave} disabled={isSaving}>
              <Save className="mr-2 h-4 w-4" />
              {isSaving ? 'Saving...' : 'Save Changes'}
            </Button>
          </div>
        </div>

        {/* Status & Metadata */}
        <div className="space-y-4">
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
                  <Button variant="destructive" size="sm" onClick={() => handleStatusChange('OFFBOARDED')}>Offboard</Button>
                </>
              )}
              {user.status === 'DISABLED' && (
                <>
                  <Button variant="outline" size="sm" onClick={() => handleStatusChange('ACTIVE')}>Re-enable</Button>
                  <Button variant="destructive" size="sm" onClick={() => handleStatusChange('OFFBOARDED')}>Offboard</Button>
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
            </div>
          </div>

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
                <dd className="text-foreground">{new Date(user.createdAt).toLocaleString()}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Last Login</dt>
                <dd className="text-foreground">{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : 'Never'}</dd>
              </div>
            </dl>
          </div>
        </div>
      </div>
    </div>
  );
}
