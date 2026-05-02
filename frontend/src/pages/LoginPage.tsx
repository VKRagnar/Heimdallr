import { LockOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Form, Select, Typography, message } from 'antd';
import { useQueryClient } from '@tanstack/react-query';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { getAuthToken, setAuthToken } from '../api/client';

const accounts = [
  { label: '平台管理员', value: 'admin-token', description: '全局资产、授权、审计和接入管理' },
  { label: 'SRE', value: 'sre-token', description: '生产与预发接入、指标、日志和审计查看' },
  { label: 'ACE 应用负责人', value: 'ace-owner-token', description: 'ACE 应用范围内的排障视图' },
];

interface LoginLocationState {
  from?: { pathname?: string };
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const state = location.state as LoginLocationState | null;
  const redirectTo = state?.from?.pathname && state.from.pathname !== '/login' ? state.from.pathname : '/home';

  if (getAuthToken()) {
    return <Navigate to={redirectTo} replace />;
  }

  const submit = async ({ token }: { token: string }) => {
    setAuthToken(token);
    await queryClient.invalidateQueries();
    message.success('已登录');
    navigate(redirectTo, { replace: true });
  };

  return (
    <main className="login-page">
      <section className="login-panel">
        <div className="login-brand">
          <div className="brand-mark">H</div>
          <div>
            <Typography.Title level={2}>Heimdallr</Typography.Title>
            <Typography.Text type="secondary">Monitor Console</Typography.Text>
          </div>
        </div>
        <Form layout="vertical" initialValues={{ token: 'admin-token' }} onFinish={(values) => void submit(values)}>
          <Form.Item name="token" label="登录身份" rules={[{ required: true, message: '请选择登录身份' }]}>
            <Select
              size="large"
              suffixIcon={<UserOutlined />}
              options={accounts.map((item) => ({
                label: (
                  <div className="login-option">
                    <span>{item.label}</span>
                    <Typography.Text type="secondary">{item.description}</Typography.Text>
                  </div>
                ),
                value: item.value,
              }))}
            />
          </Form.Item>
          <Button type="primary" htmlType="submit" size="large" block icon={<LockOutlined />}>
            登录
          </Button>
        </Form>
      </section>
      <aside className="login-status">
        <SafetyCertificateOutlined />
        <Typography.Text>开发联调环境</Typography.Text>
      </aside>
    </main>
  );
}
