import { useState, useEffect } from 'react';
import { FileCheck, Plus, Pencil, Trash2, Shield, Users, Network } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';

export interface AccessProfile {
  id: string;
  name: string;
  description: string;
  permissions: string[];
  groups: string[]; // group names to assign
  applications: string[]; // which apps this profile applies to
  assignedTo: number;
  createdAt: string;
}

interface GroupOption {
  id: string;
  name: string;
}

// Persist profiles in localStorage until backend endpoint exists
const STORAGE_KEY = 'opencrowd_access_profiles';

function loadFromStorage(): AccessProfile[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) return JSON.parse(stored);
  } catch {}
  return getDefaultProfiles();
}

function saveToStorage(profiles: AccessProfile[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(profiles));
}

function getDefaultProfiles(): AccessProfile[] {
  return [
    { id: '1', name: 'Viewer', description: 'Read-only access across all resources', permissions: ['view', 'like'], groups: ['XWikiAllGroup'], applications: ['xwiki'], assignedTo: 0, createdAt: '2026-07-10T00:00:00Z' },
    { id: '2', name: 'Contributor', description: 'Can view, comment, and edit content', permissions: ['view', 'comment', 'edit', 'like'], groups: ['XWikiAllGroup', 'QF_Contributors'], applications: ['xwiki'], assignedTo: 0, createdAt: '2026-07-10T00:00:00Z' },
    { id: '3', name: 'Editor', description: 'Full content management including delete', permissions: ['view', 'comment', 'edit', 'delete', 'like'], groups: ['XWikiAllGroup', 'QF_Contributors', 'QF_Moderators'], applications: ['xwiki'], assignedTo: 0, createdAt: '2026-07-10T00:00:00Z' },
    { id: '4', name: 'Admin', description: 'Full administrative access including user management', permissions: ['view', 'comment', 'edit', 'delete', 'admin', 'script', 'register', 'programming', 'createwiki', 'like'], groups: ['XWikiAdminGroup'], applications: ['xwiki'], assignedTo: 0, createdAt: '2026-07-10T00:00:00Z' },
    { id: '5', name: 'Auditor', description: 'Read-only access with audit log visibility', permissions: ['view'], groups: ['Auditors'], applications: ['xwiki'], assignedTo: 0, createdAt: '2026-07-12T00:00:00Z' },
    { id: '6', name: 'Customs Officer', description: 'Access to customs-related spaces and projects', permissions: ['view', 'comment', 'edit'], groups: ['CustomsManagers', 'CustomsProject1Users'], applications: ['xwiki'], assignedTo: 0, createdAt: '2026-07-15T00:00:00Z' },
    { id: '7', name: 'QA Tester', description: 'UAT project access for testing', permissions: ['view', 'comment', 'edit'], groups: ['UAT_QATeam', 'UAT_TestTeam'], applications: ['xwiki'], assignedTo: 0, createdAt: '2026-07-15T00:00:00Z' },
  ];
}

export function AccessProfilesPage() {
  const [profiles, setProfiles] = useState<AccessProfile[]>(loadFromStorage());
  const [isLoading, setIsLoading] = useState(false);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [editingProfile, setEditingProfile] = useState<AccessProfile | null>(null);
  const [allGroups, setAllGroups] = useState<GroupOption[]>([]);

  useEffect(() => {
    loadGroups();
  }, []);

  useEffect(() => {
    saveToStorage(profiles);
  }, [profiles]);

  const loadGroups = async () => {
    try {
      const response = await apiClient.get<{ content: GroupOption[] }>('/groups', { params: { size: 200 } });
      setAllGroups(response.data.content || []);
    } catch {}
  };

  const handleDelete = (profile: AccessProfile) => {
    if (window.confirm(`Delete profile "${profile.name}"? This won't remove permissions already assigned.`)) {
      setProfiles(profiles.filter(p => p.id !== profile.id));
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-foreground">Access Profiles</h2>
          <p className="text-sm text-muted-foreground">
            Predefined permission templates for quick role assignment
          </p>
        </div>
        <Button onClick={() => setShowCreateDialog(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Create Profile
        </Button>
      </div>

      {/* Info banner */}
      <div className="rounded-lg border border-primary/20 bg-primary/5 p-4">
        <p className="text-sm text-foreground">
          Access Profiles bundle permissions + group memberships into reusable templates. When onboarding a new user, select a profile and they automatically get all included permissions and group assignments across connected applications.
        </p>
      </div>

      {/* Profiles Grid */}
      {isLoading ? (
        <div className="flex h-40 items-center justify-center">
          <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : profiles.length > 0 ? (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {profiles.map((profile) => (
            <div key={profile.id} className="rounded-lg border bg-card p-5">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                    <Shield className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="font-semibold text-foreground">{profile.name}</p>
                    <p className="text-xs text-muted-foreground">{profile.description}</p>
                  </div>
                </div>
              </div>

              {/* Permissions */}
              <div className="mt-4">
                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Permissions</p>
                <div className="mt-1.5 flex flex-wrap gap-1.5">
                  {profile.permissions.map((perm) => (
                    <Badge key={perm} variant="secondary" className="text-xs">
                      {perm}
                    </Badge>
                  ))}
                </div>
              </div>

              {/* Groups */}
              {profile.groups.length > 0 && (
                <div className="mt-3">
                  <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Groups</p>
                  <div className="mt-1.5 flex flex-wrap gap-1.5">
                    {profile.groups.map((group) => (
                      <Badge key={group} variant="outline" className="text-xs">
                        <Users className="mr-1 h-2.5 w-2.5" />
                        {group}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

              {/* Applications */}
              <div className="mt-3">
                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Applications</p>
                <div className="mt-1.5 flex flex-wrap gap-1.5">
                  {profile.applications.map((app) => (
                    <Badge key={app} variant="outline" className="text-xs">
                      <Network className="mr-1 h-2.5 w-2.5" />
                      {app}
                    </Badge>
                  ))}
                </div>
              </div>

              <div className="mt-4 flex items-center justify-between border-t pt-3">
                <div className="flex items-center gap-1 text-xs text-muted-foreground">
                  <Users className="h-3.5 w-3.5" />
                  <span>{profile.assignedTo} assigned</span>
                </div>
                <div className="flex gap-1">
                  <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => setEditingProfile(profile)}>
                    <Pencil className="h-3.5 w-3.5" />
                  </Button>
                  <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive" onClick={() => handleDelete(profile)}>
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="flex h-40 flex-col items-center justify-center rounded-lg border bg-card text-muted-foreground">
          <FileCheck className="mb-2 h-8 w-8" />
          <p>No access profiles defined</p>
          <p className="text-xs">Create your first profile to get started</p>
        </div>
      )}

      {/* Create/Edit Dialog */}
      {(showCreateDialog || editingProfile) && (
        <ProfileDialog
          profile={editingProfile}
          allGroups={allGroups}
          onClose={() => { setShowCreateDialog(false); setEditingProfile(null); }}
          onSave={(profile) => {
            if (editingProfile) {
              setProfiles(profiles.map(p => p.id === profile.id ? profile : p));
            } else {
              setProfiles([...profiles, { ...profile, id: String(Date.now()), assignedTo: 0, createdAt: new Date().toISOString() }]);
            }
            setShowCreateDialog(false);
            setEditingProfile(null);
          }}
        />
      )}
    </div>
  );
}

const ALL_PERMISSIONS = ['view', 'comment', 'edit', 'delete', 'admin', 'script', 'register', 'programming', 'createwiki', 'like'];

function ProfileDialog({ profile, allGroups, onClose, onSave }: { profile: AccessProfile | null; allGroups: GroupOption[]; onClose: () => void; onSave: (p: AccessProfile) => void }) {
  const [name, setName] = useState(profile?.name || '');
  const [description, setDescription] = useState(profile?.description || '');
  const [permissions, setPermissions] = useState<string[]>(profile?.permissions || []);
  const [groups, setGroups] = useState<string[]>(profile?.groups || []);

  const togglePermission = (perm: string) => {
    setPermissions(prev => prev.includes(perm) ? prev.filter(p => p !== perm) : [...prev, perm]);
  };

  const toggleGroup = (groupName: string) => {
    setGroups(prev => prev.includes(groupName) ? prev.filter(g => g !== groupName) : [...prev, groupName]);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave({
      id: profile?.id || '',
      name,
      description,
      permissions,
      groups,
      applications: ['xwiki'],
      assignedTo: profile?.assignedTo || 0,
      createdAt: profile?.createdAt || new Date().toISOString(),
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-lg rounded-lg border bg-card p-6 shadow-lg max-h-[90vh] overflow-y-auto">
        <h2 className="text-lg font-semibold text-foreground">{profile ? 'Edit Profile' : 'Create Access Profile'}</h2>
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="text-sm font-medium text-foreground">Profile Name</label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              placeholder="e.g. Customs Officer, QA Tester"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">Description</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              placeholder="What this profile grants access to"
            />
          </div>

          {/* Permissions */}
          <div>
            <label className="text-sm font-medium text-foreground">Permissions</label>
            <p className="text-xs text-muted-foreground">What this user can do in connected applications</p>
            <div className="mt-2 grid grid-cols-3 gap-2">
              {ALL_PERMISSIONS.map((perm) => (
                <label key={perm} className="flex items-center gap-2 text-sm text-foreground">
                  <input
                    type="checkbox"
                    checked={permissions.includes(perm)}
                    onChange={() => togglePermission(perm)}
                    className="rounded border"
                  />
                  {perm}
                </label>
              ))}
            </div>
          </div>

          {/* Groups */}
          <div>
            <label className="text-sm font-medium text-foreground">Group Assignments</label>
            <p className="text-xs text-muted-foreground">Groups the user will be added to when this profile is applied</p>
            <div className="mt-2 max-h-40 overflow-y-auto rounded-md border p-2 space-y-1">
              {allGroups.length > 0 ? allGroups.map((group) => (
                <label key={group.id} className="flex items-center gap-2 py-0.5 text-sm text-foreground hover:bg-muted/30 rounded px-2">
                  <input
                    type="checkbox"
                    checked={groups.includes(group.name)}
                    onChange={() => toggleGroup(group.name)}
                    className="rounded border"
                  />
                  {group.name}
                </label>
              )) : (
                <p className="text-xs text-muted-foreground p-2">No groups available — import from xWiki first</p>
              )}
            </div>
            <p className="mt-1 text-xs text-muted-foreground">{groups.length} group(s) selected</p>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={!name || permissions.length === 0}>
              {profile ? 'Update' : 'Create'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
