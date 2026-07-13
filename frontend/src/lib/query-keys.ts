/**
 * TanStack Query key factory.
 * Organized by resource for easy invalidation.
 */
export const queryKeys = {
  users: {
    all: ['users'] as const,
    list: (params?: Record<string, unknown>) => ['users', 'list', params] as const,
    detail: (id: string) => ['users', 'detail', id] as const,
  },
  groups: {
    all: ['groups'] as const,
    list: (params?: Record<string, unknown>) => ['groups', 'list', params] as const,
    detail: (id: string) => ['groups', 'detail', id] as const,
    members: (id: string) => ['groups', id, 'members'] as const,
  },
  roles: {
    all: ['roles'] as const,
    list: (params?: Record<string, unknown>) => ['roles', 'list', params] as const,
    detail: (id: string) => ['roles', 'detail', id] as const,
  },
  connectors: {
    all: ['connectors'] as const,
    list: () => ['connectors', 'list'] as const,
    detail: (id: string) => ['connectors', 'detail', id] as const,
    health: (id: string) => ['connectors', id, 'health'] as const,
  },
  audit: {
    all: ['audit'] as const,
    list: (params?: Record<string, unknown>) => ['audit', 'list', params] as const,
  },
  dashboard: {
    stats: ['dashboard', 'stats'] as const,
    health: ['dashboard', 'health'] as const,
  },
};
