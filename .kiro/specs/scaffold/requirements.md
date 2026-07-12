# OpenCrowd — Project Scaffold Requirements

## 1. Overview

OpenCrowd is an open-source Identity & Access Governance (IGA) platform that provides a centralized governance layer for open-source collaboration ecosystems (xWiki, OpenProject, Nextcloud, GitLab, Mattermost).

This document defines the requirements for the **project scaffold** — the foundational structure, configuration, and base infrastructure that all features will be built upon.

---

## 2. Goals

- Establish a production-ready monorepo structure for frontend and backend
- Configure build tooling, dependency management, and local development environment
- Implement multi-tenancy as an architectural primitive from Day 1
- Set up authentication/authorization integration with Keycloak
- Define the Connector SDK interface contract
- Provide a working "hello world" through the full stack (UI → API → DB → response)
- Ensure the scaffold is extensible without structural refactoring

---

## 3. Functional Requirements

### FR-1: Project Structure

- **FR-1.1**: Monorepo with clear separation between `frontend/`, `backend/`, `connectors/`, `infrastructure/`, and `docs/`
- **FR-1.2**: Shared type definitions or API contracts accessible to both frontend and backend
- **FR-1.3**: Each module must be independently buildable and testable

### FR-2: Backend Foundation

- **FR-2.1**: Kotlin + Spring Boot 3.x project with Gradle (Kotlin DSL) build system
- **FR-2.2**: Multi-module Gradle project: `core`, `api`, `connectors-sdk`, `connector-xwiki`, `connector-openproject`
- **FR-2.3**: Spring Security configured for OIDC authentication with Keycloak
- **FR-2.4**: Spring Data JPA with PostgreSQL, using Flyway for schema migrations
- **FR-2.5**: Multi-tenancy support via schema-per-tenant isolation in PostgreSQL
- **FR-2.6**: Base entity classes with audit fields (createdAt, updatedAt, createdBy, updatedBy, tenantId)
- **FR-2.7**: Global exception handling with structured error responses
- **FR-2.8**: Health check and readiness endpoints (Spring Actuator)
- **FR-2.9**: OpenAPI 3.1 specification auto-generated from controllers
- **FR-2.10**: Request/response logging and correlation ID propagation

### FR-3: Frontend Foundation

- **FR-3.1**: Vite + React 18+ + TypeScript project
- **FR-3.2**: TanStack Router for type-safe file-based routing
- **FR-3.3**: TanStack Query for server state management
- **FR-3.4**: Zustand for client-side state (auth state, UI preferences)
- **FR-3.5**: ShadCN/UI component library with Tailwind CSS
- **FR-3.6**: Authentication flow: OIDC login via Keycloak, token refresh, logout
- **FR-3.7**: Base layout with sidebar navigation, header, breadcrumbs
- **FR-3.8**: Dark/light theme support
- **FR-3.9**: API client layer with automatic token injection and error handling
- **FR-3.10**: Internationalization (i18n) setup with English as default

### FR-4: Infrastructure

- **FR-4.1**: Docker Compose for local development (PostgreSQL, Redis, Keycloak)
- **FR-4.2**: Dockerfile for backend (multi-stage, JVM target)
- **FR-4.3**: Dockerfile for frontend (multi-stage, nginx static serve)
- **FR-4.4**: Environment-based configuration (dev, staging, production profiles)
- **FR-4.5**: Helm chart skeleton for Kubernetes deployment
- **FR-4.6**: GitHub Actions CI pipeline (build, test, lint for both frontend and backend)

### FR-5: Connector SDK

- **FR-5.1**: Define the Connector interface contract (Kotlin interface)
- **FR-5.2**: Operations: connect, healthCheck, syncUsers, syncGroups, syncResources, readPermissions, applyPermissions, simulate, rollback, audit
- **FR-5.3**: Connector configuration model (connection params, auth credentials, sync schedule)
- **FR-5.4**: Connector lifecycle management (register, enable, disable, remove)
- **FR-5.5**: Connector event model (sync started, sync completed, sync failed, health check result)

### FR-6: Database Foundation

- **FR-6.1**: Tenant management schema (public schema for tenant registry)
- **FR-6.2**: Per-tenant schema template with base tables: users, groups, roles, connectors, audit_events
- **FR-6.3**: Flyway migration structure supporting both public and tenant schemas
- **FR-6.4**: Seed data for development (default tenant, admin user, sample data)

### FR-7: Security

- **FR-7.1**: All API endpoints require authentication except health checks
- **FR-7.2**: RBAC enforcement at the API layer (Platform Admin, Tenant Admin, User roles)
- **FR-7.3**: CORS configuration (configurable allowed origins)
- **FR-7.4**: Rate limiting on authentication endpoints
- **FR-7.5**: Secrets management via environment variables (no hardcoded credentials)
- **FR-7.6**: Input validation on all API endpoints

---

## 4. Non-Functional Requirements

### NFR-1: Performance

- API response time < 200ms for CRUD operations (p95)
- Frontend initial load < 3 seconds on 3G connection
- Database queries use indexes; no N+1 query patterns in base setup

### NFR-2: Scalability

- Backend must be stateless (no in-memory session; use Redis for shared state)
- Horizontal scaling: multiple backend instances behind a load balancer
- Database connection pooling (HikariCP) with configurable pool size

### NFR-3: Maintainability

- Code coverage target: 80%+ for core business logic
- Consistent code style enforced by linters (ktlint for Kotlin, ESLint + Prettier for TypeScript)
- Conventional commits for git history
- Clear separation of concerns: controller → service → repository layers

### NFR-4: Observability

- Structured JSON logging (Logback with MDC for correlation IDs)
- Micrometer metrics exposed for Prometheus scraping
- Spring Actuator health/info/metrics endpoints

### NFR-5: Developer Experience

- Single command to start entire local stack (`docker compose up`)
- Hot-reload for both frontend (Vite HMR) and backend (Spring DevTools)
- Comprehensive README with setup instructions
- API documentation available at `/swagger-ui` in development

---

## 5. Constraints

- **Language**: Kotlin 1.9+ for backend, TypeScript 5+ for frontend
- **Java target**: JVM 21
- **Database**: PostgreSQL 16+
- **Auth provider**: Keycloak 24+ (delegated authentication, not embedded)
- **Container runtime**: Docker 24+ / Docker Compose v2
- **Package manager**: Gradle 8.x (backend), pnpm (frontend)
- **Node.js**: 20 LTS+
- **License**: Apache 2.0

---

## 6. Assumptions

- Developers have Docker Desktop or equivalent installed
- Keycloak is used as the IdP; no custom auth implementation needed
- The initial deployment target is Docker Compose (single machine), with Kubernetes as the production target
- Redis is used only for caching and session state; not as a primary data store
- All connectors communicate over HTTP/REST (gRPC can be added later if needed)

---

## 7. Out of Scope (for scaffold)

- Actual feature implementation (user CRUD, group management, etc.)
- AI/ML capabilities (Phase 2)
- Production Kubernetes cluster setup
- CI/CD deployment pipelines (only build/test pipeline)
- Monitoring stack (Prometheus/Grafana) — only metric exposure
- Email/notification service
- File storage service

---

## 8. Success Criteria

The scaffold is complete when:

1. `docker compose up` starts PostgreSQL, Redis, Keycloak, backend, and frontend
2. Frontend loads at `http://localhost:3000` with a login page
3. User can authenticate via Keycloak and see the dashboard layout
4. Backend serves a protected API endpoint at `http://localhost:8080/api/v1/health`
5. A second tenant can be created and data is isolated in a separate schema
6. The Connector SDK interface is defined and a no-op stub connector passes compilation
7. All tests pass (`./gradlew test` and `pnpm test`)
8. CI pipeline runs successfully on push
