import type { ReactNode } from 'react';
import { ForbiddenState } from './AppState';
import { useCurrentUser } from '../hooks/useCurrentUser';

function hasAccess(userPermissions: string[] = [], required?: string) {
  return !required || userPermissions.includes('*') || userPermissions.includes(required);
}

export function PermissionGate({ permission, children }: { permission?: string; children: ReactNode }) {
  const { data: user } = useCurrentUser();
  if (!hasAccess(user?.permissions, permission)) {
    return <ForbiddenState />;
  }
  return children;
}
