import { useQuery } from '@tanstack/react-query';
import { accessApi } from '../api/services';

export function useCurrentUser() {
  return useQuery({
    queryKey: ['me'],
    queryFn: accessApi.me,
    staleTime: 5 * 60 * 1000,
  });
}

export function useDataScope() {
  return useQuery({
    queryKey: ['me', 'data-scope'],
    queryFn: accessApi.dataScope,
    staleTime: 5 * 60 * 1000,
  });
}
