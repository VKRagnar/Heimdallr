import { Navigate, Route, Routes } from 'react-router-dom';
import { PermissionGate } from '../components/PermissionGate';
import { AppShell } from '../layouts/AppShell';
import { ApplicationDetailPage } from '../pages/ApplicationDetailPage';
import { ApplicationsPage } from '../pages/ApplicationsPage';
import { AuditEventsPage } from '../pages/AuditEventsPage';
import { HomePage } from '../pages/HomePage';
import { ServersPage } from '../pages/ServersPage';
import { SystemRolesPage } from '../pages/SystemRolesPage';
import { SystemUsersPage } from '../pages/SystemUsersPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/home" replace />} />
        <Route path="/home" element={<HomePage />} />
        <Route path="/applications" element={<ApplicationsPage />} />
        <Route path="/applications/:appId" element={<ApplicationDetailPage />} />
        <Route path="/servers" element={<ServersPage />} />
        <Route
          path="/system/users"
          element={
            <PermissionGate permission="system:users:read">
              <SystemUsersPage />
            </PermissionGate>
          }
        />
        <Route
          path="/system/roles"
          element={
            <PermissionGate permission="system:roles:read">
              <SystemRolesPage />
            </PermissionGate>
          }
        />
        <Route
          path="/system/audit-events"
          element={
            <PermissionGate permission="system:audit:read">
              <AuditEventsPage />
            </PermissionGate>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/home" replace />} />
    </Routes>
  );
}
