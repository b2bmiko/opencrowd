import { useState } from 'react';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { KaiChat } from '@/components/kai/KaiChat';

interface MainLayoutProps {
  children: React.ReactNode;
  title: string;
  subtitle?: string;
  currentPath: string;
  isAdmin?: boolean;
}

export function MainLayout({ children, title, subtitle, currentPath, isAdmin = true }: MainLayoutProps) {
  const [isDark, setIsDark] = useState(false);
  const [isKaiOpen, setIsKaiOpen] = useState(false);

  const toggleTheme = () => {
    setIsDark(!isDark);
    document.documentElement.classList.toggle('dark');
  };

  return (
    <div className="flex h-full">
      <Sidebar currentPath={currentPath} isAdmin={isAdmin} onOpenKai={() => setIsKaiOpen(true)} />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header title={title} subtitle={subtitle} onToggleTheme={toggleTheme} isDark={isDark} />
        <main className="flex-1 overflow-auto bg-background p-6">{children}</main>
      </div>
      <KaiChat isOpen={isKaiOpen} onClose={() => setIsKaiOpen(false)} />
    </div>
  );
}
