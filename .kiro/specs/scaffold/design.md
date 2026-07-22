# OpenCrowd — Technical Design Document

## 1. Architecture Overview

OpenCrowd follows a **modular monolith** architecture for Phase 1, designed to be split into microservices later if needed. The system is composed of:

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (SPA)                         │
│         Vite + React + TypeScript + ShadCN               │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTPS / REST
┌──────────────────────▼──────────────────────────────────┐
│                   API Gateway Layer                       │
│            Spring Boot + Spring Security                  │
│         (OIDC token validation, RBAC, rate limit)        │
├─────────────────────────────────────────────────────────┤
│                   Application Layer                       │
│    ┌──────────┐  ┌──────────┐  ┌───────────────────┐   │
│    │   Core   │  │   API    │  │  Connector SDK    │   │
│    │ Module   │  │  Module  │  │     Module        │   │
│    └────┬─────┘  └────┬─────┘  └────────┬──────────┘   │
│         │              │                  │              │
├─────────▼──────────────▼──────────────────▼─────────────┤
│                  Data Access Layer                        │
│          Spring Data JPA + Flyway + HikariCP             │
└──────────┬────────────────────────────┬─────────────────┘
           │                            │
┌──────────▼──────────┐    ┌───────────▼──────────────┐
│    PostgreSQL 16     │    │        Redis/Valkey       │
│  (schema-per-tenant) │    │    (cache + sessions)     │
└─────────────────────┘    └──────────────────────────┘
           │
┌──────────▼──────────┐
│      Keycloak        │
│   (Identity Provider)│
└─────────────────────┘
```

### Why Modular Monolith (not microservices)?

- **Simplicity**: Single deployable for Phase 1 reduces operational overhead
- **Refactoring ease**: Module boundaries are enforced by Gradle subprojects, making extraction to separate services straightforward later
- **Transaction safety**: Cross-module operations (e.g., create user + assign role + audit) happen in a single DB transaction
- **Cost**: One container vs. five containers saves significant infrastructure cost for early adopters

---

## 2. Repository Structure

```
opencrowd/
├── .kiro/                          # Kiro IDE configuration
│   ├── specs/                      # Feature specifications
│   └── steering/                   # Development standards
├── backend/                        # Kotlin + Spring Boot
│   ├── build.gradle.kts            # Root build file
│   ├── settings.gradle.kts         # Multi-module settings
│   ├── gradle/                     # Gradle wrapper + version catalog
│   │   └── libs.versions.toml      # Centralized dependency versions
│   ├── core/                       # Domain models, services, repositories
│   │   └── src/main/kotlin/org/opencrowd/core/
│   ├── api/                        # REST controllers, DTOs, security config
│   │   └── src/main/kotlin/org/opencrowd/api/
│   ├── connectors-sdk/             # Connector interface + base classes
│   │   └── src/main/kotlin/org/opencrowd/connectors/sdk/
│   ├── connector-xwiki/            # xWiki connector implementation
│   │   └── src/main/kotlin/org/opencrowd/connectors/xwiki/
│   └── connector-openproject/      # OpenProject connector implementation
│       └── src/main/kotlin/org/opencrowd/connectors/openproject/
├── frontend/                       # React + TypeScript SPA
│   ├── package.json
│   ├── pnpm-lock.yaml
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── tailwind.config.ts
│   ├── src/
│   │   ├── main.tsx                # App entry point
│   │   ├── routes/                 # TanStack Router file-based routes
│   │   ├── components/             # Shared UI components
│   │   │   └── ui/                 # ShadCN components
│   │   ├── lib/                    # Utilities, API client, auth
│   │   ├── stores/                 # Zustand stores
│   │   ├── hooks/                  # Custom React hooks
│   │   └── types/                  # TypeScript type definitions
│   └── public/                     # Static assets
├── infrastructure/
│   ├── docker/
│   │   ├── docker-compose.yml      # Full local stack
│   │   ├── docker-compose.dev.yml  # Dev overrides (hot reload)
│   │   ├── backend.Dockerfile
│   │   └── frontend.Dockerfile
│   ├── keycloak/
│   │   └── realm-export.json       # Pre-configured realm for dev
│   └── helm/
│       └── opencrowd/              # Helm chart
│           ├── Chart.yaml
│           ├── values.yaml
│           └── templates/
├── docs/                           # Project documentation
│   ├── architecture.md
│   ├── api-contracts.md
│   └── connector-development.md
├── .github/
│   └── workflows/
│       └── ci.yml                  # Build + test pipeline
├── .gitignore
├── LICENSE                         # Apache 2.0
└── README.md
```


---

## 3. Backend Design

### 3.1 Module Responsibilities

| Module | Responsibility |
|--------|---------------|
| `core` | Domain entities, repository interfaces, service contracts, domain events, multi-tenancy infrastructure |
| `api` | REST controllers, DTOs, request validation, security configuration, OpenAPI generation, exception handling |
| `connectors-sdk` | Connector interface contract, base classes, configuration model, event definitions |
| `connector-xwiki` | xWiki REST API client, user/group/space synchronization |
| `connector-openproject` | OpenProject API client, user/group/project synchronization |

### 3.2 Multi-Tenancy Design

**Approach: Schema-per-tenant with a shared public schema for tenant registry.**

```
PostgreSQL Instance
├── public schema (shared)
│   ├── tenants              # Tenant registry
│   ├── tenant_configs       # Tenant-level settings
│   └── platform_admins      # Cross-tenant administrators
├── tenant_acme schema
│   ├── users
│   ├── groups
│   ├── roles
│   ├── connectors
│   ├── audit_events
│   └── ...
└── tenant_globex schema
    ├── users
    ├── groups
    ├── roles
    ├── connectors
    ├── audit_events
    └── ...
```

**Tenant resolution flow:**

1. Request arrives with JWT token
2. JWT contains `tenant_id` claim (set by Keycloak)
3. `TenantContext` ThreadLocal is set via a servlet filter
4. Hibernate `CurrentTenantIdentifierResolver` reads from `TenantContext`
5. `MultiTenantConnectionProvider` sets the schema on the connection: `SET search_path TO tenant_<id>`
6. All queries execute against the tenant's schema transparently

**Why schema-per-tenant (not row-level or database-per-tenant):**

| Approach | Isolation | Ops Complexity | Cost | Chosen? |
|----------|-----------|---------------|------|---------|
| Shared table + tenant_id column | Low (one bug leaks data) | Low | Low | No |
| Schema per tenant | High (separate tables) | Medium | Medium | Yes |
| Database per tenant | Maximum | High (many DBs to manage) | High | No |

Schema-per-tenant gives strong isolation, easy per-tenant backup/export (for GDPR data deletion), and moderate operational complexity. It scales to ~1000 tenants before needing sharding.

### 3.3 Authentication & Authorization

```
┌────────┐     ┌──────────────┐     ┌──────────┐
│Frontend│────►│   Keycloak   │────►│  Backend  │
│  (SPA) │◄────│  (OIDC IdP)  │     │(Resource) │
└────────┘     └──────────────┘     └──────────┘
    │                                     ▲
    │          Access Token (JWT)         │
    └─────────────────────────────────────┘
```

**Token claims expected in JWT:**

```json
{
  "sub": "user-uuid",
  "tenant_id": "acme",
  "realm_access": {
    "roles": ["platform_admin", "tenant_admin", "user"]
  },
  "resource_access": {
    "opencrowd": {
      "roles": ["manage_users", "manage_groups", "manage_connectors"]
    }
  }
}
```

**Authorization layers:**

1. **Authentication filter**: Validates JWT signature, expiry, issuer
2. **Tenant filter**: Extracts `tenant_id`, sets context, rejects if tenant is disabled
3. **RBAC filter**: Method-level `@PreAuthorize` annotations check roles/permissions
4. **Data-level**: Repository queries are automatically scoped to tenant via schema isolation

### 3.4 LDAP / Active Directory Integration

OpenCrowd supports enterprise directory services (Active Directory, OpenLDAP, FreeIPA) through **Keycloak's built-in LDAP federation** — no custom LDAP connector needed.

```
┌─────────────────┐         ┌──────────────┐         ┌──────────────┐
│  Active Directory│◄───────►│   Keycloak   │◄───────►│  OpenCrowd   │
│  / LDAP Server   │  LDAP   │  (federation)│  OIDC   │  (governance)│
└─────────────────┘         └──────────────┘         └──────────────┘
                                    │
                            ┌───────┴────────┐
                            │                │
                       ┌────▼─────┐   ┌──────▼──────┐
                       │  xWiki   │   │ OpenProject  │
                       │  (OIDC)  │   │   (OIDC)    │
                       └──────────┘   └─────────────┘
```

**How it works:**

1. **Keycloak LDAP Federation** (one-time admin setup):
   - Keycloak connects to the organization's AD/LDAP directory
   - Users are synced automatically (full sync + periodic delta sync)
   - AD groups map to Keycloak groups/roles
   - User attributes (department, title, manager) flow through to JWT claims

2. **OpenCrowd sees LDAP users as regular Keycloak users:**
   - No awareness of LDAP at the OpenCrowd level
   - Users have the same JWT claims regardless of origin (local, LDAP, social)
   - AD group memberships available via Keycloak role mappings
   - Import from Keycloak (Task 19) pulls LDAP-federated users into OpenCrowd

3. **For the end user:**
   - Login with their Windows/AD credentials (same password as everything else)
   - SSO across all connected apps (one login, all day)
   - No separate OpenCrowd password to manage

**Enterprise deployment with AD:**

| Step | What | Who |
|------|------|-----|
| 1 | Install Keycloak, create realm | Platform admin |
| 2 | Add LDAP federation (AD connection string, bind DN, search base) | Platform admin |
| 3 | Map AD groups → Keycloak roles (e.g., `CN=IT-Admins` → `manage_users`) | Platform admin |
| 4 | Configure OpenCrowd, xWiki, OpenProject as OIDC clients | Platform admin |
| 5 | Users log in with AD credentials → SSO everywhere | End users |

**Supported directories:**
- Microsoft Active Directory (AD DS)
- Azure Active Directory (via Keycloak's Azure AD identity provider)
- OpenLDAP
- FreeIPA / Red Hat IdM
- Any LDAPv3-compliant directory

### 3.5 Single Sign-On (SSO) Architecture

OpenCrowd enables SSO across all connected applications through Keycloak as the central Identity Provider.

**SSO Flow:**

```
User opens OpenCrowd → Redirected to Keycloak → Logs in once
     ↓
User opens xWiki → Redirected to Keycloak → Already authenticated → Instant access
     ↓
User opens OpenProject → Same → Instant access
     ↓
All apps share the same Keycloak session (configurable: 8-12 hours)
```

**What each app needs:**
- Register as an OIDC client in the same Keycloak realm
- Configure redirect URIs
- Map Keycloak roles to app-specific permissions

**OpenCrowd's role in SSO:**
- OpenCrowd does NOT provide the SSO session (Keycloak does)
- OpenCrowd GOVERNS what the authenticated user can access in each app
- Think: Keycloak = "are you who you say you are?" / OpenCrowd = "what are you allowed to do?"

**Supported SSO protocols:**
- OpenID Connect (OIDC) — primary, used by OpenCrowd
- SAML 2.0 — supported via Keycloak for legacy apps
- OAuth 2.0 — for API-to-API authentication

**Session management:**
- Token lifetime: configurable (default 5 min access token, 30 min refresh)
- SSO session: configurable (default 10 hours)
- Silent refresh: OpenCrowd refreshes tokens automatically before expiry
- Logout: terminates Keycloak session → logs out of all apps simultaneously

### 3.6 API Design Conventions

- Base path: `/api/v1`
- Resource naming: plural nouns (`/api/v1/users`, `/api/v1/groups`)
- Pagination: cursor-based for lists (`?cursor=xxx&limit=20`)
- Filtering: query params (`?status=active&department=engineering`)
- Sorting: `?sort=createdAt,desc`
- Error format:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      { "field": "email", "message": "must be a valid email address" }
    ],
    "correlationId": "req-abc-123",
    "timestamp": "2026-07-01T12:00:00Z"
  }
}
```

### 3.7 Event System (Internal)

Domain events for audit trail and future async processing:

```kotlin
sealed interface DomainEvent {
    val tenantId: String
    val actorId: String
    val timestamp: Instant
    val correlationId: String
}

data class UserCreated(
    override val tenantId: String,
    override val actorId: String,
    override val timestamp: Instant,
    override val correlationId: String,
    val userId: String,
    val email: String
) : DomainEvent
```

For MVP, events are persisted synchronously to the `audit_events` table. In Phase 2, they can be published to a message broker for async processing.


---

## 4. Frontend Design

### 4.1 Application Architecture

```
src/
├── main.tsx                    # React root, providers setup
├── routes/
│   ├── __root.tsx              # Root layout (sidebar + header)
│   ├── _authenticated/         # Protected route group
│   │   ├── dashboard.tsx
│   │   ├── users/
│   │   │   ├── index.tsx       # User list
│   │   │   └── $userId.tsx     # User detail
│   │   ├── groups/
│   │   ├── roles/
│   │   ├── connectors/
│   │   └── settings/
│   └── _public/                # Unprotected routes
│       ├── login.tsx
│       └── callback.tsx        # OIDC callback
├── components/
│   ├── ui/                     # ShadCN primitives (Button, Input, Dialog, etc.)
│   ├── layout/                 # Sidebar, Header, Breadcrumbs
│   ├── data-table/             # Generic data table with TanStack Table
│   └── forms/                  # Form primitives (using react-hook-form + zod)
├── lib/
│   ├── api-client.ts           # Axios/fetch wrapper with auth interceptor
│   ├── auth.ts                 # OIDC flow (oidc-client-ts)
│   ├── query-keys.ts           # TanStack Query key factory
│   └── utils.ts                # cn(), formatDate, etc.
├── stores/
│   ├── auth.store.ts           # User session, tokens
│   └── ui.store.ts             # Theme, sidebar state, locale
├── hooks/
│   ├── use-auth.ts             # Auth state + actions
│   └── use-tenant.ts           # Current tenant context
└── types/
    ├── api.ts                  # API response/request types
    └── models.ts               # Domain model types (mirroring backend DTOs)
```

### 4.2 Authentication Flow

```
1. User visits /dashboard
2. TanStack Router loader checks auth state
3. If unauthenticated → redirect to /login
4. /login initiates OIDC authorization code flow with PKCE
5. Keycloak login page shown
6. On success → redirect to /callback with auth code
7. /callback exchanges code for tokens
8. Tokens stored in memory (access) + httpOnly cookie (refresh)
9. User redirected to original destination
10. API client attaches access token to all requests
11. On 401 → attempt silent token refresh
12. If refresh fails → redirect to /login
```

### 4.3 Component Library Strategy

- **ShadCN/UI** as the base component library (copy-paste, fully customizable)
- **Tailwind CSS** for all styling (no CSS modules, no styled-components)
- **Design tokens** defined in `tailwind.config.ts` (colors, spacing, typography)
- **TanStack Table** for all data grids (sorting, filtering, pagination, row selection)
- **react-hook-form + zod** for all forms (type-safe validation)
- **Sonner** for toast notifications
- **cmdk** for command palette (Phase 1.5)

### 4.4 State Management

| State Type | Tool | Example |
|-----------|------|---------|
| Server state | TanStack Query | User list, connector status, audit logs |
| Auth state | Zustand (persisted) | Current user, tokens, tenant |
| UI state | Zustand | Sidebar open/closed, theme, locale |
| Form state | react-hook-form | Create user form, connector wizard |
| URL state | TanStack Router | Filters, pagination, active tab |

### 4.5 API Client Design

```typescript
// lib/api-client.ts
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: { 'Content-Type': 'application/json' }
});

// Request interceptor: inject auth token
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor: handle 401, transform errors
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Attempt token refresh
      const refreshed = await refreshToken();
      if (refreshed) return apiClient(error.config);
      // Refresh failed — logout
      useAuthStore.getState().logout();
    }
    return Promise.reject(transformError(error));
  }
);
```


---

## 5. Connector SDK Design

### 5.1 Interface Contract

```kotlin
interface Connector {
    val id: String
    val name: String
    val version: String
    val supportedOperations: Set<ConnectorOperation>

    // Lifecycle
    suspend fun connect(config: ConnectorConfig): ConnectorResult<Unit>
    suspend fun disconnect(): ConnectorResult<Unit>
    suspend fun healthCheck(): ConnectorResult<HealthStatus>

    // Synchronization
    suspend fun syncUsers(options: SyncOptions): ConnectorResult<SyncReport>
    suspend fun syncGroups(options: SyncOptions): ConnectorResult<SyncReport>
    suspend fun syncResources(options: SyncOptions): ConnectorResult<SyncReport>

    // Permissions
    suspend fun readPermissions(resource: ResourceRef): ConnectorResult<List<Permission>>
    suspend fun applyPermissions(changes: List<PermissionChange>): ConnectorResult<ApplyReport>
    suspend fun simulate(changes: List<PermissionChange>): ConnectorResult<SimulationReport>
    suspend fun rollback(operationId: String): ConnectorResult<RollbackReport>
}

enum class ConnectorOperation {
    SYNC_USERS, SYNC_GROUPS, SYNC_RESOURCES,
    READ_PERMISSIONS, APPLY_PERMISSIONS,
    SIMULATE, ROLLBACK
}
```

### 5.2 Result Type

```kotlin
sealed class ConnectorResult<out T> {
    data class Success<T>(val data: T) : ConnectorResult<T>()
    data class Failure(
        val code: ErrorCode,
        val message: String,
        val retryable: Boolean = false,
        val details: Map<String, Any>? = null
    ) : ConnectorResult<Nothing>()
}
```

### 5.3 Connector Configuration Model

```kotlin
data class ConnectorConfig(
    val baseUrl: String,
    val authentication: AuthConfig,
    val syncSchedule: CronExpression?,
    val options: Map<String, String> = emptyMap()
)

sealed class AuthConfig {
    data class BasicAuth(val username: String, val password: String) : AuthConfig()
    data class BearerToken(val token: String) : AuthConfig()
    data class OAuth2(
        val clientId: String,
        val clientSecret: String,
        val tokenUrl: String,
        val scopes: List<String>
    ) : AuthConfig()
    data class ApiKey(val header: String, val key: String) : AuthConfig()
}
```

### 5.4 Connector Registration

Connectors register themselves at startup via Spring's service discovery:

```kotlin
@Component
class XWikiConnector : Connector {
    override val id = "xwiki"
    override val name = "xWiki"
    override val version = "1.0.0"
    override val supportedOperations = setOf(
        ConnectorOperation.SYNC_USERS,
        ConnectorOperation.SYNC_GROUPS,
        ConnectorOperation.SYNC_RESOURCES,
        ConnectorOperation.READ_PERMISSIONS,
        ConnectorOperation.APPLY_PERMISSIONS
    )
    // ...
}
```

The `ConnectorRegistry` (in `core` module) collects all `Connector` beans and exposes them via the API.

---

## 6. Connector Connectivity Architecture

### 6.1 How OpenCrowd Connects to External Applications

OpenCrowd communicates with connected applications (xWiki, OpenProject, Nextcloud, etc.) via their **REST APIs over HTTP/HTTPS**. The connection is URL-based and credential-based — no special networking, agents, or tunnels required.

```
┌──────────────┐         HTTPS / REST API         ┌──────────────────┐
│              │ ◄──────────────────────────────► │                  │
│  OpenCrowd   │    Basic Auth / Bearer Token      │   xWiki          │
│  Backend     │    (stored per connector)         │   OpenProject    │
│              │                                   │   Nextcloud      │
└──────────────┘                                   └──────────────────┘
     Can be anywhere:                                 Can be anywhere:
     - Cloud VM                                       - Same network
     - Docker container                               - Different cloud
     - On-premise server                              - Public internet
     - Developer laptop                               - Behind VPN
```

**Connection model:**
- Each connector stores a `baseUrl` (e.g., `https://xwiki.company.com`) and credentials
- OpenCrowd initiates all connections **outbound** — no inbound ports needed on the target app
- Credentials are stored encrypted in the database and used per-request
- Works identically whether both systems are on the same LAN, different clouds, or across the public internet

### 6.2 Deployment Scenarios for Open-Source Users

| Scenario | OpenCrowd | Target Apps | Network |
|----------|-----------|-------------|---------|
| **All local** | `localhost:8080` | `localhost:8081` (xWiki) | Same machine |
| **Docker Compose** | `opencrowd:8080` | `xwiki:8080` (same compose network) | Docker bridge |
| **Same LAN** | `192.168.1.10:8080` | `192.168.1.20:8080` (xWiki server) | LAN / private |
| **Cloud + On-prem** | Cloud VM | `vpn.company.com:443` (behind firewall) | VPN or public |
| **Full cloud** | AWS/GCP VM | `xwiki.company.io` (public URL) | Internet |

### 6.3 What Open-Source Users Do

1. **Install OpenCrowd** (Docker Compose, Helm, or manual)
2. **Go to Applications** → "Connect Application"
3. **Enter the target app's URL** (wherever it's reachable from OpenCrowd)
4. **Enter credentials** (admin user with API access)
5. **Test Connection** → verifies reachability and auth
6. **Sync** → pulls users, groups, and permissions

No special agent, SDK installation, or webhook configuration is needed on the target application. OpenCrowd uses only standard REST APIs that these platforms already expose.

### 6.4 Security Considerations for Connectivity

- **HTTPS strongly recommended** in production (encrypt credentials in transit)
- **API-level credentials** — use a dedicated service account, not an admin's personal credentials
- **Network policies** — in Kubernetes/Docker, restrict outbound to known target IPs if desired
- **Credential rotation** — re-test connection after rotating credentials in the target app
- **Firewall rules** — OpenCrowd only needs outbound HTTP/HTTPS to the target app's API port


---

## 7. Database Design

### 7.1 Public Schema (Tenant Registry)

```sql
-- Tenant management (shared across all tenants)
CREATE TABLE public.tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            VARCHAR(63) UNIQUE NOT NULL,  -- used as schema name
    name            VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'active',  -- active, suspended, deleted
    plan            VARCHAR(50) NOT NULL DEFAULT 'community',
    settings        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE public.platform_admins (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id     VARCHAR(255) UNIQUE NOT NULL,  -- Keycloak subject ID
    email           VARCHAR(255) UNIQUE NOT NULL,
    name            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 7.2 Tenant Schema Template

```sql
-- Applied per tenant: CREATE SCHEMA tenant_<slug>; SET search_path TO tenant_<slug>;

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id     VARCHAR(255) UNIQUE,           -- Keycloak subject ID
    username        VARCHAR(255) UNIQUE NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    first_name      VARCHAR(255),
    last_name       VARCHAR(255),
    display_name    VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'active',  -- active, disabled, locked, pending
    department      VARCHAR(255),
    title           VARCHAR(255),
    phone           VARCHAR(50),
    avatar_url      TEXT,
    metadata        JSONB DEFAULT '{}',
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID
);

CREATE TABLE groups (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    type            VARCHAR(20) NOT NULL DEFAULT 'static',  -- static, dynamic
    parent_id       UUID REFERENCES groups(id),
    owner_id        UUID REFERENCES users(id),
    dynamic_filter  JSONB,   -- for dynamic groups: filter criteria
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID
);

CREATE TABLE group_members (
    group_id        UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    added_by        UUID,
    PRIMARY KEY (group_id, user_id)
);

CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    scope           VARCHAR(20) NOT NULL DEFAULT 'global',  -- global, application, resource
    parent_id       UUID REFERENCES roles(id),
    permissions     JSONB NOT NULL DEFAULT '[]',
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID
);

CREATE TABLE user_roles (
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id         UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by      UUID,
    expires_at      TIMESTAMPTZ,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE connectors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    connector_type  VARCHAR(50) NOT NULL,          -- xwiki, openproject, etc.
    name            VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'disconnected',  -- connected, disconnected, error
    config          JSONB NOT NULL DEFAULT '{}',   -- encrypted connection params
    last_sync_at    TIMESTAMPTZ,
    last_health_at  TIMESTAMPTZ,
    health_status   VARCHAR(20),                   -- healthy, degraded, unhealthy
    sync_schedule   VARCHAR(100),                  -- cron expression
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID
);

CREATE TABLE audit_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(100) NOT NULL,
    actor_id        UUID,
    actor_email     VARCHAR(255),
    target_type     VARCHAR(50),                   -- user, group, role, connector
    target_id       UUID,
    action          VARCHAR(50) NOT NULL,          -- created, updated, deleted, assigned, etc.
    details         JSONB DEFAULT '{}',
    correlation_id  VARCHAR(100),
    ip_address      INET,
    user_agent      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_department ON users(department);
CREATE INDEX idx_groups_type ON groups(type);
CREATE INDEX idx_groups_parent ON groups(parent_id);
CREATE INDEX idx_roles_scope ON roles(scope);
CREATE INDEX idx_audit_events_type ON audit_events(event_type);
CREATE INDEX idx_audit_events_actor ON audit_events(actor_id);
CREATE INDEX idx_audit_events_target ON audit_events(target_type, target_id);
CREATE INDEX idx_audit_events_created ON audit_events(created_at DESC);
CREATE INDEX idx_connectors_type ON connectors(connector_type);
```


---

## 8. Infrastructure Design

### 8.1 Local Development Stack (Docker Compose)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: opencrowd
      POSTGRES_USER: opencrowd
      POSTGRES_PASSWORD: opencrowd_dev
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: valkey/valkey:8-alpine
    ports: ["6379:6379"]

  keycloak:
    image: quay.io/keycloak/keycloak:24.0
    ports: ["8180:8080"]
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: opencrowd
      KC_DB_PASSWORD: opencrowd_dev
    command: start-dev --import-realm
    volumes:
      - ./keycloak/realm-export.json:/opt/keycloak/data/import/realm.json

  backend:
    build:
      context: ../../backend
      dockerfile: ../infrastructure/docker/backend.Dockerfile
    ports: ["8080:8080"]
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/opencrowd
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/opencrowd
    depends_on: [postgres, redis, keycloak]

  frontend:
    build:
      context: ../../frontend
      dockerfile: ../infrastructure/docker/frontend.Dockerfile
    ports: ["3000:80"]
    depends_on: [backend]
```

### 8.2 Keycloak Realm Configuration

Pre-configured realm (`opencrowd`) with:

- **Clients:**
  - `opencrowd-frontend` (public, PKCE authorization code flow)
  - `opencrowd-backend` (confidential, for service-to-service)
- **Realm Roles:** `platform_admin`, `tenant_admin`, `user`
- **Client Roles (opencrowd-backend):** `manage_users`, `manage_groups`, `manage_roles`, `manage_connectors`, `view_audit`
- **Custom Token Mapper:** Adds `tenant_id` claim from user attribute to access token
- **Default Users (dev only):**
  - `admin@opencrowd.local` / `admin` (platform_admin)
  - `tenant-admin@acme.local` / `admin` (tenant_admin, tenant=acme)
  - `user@acme.local` / `user` (user, tenant=acme)

### 8.3 Backend Dockerfile (Multi-stage)

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY core/ core/
COPY api/ api/
COPY connectors-sdk/ connectors-sdk/
COPY connector-xwiki/ connector-xwiki/
COPY connector-openproject/ connector-openproject/
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/api/build/libs/api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 8.4 CI Pipeline (GitHub Actions)

```yaml
name: CI
on: [push, pull_request]
jobs:
  backend:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env: { POSTGRES_DB: opencrowd_test, POSTGRES_USER: test, POSTGRES_PASSWORD: test }
        ports: ["5432:5432"]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: ./gradlew build
        working-directory: backend
      - run: ./gradlew test
        working-directory: backend

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: 'pnpm', cache-dependency-path: frontend/pnpm-lock.yaml }
      - run: pnpm install --frozen-lockfile
        working-directory: frontend
      - run: pnpm lint
        working-directory: frontend
      - run: pnpm build
        working-directory: frontend
      - run: pnpm test
        working-directory: frontend
```

---

## 9. Security Design

### 9.1 Defense in Depth

| Layer | Mechanism |
|-------|-----------|
| Network | HTTPS everywhere, CORS whitelist, rate limiting |
| Authentication | Keycloak OIDC, JWT validation, token expiry |
| Authorization | RBAC via Spring Security `@PreAuthorize`, method-level |
| Data | Schema isolation (multi-tenancy), parameterized queries (JPA) |
| Secrets | Environment variables, never in code or config files |
| Audit | All mutations logged with actor, timestamp, correlation ID |
| Input | Bean Validation (Jakarta), Zod schemas (frontend) |

### 9.2 Sensitive Data Handling

- Connector credentials stored encrypted in DB (AES-256-GCM, key from env)
- Passwords never stored in OpenCrowd (delegated to Keycloak)
- PII fields flagged in entity model for GDPR export/deletion
- Audit logs retained based on tenant configuration (default: 1 year)

---

## 10. Technology Decisions & Rationale

| Decision | Choice | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Backend language | Kotlin | Java, Go | Concise syntax, null safety, coroutines; full Spring ecosystem access |
| Backend framework | Spring Boot 3 | Quarkus, Ktor | Largest ecosystem, Spring Security is unmatched for IAM, most hiring pool |
| Build tool | Gradle (Kotlin DSL) | Maven | Better multi-module support, incremental builds, Kotlin DSL for type safety |
| Frontend framework | React + Vite | Next.js, SvelteKit | SPA is sufficient (admin tool behind auth), simpler deployment, no SSR needed |
| Router | TanStack Router | React Router | Type-safe, file-based, better loader pattern |
| State management | TanStack Query + Zustand | Redux, Jotai | Query handles server state cleanly; Zustand is minimal for client state |
| UI library | ShadCN + Tailwind | MUI, Ant Design | Full control, no vendor lock-in, excellent accessibility, consistent design |
| Database | PostgreSQL 16 | MySQL, CockroachDB | Best JSON support, schema-per-tenant, RLS, pgvector for Phase 2 |
| Cache | Valkey (Redis-compatible) | Memcached | Rich data structures, pub/sub for future events, open-source license |
| Auth provider | Keycloak | Authentik, Zitadel | Most mature, best LDAP/AD integration, widest protocol support |
| Multi-tenancy | Schema-per-tenant | Row-level, DB-per-tenant | Strong isolation + moderate ops complexity |
| Container | Docker + Compose | Podman | Widest adoption, best tooling support |
| CI | GitHub Actions | GitLab CI, Jenkins | Free for open source, good ecosystem |

---

## 11. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Schema-per-tenant hits PostgreSQL limits at scale | High | Design for migration to Citus (distributed PostgreSQL) at 500+ tenants |
| Keycloak is complex to configure | Medium | Provide pre-configured realm export; document setup clearly |
| Connector API changes break sync | High | Version connectors independently; integration tests against live APIs |
| Frontend bundle grows large | Medium | Code-splitting per route; lazy load heavy components (TanStack Table) |
| Multi-tenancy bugs leak data | Critical | Automated tests asserting cross-tenant isolation; schema-level guarantees |
