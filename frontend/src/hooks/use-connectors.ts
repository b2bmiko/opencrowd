import { useQuery } from '@tanstack/react-query';
import { apiClient, type ListResponse } from '@/lib/api-client';
import { queryKeys } from '@/lib/query-keys';
import type { Connector } from '@/types/models';

export function useConnectors() {
  return useQuery({
    queryKey: queryKeys.connectors.list(),
    queryFn: async () => {
      const response = await apiClient.get<ListResponse<Connector>>('/connectors');
      return response.data;
    },
  });
}

export function useConnector(id: string) {
  return useQuery({
    queryKey: queryKeys.connectors.detail(id),
    queryFn: async () => {
      const response = await apiClient.get<{ data: Connector }>(`/connectors/${id}`);
      return response.data.data;
    },
    enabled: !!id,
  });
}
