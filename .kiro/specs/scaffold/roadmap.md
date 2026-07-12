# OpenCrowd — Implementation Roadmap

## Overview

This roadmap defines the phased implementation plan for OpenCrowd, from scaffold to production-ready governance platform. Each milestone produces a shippable increment that can be demonstrated, tested, and aligned with the opencrowd.io website messaging.

**Domain:** opencrowd.io (WordPress on cPanel — marketing/branding site)
**Platform:** app.opencrowd.io (the actual SaaS application, deployed separately)
**Docs:** docs.opencrowd.io (developer/user documentation)

---

## Relationship: Website ↔ Platform

| Asset | Purpose | Tech | Host |
|-------|---------|------|------|
| opencrowd.io | Marketing, product info, pricing, blog, community | WordPress (Codex theme) | cPanel |
| app.opencrowd.io | The governance platform (SPA + API) | React + Spring Boot | AWS |
| docs.opencrowd.io | Technical docs, API reference, guides | Docusaurus or similar | GitHub Pages / Vercel |
| github.com/opencrowd | Source code, issues, discussions | GitHub | GitHub |

**Sync points between website and platform:**
- Website feature pages must match what the platform actually delivers per milestone
- Pricing page reflects Community / Professional / Enterprise editions
- Screenshots and demos on the website come from the actual running platform
- Blog posts announce each milestone release
- Documentation links from the website point to docs site

---

## Timeline Overview

```
2026 Q3 (Jul-Sep)     Milestone 0: Scaffold + Infrastructure
2026 Q3-Q4 (Sep-Nov)  Milestone 1: Identity & Connectors (Community MVP)
2026 Q4 (Nov-Dec)     Milestone 2: Access Governance (Community Complete)
2027 Q1 (Jan-Mar)     Milestone 3: Policy & Compliance (Professional Edition)
2027 Q2 (Apr-Jun)     Milestone 4: Enterprise Features
2027 Q3-Q4            Milestone 5: AI Governance (Phase 2)
```

---

## Milestone 0: Scaffold & Infrastructure

**Duration:** 2-3 weeks
**Goal:** Working foundation with full stack running end-to-end

### Deliverables

**Platform:**
- [ ] Backend: Kotlin + Spring Boot project, multi-module, compiles and runs
- [ ] Frontend: React + Vite + ShadCN project, routing, auth flow
- [ ] Database: PostgreSQL schema, Flyway migrations, multi-tenancy
- [ ] Auth: Keycloak realm configured, OIDC login works
- [ ] Connector SDK: Interface defined, stub connectors registered
- [ ] Infrastructure: Docker Compose (local), Dockerfiles, CI pipeline
- [ ] Edition/License: Feature flag infrastructure, edition enum

**Website (opencrowd.io) — sync:**
- [ ] Landing page with product vision, pillars, positioning
- [ ] "Coming Soon" or waitlist for early access
- [ ] Brand identity: logo, colors, typography defined
- [ ] These brand tokens shared with the platform frontend (Tailwind config)

**Brand Alignment:**
The platform's color scheme, typography, and logo must match opencrowd.io. We'll define design tokens in the frontend's `tailwind.config.ts` that mirror whatever the WordPress theme uses.

### Exit Criteria
- `docker compose up` → full stack running
- Login via Keycloak → see dashboard shell
- API responds to authenticated requests
- Website live at opencrowd.io with consistent branding

---

## Milestone 1: Identity & Connectors (Community MVP)

**Duration:** 6-8 weeks
**Goal:** Manage identities across xWiki, OpenProject, and Nextcloud from one place

### Deliverables

**Identity Lifecycle:**
- [ ] User Management: CRUD, search, filter, pagination
- [ ] User profiles: department, title, lifecycle status
- [ ] User lifecycle states: active, disabled, locked, pending, offboarded
- [ ] Joiner flow: create user → assign to groups → provision to connectors
- [ ] Leaver flow: disable user → revoke access across connectors → audit
- [ ] Import users from connected applications
- [ ] Import users from LDAP/Keycloak

**Group Management:**
- [ ] CRUD groups, nested groups, group ownership
- [ ] Static + dynamic groups (filter-based membership)
- [ ] Bulk member add/remove
- [ ] Group sync from/to connected applications

**Connectors (functional):**
- [ ] xWiki Connector: sync users, groups, spaces; read/write permissions
- [ ] OpenProject Connector: sync users, groups, projects; read/write memberships
- [ ] Nextcloud Connector: sync users, groups; read/write folder shares
- [ ] Connection wizard UI (guided setup)
- [ ] Health check + sync status dashboard
- [ ] Manual sync trigger + sync history

**Audit:**
- [ ] All mutations logged (who did what, when, to what)
- [ ] Audit log viewer with search and filtering
- [ ] Export audit logs (CSV, JSON)

**Dashboard:**
- [ ] User/group/connector statistics
- [ ] Connector health overview
- [ ] Recent activity feed
- [ ] Quick actions

### Website Sync
- [ ] Update opencrowd.io with feature pages: Identity, Groups, Connectors
- [ ] Screenshots from actual running platform
- [ ] Documentation: user guide for identity management
- [ ] Documentation: connector setup guides (xWiki, OpenProject, Nextcloud)
- [ ] Blog post: "OpenCrowd Milestone 1 — Managing Identities Across Your Open Source Workspace"

### Exit Criteria
- Admin can create a user and see them provisioned in xWiki + OpenProject + Nextcloud
- Admin can disable a user and see access revoked across all connected apps
- Connector health visible in dashboard
- Audit trail shows all operations
- Community Edition fully functional for this scope

---

## Milestone 2: Access Governance (Community Complete)

**Duration:** 6-8 weeks
**Goal:** Business Roles, Access Profiles, and the Unified Access Matrix

### Deliverables

**Business Roles:**
- [ ] Business Role catalog: CRUD, search, categorize
- [ ] Role → permission mapping across applications
- [ ] Role hierarchy (composable roles)
- [ ] Assign roles to users (grants all mapped permissions)
- [ ] Role membership review (who has what role and why)

**Access Profiles:**
- [ ] Access Profile builder: define what a role provisions across apps
- [ ] Profile entitlements: connector + resource + permission set
- [ ] Apply profile → auto-provision across connectors
- [ ] Preview/simulate before applying
- [ ] Profile versioning (track changes over time)

**Unified Access Matrix:**
- [ ] Cross-application permission visualization
- [ ] Rows: Users, Groups, Roles
- [ ] Columns: Applications, Resources, Permissions
- [ ] Filter, search, sort
- [ ] Bulk permission updates
- [ ] Permission comparison (user A vs user B)
- [ ] Export matrix (CSV, PDF)

**Mover Flow:**
- [ ] Change department/role → recalculate entitlements
- [ ] Show what will change (simulation)
- [ ] Apply changes → audit generated

**Basic Reporting:**
- [ ] User access report
- [ ] Group membership report
- [ ] Application access report
- [ ] Inactive users report
- [ ] Export: CSV, PDF

### Website Sync
- [ ] Feature pages: Business Roles, Access Profiles, Access Matrix
- [ ] Interactive demo or video walkthrough
- [ ] Pricing page: clearly show Community vs Pro vs Enterprise
- [ ] Documentation: governance concepts, role modeling guide
- [ ] Blog post: "The Unified Access Matrix — See Everything in One Screen"

### Exit Criteria
- "Finance Manager" business role provisions correct xWiki spaces, OpenProject projects, Nextcloud folders
- Access Matrix shows cross-app permissions for any user
- Mover workflow recalculates access on role change
- Community Edition is production-ready for small organizations
- **This is the first public release (v0.1.0)**

---

## Milestone 3: Policy & Compliance (Professional Edition)

**Duration:** 8-10 weeks
**Goal:** Approval workflows, policies, access reviews, compliance reporting

### Deliverables

**Approval Workflows:**
- [ ] Access request portal (user self-service)
- [ ] Multi-step approval chain: Manager → Resource Owner → Auto-provision
- [ ] Temporary access (with expiration)
- [ ] Approval delegation (out-of-office)
- [ ] Email/notification on pending approvals
- [ ] Approval history and audit trail

**Policy Engine:**
- [ ] Define business policies (rules)
- [ ] Policy types: access duration, review frequency, SoD (basic)
- [ ] Policy assignment: per role, per group, per application
- [ ] Policy violation detection (non-blocking in Community, blocking in Enterprise)
- [ ] Expiration enforcement (auto-revoke expired access)

**Access Reviews:**
- [ ] Scheduled review campaigns
- [ ] Manager certifies team access
- [ ] Resource owner certifies who has access
- [ ] Revoke access from review UI
- [ ] Review completion tracking and reminders

**Advanced Reporting:**
- [ ] Compliance dashboard
- [ ] Review campaign status
- [ ] Policy violation summary
- [ ] Scheduled report generation
- [ ] Report templates

**Notifications:**
- [ ] Email notifications (approval requests, reviews, expirations)
- [ ] In-app notification center
- [ ] Notification preferences per user

### Website Sync
- [ ] Professional Edition feature page
- [ ] Pricing update: show Professional tier active
- [ ] Case study or use case: "How a University Manages Annual Access Reviews"
- [ ] Documentation: workflow configuration, policy authoring
- [ ] Blog post: "From Manual Governance to Automated Compliance"

### Exit Criteria
- User requests access → manager approves → auto-provisioned → audit generated
- Access expires after defined period → auto-revoked
- Review campaign: manager certifies team access, revokes outdated permissions
- Professional Edition license gates these features
- **Release v0.2.0**

---

## Milestone 4: Enterprise Features

**Duration:** 8-10 weeks
**Goal:** Multi-tenancy, compliance packs, SoD, SCIM, premium connectors

### Deliverables

**Multi-Tenant Management:**
- [ ] Platform admin UI for managing tenants
- [ ] Tenant provisioning (create org → schema + Keycloak realm)
- [ ] Tenant settings: branding, policies, features
- [ ] Tenant billing/plan management (integration-ready)
- [ ] Cross-tenant reporting (platform admin only)

**Compliance Packs:**
- [ ] ISO 27001 access control evidence report
- [ ] GDPR data processing inventory + right-to-erasure workflow
- [ ] NIS2 access governance evidence
- [ ] Compliance dashboard per framework
- [ ] Audit trail mapping to compliance controls

**Segregation of Duties (SoD):**
- [ ] Define toxic combinations (conflicting roles/permissions)
- [ ] Real-time SoD violation detection
- [ ] Block or warn on SoD violation during provisioning
- [ ] SoD violation report
- [ ] Exception management (approved violations with justification)

**SCIM Provisioning:**
- [ ] SCIM 2.0 server (inbound: receive user/group updates from IdPs)
- [ ] SCIM 2.0 client (outbound: push users/groups to connected apps that support SCIM)
- [ ] Microsoft Entra ID integration via SCIM
- [ ] Automated user provisioning from HR systems

**High Availability:**
- [ ] Helm chart supports multi-replica deployments
- [ ] Database connection pooling tuned for HA
- [ ] Redis cluster support
- [ ] Health check / readiness / liveness probes
- [ ] Zero-downtime deployment strategy documented

**Delegated Administration:**
- [ ] Organizational units (department-level admin delegation)
- [ ] Scoped admin roles (manage only your department's users)
- [ ] Delegated approval authority

### Website Sync
- [ ] Enterprise Edition page with full feature list
- [ ] Compliance/sovereignty messaging for EU market
- [ ] Enterprise contact/demo request form
- [ ] Case study: "How a Government Agency Achieved NIS2 Compliance"
- [ ] Blog post: "Enterprise Identity Governance Without Vendor Lock-In"

### Exit Criteria
- Platform admin manages multiple tenants from single dashboard
- SoD engine blocks conflicting role assignment with explanation
- SCIM endpoint accepts user provisioning from Entra ID
- Compliance pack generates ISO 27001 evidence report
- **Release v1.0.0 — production-ready Enterprise**

---

## Milestone 5: AI Governance (Phase 2)

**Duration:** 10-12 weeks
**Goal:** AI-powered governance copilot with BYOAI

### Deliverables

**AI Infrastructure:**
- [ ] AI provider abstraction (interface for any LLM)
- [ ] Providers: OpenAI, Azure OpenAI, Anthropic, Ollama, Mistral
- [ ] BYOAI configuration: customer brings their own API key/endpoint
- [ ] pgvector extension for semantic search over governance data
- [ ] RAG pipeline over audit logs, policies, access data

**Governance Copilot:**
- [ ] Natural language queries: "Who has access to Finance space?"
- [ ] Access explanation: "Why does John have admin on Project X?"
- [ ] Risk analysis: "Show me overprivileged users"
- [ ] Recommendation engine: "These users haven't used their access in 90 days"
- [ ] Workflow suggestions: "Create an onboarding workflow for Engineering"

**Risk Engine:**
- [ ] Dormant account detection
- [ ] Overprivileged user detection
- [ ] Orphaned group detection
- [ ] Permission drift detection
- [ ] Risk scoring per user/group/connector
- [ ] Risk dashboard with trends

**Compliance Assistant:**
- [ ] Generate audit summaries in natural language
- [ ] Generate compliance evidence narratives
- [ ] Explain policy violations in plain language
- [ ] Suggest remediation actions

**Report Generation:**
- [ ] AI-generated executive summaries
- [ ] Natural language report builder
- [ ] Scheduled AI-generated governance reports

### Website Sync
- [ ] AI Edition page with demos/videos
- [ ] "Bring Your Own AI" messaging (sovereignty angle)
- [ ] Live demo: AI copilot answering governance questions
- [ ] Blog post: "AI-Powered Identity Governance — Without Sending Your Data to the Cloud"
- [ ] Update pricing for AI tier

### Exit Criteria
- Copilot answers "Who has access to X?" accurately from live data
- Risk engine identifies dormant accounts and overprivileged users
- Works with Ollama (fully local, air-gapped compatible)
- Works with OpenAI/Azure (cloud customers)
- **Release v1.5.0**

---

## Milestone 6: Ecosystem & Marketplace (Future)

**Duration:** Ongoing
**Goal:** Third-party connectors, community contributions, marketplace

### Deliverables
- [ ] Connector SDK published as separate package (Apache-2.0)
- [ ] Connector development documentation + tutorials
- [ ] Connector certification process
- [ ] Marketplace UI (browse, install, configure connectors)
- [ ] Community connectors: GitLab, Mattermost, Moodle, Jenkins
- [ ] Enterprise connectors: Entra ID, ServiceNow, SAP (Professional Services)
- [ ] Plugin architecture for custom extensions

---

## Release Strategy

| Version | Milestone | Edition | Target |
|---------|-----------|---------|--------|
| v0.1.0-alpha | M1 complete | Community | Early adopters, feedback |
| v0.1.0-beta | M2 complete | Community | Production-ready for small orgs |
| v0.2.0 | M3 complete | Community + Professional | Growing organizations |
| v1.0.0 | M4 complete | All editions | Enterprise customers |
| v1.5.0 | M5 complete | All + AI | AI-forward customers |

### Versioning
- Semantic Versioning (SemVer): MAJOR.MINOR.PATCH
- Breaking API changes only in MAJOR versions
- LTS releases for Enterprise (18-month support window)

---

## Website Content Roadmap (opencrowd.io sync)

| When | Website Content |
|------|----------------|
| Now (M0) | Landing page, vision, waitlist, brand identity |
| M0 complete | Product overview, 6 pillars explained, "Why OpenCrowd" |
| M1 complete | Feature pages (Identity, Connectors), first screenshots, docs site live |
| M2 complete | Access Matrix showcase, demo video, Community Edition download page |
| M3 complete | Professional pricing live, compliance/governance messaging |
| M4 complete | Enterprise page, contact sales, case studies |
| M5 complete | AI demo, BYOAI messaging, updated pricing |
| Ongoing | Blog, community forum, GitHub Discussions, changelog |

### Brand Consistency Checklist

To keep opencrowd.io and the platform in sync:

1. **Design tokens** — Define colors, typography, spacing in a shared `brand.json`:
   ```json
   {
     "colors": {
       "primary": "#...",
       "secondary": "#...",
       "accent": "#...",
       "background": "#...",
       "surface": "#...",
       "text": "#...",
       "muted": "#..."
     },
     "typography": {
       "heading": "Inter",
       "body": "Inter",
       "mono": "JetBrains Mono"
     }
   }
   ```
2. **WordPress theme** uses these tokens via CSS variables
3. **Platform frontend** uses these tokens in `tailwind.config.ts`
4. **Logo/assets** stored in `/docs/brand/` in the repo, shared with website
5. **Screenshots** generated from actual platform, not mockups

---

## Infrastructure Roadmap

| Phase | Environment | Purpose |
|-------|-------------|---------|
| M0 | Local (Docker Compose) | Development |
| M0 | EC2 dev instance | Your development machine |
| M1 | AWS staging (ECS + RDS) | Testing, demos |
| M2 | AWS production (ECS + RDS + CloudFront) | Community Edition hosted |
| M3+ | Multi-region, HA | Enterprise deployments |

### DNS Plan

| Subdomain | Points To | Purpose |
|-----------|-----------|---------|
| opencrowd.io | cPanel (WordPress) | Marketing site |
| app.opencrowd.io | AWS ALB → ECS | Platform application |
| api.opencrowd.io | AWS ALB → ECS | Backend API |
| auth.opencrowd.io | Keycloak on ECS | Identity provider |
| docs.opencrowd.io | GitHub Pages / Vercel | Documentation |

---

## What We Build First (Next Session)

When you're ready to start coding, the order is:

1. **Set up EC2 dev instance** (your development environment)
2. **Milestone 0 tasks** (scaffold — 20 tasks, ~10-12 hours)
3. **Share brand tokens** (get colors/fonts from your WordPress theme for platform consistency)
4. **Milestone 1** (Identity + Connectors — the first real value)

---

## Success Metrics

| Milestone | Metric |
|-----------|--------|
| M0 | Full stack runs, CI green, login works |
| M1 | 3 connectors sync users/groups successfully |
| M2 | Access Matrix shows cross-app permissions accurately |
| M3 | Complete approval workflow end-to-end |
| M4 | Multi-tenant with data isolation proven |
| M5 | AI copilot answers governance questions from live data |
| Overall | First external organization using OpenCrowd in production |
