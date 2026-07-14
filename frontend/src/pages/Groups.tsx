import { useState } from 'react';
import { Group, Plus, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useQuery } from '@tanstack/react-query';
import { apiClient, type PaginatedResponse } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';
import type { Group as GroupModel } from '@/types/models';
import { CreateGroupDialog } from '@/components/groups/CreateGroupDialog';

export function GroupsPage() {
  const [page, setPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateDialog, setShowCreateDialog] = useState(false);

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: queryKeys.groups.list({ page }),
    queryFn: async () => {
      const response = await apiClient.get<PaginatedResponse<GroupModel>>('/groups', {
        params: { page, size: 20 },
      });
      return response.data;
    },
  });

  const filteredContent = data?.content.filter((group) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      group.name.toLowerCase().includes(q) ||
      group.description?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-foreground">Group Management</h2>
          <p className="text-sm text-muted-foreground">
            Organize users into groups for bulk permission assignment
          </p>
        </div>
        <Button onClick={() => setShowCreateDialog(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Create Group
        </Button>
      </div>

      {/* Search */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder="Search groups..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="h-9 w-full rounded-md border bg-background pl-9 pr-4 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
        />
      </div>

      {/* Table */}
      <div className="rounded-lg border bg-card">
        {isLoading ? (
          <div className="flex h-40 items-center justify-center">
            <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          </div>
        ) : error ? (
          <div className="flex h-40 items-center justify-center text-destructive">
            Failed to load groups.
          </div>
        ) : filteredContent && filteredContent.length > 0 ? (
          <table className="w-full">
            <thead>
              <tr className="border-b bg-muted/50 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                <th className="px-4 py-3">Group</th>
                <th className="px-4 py-3">Type</th>
                <th className="px-4 py-3">Members</th>
                <th className="px-4 py-3">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {filteredContent.map((group) => (
                <GroupRow key={group.id} group={group} />
              ))}
            </tbody>
          </table>
        ) : (
          <div className="flex h-40 flex-col items-center justify-center text-muted-foreground">
            <Group className="mb-2 h-8 w-8" />
            <p>No groups found</p>
            <p className="text-xs">Create your first group to get started</p>
          </div>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Page {data.page + 1} of {data.totalPages} ({data.totalElements} groups)
          </p>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Previous
            </Button>
            <Button variant="outline" size="sm" disabled={!data.hasNext} onClick={() => setPage((p) => p + 1)}>
              Next
            </Button>
          </div>
        </div>
      )}

      <CreateGroupDialog
        open={showCreateDialog}
        onClose={() => setShowCreateDialog(false)}
        onSuccess={() => { setShowCreateDialog(false); refetch(); }}
      />
    </div>
  );
}

function GroupRow({ group }: { group: GroupModel }) {
  return (
    <tr className="hover:bg-muted/30">
      <td className="px-4 py-3">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-xs font-medium text-primary">
            <Group className="h-4 w-4" />
          </div>
          <div>
            <p className="text-sm font-medium text-foreground">{group.name}</p>
            <p className="text-xs text-muted-foreground">{group.description || 'No description'}</p>
          </div>
        </div>
      </td>
      <td className="px-4 py-3">
        <Badge variant={group.type === 'STATIC' ? 'secondary' : 'default'}>
          {group.type}
        </Badge>
      </td>
      <td className="px-4 py-3 text-sm text-foreground">{group.memberCount}</td>
      <td className="px-4 py-3 text-sm text-muted-foreground">
        {new Date(group.createdAt).toLocaleDateString()}
      </td>
    </tr>
  );
}
