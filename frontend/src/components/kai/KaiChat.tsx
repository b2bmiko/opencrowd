import { useState, useRef, useEffect } from 'react';
import { MessageCircle, X, Send, Sparkles } from 'lucide-react';
import { apiClient } from '@/lib/api-client';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

const WELCOME_MESSAGE: Message = {
  id: 'welcome',
  role: 'assistant',
  content: "Hi! I'm Kai, your access governance assistant. I can help you with:\n\n• Understanding permissions and groups\n• Submitting access requests\n• Finding who has access to what\n• Navigating OpenCrowd features\n\nHow can I help you today?",
  timestamp: new Date(),
};

export function KaiChat({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) {
  const [messages, setMessages] = useState<Message[]>([WELCOME_MESSAGE]);
  const [input, setInput] = useState('');
  const [isThinking, setIsThinking] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = async () => {
    if (!input.trim() || isThinking) return;

    const userMessage: Message = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: input.trim(),
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setIsThinking(true);

    try {
      const response = await apiClient.post<{ reply: string }>('/assistant/chat', {
        message: userMessage.content,
        history: messages.slice(-10).map((m) => ({ role: m.role, content: m.content })),
      });

      const aiMessage: Message = {
        id: `ai-${Date.now()}`,
        role: 'assistant',
        content: response.data.reply,
        timestamp: new Date(),
      };
      setMessages((prev) => [...prev, aiMessage]);
    } catch (e) {
      // Fallback: local responses when backend isn't available
      const fallbackReply = getLocalResponse(userMessage.content);
      const aiMessage: Message = {
        id: `ai-${Date.now()}`,
        role: 'assistant',
        content: fallbackReply,
        timestamp: new Date(),
      };
      setMessages((prev) => [...prev, aiMessage]);
    } finally {
      setIsThinking(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <>
      {/* Chat panel */}
      {isOpen && (
        <div className="fixed bottom-6 left-72 z-50 flex h-[32rem] w-96 flex-col rounded-xl border bg-card shadow-2xl">
          {/* Header */}
          <div className="flex items-center justify-between rounded-t-xl bg-primary px-4 py-3">
            <div className="flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-primary-foreground" />
              <div>
                <p className="text-sm font-semibold text-primary-foreground">Kai</p>
                <p className="text-xs text-primary-foreground/70">AI Governance Assistant</p>
              </div>
            </div>
            <button onClick={onClose} className="rounded p-1 text-primary-foreground/70 hover:text-primary-foreground">
              <X className="h-4 w-4" />
            </button>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-[85%] rounded-lg px-3 py-2 text-sm ${
                    message.role === 'user'
                      ? 'bg-primary text-primary-foreground'
                      : 'bg-muted text-foreground'
                  }`}
                >
                  <p className="whitespace-pre-wrap">{message.content}</p>
                </div>
              </div>
            ))}
            {isThinking && (
              <div className="flex justify-start">
                <div className="rounded-lg bg-muted px-3 py-2 text-sm text-muted-foreground">
                  <span className="inline-flex items-center gap-1">
                    <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-muted-foreground" style={{ animationDelay: '0ms' }} />
                    <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-muted-foreground" style={{ animationDelay: '150ms' }} />
                    <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-muted-foreground" style={{ animationDelay: '300ms' }} />
                  </span>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input */}
          <div className="border-t px-4 py-3">
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Ask Kai anything..."
                className="h-9 flex-1 rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              />
              <button
                onClick={sendMessage}
                disabled={!input.trim() || isThinking}
                className="flex h-9 w-9 items-center justify-center rounded-md bg-primary text-primary-foreground disabled:opacity-50"
              >
                <Send className="h-4 w-4" />
              </button>
            </div>
            <p className="mt-1.5 text-center text-xs text-muted-foreground">
              Kai uses AI to help — answers may not always be perfect
            </p>
          </div>
        </div>
      )}
    </>
  );
}

/**
 * Local fallback responses when the AI backend isn't available.
 * Provides basic guidance without needing Mistral API.
 */
function getLocalResponse(input: string): string {
  const q = input.toLowerCase();

  if (q.includes('request') && (q.includes('access') || q.includes('how'))) {
    return "To request access:\n\n1. Go to the Requests page (or use the public link: /request)\n2. Fill in your name, the application, and the permission you need\n3. Add a justification explaining why\n4. Submit — an admin will review and approve or reject\n\nNeed help with something specific?";
  }
  if (q.includes('group') && (q.includes('what') || q.includes('how') || q.includes('join'))) {
    return "Groups organize users by team or role. Each group can have permissions assigned in the Access Matrix.\n\nTo join a group:\n• Ask your admin to add you (Identity → user profile → group memberships)\n• Or submit an access request mentioning the group you need\n\nTo see all groups: go to the Groups page in the sidebar.";
  }
  if (q.includes('permission') || q.includes('rights')) {
    return "Permissions control what you can do in connected applications:\n\n• **View** — read content\n• **Comment** — add comments\n• **Edit** — modify content\n• **Delete** — remove content\n• **Admin** — full management\n• **Script** — run automated scripts\n\nPermissions are managed in the Access Matrix. You can request new permissions via the Requests page.";
  }
  if (q.includes('new') && (q.includes('user') || q.includes('employee') || q.includes('joiner'))) {
    return "To onboard a new user:\n\n1. Go to Identity → click 'Onboard User'\n2. Fill in their details (name, email, department)\n3. Select an Access Profile (predefined permission template)\n4. The system will create the user, assign groups, and provision to connected apps automatically\n\nOr: share the /request link so they can self-request access.";
  }
  if (q.includes('offboard') || q.includes('leaver') || q.includes('remove user')) {
    return "To offboard a user:\n\n1. Go to Identity → click the user\n2. In the Lifecycle section, click 'Offboard'\n3. Review what will happen (groups removed, apps deprovisioned)\n4. Confirm\n\nThe user will be removed from all groups and disabled in connected applications.";
  }
  if (q.includes('sync') || q.includes('xwiki') || q.includes('connect')) {
    return "To sync with xWiki:\n\n1. Go to Applications → click 'Sync' on your connected xWiki\n2. If credentials are saved, it syncs immediately\n3. Auto-sync runs every 30 minutes automatically\n\nThe sync imports users, groups, and memberships from xWiki into OpenCrowd.";
  }
  if (q.includes('profile') || q.includes('template')) {
    return "Access Profiles are permission templates:\n\n• Define once: name, permissions, and groups\n• Apply to many: when onboarding, just pick a profile\n• All permissions and group memberships are granted automatically\n\nManage profiles in the Access Profiles page. Examples: Viewer, Editor, Customs Officer, QA Tester.";
  }
  if (q.includes('audit') || q.includes('log') || q.includes('history')) {
    return "The Audit page shows a complete history of all actions:\n\n• User created/updated/deleted\n• Permissions granted/revoked\n• Group membership changes\n• Connector sync operations\n\nYou can filter by type, date, and export as CSV.";
  }
  if (q.includes('hello') || q.includes('hi') || q.includes('hey')) {
    return "Hey! I'm Kai, happy to help. You can ask me about:\n\n• How to request access\n• How to onboard/offboard users\n• What permissions mean\n• How groups work\n• How to sync with apps\n\nWhat would you like to know?";
  }

  return "I can help you with:\n\n• Requesting access to applications\n• Understanding permissions and groups\n• Onboarding/offboarding users\n• Syncing with connected apps\n• Access profiles and templates\n• Audit and compliance\n\nCould you rephrase your question? I'll do my best to help.";
}
