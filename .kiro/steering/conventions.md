# OpenCrowd — Project Conventions & Standards

## Project Context

OpenCrowd is an open-source Identity & Access Governance platform. The codebase is a monorepo with a Kotlin/Spring Boot backend and a React/TypeScript frontend.

---

## Repository Layout

```
opencrowd/
├── backend/          # Kotlin + Spring Boot (Gradle multi-module)
├── frontend/         # React + TypeScript + Vite (pnpm)
├── infrastructure/   # Docker, Helm, Keycloak config
├── docs/             # Project documentation
└── .kiro/            # IDE specs and steering
```

---

## Backend Conventions (Kotlin + Spring Boot)

### Language & Style

- Language: Kotlin 1.9+ targeting JVM 21
- Follow Kotlin official coding conventions
- Use `ktlint` for formatting (no manual style debates)
- Prefer immutable data: `val` over `var`, data classes for DTOs
- Use sealed classes/interfaces for type hierarchies
- Prefer expression functions for single-expression methods
- No wildcard imports

### Package Structure

```
org.opencrowd.<module>/
├── config/          # Spring configuration classes
├── controller/      # REST controllers (API module only)
├── dto/             # Request/Response data transfer objects
├── entity/          # JPA entities
├── repository/      # Spring Data repositories
├── service/         # Business logic
├── event/           # Domain events
└── exception/       # Custom exceptions
```

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Entity | PascalCase, singular | `User`, `AuditEvent` |
| Repository | Entity + Repository | `UserRepository` |
| Service | Entity + Service | `UserService` |
| Controller | Entity + Controller | `UserController` |
| DTO (request) | Create/Update + Entity + Request | `CreateUserRequest` |
| DTO (response) | Entity + Response | `UserResponse` |
| Migration | V{n}__{description}.sql | `V1__create_users_table.sql` |
| Test | Class + Test / Class + IntegrationTest | `UserServiceTest` |

### API Conventions

- Base path: `/api/v1`
- Resource paths: plural nouns (`/users`, `/groups`, `/roles`)
- Use HTTP methods correctly: GET (read), POST (create), PUT (full update), PATCH (partial), DELETE
- Pagination: `?cursor=<id>&limit=20` (cursor-based)
- Filtering: query params matching field names (`?status=active`)
- Sorting: `?sort=fieldName,direction` (`?sort=createdAt,desc`)
- Always return structured errors (see design.md for format)
- Use `@Valid` on request bodies for input validation
- Use `@PreAuthorize` for method-level authorization

### Testing

- Unit tests: JUnit 5 + MockK (mock dependencies)
- Integration tests: Testcontainers (PostgreSQL, Keycloak)
- Test naming: `should <expected behavior> when <condition>`
- Test file placement: mirror source structure in `src/test/kotlin`
- Minimum coverage: 80% for service layer

### Dependencies

- Add dependencies via `gradle/libs.versions.toml` (version catalog)
- Pin exact versions (no dynamic version ranges)
- Prefer well-maintained, widely-used libraries
- Document why a dependency was added if non-obvious

---

## Frontend Conventions (React + TypeScript)

### Language & Style

- TypeScript strict mode (`strict: true` in tsconfig)
- ESLint + Prettier for formatting
- Prefer functional components (no class components)
- Prefer named exports over default exports
- Use `interface` for object shapes, `type` for unions/intersections
- No `any` — use `unknown` and narrow with type guards

### File Structure

| File Type | Location | Naming |
|-----------|----------|--------|
| Pages/Routes | `src/routes/` | kebab-case matching URL (`users/index.tsx`) |
| Shared components | `src/components/` | PascalCase (`DataTable.tsx`) |
| ShadCN components | `src/components/ui/` | kebab-case (ShadCN default) |
| Hooks | `src/hooks/` | `use-<name>.ts` |
| Stores | `src/stores/` | `<name>.store.ts` |
| Utilities | `src/lib/` | kebab-case (`api-client.ts`) |
| Types | `src/types/` | kebab-case (`models.ts`) |

### Component Conventions

```typescript
// Component file structure
interface UserCardProps {
  user: User;
  onEdit?: (user: User) => void;
}

export function UserCard({ user, onEdit }: UserCardProps) {
  // hooks first
  // derived state
  // handlers
  // render
}
```

- Props interface defined above the component
- Destructure props in function signature
- Hooks at the top, then derived state, then handlers, then JSX
- One component per file (exceptions: small helper components used only in that file)
- Colocate component-specific types in the same file

### State Management Rules

| State Type | Where | Tool |
|-----------|-------|------|
| Server/async data | Close to usage | TanStack Query |
| Global UI state | `stores/` | Zustand |
| Form state | Inside form component | react-hook-form |
| URL state | Route params/search | TanStack Router |
| Component-local state | Component | `useState` / `useReducer` |

- Never duplicate server state in Zustand — use TanStack Query
- Never store derived state — compute it

### API Integration Pattern

```typescript
// hooks/use-users.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/lib/query-keys';
import { apiClient } from '@/lib/api-client';

export function useUsers(params?: UserListParams) {
  return useQuery({
    queryKey: queryKeys.users.list(params),
    queryFn: () => apiClient.get<PaginatedResponse<User>>('/users', { params }),
  });
}

export function useCreateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserRequest) => apiClient.post<User>('/users', data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.users.all }),
  });
}
```

### Styling

- Tailwind CSS only (no inline styles, no CSS modules)
- Use `cn()` utility for conditional classes
- Design tokens in `tailwind.config.ts` (never hardcode colors/spacing)
- Responsive-first: start with mobile, add `md:` / `lg:` breakpoints
- Dark mode via `class` strategy (Tailwind dark mode)

### Testing

- Component tests: Vitest + React Testing Library
- Test user behavior, not implementation details
- Test file placement: colocated (`Component.test.tsx`) or in `__tests__/`
- E2E tests (future): Playwright

---

## Git Conventions

### Branching

- `main` — stable, deployable
- `develop` — integration branch (if needed)
- Feature branches: `feat/<short-description>`
- Fix branches: `fix/<short-description>`
- Chore branches: `chore/<short-description>`

### Commits

Follow Conventional Commits:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `ci`, `style`, `perf`

Scopes: `backend`, `frontend`, `infra`, `sdk`, `connector-xwiki`, `connector-openproject`

Examples:
```
feat(backend): add multi-tenancy schema resolution
fix(frontend): handle token refresh race condition
chore(infra): update PostgreSQL to 16.3
docs: add connector development guide
```

### Pull Requests

- Keep PRs focused (one feature/fix per PR)
- Title matches conventional commit format
- Description includes: what changed, why, how to test
- All CI checks must pass before merge
- Squash merge to main (clean history)

---

## Documentation

- Code comments: explain "why", not "what"
- KDoc on all public APIs (backend)
- JSDoc on exported hooks and utilities (frontend)
- ADRs (Architecture Decision Records) in `docs/adr/` for significant decisions
- Keep README.md focused on getting started; detailed docs in `docs/`

---

## Security Practices

- Never commit secrets (`.env` files are gitignored)
- Use environment variables for all configuration
- Validate all input at the API boundary
- Use parameterized queries (JPA handles this)
- Log security events (auth failures, permission denials)
- Review dependencies for known vulnerabilities (`./gradlew dependencyCheckAnalyze`, `pnpm audit`)
- Connector credentials encrypted at rest (AES-256-GCM)

---

## Performance Guidelines

- Backend: avoid N+1 queries (use `@EntityGraph` or fetch joins)
- Backend: paginate all list endpoints (no unbounded queries)
- Frontend: lazy-load routes (TanStack Router handles this)
- Frontend: virtualize long lists (TanStack Virtual if >100 rows)
- Cache aggressively: use Redis for connector health status, session data
- Measure before optimizing: use Spring Actuator metrics + browser DevTools

---

## Accessibility

- All interactive elements must be keyboard navigable
- Use semantic HTML (`<nav>`, `<main>`, `<aside>`, `<button>`)
- ShadCN components provide ARIA attributes by default — don't remove them
- Color contrast: WCAG AA minimum (4.5:1 for text)
- Screen reader labels on icon-only buttons (`aria-label`)
- Focus management on dialogs and route changes
