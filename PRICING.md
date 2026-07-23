# OpenCrowd — Editions & Pricing

## Open Identity & Access Governance

OpenCrowd is an open-source identity governance platform that helps organizations manage who has access to what, across all their applications, from one place.

---

## Editions

### Community Edition (Free & Open Source)

**$0 forever** — Apache 2.0 License

Everything you need to get started with identity governance.

**Included:**
- Unlimited users and groups
- Identity management (create, update, onboard, offboard)
- Groups and membership management
- Access Matrix (view and assign permissions)
- Bidirectional sync with connected applications
- Manual sync + automatic sync (every 30 minutes)
- Basic audit log
- Public access request form
- Joiner/Leaver lifecycle flows
- Keycloak SSO/OIDC integration
- REST API access
- 2 connectors included:
  - xWiki
  - OpenProject

**Deployment:** Self-hosted (Docker Compose, Helm)

**Support:** Community (GitHub Issues, Discussions)

---

### Professional Edition

**$15/user/month** (billed annually) | **$20/user/month** (billed monthly)

*Minimum 10 users*

For growing teams that need advanced governance, reporting, and more connectors.

**Everything in Community, plus:**
- Access Profiles (permission templates for quick role assignment)
- Governance Reports (score, compliance checklist, risk analysis)
- Governance Alerts (actionable drill-down, detect over-privileged users)
- Kai AI Assistant (governance chatbot, data-aware answers)
- Real-time sync (webhook-based, 5-minute intervals)
- Advanced audit with expandable details and CSV/PDF export
- Email notifications (joiner/leaver, access requests, sync failures)
- Role-based UI (admin vs user views)
- Additional connectors:
  - Nextcloud
  - GitLab
  - Jira / Confluence (Atlassian)
  - Microsoft 365 / Azure AD
- White-label option (custom logo, colors)
- Priority email support (48h SLA)

**Deployment:** Self-hosted or OpenCrowd Cloud (managed)

---

### Enterprise Edition

**Custom pricing** — Contact sales@opencrowd.io

For organizations with complex governance requirements, compliance mandates, or large-scale deployments.

**Everything in Professional, plus:**
- SCIM 2.0 provisioning (real-time user lifecycle)
- Access Certification Campaigns (scheduled periodic reviews)
- Custom approval workflows (multi-level, role-based routing)
- Segregation of Duties (SoD) rules and conflict detection
- Compliance dashboards (SOX, ISO 27001, GDPR, NIS2)
- Scheduled governance reports (weekly/monthly email digests)
- API rate limits removed
- Custom connector development (your internal apps)
- Multi-tenant deployment (manage multiple organizations)
- Data residency options (EU, US, custom)
- On-premise deployment support
- Dedicated Customer Success Manager
- SLA: 4h response for critical issues
- Onboarding and training sessions
- Phone/video support

**Deployment:** Cloud, on-premise, or hybrid

---

## Feature Comparison

| Feature | Community | Professional | Enterprise |
|---------|:---------:|:------------:|:----------:|
| Users & Groups | Unlimited | Unlimited | Unlimited |
| Access Matrix | View + Assign | View + Assign | View + Assign |
| Bidirectional Sync | ✓ | ✓ | ✓ |
| Auto-sync interval | 30 min | 5 min | Real-time |
| Connectors | 2 | 8+ | Unlimited + Custom |
| Joiner/Leaver flows | ✓ | ✓ | ✓ |
| Access Profiles | — | ✓ | ✓ |
| Governance Reports | — | ✓ | ✓ |
| Governance Alerts | — | ✓ | ✓ |
| Kai AI Assistant | — | ✓ | ✓ |
| Access Requests | Basic | Advanced (notifications) | Workflow engine |
| Audit Log | Basic | Detailed + Export | Detailed + Retention policies |
| SSO (Keycloak/OIDC) | ✓ | ✓ | ✓ |
| LDAP/AD | Via Keycloak | Guided setup | Full integration service |
| SCIM Provisioning | — | — | ✓ |
| Access Certifications | — | — | ✓ |
| SoD Rules | — | — | ✓ |
| Compliance Dashboards | — | — | ✓ |
| Multi-tenancy | Single | Multi-tenant | Isolated multi-tenant |
| White-label | — | ✓ | ✓ |
| Deployment | Self-hosted | Self-hosted / Cloud | All options |
| Support | Community | Email (48h) | Dedicated CSM + SLA |

---

## Connector Availability

| Connector | Community | Professional | Enterprise |
|-----------|:---------:|:------------:|:----------:|
| xWiki | ✓ | ✓ | ✓ |
| OpenProject | ✓ | ✓ | ✓ |
| Nextcloud | — | ✓ | ✓ |
| GitLab | — | ✓ | ✓ |
| Jira / Confluence | — | ✓ | ✓ |
| Microsoft 365 | — | ✓ | ✓ |
| Azure AD | — | ✓ | ✓ |
| Google Workspace | — | ✓ | ✓ |
| Custom (your apps) | — | — | ✓ |

---

## Frequently Asked Questions

**Is the Community Edition really free?**
Yes. It's Apache 2.0 licensed. You can use it, modify it, and deploy it commercially. No feature expiration, no user limits, no hidden costs.

**Can I upgrade from Community to Professional?**
Yes. Your data stays the same. You just unlock additional features. No migration needed.

**What happens if I stop paying for Professional?**
Your instance keeps running. You retain all your data. Pro-only features become read-only (you can view reports but not generate new ones). You can always downgrade cleanly.

**Do you offer a free trial of Professional?**
Yes. 30-day free trial with full Professional features. No credit card required.

**How does the Cloud version work?**
We host and manage everything for you. Your data is encrypted at rest and in transit. You get a dedicated subdomain (yourcompany.opencrowd.io). Available in EU and US regions.

**Can I contribute to the open-source project?**
Absolutely. We welcome contributions. See CONTRIBUTING.md for guidelines. Core features that benefit everyone go into the Community Edition.

---

## Contact

- Website: https://opencrowd.io
- Email: sales@opencrowd.io
- GitHub: https://github.com/opencrowd/opencrowd
- Documentation: https://docs.opencrowd.io

---

*Last updated: July 2026*
