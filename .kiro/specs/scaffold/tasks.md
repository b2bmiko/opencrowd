# OpenCrowd — Scaffold Implementation Tasks

## Overview

These tasks implement the project scaffold as defined in `requirements.md` and `design.md`. They are ordered by dependency — each task builds on the previous ones. Estimated effort is per-task, assuming a single developer working with AI assistance.

---

## Phase A: Project Foundation (Backend)

### Task 1: Initialize Backend Gradle Multi-Module Project

**Effort:** 1 session (~30 min)

**Deliverables:**
- `backend/build.gradle.kts` — root build file with shared configuration
- `backend/settings.gradle.kts` — multi-module project definition
- `backend/gradle/libs.versions.toml` — centralized dependency version catalog
- Submodule skeletons: `core`, `api`, `connectors-sdk`, `connector-xwiki`, `connector-openproject`
- Each submodule has `build.gradle.kts` and `src/main/kotlin` + `src/test/kotlin` directories

**Acceptance Criteria:**
- [ ] `./gradlew build` succeeds with no errors
- [ ] All five submodules are recognized by Gradle
- [ ] Version catalog defines: Spring Boot, Kotlin, PostgreSQL driver, Flyway, Jackson, JUnit 5, MockK, Testcontainers

**Dependencies:** None

---

### Task 2: Core Module — Domain Entities & Multi-Tenancy Infrastructure

**Effort:** 1-2 sessions (~1 hour)

**Deliverables:**
- Base entity classes: `BaseEntity`, `AuditableEntity` with audit fields
- Domain entities: `User`, `Group`, `Role`, `Connector`, `AuditEvent` (JPA entities)
- Multi-tenancy infrastructure:
  - `TenantContext` (ThreadLocal holder)
  - `TenantFilter` (servlet filter extracting tenant from JWT)
  - `TenantIdentifierResolver` (Hibernate `CurrentTenantIdentifierResolver`)
  - `TenantConnectionProvider` (schema switching)
- Repository interfaces: `UserRepository`, `GroupRepository`, `RoleRepository`, `ConnectorRepository`, `AuditEventRepository`
- Domain event interfaces: `DomainEvent` sealed interface, initial event types

**Acceptance Criteria:**
- [ ] Entities compile and have proper JPA annotations
- [ ] Multi-tenancy classes compile and have correct Spring/Hibernate contracts
- [ ] Unit tests verify `TenantContext` set/get/clear behavior

**Dependencies:** Task 1

---

### Task 3: Core Module — Service Layer & Event System

**Effort:** 1 session (~30 min)

**Deliverables:**
- Service interfaces: `UserService`, `GroupService`, `RoleService`, `ConnectorService`, `AuditService`
- `DomainEventPublisher` — synchronous event bus (Spring `ApplicationEventPublisher` wrapper)
- `AuditEventListener` — listens to domain events, persists to `audit_events` table
- Base service implementation pattern (template for feature tasks)

**Acceptance Criteria:**
- [ ] Service interfaces define CRUD + search contracts
- [ ] Event publisher and listener compile correctly
- [ ] Unit test demonstrates event publish → listener receives

**Dependencies:** Task 2

---

### Task 4: API Module — Spring Boot Application & Security Configuration

**Effort:** 1-2 sessions (~1 hour)

**Deliverables:**
- `OpenCrowdApplication.kt` — Spring Boot main class
- `SecurityConfig.kt` — Spring Security with OIDC resource server configuration
- `CorsConfig.kt` — configurable CORS
- `WebConfig.kt` — Jackson ObjectMapper configuration (camelCase, Instant handling)
- `application.yml` — base config with profiles (dev, test, prod)
- `application-dev.yml` — local development settings
- `GlobalExceptionHandler.kt` — `@ControllerAdvice` with structured error responses
- `CorrelationIdFilter.kt` — generates/propagates X-Correlation-ID header
- Health check endpoint: `GET /api/v1/health` (public)
- Info endpoint: `GET /api/v1/info` (public, returns version + build info)

**Acceptance Criteria:**
- [ ] Application starts with `./gradlew :api:bootRun` (may fail on DB connection, that's OK)
- [ ] Security config rejects requests without valid JWT
- [ ] Health endpoint is accessible without auth
- [ ] Error handler returns structured JSON for 400, 401, 403, 404, 500

**Dependencies:** Task 2, Task 3

---

### Task 5: API Module — Base Controllers & DTO Pattern

**Effort:** 1 session (~30 min)

**Deliverables:**
- Base DTO classes: `PageRequest`, `PageResponse<T>`, `ApiError`, `ApiResponse<T>`
- Stub controllers (empty implementations, wired to services):
  - `UserController` — `GET /api/v1/users`, `GET /api/v1/users/{id}`, `POST /api/v1/users`
  - `GroupController` — same pattern
  - `RoleController` — same pattern
  - `ConnectorController` — same pattern
  - `AuditController` — `GET /api/v1/audit-events`
- OpenAPI configuration (springdoc-openapi): auto-generate spec at `/v3/api-docs`
- Swagger UI available at `/swagger-ui` in dev profile

**Acceptance Criteria:**
- [ ] Controllers compile and have proper annotations
- [ ] OpenAPI spec generates correctly
- [ ] Swagger UI renders in browser (dev profile)

**Dependencies:** Task 4

---

### Task 6: Database Migrations (Flyway)

**Effort:** 1 session (~30 min)

**Deliverables:**
- Flyway configuration for multi-tenancy (public schema + tenant schemas)
- `V1__create_public_schema.sql` — tenants table, platform_admins table
- `V1__create_tenant_schema.sql` — all tenant tables (users, groups, roles, connectors, audit_events, join tables, indexes)
- Tenant provisioning logic: when a new tenant is created, run tenant migrations against new schema
- Seed data migration for dev: default tenant "acme", admin user reference

**Acceptance Criteria:**
- [ ] Flyway runs on application start, creates public schema tables
- [ ] Tenant creation triggers schema creation + migration
- [ ] Seed data is present after first run in dev profile
- [ ] Integration test using Testcontainers verifies migration + tenant isolation

**Dependencies:** Task 4

---

## Phase B: Connector SDK

### Task 7: Connector SDK Module

**Effort:** 1 session (~30 min)

**Deliverables:**
- `Connector` interface (as defined in design.md)
- `ConnectorResult<T>` sealed class
- `ConnectorConfig`, `AuthConfig` data classes
- `ConnectorOperation` enum
- Model classes: `SyncOptions`, `SyncReport`, `HealthStatus`, `Permission`, `PermissionChange`, `ResourceRef`, `ApplyReport`, `SimulationReport`, `RollbackReport`
- `ConnectorRegistry` — collects all connector beans, provides lookup

**Acceptance Criteria:**
- [ ] SDK module compiles independently
- [ ] All types are documented with KDoc
- [ ] A no-op `StubConnector` implements the interface (for testing)
- [ ] Unit test verifies ConnectorRegistry collects beans

**Dependencies:** Task 1

---

### Task 8: Stub Connector Implementations

**Effort:** 1 session (~30 min)

**Deliverables:**
- `connector-xwiki` module: `XWikiConnector` class implementing `Connector` interface
  - Stub implementations returning `ConnectorResult.Failure("not_implemented")`
  - `XWikiConfig` data class for xWiki-specific settings
- `connector-openproject` module: `OpenProjectConnector` class implementing `Connector` interface
  - Same stub pattern
  - `OpenProjectConfig` data class
- Both registered as Spring `@Component` beans

**Acceptance Criteria:**
- [ ] Both connectors compile and are picked up by `ConnectorRegistry`
- [ ] `healthCheck()` returns a meaningful stub response
- [ ] Integration test verifies both appear in registry

**Dependencies:** Task 7

---

## Phase C: Frontend Foundation

### Task 9: Initialize Frontend Project

**Effort:** 1 session (~30 min)

**Deliverables:**
- `frontend/package.json` with all dependencies
- Vite configuration (`vite.config.ts`) with proxy to backend API
- TypeScript configuration (`tsconfig.json`, `tsconfig.app.json`)
- Tailwind CSS setup (`tailwind.config.ts`, `postcss.config.js`, `globals.css`)
- ESLint + Prettier configuration
- `src/main.tsx` — app entry point with providers
- `src/vite-env.d.ts` — environment variable types
- `.env.development` — local API URL, Keycloak URL

**Acceptance Criteria:**
- [ ] `pnpm install` succeeds
- [ ] `pnpm dev` starts Vite dev server
- [ ] `pnpm build` produces production bundle
- [ ] `pnpm lint` passes with no errors

**Dependencies:** None (can be done in parallel with backend tasks)

---

### Task 10: ShadCN/UI Setup & Base Components

**Effort:** 1 session (~30 min)

**Deliverables:**
- ShadCN/UI initialized with theme configuration
- Core components installed: Button, Input, Label, Card, Dialog, DropdownMenu, Table, Badge, Avatar, Separator, Sheet, Skeleton, Tabs, Tooltip, Toast (Sonner)
- Custom theme tokens: OpenCrowd brand colors, dark/light mode
- `components/ui/` directory populated
- `lib/utils.ts` — `cn()` utility function

**Acceptance Criteria:**
- [ ] All ShadCN components render without errors
- [ ] Dark/light theme toggle works
- [ ] Custom brand colors are applied

**Dependencies:** Task 9

---

### Task 11: Routing & Layout

**Effort:** 1-2 sessions (~1 hour)

**Deliverables:**
- TanStack Router setup with file-based routing
- Route tree:
  - `__root.tsx` — root layout
  - `_authenticated/` — protected route wrapper (checks auth)
  - `_authenticated/dashboard.tsx` — placeholder dashboard
  - `_authenticated/users/index.tsx` — placeholder user list
  - `_authenticated/groups/index.tsx` — placeholder
  - `_authenticated/roles/index.tsx` — placeholder
  - `_authenticated/connectors/index.tsx` — placeholder
  - `_authenticated/audit/index.tsx` — placeholder
  - `_authenticated/settings/index.tsx` — placeholder
  - `_public/login.tsx` — login page
  - `_public/callback.tsx` — OIDC callback handler
- Layout components:
  - `Sidebar` — navigation with icons, collapsible
  - `Header` — user avatar, tenant name, theme toggle
  - `Breadcrumbs` — auto-generated from route path
  - `MainLayout` — composed layout wrapper

**Acceptance Criteria:**
- [ ] All routes render their placeholder content
- [ ] Sidebar navigation works (active state highlighted)
- [ ] Breadcrumbs update on navigation
- [ ] Responsive: sidebar collapses on mobile
- [ ] Unauthenticated users are redirected to /login

**Dependencies:** Task 10

---

### Task 12: Authentication Integration (OIDC)

**Effort:** 1-2 sessions (~1 hour)

**Deliverables:**
- `lib/auth.ts` — OIDC client configuration (using `oidc-client-ts`)
- `stores/auth.store.ts` — Zustand store: user, tokens, login/logout actions
- `hooks/use-auth.ts` — hook exposing auth state and actions
- Login flow: redirect to Keycloak → callback → store tokens
- Token refresh: automatic silent refresh before expiry
- Logout: clear state + redirect to Keycloak logout
- `_authenticated` route guard: check auth state, redirect if not logged in
- Protected API client: `lib/api-client.ts` with token injection

**Acceptance Criteria:**
- [ ] User can log in via Keycloak
- [ ] Access token is included in API requests
- [ ] Token refreshes silently before expiry
- [ ] Logout clears state and redirects
- [ ] Unauthenticated API calls return 401 and trigger re-auth

**Dependencies:** Task 11, Task 4 (backend must be running for full test)

---

### Task 13: API Client & Query Setup

**Effort:** 1 session (~30 min)

**Deliverables:**
- `lib/api-client.ts` — Axios instance with interceptors (auth, error transform, correlation ID)
- `lib/query-keys.ts` — TanStack Query key factory (organized by resource)
- `hooks/use-users.ts` — example query hook: `useUsers()`, `useUser(id)`
- `types/api.ts` — shared API types: `PaginatedResponse<T>`, `ApiError`, `ApiResponse<T>`
- `types/models.ts` — domain model types: `User`, `Group`, `Role`, `Connector`, `AuditEvent`
- TanStack Query provider configured in `main.tsx` with dev tools

**Acceptance Criteria:**
- [ ] API client correctly attaches auth token
- [ ] Query hooks return typed data
- [ ] Error interceptor transforms backend errors to consistent frontend type
- [ ] React Query DevTools visible in development

**Dependencies:** Task 12

---

### Task 14: Internationalization (i18n) Setup

**Effort:** 1 session (~20 min)

**Deliverables:**
- `i18next` + `react-i18next` configuration
- `locales/en/common.json` — English translations (navigation, common labels, error messages)
- Language detection (browser preference)
- All existing UI text wrapped in `t()` calls
- `useTranslation` hook usage pattern documented

**Acceptance Criteria:**
- [ ] All visible text comes from translation files
- [ ] Changing browser language preference is detected
- [ ] Adding a new locale requires only adding a JSON file

**Dependencies:** Task 11

---

## Phase D: Infrastructure

### Task 15: Docker Compose & Dockerfiles

**Effort:** 1 session (~30 min)

**Deliverables:**
- `infrastructure/docker/docker-compose.yml` — full stack (postgres, redis, keycloak, backend, frontend)
- `infrastructure/docker/docker-compose.dev.yml` — dev overrides (volume mounts for hot-reload)
- `infrastructure/docker/backend.Dockerfile` — multi-stage JVM build
- `infrastructure/docker/frontend.Dockerfile` — multi-stage nginx build
- `infrastructure/docker/nginx.conf` — SPA-friendly nginx config (fallback to index.html)
- Root `Makefile` or scripts: `make up`, `make down`, `make logs`, `make rebuild`

**Acceptance Criteria:**
- [ ] `docker compose up` starts all services successfully
- [ ] Frontend accessible at `http://localhost:3000`
- [ ] Backend accessible at `http://localhost:8080`
- [ ] Keycloak accessible at `http://localhost:8180`
- [ ] Services survive restart (data persisted in volumes)

**Dependencies:** Task 6 (backend needs DB), Task 9 (frontend needs build)

---

### Task 16: Keycloak Realm Export

**Effort:** 1 session (~30 min)

**Deliverables:**
- `infrastructure/keycloak/realm-export.json` — complete OpenCrowd realm:
  - Client: `opencrowd-frontend` (public, PKCE)
  - Client: `opencrowd-backend` (confidential)
  - Realm roles: `platform_admin`, `tenant_admin`, `user`
  - Client roles: `manage_users`, `manage_groups`, `manage_roles`, `manage_connectors`, `view_audit`
  - Protocol mapper: `tenant_id` user attribute → token claim
  - Test users: admin, tenant-admin, regular user
  - Redirect URIs configured for localhost dev

**Acceptance Criteria:**
- [ ] Keycloak imports realm on first start
- [ ] Frontend can authenticate against Keycloak
- [ ] JWT contains `tenant_id` claim
- [ ] All three test users can log in with expected roles

**Dependencies:** None (can be prepared independently)

---

### Task 17: CI Pipeline (GitHub Actions)

**Effort:** 1 session (~20 min)

**Deliverables:**
- `.github/workflows/ci.yml`:
  - Backend job: setup JDK 21, Gradle cache, build, test (with Testcontainers for PostgreSQL)
  - Frontend job: setup Node 20, pnpm cache, install, lint, build, test
  - Both jobs run in parallel
- `.github/workflows/docker-build.yml`:
  - Builds Docker images on tag push
  - Pushes to GitHub Container Registry (ghcr.io)

**Acceptance Criteria:**
- [ ] CI runs on push to main and on pull requests
- [ ] Backend tests pass in CI (Testcontainers + PostgreSQL)
- [ ] Frontend lint + build pass in CI
- [ ] Docker build workflow produces valid images

**Dependencies:** Tasks 1-14 (needs buildable code)

---

### Task 18: Helm Chart Skeleton

**Effort:** 1 session (~30 min)

**Deliverables:**
- `infrastructure/helm/opencrowd/Chart.yaml`
- `infrastructure/helm/opencrowd/values.yaml` — configurable: replicas, image tags, resource limits, env vars
- Templates:
  - `deployment-backend.yaml`
  - `deployment-frontend.yaml`
  - `service-backend.yaml`
  - `service-frontend.yaml`
  - `ingress.yaml`
  - `configmap.yaml`
  - `secret.yaml` (template, actual secrets from external source)
  - `hpa.yaml` (horizontal pod autoscaler)
- `infrastructure/helm/opencrowd/templates/_helpers.tpl`

**Acceptance Criteria:**
- [ ] `helm template opencrowd ./infrastructure/helm/opencrowd` renders valid YAML
- [ ] `helm lint ./infrastructure/helm/opencrowd` passes
- [ ] Values are configurable for different environments

**Dependencies:** Task 15 (needs Dockerfiles defined)

---

## Phase E: Integration & Verification

### Task 19: End-to-End Integration Test

**Effort:** 1 session (~30 min)

**Deliverables:**
- Backend integration test using Testcontainers:
  - Starts PostgreSQL + Keycloak containers
  - Verifies multi-tenancy: create 2 tenants, verify data isolation
  - Verifies health endpoint returns 200
  - Verifies protected endpoint returns 401 without token
  - Verifies connector registry has both stub connectors
- Frontend smoke test (Playwright or Vitest + happy-dom):
  - Renders login page
  - Navigation links are present
  - Theme toggle works

**Acceptance Criteria:**
- [ ] `./gradlew :api:integrationTest` passes
- [ ] `pnpm test` passes in frontend
- [ ] Both can run in CI

**Dependencies:** All previous tasks

---

### Task 20: Documentation & README

**Effort:** 1 session (~20 min)

**Deliverables:**
- `README.md` — project overview, quick start, architecture diagram, contributing guide link
- `docs/architecture.md` — condensed version of design.md for contributors
- `docs/development-setup.md` — step-by-step local setup instructions
- `docs/connector-development.md` — how to build a new connector
- `CONTRIBUTING.md` — contribution guidelines, coding standards, PR process
- `LICENSE` — Apache 2.0

**Acceptance Criteria:**
- [ ] A new developer can set up the project by following `docs/development-setup.md`
- [ ] Architecture is understandable from `docs/architecture.md`
- [ ] License file is present and correct

**Dependencies:** All previous tasks

---

## Summary

| Phase | Tasks | Estimated Sessions | Estimated Time |
|-------|-------|-------------------|----------------|
| A: Backend Foundation | 1-6 | 6-8 | 3-4 hours |
| B: Connector SDK | 7-8 | 2 | 1 hour |
| C: Frontend Foundation | 9-14 | 6-8 | 3-4 hours |
| D: Infrastructure | 15-18 | 4 | 2 hours |
| E: Integration & Docs | 19-20 | 2 | 1 hour |
| **Total** | **20 tasks** | **20-24 sessions** | **10-12 hours** |

### Parallelization Opportunities

These task groups can be worked on in parallel:
- Backend (Tasks 1-8) and Frontend (Tasks 9-14) are independent until Task 12 (auth integration)
- Infrastructure (Tasks 15-18) can start once backend Task 6 and frontend Task 9 are done
- Keycloak realm export (Task 16) has no dependencies

### Suggested Execution Order

For a single developer working sequentially:

```
Task 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 (backend complete)
Task 9 → 10 → 11 → 14 (frontend structure)
Task 16 (Keycloak realm)
Task 12 → 13 (auth integration — needs backend + Keycloak)
Task 15 (Docker Compose — brings everything together)
Task 17 → 18 (CI + Helm)
Task 19 → 20 (verification + docs)
```
