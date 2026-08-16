# Contributing to Forge AI Platform

Thank you for contributing. This document explains how to contribute in a consistent, high-quality way.

What to contribute
- Bug reports and feature requests (use issue templates).
- Small, focused pull requests with tests and documentation.
- ADRs for architecture changes (docs/07_ARCHITECTURE_DECISIONS.md).

How to contribute
1. Fork the repository and create a feature branch from main.
2. Use a descriptive branch name: feat/<scope>-short-description.
3. Follow Conventional Commits for commit messages.
4. Run formatting, static analysis, and tests locally before pushing (see scripts/).
5. Open a pull request describing: problem, solution, tests, risk, rollback, and link to relevant ADRs.
6. Request review; at least one approving review required for non-trivial changes.

Local development
- Prerequisite: JDK 25 (see README.md's Getting Started for details — no devcontainer exists; a plain local JDK 25 + the committed Maven wrapper is all that's needed).
- Use `./mvnw clean verify` to build locally — matches CI exactly (compiles, runs the full test suite, checks formatting via spotless, generates coverage).

Quality gates
- All PRs must pass CI (build, tests, linters, security scans).
- No breaking the build on main.
- Architecture tests must pass for cross-module changes.

Code of conduct, security, and licensing
- See CODE_OF_CONDUCT.md and SECURITY.md for reporting and expected behaviour.
- All contributions are under the repository LICENSE.

Maintainers
- See CODEOWNERS for review responsibilities.
