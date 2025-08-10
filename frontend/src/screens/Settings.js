import { Card, Radio, Typography, Tabs } from 'antd';
import React, { useEffect, useState } from 'react';

import NotificationPreferences from '../components/NotificationPreferences';

const { Title } = Typography;
const { TabPane } = Tabs;

const THEME_KEY = 'themeMode';

const getSystemTheme = () => {
  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark';
  }
  return 'light';
};

const Settings = () => {
  const [mode, setMode] = useState(() => {
    return localStorage.getItem(THEME_KEY) || 'system';
  });

  useEffect(() => {
    let theme = mode;
    if (mode === 'system') {
      theme = getSystemTheme();
    }
    document.body.setAttribute('data-theme', theme);
    localStorage.setItem(THEME_KEY, mode);

    // Add system theme change listener when in system mode
    if (mode === 'system') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const handleSystemThemeChange = e => {
        const newTheme = e.matches ? 'dark' : 'light';
        document.body.setAttribute('data-theme', newTheme);
      };

      mediaQuery.addEventListener('change', handleSystemThemeChange);

      return () => {
        mediaQuery.removeEventListener('change', handleSystemThemeChange);
      };
    }
  }, [mode]);

  return (
    <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
      <Title level={2}>Settings</Title>

      <Tabs defaultActiveKey="notifications" type="card">
        <TabPane tab="Notifications" key="notifications">
          <NotificationPreferences />
        </TabPane>

        <TabPane tab="Appearance" key="appearance">
          <Card style={{ maxWidth: 400 }}>
            <Title level={4}>Theme Settings</Title>
            <div style={{ marginBottom: 16 }}>Theme Mode:</div>
            <Radio.Group value={mode} onChange={e => setMode(e.target.value)} buttonStyle="solid">
              <Radio.Button value="system">System</Radio.Button>
              <Radio.Button value="light">Light</Radio.Button>
              <Radio.Button value="dark">Dark</Radio.Button>
            </Radio.Group>
          </Card>
        </TabPane>
      </Tabs>
    </div>
  );
};

export default Settings;
