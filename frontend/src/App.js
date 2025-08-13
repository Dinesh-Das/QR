import { Layout, notification } from 'antd';
import React, { Suspense } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';

import 'antd/dist/reset.css';
import './App.css';
import { AppErrorBoundary, RouteErrorBoundary } from './components/ErrorBoundaries';
import Navigation from './components/Navigation';
import PageSkeleton from './components/PageSkeleton';
import ProtectedRoute from './components/ProtectedRoute';
import { useActivityTracking } from './components/useActivityTracking';
import { isAuthenticated } from './services/auth';

// Lazy load all route components for code splitting
const Login = React.lazy(() => import('./screens/Login'));
const Settings = React.lazy(() => import('./screens/Settings'));
const Dashboard = React.lazy(() => import('./screens/Dashboard'));
const Reports = React.lazy(() => import('./screens/Reports'));
const Home = React.lazy(() => import('./screens/Home'));
const AdminPanel = React.lazy(() => import('./screens/admin'));
const JVCView = React.lazy(() => import('./screens/JVCView'));
const CQSView = React.lazy(() => import('./screens/CQSView'));
const TechView = React.lazy(() => import('./screens/TechView'));
const PlantView = React.lazy(() => import('./screens/PlantView'));

const WorkflowPage = React.lazy(() => import('./screens/WorkflowPage'));
const QuestionnaireViewerPage = React.lazy(() => import('./screens/QuestionnaireViewerPage'));

const Auditlogs = React.lazy(() => import('./screens/Auditlogs'));
const Users = React.lazy(() => import('./screens/Users'));
const Roles = React.lazy(() => import('./screens/Roles'));
const Sessions = React.lazy(() => import('./screens/Sessions'));
const UserRoleManagement = React.lazy(() => import('./screens/UserRoleManagement'));
const WorkflowMonitoring = React.lazy(() => import('./screens/WorkflowMonitoring'));
const QRAnalytics = React.lazy(() => import('./screens/QRAnalytics'));

const { Header, Content, Footer } = Layout;

function App() {
  // Activity tracking
  const { trackAction } = useActivityTracking('app', true);

  // State to track authentication status
  const [authState, setAuthState] = React.useState(isAuthenticated());
  const [currentPath, setCurrentPath] = React.useState(window.location.pathname);

  // Example notification usage
  const openNotification = () => {
    notification.open({
      message: 'Welcome',
      description: 'QRMFG Portal'
    });
  };

  React.useEffect(() => {
    openNotification();
    trackAction('app_start');
  }, [trackAction]);

  // Listen for authentication state changes
  React.useEffect(() => {
    const checkAuthState = () => {
      const newAuthState = isAuthenticated();
      const newPath = window.location.pathname;

      if (newAuthState !== authState) {
        setAuthState(newAuthState);
      }

      if (newPath !== currentPath) {
        setCurrentPath(newPath);
      }
    };

    // Check auth state periodically
    const interval = setInterval(checkAuthState, 1000);

    // Listen for storage changes (logout from another tab)
    const handleStorageChange = () => {
      checkAuthState();
    };

    window.addEventListener('storage', handleStorageChange);
    window.addEventListener('popstate', checkAuthState);

    return () => {
      clearInterval(interval);
      window.removeEventListener('storage', handleStorageChange);
      window.removeEventListener('popstate', checkAuthState);
    };
  }, [authState, currentPath]);

  const unauthRoutes = ['/qrmfg/login'];
  const isLoginPage = unauthRoutes.includes(currentPath);
  const showSidebar = !isLoginPage && authState;

  // Render login page completely standalone
  if (isLoginPage) {
    return (
      <AppErrorBoundary>
        <Router>
          <Suspense fallback={<PageSkeleton />}>
            <RouteErrorBoundary routeName="Login">
              <Login />
            </RouteErrorBoundary>
          </Suspense>
        </Router>
      </AppErrorBoundary>
    );
  }

  // Render main app with layout
  return (
    <AppErrorBoundary>
      <Router>
        <Layout>
          {showSidebar && <Navigation />}
          <Layout
            style={{
              minHeight: '100vh',
              marginLeft: showSidebar ? 250 : 0,
              transition: 'margin-left 0.2s'
            }}
          >
            <Header
              style={{
                padding: '0 24px',
                background: '#fff',
                position: 'sticky',
                top: 0,
                zIndex: 1,
                width: '100%',
                display: 'flex',
                alignItems: 'center'
              }}
            />
            <Content
              style={{
                margin: '24px 16px',
                padding: 24,
                minHeight: 280,
                background: '#fff',
                borderRadius: '4px'
              }}
            >
              <Suspense fallback={<PageSkeleton />}>
                <Routes>
                  <Route
                    path="/qrmfg/login"
                    element={
                      <RouteErrorBoundary routeName="Login">
                        <Login />
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/admin/*"
                    element={
                      <RouteErrorBoundary routeName="Admin Panel">
                        <ProtectedRoute>
                          <AdminPanel />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/settings"
                    element={
                      <RouteErrorBoundary routeName="Settings">
                        <ProtectedRoute>
                          <Settings />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/dashboard"
                    element={
                      <RouteErrorBoundary routeName="Dashboard">
                        <ProtectedRoute>
                          <Dashboard />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/reports"
                    element={
                      <RouteErrorBoundary routeName="Reports">
                        <ProtectedRoute>
                          <Reports />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                    <Route
                    path="/qrmfg/test"
                    element={
                      <div style={{ padding: 20 }}>
                        <h1>Test Route Works!</h1>
                        <p>If you see this, React routing is working.</p>
                      </div>
                    }
                  />
                  <Route
                    path="/qrmfg/auditlogs"
                    element={
                      <RouteErrorBoundary routeName="Audit Logs">
                        <ProtectedRoute>
                          <Auditlogs />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                    <Route
                    path="/qrmfg/workflow-monitoring"
                    element={
                      <RouteErrorBoundary routeName="Workflow Monitoring">
                        <ProtectedRoute>
                          <WorkflowMonitoring />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/qr-analytics"
                    element={
                      <RouteErrorBoundary routeName="QR Analytics">
                        <ProtectedRoute>
                          <QRAnalytics />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/users"
                    element={
                      <RouteErrorBoundary routeName="Users">
                        <ProtectedRoute>
                          <Users />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/roles"
                    element={
                      <RouteErrorBoundary routeName="Roles">
                        <ProtectedRoute>
                          <Roles />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/sessions"
                    element={
                      <RouteErrorBoundary routeName="Sessions">
                        <ProtectedRoute>
                          <Sessions />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/user-role-management"
                    element={
                      <RouteErrorBoundary routeName="User Role Management">
                        <ProtectedRoute>
                          <UserRoleManagement />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/jvc"
                    element={
                      <RouteErrorBoundary routeName="JVC View">
                        <ProtectedRoute>
                          <JVCView />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/cqs"
                    element={
                      <RouteErrorBoundary routeName="CQS View">
                        <ProtectedRoute>
                          <CQSView />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/tech"
                    element={
                      <RouteErrorBoundary routeName="Tech View">
                        <ProtectedRoute>
                          <TechView />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/plant"
                    element={
                      <RouteErrorBoundary routeName="Plant View">
                        <ProtectedRoute>
                          <PlantView />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />

                  <Route
                    path="/qrmfg/workflows"
                    element={
                      <RouteErrorBoundary routeName="Workflows">
                        <ProtectedRoute>
                          <WorkflowPage />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg/questionnaire/:workflowId"
                    element={
                      <RouteErrorBoundary routeName="Questionnaire Viewer">
                        <ProtectedRoute>
                          <QuestionnaireViewerPage />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/qrmfg"
                    element={
                      <RouteErrorBoundary routeName="Home">
                        <ProtectedRoute>
                          <Home />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                    <Route
                    path="/qrmfg/"
                    element={
                      <RouteErrorBoundary routeName="Home">
                        <ProtectedRoute>
                          <Home />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                  <Route
                    path="/"
                    element={
                      <RouteErrorBoundary routeName="Home">
                        <ProtectedRoute>
                          <Home />
                        </ProtectedRoute>
                      </RouteErrorBoundary>
                    }
                  />
                </Routes>
              </Suspense>
            </Content>
            <Footer
              style={{
                textAlign: 'center',
                padding: '13px'
              }}
            >
              QRMFG System © {new Date().getFullYear()} Asian Paints Limited | Developed by IT
              Manufacturing Automation Team
            </Footer>
          </Layout>
        </Layout>
      </Router>
    </AppErrorBoundary>
  );
}

export default App;
