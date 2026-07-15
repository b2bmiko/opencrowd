import { cn } from '@/lib/utils';
import {
  LayoutDashboard,
  Users,
  Group,
  Shield,
  Network,
  FileCheck,
  ClipboardList,
  BarChart3,
  Settings,
} from 'lucide-react';

interface NavItem {
  label: string;
  icon: React.ElementType;
  href: string;
}

const navItems: NavItem[] = [
  { label: 'Dashboard', icon: LayoutDashboard, href: '/' },
  { label: 'Applications', icon: Network, href: '/applications' },
  { label: 'Identity', icon: Users, href: '/identity' },
  { label: 'Groups', icon: Group, href: '/groups' },
  { label: 'Access Matrix', icon: Shield, href: '/access-matrix' },
  { label: 'Access Profiles', icon: FileCheck, href: '/access-profiles' },
  { label: 'Requests', icon: ClipboardList, href: '/requests' },
  { label: 'Audit', icon: ClipboardList, href: '/audit' },
  { label: 'Reports', icon: BarChart3, href: '/reports' },
  { label: 'Settings', icon: Settings, href: '/settings' },
];

interface SidebarProps {
  currentPath: string;
}

export function Sidebar({ currentPath }: SidebarProps) {
  return (
    <aside className="flex h-full w-64 flex-col bg-sidebar text-sidebar-foreground">
      {/* Logo */}
      <div className="flex h-20 items-center gap-3 px-4">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-6 w-6 text-primary-foreground">
            <circle cx="12" cy="12" r="3" />
            <circle cx="12" cy="12" r="8" strokeDasharray="4 2" />
            <line x1="12" y1="2" x2="12" y2="4" />
            <line x1="12" y1="20" x2="12" y2="22" />
            <line x1="2" y1="12" x2="4" y2="12" />
            <line x1="20" y1="12" x2="22" y2="12" />
          </svg>
        </div>
        <div className="flex flex-col">
          <span className="text-lg font-bold leading-tight text-sidebar-foreground">OpenCrowd</span>
          <span className="text-[11px] font-semibold leading-tight text-sidebar-foreground/50" style={{ textShadow: '0 1px 2px rgba(0,0,0,0.3)' }}>Open Identity & Access Governance Hub</span>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 px-3 py-4">
        {navItems.map((item) => {
          const isActive = currentPath === item.href;
          return (
            <a
              key={item.href}
              href={item.href}
              className={cn(
                'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary text-primary-foreground'
                  : 'text-sidebar-foreground/70 hover:bg-sidebar-foreground/10 hover:text-sidebar-foreground',
              )}
            >
              <item.icon className="h-5 w-5" />
              {item.label}
            </a>
          );
        })}
      </nav>

      {/* Footer */}
      <div className="border-t border-sidebar-foreground/10 px-6 py-4">
        <p className="text-xs text-sidebar-foreground/50">OpenCrowd v0.1.0</p>
      </div>
    </aside>
  );
}
