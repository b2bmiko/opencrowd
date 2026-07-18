I i# OpenCrowd — Milestone 1: Identity & Connectors (Community MVP)

## Overview

Milestone 1 transforms the scaffold stubs into a working governance platform. At the end of this milestone, an admin can manage users, groups, and connectors — and see identities synchronized across xWiki, OpenProject, and Nextcloud from a single dashboard.

**Duration:** 6-8 weeks
**Edition:** Community (all features open source)
**Release:** v0.1.0-alpha

---

## Phase A: Authentication & Authorization

### Task 1: Frontend OIDC Login Flow

**Effort:** 1-2 sessions

**Deliverables:**
- `frontend/src/lib/auth.ts` — OIDC client using `oidc-client-ts`
- `frontend/src/stores/auth.store.ts` — Zustand store: user, tokens, login/logout actions
- `frontend/src/hooks/use-auth.ts` — hook exposing auth state
- Login page redirects to Keycloak
- Callback page exchanges code for tokens
- Protected route wrapper (redirect to login if unauthenticated)
- Token stored in memory, silent refresh before expiry
- Logout clears state + Keycloak session

**Acceptance Criteria:**
- [ ] User clicks "Login" → Keycloak login page shown
- [ ] After login → redirected back to dashboard with user info visible
- [ ] Token refresh works silently
- [ ] Logout clears session entirely
- [ ] Unauthenticated users can't access protected routes

**Dependencies:** Keycloak running (Task 16 from scaffold)

---

### Task 2: Backend JWT Validation (Production Security)

**Effort:** 1 session

**Deliverables:**
- Remove the dev profile `permitAll()` bypass for authenticated endpoints
- Configure proper JWT validation against Keycloak issuer
- Extract `tenant_id` claim in `TenantFilter`
- Extract roles/permissions for `@PreAuthorize` checks
- Return 401 with structured error for invalid/expired tokens
- Add `SecurityContext` helper to get current user info from JWT

**Acceptance Criteria:**
- [ ] Request without token → 401
- [ ] Request with expired token → 401
- [ ] Request with valid token → TenantContext populated correctly
- [ ] `@PreAuthorize("hasRole('manage_users')")` blocks unauthorized users
- [ ] Health/Swagger endpoints remain public

**Dependencies:** Task 1

---

### Task 3: API Client Token Injection

**Effort:** 30 min

**Deliverables:**
- Update `frontend/src/lib/api-client.ts` to inject Bearer token from auth store
- Handle 401 response → attempt silent refresh → retry request
- Handle refresh failure → redirect to login

**Acceptance Criteria:**
- [ ] All API calls include `Authorization: Bearer <token>` header
- [ ] Expired token triggers automatic refresh + retry
- [ ] If refresh fails → user logged out and redirected

**Dependencies:** Task 1, Task 2

---

## Phase B: User Management

### Task 4: User Service Implementation

**Effort:** 1-2 sessions

**Deliverables:**
- `UserServiceImpl` implementing `UserService` interface
- CRUD operations with proper validation
- Status change logic (active → disabled → offboarded)
- Domain events published on create/update/status change
- Pagination and filtering (by status, department)
- Duplicate email/username prevention

**Acceptance Criteria:**
- [ ] Create user → persisted in tenant schema + UserCreated event emitted
- [ ] Update user → fields updated + UserUpdated event emitted
- [ ] Change status → state machine enforced (can't go from offboarded → active)
- [ ] Duplicate email → validation error
- [ ] Pagination works with cursor-based approach

**Dependencies:** None (scaffold entities exist)

---

### Task 5: User Controller Implementation

**Effort:** 1 session

**Deliverables:**
- Replace stub responses in `UserController` with real service calls
- Add PATCH endpoint for partial updates
- Add `PUT /api/v1/users/{id}/status` for status changes
- Add `GET /api/v1/users/count` for dashboard stats
- Input validation with structured error responses
- `@PreAuthorize` on mutation endpoints

**Acceptance Criteria:**
- [ ] `POST /api/v1/users` creates user, returns 201
- [ ] `GET /api/v1/users` returns paginated list from DB
- [ ] `GET /api/v1/users/{id}` returns single user or 404
- [ ] `PATCH /api/v1/users/{id}` updates specific fields
- [ ] `PUT /api/v1/users/{id}/status` changes lifecycle state
- [ ] Invalid input returns structured 400 error

**Dependencies:** Task 4

---

### Task 6: User Management Frontend

**Effort:** 2-3 sessions

**Deliverables:**
- Identity page (`/identity`) with data table (TanStack Table)
- Columns: name, email, status, department, last login, actions
- Sorting, filtering (by status, department), search
- Pagination
- Create user dialog (form with validation using react-hook-form + zod)
- User detail page (`/identity/{id}`) with profile view
- Status change action (disable, lock, offboard)
- Toast notifications on success/error

**Acceptance Criteria:**
- [ ] User list loads from API with real data
- [ ] Search filters users by name/email
- [ ] Status filter works
- [ ] Create user form validates and submits
- [ ] New user appears in list after creation
- [ ] Status change reflected immediately
- [ ] Empty state shown when no users

**Dependencies:** Task 5, Task 3

---

## Phase C: Group Management

### Task 7: Group Service Implementation

**Effort:** 1-2 sessions

**Deliverables:**
- `GroupServiceImpl` implementing `GroupService` interface
- CRUD with nested group support (parent_id)
- Member management (add/remove users from group)
- Dynamic group evaluation (filter-based, runs on demand)
- Domain events for group operations
- Ownership assignment

**Acceptance Criteria:**
- [ ] Create group → persisted + GroupCreated event
- [ ] Add member → join table updated + GroupMemberAdded event
- [ ] Remove member → removed + GroupMemberRemoved event
- [ ] Nested groups: child group's parent_id is set
- [ ] Dynamic groups: filter evaluates correctly

**Dependencies:** Task 4 (user service needed for member operations)

---

### Task 8: Group Controller Implementation

**Effort:** 1 session

**Deliverables:**
- Replace stubs in `GroupController`
- `POST /api/v1/groups/{id}/members` — add members (bulk)
- `DELETE /api/v1/groups/{id}/members/{userId}` — remove member
- `GET /api/v1/groups/{id}/members` — list members
- Nested group support in responses

**Acceptance Criteria:**
- [ ] CRUD endpoints work end-to-end
- [ ] Members endpoint returns user list for a group
- [ ] Bulk add accepts array of user IDs
- [ ] Group with members can't be deleted (or cascade configured)

**Dependencies:** Task 7

---

### Task 9: Group Management Frontend

**Effort:** 2 sessions

**Deliverables:**
- Groups page with data table
- Create group dialog
- Group detail page showing members
- Add/remove members UI (searchable user picker)
- Nested group visualization (tree or indented list)

**Acceptance Criteria:**
- [ ] Group list loads from API
- [ ] Create group works
- [ ] Group detail shows member list
- [ ] Can add/remove members
- [ ] Nested groups shown with hierarchy

**Dependencies:** Task 8, Task 6

---

## Phase D: Connectors (Real Implementation)

### Task 10: Connector Management Service

**Effort:** 1 session

**Deliverables:**
- `ConnectorServiceImpl` — manages connector registrations in DB
- Store connector config (encrypted)
- Track health status, last sync timestamps
- Trigger sync operations via connector SDK interface
- Scheduled health checks (using `@Scheduled`)

**Acceptance Criteria:**
- [ ] Register connector → stored in DB
- [ ] Health check runs → updates health_status and last_health_at
- [ ] Sync trigger → calls appropriate connector SDK method
- [ ] Connection test before saving config

**Dependencies:** Task 4

---

### Task 11: xWiki Connector Implementation

**Effort:** 3-4 sessions

**Deliverables:**
- HTTP client for xWiki REST API
- `connect()` — validate URL + credentials
- `healthCheck()` — ping xWiki API
- `syncUsers()` — fetch users from xWiki, reconcile with OpenCrowd
- `syncGroups()` — fetch groups from xWiki, reconcile
- `syncResources()` — fetch spaces from xWiki
- `readPermissions()` — read space permissions
- `applyPermissions()` — write permissions to xWiki
- User mapping: OpenCrowd user ↔ xWiki user (by email or username)

**Acceptance Criteria:**
- [ ] Health check returns actual xWiki status
- [ ] Sync users imports xWiki users into OpenCrowd
- [ ] Sync creates new users, updates existing, flags removed
- [ ] Permissions read from xWiki space and displayed
- [ ] Apply permission change reflected in xWiki

**Dependencies:** Task 10, requires a test xWiki instance

---

### Task 12: OpenProject Connector Implementation

**Effort:** 3-4 sessions

**Deliverables:**
- HTTP client for OpenProject API v3
- Same operations as xWiki connector (sync users, groups, projects)
- Read project memberships
- Apply role assignments in OpenProject
- User mapping: OpenCrowd user ↔ OpenProject user

**Acceptance Criteria:**
- [ ] Health check returns actual OpenProject status
- [ ] Sync users imports from OpenProject
- [ ] Sync groups maps to OpenProject groups
- [ ] Project memberships visible as permissions
- [ ] Can assign/remove user from project

**Dependencies:** Task 10, requires a test OpenProject instance

---

### Task 13: Nextcloud Connector Implementation

**Effort:** 2-3 sessions

**Deliverables:**
- HTTP client for Nextcloud OCS/provisioning API
- Sync users and groups
- Read folder/share permissions
- Apply share permissions
- User mapping

**Acceptance Criteria:**
- [ ] Health check works
- [ ] Users sync from Nextcloud
- [ ] Groups sync from Nextcloud
- [ ] Folder shares visible as permissions

**Dependencies:** Task 10, requires a test Nextcloud instance

---

### Task 14: Connector Management Frontend

**Effort:** 2 sessions

**Deliverables:**
- Applications page (`/applications`) with connector list
- Connector cards showing: name, type, status badge, health, last sync
- Connection wizard (multi-step form: URL → credentials → test → save)
- Manual sync button
- Sync history view
- Health check indicator (real-time from API)

**Acceptance Criteria:**
- [ ] Connector list shows registered connectors
- [ ] Health badge updates (green/yellow/red)
- [ ] Wizard guides through connection setup
- [ ] Test connection validates before saving
- [ ] Manual sync triggers and shows progress

**Dependencies:** Tasks 11-13, Task 3

---

## Phase E: Audit & Dashboard

### Task 15: Audit Service Implementation

**Effort:** 1 session

**Deliverables:**
- `AuditServiceImpl` implementing `AuditService`
- AuditEventListener persists all domain events (already wired)
- Search/filter audit events by type, actor, target, date range
- Export audit events as CSV and JSON
- Correlation ID tracking (group related events)

**Acceptance Criteria:**
- [ ] Every user/group/connector operation generates an audit event
- [ ] Audit events searchable by type, actor, target
- [ ] Date range filtering works
- [ ] CSV export produces valid file
- [ ] Correlation ID groups related events together

**Dependencies:** Task 4

---

### Task 16: Audit Controller & Frontend

**Effort:** 1-2 sessions

**Deliverables:**
- Replace audit controller stub with real implementation
- Add export endpoints (`GET /api/v1/audit-events/export?format=csv`)
- Audit page with data table, filters, date range picker
- Event detail view (expandable row showing full details JSON)
- Export button (CSV, JSON)

**Acceptance Criteria:**
- [ ] Audit page shows events from all operations
- [ ] Filter by event type, actor, target works
- [ ] Date range picker filters correctly
- [ ] Export downloads CSV/JSON file

**Dependencies:** Task 15

---

### Task 17: Live Dashboard

**Effort:** 2 sessions

**Deliverables:**
- Dashboard stats from real API (`/api/v1/dashboard/stats`)
- Dashboard stats endpoint: user count, group count, connector count, governance score
- Applications health from real connector health checks
- Recent activity feed (last 10 audit events)
- Auto-refresh every 30 seconds
- Governance score calculation (basic: based on review status, inactive users, connector health)

**Acceptance Criteria:**
- [ ] Stats reflect actual database counts
- [ ] Connector health shows real status
- [ ] Recent activity shows latest audit events
- [ ] Data refreshes automatically
- [ ] Empty state handled gracefully for new installs

**Dependencies:** Tasks 5, 10, 15

---

## Phase F: Import & Lifecycle

### Task 18: User Import from Connectors

**Effort:** 1-2 sessions

**Deliverables:**
- Import users button on Identity page
- Select connector source (xWiki, OpenProject, Nextcloud)
- Preview imported users before confirming
- Conflict resolution: match by email, skip duplicates
- Bulk import with progress indicator
- Audit trail for import operations

**Acceptance Criteria:**
- [ ] Import from xWiki shows preview of users to import
- [ ] Duplicate detection (by email) works
- [ ] Import creates users + generates audit events
- [ ] Progress visible for large imports

**Dependencies:** Tasks 11-13

---

### Task 19: User Import from LDAP/Keycloak

**Effort:** 1-2 sessions

**Deliverables:**
- Import users from Keycloak realm (using Keycloak Admin API)
- Map Keycloak user attributes to OpenCrowd user fields
- Scheduled sync option (pull new users periodically)
- LDAP import (via Keycloak's LDAP federation, or direct LDAP query)

**Acceptance Criteria:**
- [ ] Import from Keycloak pulls users from realm
- [ ] Attributes mapped correctly (name, email, department from attributes)
- [ ] Scheduled sync detects new/removed users
- [ ] Works with Keycloak LDAP federation

**Dependencies:** Task 4, Keycloak running

---

### Task 20: Joiner / Leaver Flows

**Effort:** 2 sessions

**Deliverables:**
- Joiner flow: create user → assign groups → trigger connector provisioning
- Leaver flow: set status to offboarded → revoke all group memberships → trigger connector deprovisioning
- Confirmation step before leaver execution
- Audit trail for full lifecycle
- Dashboard indicator for pending joiners/leavers

**Acceptance Criteria:**
- [ ] Joiner: new user auto-provisioned to assigned connectors
- [ ] Leaver: user removed from all connected apps
- [ ] Confirmation required before leaver execution
- [ ] Full audit trail shows lifecycle events
- [ ] Reversal possible (re-enable offboarded user)

**Dependencies:** Tasks 5, 11-13

---

## Summary

| Phase | Tasks | Estimated Sessions | Focus |
|-------|-------|-------------------|-------|
| A: Auth | 1-3 | 3-4 | Login flow, JWT, token injection |
| B: Users | 4-6 | 4-6 | Real user CRUD, frontend |
| C: Groups | 7-9 | 4-5 | Group management, members |
| D: Connectors | 10-14 | 11-14 | Real connector implementations |
| E: Audit & Dashboard | 15-17 | 4-5 | Real data, live dashboard |
| F: Import & Lifecycle | 18-20 | 5-6 | Import, JML flows |
| **Total** | **20 tasks** | **31-40 sessions** | **~6-8 weeks** |

---

## Execution Order (Suggested)

```
Task 1 → 2 → 3 (auth — unlocks everything)
Task 4 → 5 → 6 (user management — core feature)
Task 7 → 8 → 9 (groups — builds on users)
Task 10 → 11 → 12 → 13 → 14 (connectors — the differentiator)
Task 15 → 16 (audit — records everything)
Task 17 (dashboard — brings it together)
Task 18 → 19 → 20 (import + lifecycle — governance value)
```

---

## Test Infrastructure Needed

| Service | Purpose | How to Get |
|---------|---------|-----------|
| xWiki test instance | Connector testing | Docker: `docker run -p 8081:8080 xwiki:lts-postgres` |
| OpenProject test instance | Connector testing | Docker: `docker run -p 8082:80 openproject/openproject:14` |
| Nextcloud test instance | Connector testing | Docker: `docker run -p 8083:80 nextcloud:latest` |

These can be added to Docker Compose as optional services for development.

---

## Exit Criteria (Milestone 1 Complete)

1. Admin logs in via Keycloak, sees dashboard with real stats
2. Admin creates a user → user appears in xWiki, OpenProject, and Nextcloud
3. Admin disables a user → access revoked across all connected apps
4. Connector health visible and accurate on dashboard
5. Audit trail records all operations with searchable history
6. All operations available via REST API (Swagger documented)
7. Community Edition fully functional for this scope
