import React, { useEffect } from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { useAuthStore } from '@/stores/auth.store';
import { useAuth } from '@/hooks/use-auth';
import { MainLayout } from '@/components/layout/MainLayout';
import { DashboardPage } from '@/pages/Dashboard';
import { IdentityPage } from '@/pages/Identity';
import { UserDetailPage } from '@/pages/UserDetail';
import { GroupsPage } from '@/pages/Groups';
import { GroupDetailPage } from '@/pages/GroupDetail';
import { ApplicationsPage } from '@/pages/Applications';
import { AccessMatrixPage } from '@/pages/AccessMatrix';
import { AuditPage } from '@/pages/Audit';
import { LoginPage } from '@/pages/Login';
import { CallbackPage } from '@/pages/Callback';
import './globals.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function AppRouter() {
  const { isAuthenticated, isLoading } = useAuth();
  const initialize = useAuthStore((s) => s.initialize);
  const path = window.location.pathname;

  useEffect(() => {
    initialize();
  }, [initialize]);

  // Handle OIDC callback
  if (path === '/callback') {
    return <CallbackPage />;
  }

  // Loading state
  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  // Not authenticated — show login
  if (!isAuthenticated) {
    return <LoginPage />;
  }

  // Authenticated — show app
  const getPageContent = () => {
    if (path.startsWith('/identity/') && path.length > '/identity/'.length) {
      const userId = path.replace('/identity/', '');
      return { title: 'User Profile', subtitle: 'View and edit user details', component: <UserDetailPage userId={userId} onBack={() => window.location.href = '/identity'} /> };
    }
    if (path.startsWith('/groups/') && path.length > '/groups/'.length) {
      const groupId = path.replace('/groups/', '');
      return { title: 'Group Details', subtitle: 'View and manage group members', component: <GroupDetailPage groupId={groupId} onBack={() => window.location.href = '/groups'} /> };
    }
    switch (path) {
      case '/identity':
        return { title: 'Identity', subtitle: 'Manage user identities', component: <IdentityPage /> };
      case '/groups':
        return { title: 'Groups', subtitle: 'Organize users into groups', component: <GroupsPage /> };
      case '/applications':
        return { title: 'Applications', subtitle: 'Manage connected applications', component: <ApplicationsPage /> };
      case '/access-matrix':
        return { title: 'Access Matrix', subtitle: 'Unified cross-application permission view', component: <AccessMatrixPage /> };
      case '/audit':
        return { title: 'Audit', subtitle: 'Track all governance actions', component: <AuditPage /> };
      default:
        return { title: 'Dashboard', subtitle: 'Unified view of your governance landscape', component: <DashboardPage /> };
    }
  };

  const { title, subtitle, component } = getPageContent();

  return (
    <MainLayout title={title} subtitle={subtitle} currentPath={path}>
      {component}
    </MainLayout>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <AppRouter />
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  </React.StrictMode>,
);
