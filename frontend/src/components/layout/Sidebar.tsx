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
      <div className="flex min-h-24 items-center gap-3 px-4 py-5">
        <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white p-1.5 shadow-sm ring-1 ring-white/10">
          <img src="/logo-icon.svg" alt="" className="h-full w-full object-contain" />
        </div>
        <div className="min-w-0">
          <span className="block text-[22px] font-extrabold leading-none tracking-[-0.02em] text-sidebar-foreground">
            OpenCrowd
          </span>
          <span className="mt-1.5 block text-[10.5px] font-semibold leading-snug tracking-[0.01em] text-sidebar-foreground/70">
            Open Identity &amp; Access
            <br />
            Governance Hub
          </span>
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
