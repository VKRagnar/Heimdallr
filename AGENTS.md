# Heimdallr Agent Team Guide

This repository uses a focused Agent team workflow for software delivery:

```text
Architecture -> Exploration -> Implementation -> Testing -> Review
```

Before any work starts, read `project/PROJECT_PROGRESS.md`. For the complete team operating model, read `project/AGENT_TEAM.md`.

## Project Context

- Backend: Java 25, Spring Boot 4, Maven multi-module project under `backend/`.
- Frontend: React 19, Vite, TypeScript, Ant Design, React Query under `frontend/`.
- Documentation: product, architecture, UI, implementation, and acceptance materials under `docs/pre-implementation/`.
- Runtime handoff: `project/PROJECT_PROGRESS.md` is the canonical progress record.

## Agent Roles

| Role | Primary Focus | Main Outputs |
| --- | --- | --- |
| Architect Agent | Scope, boundaries, architecture fit, interface contracts | Design note, impacted modules, acceptance gates |
| Explorer Agent | Codebase discovery, existing patterns, dependency and risk mapping | Findings with file paths, recommended reuse points |
| Implementer Agent | Small, coherent code changes aligned with local style | Patch, changed file list, migration notes |
| Tester Agent | Verification plan, automated tests, regression checks | Test commands, evidence, uncovered risks |
| Reviewer Agent | Bug/risk review, security, maintainability, missing tests | Findings ordered by severity, file/line references |

## Workflow Contract

1. Architecture Agent defines the target behavior, constraints, ownership boundaries, and verification gates.
2. Explorer Agent answers concrete codebase questions before implementation starts.
3. Implementer Agent changes only the agreed files or module slice and preserves unrelated user edits.
4. Tester Agent runs the smallest credible verification set, then expands when shared behavior is touched.
5. Reviewer Agent reviews after tests, leading with defects and risks rather than summaries.

## Repository-Specific Gates

- Backend changes should normally run `mvn test` from `backend/`.
- Frontend changes should normally run `npm run lint`, `npm test`, and `npm run build` from `frontend/`.
- Observability stack checks can use `scripts/verify-observability-stack.ps1` when the change touches Prometheus, Elasticsearch, Agent Gateway, or related docs.
- Any session that changes meaningful behavior should update `project/PROJECT_PROGRESS.md` with date, summary, verification, blockers, and next actions.

## Coordination Rules

- Keep tasks narrow and assign clear file or module ownership before parallel work.
- Do not revert or overwrite unrelated edits in the worktree.
- Prefer existing architecture and implementation patterns over new abstractions.
- When a role cannot complete its gate, record the blocker and the exact follow-up needed.
- The final response should name changed files and verification evidence.
