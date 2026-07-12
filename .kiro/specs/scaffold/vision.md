# OpenCrowd — Product Vision & Architecture Summary

## What OpenCrowd Is

OpenCrowd is an Identity Governance & Administration (IGA) platform for the Open Source Digital Workplace.

OpenCrowd is NOT an Identity Provider. It sits ABOVE identity providers.

Identity Providers answer: "Who are you?"
OpenCrowd answers: "What should you have access to? Why? Who approved it? When does it expire? Is it still compliant?"

## Mission

Simplify Identity Governance for Open Source Collaboration Platforms.

## Vision

Become the governance platform for the Open Source Digital Workplace.

## Core Values

- Open Source
- Privacy by Design
- Security by Design
- Digital Sovereignty
- Simplicity
- Enterprise Ready
- No Vendor Lock-In
- Open Standards
- Community Driven

---

## Six Product Pillars

1. **Identity Lifecycle** — Joiner / Mover / Leaver
2. **Access Governance** — Business Roles, Access Profiles, Unified Access Matrix
3. **Policy & Compliance** — Policies, Approvals, Reviews, Audit
4. **Open Ecosystem** — Connector SDK, Marketplace
5. **AI Governance** (Phase 2) — Copilot, Risk Engine, BYOAI
6. **Sovereignty & Trust** — Privacy, Self-hosting, Open Standards, No Lock-in

---

## Target Market

- European Commission & governments
- Universities
- Healthcare organizations
- Enterprises adopting openDesk
- Organizations running open-source collaboration stacks
- Privacy-conscious organizations

---

## Technology Decisions

| Layer | Choice | Rationale |
|-------|--------|-----------|
| Frontend | Vite + React + TypeScript + ShadCN + Tailwind | SPA sufficient for admin tool; simpler than Next.js for this use case |
| Backend | Kotlin + Spring Boot 3.x | Best IAM ecosystem (Spring Security), null safety, coroutines |
| Database | PostgreSQL 16+ | Schema-per-tenant, JSONB, pgvector (Phase 2) |
| Cache | Valkey/Redis | Session state, connector health cache |
| Auth | Keycloak (delegated) | Mature OIDC/SAML/LDAP; never rebuild auth |
| Connectors | SDK interface, Spring beans | Extensible, any language for future SDK |
| Deployment | Docker Compose (dev) → Kubernetes + Helm (prod) | Self-hosted first, cloud-optional |
| CI/CD | GitHub Actions | Free for open source |
| License | AGPL-3.0 (core) + Apache-2.0 (SDK) | Open source with cloud-rider protection |

---

## Business Model: Open Core

| Edition | Target | Key Features |
|---------|--------|-------------|
| Community (AGPL) | SMBs, universities, NGOs | Full identity management, 3 connectors, access matrix, basic audit |
| Professional | Growing companies | Approval workflows, scheduled sync, access reviews, notifications |
| Enterprise | Governments, large enterprises | Multi-tenant, compliance packs, SoD, SCIM, HA, premium connectors |
| AI (Phase 2) | All tiers (add-on) | Copilot, risk engine, BYOAI |
| Hosted SaaS | Convenience seekers | Managed cloud, same features |

---

## Digital Sovereignty by Design

- Self-hosted by default
- Customer-owned encryption keys
- No vendor lock-in (no cloud-specific dependencies in core)
- Open standards only (OIDC, SAML, SCIM, REST/OpenAPI)
- Full data export always available
- Air-gapped deployment supported
- Bring Your Own AI (Phase 2)
- Every UI action has an API equivalent

---

## Architecture: Modular Monolith

```
Frontend (SPA) → REST API → Spring Boot (modular monolith) → PostgreSQL
                                    ↕
                               Keycloak (auth)
                               Valkey (cache)
                               Connectors (xWiki, OpenProject, Nextcloud)
```

Modules enforced by Gradle subprojects. Extractable to microservices when scale demands it.

---

## Online Presence

| Domain | Purpose | Tech |
|--------|---------|------|
| opencrowd.io | Marketing, branding | WordPress (cPanel) |
| app.opencrowd.io | Platform (SaaS) | React + Spring Boot (AWS) |
| docs.opencrowd.io | Documentation | Static site |
| github.com/opencrowd | Source, issues | GitHub |
