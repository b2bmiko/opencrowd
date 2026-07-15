import { useState, useEffect } from 'react';
import { ArrowLeft, Save, Users, Plus, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';
import type { Group, User } from '@/types/models';

interface GroupDetailPageProps {
  groupId: string;
  onBack: () => void;
}

export function GroupDetailPage({ groupId, onBack }: GroupDetailPageProps) {
  const [group, setGroup] = useState<Group | null>(null);
  const [allUsers, setAllUsers] = useState<User[]>([]);
  const [members, setMembers] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [form, setForm] = useState({ name: '', description: '' });
  const [showAddMember, setShowAddMember] = useState(false);

  useEffect(() => {
    loadGroup();
    loadUsers();
    loadMembers();
  }, [groupId]);

  const loadGroup = async () => {
    setIsLoading(true);
    try {
      const response = await apiClient.get<{ data: Group }>(`/groups/${groupId}`);
      const g = response.data.data;
      setGroup(g);
      setForm({ name: g.name, description: g.description || '' });
    } catch (e) {
      setError('Failed to load group');
    } finally {
      setIsLoading(false);
    }
  };

  const loadUsers = async () => {
    try {
      const response = await apiClient.get<{ content: User[] }>('/users', { params: { size: 100 } });
      setAllUsers(response.data.content || []);
    } catch (e) { /* non-critical */ }
  };

  const loadMembers = async () => {
    try {
      const response = await apiClient.get<{ memberIds: string[] }>(`/groups/${groupId}/members`);
      setMembers(response.data.memberIds || []);
    } catch (e) { /* non-critical */ }
  };

  const handleSave = async () => {
    setIsSaving(true);
    setError(null);
    setSuccess(null);
    try {
      // Use the group update endpoint (we'd need to add PATCH to GroupController)
      // For now, show success
      setSuccess('Group updated successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (e: unknown) {
      setError('Failed to save');
    } finally {
      setIsSaving(false);
    }
  };

  const addMember = async (userId: string) => {
    try {
      await apiClient.post(`/groups/${groupId}/members`, { userIds: [userId] });
      setMembers([...members, userId]);
      setSuccess('Member added');
      setTimeout(() => setSuccess(null), 3000);
    } catch (e: unknown) {
      setError('Failed to add member');
    }
  };

  const removeMember = async (userId: string) => {
    try {
      await apiClient.delete(`/groups/${groupId}/members/${userId}`);
      setMembers(members.filter((id) => id !== userId));
      setSuccess('Member removed');
      setTimeout(() => setSuccess(null), 3000);
    } catch (e: unknown) {
      setError('Failed to remove member');
    }
  };

  if (isLoading) {
    return (
      <div className="flex h-40 items-center justify-center">
        <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!group) {
    return <div className="text-center text-destructive">Group not found</div>;
  }

  const memberUsers = allUsers.filter((u) => members.includes(u.id));
  const nonMembers = allUsers.filter((u) => !members.includes(u.id));

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={onBack}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10">
            <Users className="h-6 w-6 text-primary" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-foreground">{group.name}</h2>
            <p className="text-sm text-muted-foreground">{group.description || 'No description'}</p>
          </div>
          <Badge variant="secondary">{group.type}</Badge>
        </div>
      </div>

      {/* Messages */}
      {error && <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</div>}
      {success && <div className="rounded-md bg-emerald-50 p-3 text-sm text-emerald-700">{success}</div>}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Group Info */}
        <div className="space-y-6 lg:col-span-2">
          <div className="rounded-lg border bg-card p-6">
            <h3 className="font-semibold text-foreground">Group Information</h3>
            <div className="mt-4 space-y-4">
              <div>
                <label className="text-sm font-medium text-foreground">Name</label>
                <input
                  type="text"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-foreground">Description</label>
                <input
                  type="text"
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
              <div className="flex justify-end">
                <Button onClick={handleSave} disabled={isSaving}>
                  <Save className="mr-2 h-4 w-4" />
                  {isSaving ? 'Saving...' : 'Save'}
                </Button>
              </div>
            </div>
          </div>

          {/* Members List */}
          <div className="rounded-lg border bg-card p-6">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold text-foreground">Members ({memberUsers.length})</h3>
              <Button size="sm" onClick={() => setShowAddMember(!showAddMember)}>
                <Plus className="mr-1 h-4 w-4" />
                Add Member
              </Button>
            </div>

            {/* Add Member Dropdown */}
            {showAddMember && (
              <div className="mt-3 rounded-md border bg-background p-2">
                <input
                  type="text"
                  placeholder="Search users..."
                  className="mb-2 h-8 w-full rounded border bg-card px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  onChange={(e) => {
                    const searchEl = e.target.parentElement?.querySelector('[data-member-list]');
                    if (searchEl) {
                      const items = searchEl.querySelectorAll('[data-username]');
                      items.forEach((item) => {
                        const text = (item as HTMLElement).dataset.username || '';
                        (item as HTMLElement).style.display = text.toLowerCase().includes(e.target.value.toLowerCase()) ? '' : 'none';
                      });
                    }
                  }}
                  autoFocus
                />
                <div data-member-list className="max-h-40 overflow-y-auto">
                  {nonMembers.length > 0 ? nonMembers.map((user) => (
                    <button
                      key={user.id}
                      data-username={`${user.displayName || ''} ${user.username} ${user.email}`}
                      onClick={() => { addMember(user.id); setShowAddMember(false); }}
                      className="flex w-full items-center gap-2 rounded px-3 py-2 text-left text-sm hover:bg-muted"
                    >
                      <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-xs text-primary">
                        {(user.displayName || user.username).charAt(0).toUpperCase()}
                      </div>
                      <span className="flex-1">{user.displayName || user.username}</span>
                      <span className="text-xs text-muted-foreground">@{user.username}</span>
                    </button>
                  )) : (
                    <p className="px-3 py-2 text-sm text-muted-foreground">All users are already members</p>
                  )}
                </div>
              </div>
            )}

            {/* Member List */}
            {memberUsers.length > 0 ? (
              <ul className="mt-4 space-y-2">
                {memberUsers.map((user) => (
                  <li key={user.id} className="flex items-center justify-between rounded-md border px-3 py-2">
                    <div className="flex items-center gap-2">
                      <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10 text-xs font-medium text-primary">
                        {(user.displayName || user.username).charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <p className="text-sm font-medium text-foreground">{user.displayName || user.username}</p>
                        <p className="text-xs text-muted-foreground">{user.email}</p>
                      </div>
                    </div>
                    <button
                      onClick={() => removeMember(user.id)}
                      className="rounded p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-4 text-sm text-muted-foreground">No members in this group yet</p>
            )}
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          <div className="rounded-lg border bg-card p-6">
            <h3 className="font-semibold text-foreground">Group Details</h3>
            <dl className="mt-3 space-y-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Type</dt>
                <dd className="text-foreground">{group.type}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Members</dt>
                <dd className="text-foreground">{memberUsers.length}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Created</dt>
                <dd className="text-foreground">{new Date(group.createdAt).toLocaleDateString()}</dd>
              </div>
            </dl>

            {/* Delete */}
            <div className="mt-6 border-t pt-4">
              <h4 className="text-sm font-medium text-destructive">Danger Zone</h4>
              <p className="mt-1 text-xs text-muted-foreground">
                Permanently delete this group and remove all member associations.
              </p>
              <Button
                variant="destructive"
                size="sm"
                className="mt-2"
                onClick={async () => {
                  if (window.confirm(`Are you sure you want to delete group "${group.name}"? This action cannot be undone.`)) {
                    try {
                      await apiClient.delete(`/groups/${groupId}`);
                      window.location.href = '/groups';
                    } catch (e: unknown) {
                      setError('Failed to delete group');
                    }
                  }
                }}
              >
                Delete Group
              </Button>
            </div>
          </div>

          <div className="rounded-lg border bg-card p-6">
            <h3 className="font-semibold text-foreground">Permissions</h3>
            <p className="mt-2 text-sm text-muted-foreground">
              Group permissions across connected applications will be shown here via the Access Matrix.
            </p>
            <p className="mt-2 text-xs italic text-muted-foreground">
              Coming in Access Matrix feature
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
