# OpenCrowd

**Identity & Access Governance for the Open Source World**

OpenCrowd is an open-source Identity Governance & Administration (IGA) platform that centralizes governance across open-source collaboration platforms.

OpenCrowd is NOT an Identity Provider. It sits ABOVE identity providers.

- Identity Providers answer: *"Who are you?"*
- OpenCrowd answers: *"What should you have access to? Why? Who approved it? When does it expire? Is it still compliant?"*

## Supported Platforms

| Platform | Status |
|----------|--------|
| xWiki | Connector (Phase 1) |
| OpenProject | Connector (Phase 1) |
| Nextcloud | Connector (Phase 1) |
| GitLab | Planned |
| Mattermost | Planned |

## Quick Start (Development)

### Prerequisites

- Docker + Docker Compose
- JDK 21
- Node.js 20+ / pnpm
- Git

### Start the stack

```bash
# Clone the repo
git clone git@github.com:b2bmiko/opencrowd.git
cd opencrowd

# Start infrastructure (PostgreSQL, Redis, Keycloak)
make up

# Start frontend dev server
cd frontend
pnpm install
pnpm dev
```

### Access

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api/v1/health |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Keycloak | http://localhost:8180 (admin/admin) |

### Test Users (Keycloak)

| User | Password | Role |
|------|----------|------|
| admin@opencrowd.local | admin | Platform Admin |
| tenant-admin@acme.local | admin | Tenant Admin |
| user@acme.local | user | User |

## Architecture

```
Frontend (SPA) → REST API → Spring Boot (modular monolith) → PostgreSQL
                                    ↕
                               Keycloak (auth)
                               Valkey (cache)
                               Connectors (xWiki, OpenProject, Nextcloud)
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Vite + React + TypeScript + ShadCN + Tailwind |
| Backend | Kotlin + Spring Boot 3.x |
| Database | PostgreSQL 16 (schema-per-tenant) |
| Cache | Valkey (Redis-compatible) |
| Auth | Keycloak (OIDC) |
| Deployment | Docker Compose (dev) → Kubernetes + Helm (prod) |

## Project Structure

```
opencrowd/
├── backend/                    # Kotlin + Spring Boot
│   ├── core/                   # Domain entities, services, multi-tenancy
│   ├── api/                    # REST controllers, security, DTOs
│   ├── connectors-sdk/         # Connector interface contract
│   ├── connector-xwiki/        # xWiki connector
│   └── connector-openproject/  # OpenProject connector
├── frontend/                   # React + TypeScript SPA
├── infrastructure/
│   ├── docker/                 # Docker Compose + Dockerfiles
│   ├── keycloak/               # Realm export
│   └── helm/                   # Kubernetes Helm chart
└── docs/                       # Documentation
```

## Make Commands

```bash
make up        # Start all Docker services
make down      # Stop all services
make logs      # Follow logs
make ps        # Show running containers
make clean     # Stop + delete data (fresh start)
make rebuild   # Rebuild containers
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

AGPL-3.0 (core platform) — see [LICENSE](LICENSE)
Apache-2.0 (Connector SDK)

## Links

- Website: https://opencrowd.io
- Documentation: https://docs.opencrowd.io
- Issues: https://github.com/b2bmiko/opencrowd/issues
