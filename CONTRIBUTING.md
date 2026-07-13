# Contributing to OpenCrowd

Thank you for considering contributing to OpenCrowd!

## Development Setup

See the [Development Setup Guide](docs/development-setup.md) for detailed instructions.

## Code Style

- **Backend (Kotlin):** Follow Kotlin official conventions. We use `ktlint` for formatting.
- **Frontend (TypeScript):** ESLint + Prettier enforce style automatically.

## Git Workflow

1. Fork the repository
2. Create a feature branch: `feat/my-feature`
3. Write your changes with tests
4. Commit using [Conventional Commits](https://www.conventionalcommits.org/):
   ```
   feat(backend): add user search by department
   fix(frontend): handle empty state in user list
   ```
5. Open a Pull Request against `main`

## Pull Request Guidelines

- Keep PRs focused (one feature or fix per PR)
- All CI checks must pass
- Include a description: what changed, why, how to test
- Screenshots for UI changes

## Reporting Bugs

Open an issue with:
- Steps to reproduce
- Expected vs actual behavior
- Environment details (OS, browser, versions)

## Feature Requests

Open an issue tagged `enhancement` with:
- Problem description
- Proposed solution
- Alternatives considered

## Code of Conduct

Be respectful, inclusive, and constructive. We're building this together.
