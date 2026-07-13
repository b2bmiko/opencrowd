import { useQuery } from '@tanstack/react-query';
import { apiClient, type PaginatedResponse } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';
import type { User } from '@/types/models';

interface UseUsersParams {
  page?: number;
  size?: number;
  status?: string;
  department?: string;
}

export function useUsers(params?: UseUsersParams) {
  return useQuery({
    queryKey: queryKeys.users.list(params),
    queryFn: async () => {
      const response = await apiClient.get<PaginatedResponse<User>>('/users', { params });
      return response.data;
    },
  });
}

export function useUser(id: string) {
  return useQuery({
    queryKey: queryKeys.users.detail(id),
    queryFn: async () => {
      const response = await apiClient.get<{ data: User }>(`/users/${id}`);
      return response.data.data;
    },
    enabled: !!id,
  });
}
