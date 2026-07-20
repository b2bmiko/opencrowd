import { useState } from 'react';
import { CheckCircle, Shield } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api-client';

/**
 * Public-facing access request page.
 * Accessible to any authenticated user (no admin role required).
 * Users see only this form — no sidebar, no admin features.
 */
export function PublicRequestPage() {
  const [form, setForm] = useState({
    requestorName: '',
    requestorEmail: '',
    type: 'access',
    application: 'xwiki',
    resourceName: '(global)',
    permission: '',
    justification: '',
    projectCode: '',
    duration: '',
    managerName: '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);
    try {
      const customFields: Record<string, string> = {};
      if (form.projectCode) customFields['Project Code'] = form.projectCode;
      if (form.duration) customFields['Duration'] = form.duration;
      if (form.managerName) customFields['Manager'] = form.managerName;

      await apiClient.post('/requests', {
        requestorName: form.requestorName,
        requestorEmail: form.requestorEmail || null,
        type: form.type,
        application: form.application,
        resourceName: form.resourceName,
        permission: form.permission,
        justification: form.justification || null,
        customFields: Object.keys(customFields).length > 0 ? JSON.stringify(customFields) : null,
      });
      setSubmitted(true);
    } catch (e: unknown) {
      setError(e && typeof e === 'object' && 'message' in e ? (e as { message: string }).message : 'Failed to submit request');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (submitted) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background p-6">
        <div className="w-full max-w-md text-center">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100">
            <CheckCircle className="h-8 w-8 text-emerald-600" />
          </div>
          <h1 className="mt-4 text-2xl font-bold text-foreground">Request Submitted</h1>
          <p className="mt-2 text-muted-foreground">
            Your access request has been sent to the administrator for review. You'll be notified once it's processed.
          </p>
          <Button className="mt-6" onClick={() => { setSubmitted(false); setForm({ ...form, permission: '', justification: '', projectCode: '', duration: '', managerName: '' }); }}>
            Submit Another Request
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-6">
      <div className="w-full max-w-lg">
        {/* Header */}
        <div className="mb-6 text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10">
            <Shield className="h-7 w-7 text-primary" />
          </div>
          <h1 className="mt-3 text-2xl font-bold text-foreground">Request Access</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Submit a request for application access. An administrator will review and approve or reject your request.
          </p>
        </div>

        {/* Form */}
        <div className="rounded-lg border bg-card p-6">
          {error && <div className="mb-4 rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</div>}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Who */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-sm font-medium text-foreground">Your Full Name *</label>
                <input
                  type="text"
                  required
                  value={form.requestorName}
                  onChange={(e) => setForm({ ...form, requestorName: e.target.value })}
                  className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  placeholder="John Smith"
                />
              </div>
              <div>
                <label className="text-sm font-medium text-foreground">Email *</label>
                <input
                  type="email"
                  required
                  value={form.requestorEmail}
                  onChange={(e) => setForm({ ...form, requestorEmail: e.target.value })}
                  className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  placeholder="john@company.com"
                />
              </div>
            </div>

            {/* What */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-sm font-medium text-foreground">Request Type</label>
                <select
                  value={form.type}
                  onChange={(e) => setForm({ ...form, type: e.target.value })}
                  className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                >
                  <option value="access">New Access</option>
                  <option value="elevation">More Permissions</option>
                  <option value="temporary">Temporary Access</option>
                  <option value="removal">Remove My Access</option>
                </select>
              </div>
              <div>
                <label className="text-sm font-medium text-foreground">Application *</label>
                <select
                  value={form.application}
                  onChange={(e) => setForm({ ...form, application: e.target.value })}
                  className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                >
                  <option value="xwiki">xWiki</option>
                  <option value="openproject">OpenProject</option>
                  <option value="nextcloud">Nextcloud</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-sm font-medium text-foreground">Permission Needed *</label>
                <select
                  required
                  value={form.permission}
                  onChange={(e) => setForm({ ...form, permission: e.target.value })}
                  className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                >
                  <option value="">Select...</option>
                  <option value="view">View (read-only)</option>
                  <option value="comment">Comment</option>
                  <option value="edit">Edit (write)</option>
                  <option value="delete">Delete</option>
                  <option value="admin">Admin</option>
                </select>
              </div>
              <div>
                <label className="text-sm font-medium text-foreground">Space / Resource</label>
                <input
                  type="text"
                  value={form.resourceName}
                  onChange={(e) => setForm({ ...form, resourceName: e.target.value })}
                  className="mt-1 h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  placeholder="All (global) or specific space"
                />
              </div>
            </div>

            {/* Why */}
            <div>
              <label className="text-sm font-medium text-foreground">Why do you need this access? *</label>
              <textarea
                required
                value={form.justification}
                onChange={(e) => setForm({ ...form, justification: e.target.value })}
                className="mt-1 w-full rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                rows={3}
                placeholder="Briefly describe why you need this access..."
              />
            </div>

            {/* Additional info */}
            <div className="border-t pt-4">
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Additional Information (optional)</p>
              <div className="mt-3 grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-muted-foreground">Project Code</label>
                  <input
                    type="text"
                    value={form.projectCode}
                    onChange={(e) => setForm({ ...form, projectCode: e.target.value })}
                    className="mt-1 h-8 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                    placeholder="PRJ-001"
                  />
                </div>
                <div>
                  <label className="text-xs text-muted-foreground">Your Manager</label>
                  <input
                    type="text"
                    value={form.managerName}
                    onChange={(e) => setForm({ ...form, managerName: e.target.value })}
                    className="mt-1 h-8 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                    placeholder="Manager's name"
                  />
                </div>
              </div>
              {form.type === 'temporary' && (
                <div className="mt-3">
                  <label className="text-xs text-muted-foreground">How long do you need access?</label>
                  <input
                    type="text"
                    value={form.duration}
                    onChange={(e) => setForm({ ...form, duration: e.target.value })}
                    className="mt-1 h-8 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                    placeholder="e.g. 2 weeks, 30 days"
                  />
                </div>
              )}
            </div>

            <Button type="submit" className="w-full" disabled={isSubmitting || !form.requestorName || !form.permission || !form.justification}>
              {isSubmitting ? 'Submitting...' : 'Submit Request'}
            </Button>
          </form>
        </div>

        <p className="mt-4 text-center text-xs text-muted-foreground">
          Powered by OpenCrowd — Open Identity & Access Governance
        </p>
      </div>
    </div>
  );
}
