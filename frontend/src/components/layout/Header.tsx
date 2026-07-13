import { Moon, Sun, Search, LogOut } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/hooks/use-auth';

interface HeaderProps {
  title: string;
  subtitle?: string;
  onToggleTheme?: () => void;
  isDark?: boolean;
}

export function Header({ title, subtitle, onToggleTheme, isDark }: HeaderProps) {
  const { displayName, initials, logout } = useAuth();

  return (
    <header className="flex h-16 items-center justify-between border-b bg-card px-6">
      <div>
        <h1 className="text-xl font-semibold text-foreground">{title}</h1>
        {subtitle && <p className="text-sm text-muted-foreground">{subtitle}</p>}
      </div>

      <div className="flex items-center gap-3">
        {/* Search */}
        <div className="relative hidden md:block">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search users, groups, applications..."
            className="h-9 w-72 rounded-md border bg-background pl-9 pr-4 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>

        {/* Theme toggle */}
        <Button variant="ghost" size="icon" onClick={onToggleTheme} aria-label="Toggle theme">
          {isDark ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
        </Button>

        {/* Logout */}
        <Button variant="ghost" size="icon" onClick={() => logout()} aria-label="Sign out">
          <LogOut className="h-5 w-5" />
        </Button>

        {/* User avatar */}
        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs font-medium text-primary-foreground">
          {initials}
        </div>
        <span className="hidden text-sm font-medium text-foreground md:inline">{displayName}</span>
      </div>
    </header>
  );
}
