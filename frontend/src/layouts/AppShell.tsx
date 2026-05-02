import {
  AppstoreOutlined,
  AuditOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  DeploymentUnitOutlined,
  FileSearchOutlined,
  LineChartOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { Avatar, Badge, Button, Layout, Menu, Select, Space, Typography } from 'antd';
import type { MenuProps } from 'antd';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { clearAuthToken } from '../api/client';
import { useCurrentUser, useDataScope } from '../hooks/useCurrentUser';

const { Header, Sider, Content } = Layout;

const navItems: MenuProps['items'] = [
  { key: '/home', icon: <DashboardOutlined />, label: <Link to="/home">首页</Link> },
  { key: '/applications', icon: <AppstoreOutlined />, label: <Link to="/applications">应用</Link> },
  { key: '/servers', icon: <DatabaseOutlined />, label: <Link to="/servers">服务器</Link> },
  { key: '/logs/search', icon: <FileSearchOutlined />, label: <Link to="/logs/search">日志查询</Link> },
  { key: '/metrics', icon: <LineChartOutlined />, label: <Link to="/metrics">指标查询</Link> },
  {
    key: 'access',
    icon: <DeploymentUnitOutlined />,
    label: '接入管理',
    children: [
      { key: '/access/applications', label: <Link to="/access/applications">应用接入</Link> },
      { key: '/access/data-sources', label: <Link to="/access/data-sources">数据源</Link> },
      { key: '/access/agents', label: <Link to="/access/agents">Agent</Link> },
    ],
  },
  {
    key: 'system',
    icon: <SafetyCertificateOutlined />,
    label: '系统管理',
    children: [
      { key: '/system/users', icon: <UserOutlined />, label: <Link to="/system/users">系统用户</Link> },
      { key: '/system/roles', icon: <TeamOutlined />, label: <Link to="/system/roles">角色</Link> },
      { key: '/system/audit-events', icon: <AuditOutlined />, label: <Link to="/system/audit-events">审计</Link> },
    ],
  },
];

function getSelectedKey(pathname: string) {
  if (pathname.startsWith('/applications/')) return '/applications';
  if (pathname.startsWith('/logs/search')) return '/logs/search';
  if (pathname.startsWith('/metrics')) return '/metrics';
  if (pathname.startsWith('/access/applications')) return '/access/applications';
  if (pathname.startsWith('/access/data-sources')) return '/access/data-sources';
  if (pathname.startsWith('/access/agents')) return '/access/agents';
  if (pathname.startsWith('/system/users')) return '/system/users';
  if (pathname.startsWith('/system/roles')) return '/system/roles';
  if (pathname.startsWith('/system/audit-events')) return '/system/audit-events';
  return pathname;
}

export function AppShell() {
  const location = useLocation();
  const navigate = useNavigate();
  const { data: user } = useCurrentUser();
  const { data: scope } = useDataScope();

  const logout = () => {
    clearAuthToken();
    navigate('/login', { replace: true });
  };

  return (
    <Layout className="app-shell">
      <Sider width={232} className="app-sider">
        <div className="brand">
          <div className="brand-mark">H</div>
          <div>
            <Typography.Text strong>Heimdallr</Typography.Text>
            <Typography.Text type="secondary">Monitor Console</Typography.Text>
          </div>
        </div>
        <Menu mode="inline" theme="dark" items={navItems} selectedKeys={[getSelectedKey(location.pathname)]} defaultOpenKeys={['access', 'system']} />
      </Sider>
      <Layout>
        <Header className="app-header">
          <Space size={16}>
            <Select
              size="middle"
              value={scope?.environments[0] ?? 'prod'}
              options={(scope?.environments ?? ['prod']).map((env) => ({ label: env.toUpperCase(), value: env }))}
              className="env-select"
            />
            <Badge status="processing" text="开发联调环境" />
          </Space>
          <Space size={12}>
            <Avatar icon={<UserOutlined />} />
            <div className="user-meta">
              <Typography.Text strong>{user?.name ?? '加载中'}</Typography.Text>
              <Typography.Text type="secondary">{user?.roles.join(' / ') ?? '-'}</Typography.Text>
            </div>
            <Button aria-label="退出登录" icon={<LogoutOutlined />} onClick={logout} />
          </Space>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
