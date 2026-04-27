import { Navigate, Route, Routes } from 'react-router-dom';
import { PermissionGate } from '../components/PermissionGate';
import { AppShell } from '../layouts/AppShell';
import { AgentsPage } from '../pages/AgentsPage';
import { ApplicationDetailPage } from '../pages/ApplicationDetailPage';
import { ApplicationAccessPage } from '../pages/ApplicationAccessPage';
import { ApplicationsPage } from '../pages/ApplicationsPage';
import { AuditEventsPage } from '../pages/AuditEventsPage';
import { DataSourcesPage } from '../pages/DataSourcesPage';
import { HomePage } from '../pages/HomePage';
import { LogsSearchPage } from '../pages/LogsSearchPage';
import { MetricsPage } from '../pages/MetricsPage';
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
          path="/logs/search"
          element={
            <PermissionGate permission="logs:read">
              <LogsSearchPage />
            </PermissionGate>
          }
        />
        <Route
          path="/metrics"
          element={
            <PermissionGate permission="metrics:read">
              <MetricsPage />
            </PermissionGate>
          }
        />
        <Route
          path="/access/applications"
          element={
            <PermissionGate permission="data-sources:read">
              <ApplicationAccessPage />
            </PermissionGate>
          }
        />
        <Route
          path="/access/data-sources"
          element={
            <PermissionGate permission="data-sources:read">
              <DataSourcesPage />
            </PermissionGate>
          }
        />
        <Route
          path="/access/agents"
          element={
            <PermissionGate permission="agents:read">
              <AgentsPage />
            </PermissionGate>
          }
        />
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
