# Agent Team Operating Model

## Purpose

This project uses an Agent team to make each software delivery step sharper and easier to review. The team mirrors the development pipeline:

```text
Architecture -> Exploration -> Implementation -> Testing -> Review
```

The goal is not to create more ceremony. The goal is to keep each Agent focused on one kind of thinking, produce handoffs that the next Agent can trust, and avoid mixing design guesses, code edits, testing, and review into one blurry pass.

## Team Topology

| Agent | Mission | Owns | Does Not Own |
| --- | --- | --- | --- |
| Architecture Agent | Decide shape before motion | Scope, boundaries, contracts, data flow, rollout risk | Large code edits |
| Exploration Agent | Find the local truth | Existing files, patterns, APIs, tests, gaps, constraints | Product decisions |
| Implementation Agent | Make the smallest correct change | Code, docs, migrations, config in assigned files | Broad refactors outside scope |
| Testing Agent | Prove behavior and surface risk | Test strategy, commands, evidence, failing cases | Silent acceptance without evidence |
| Review Agent | Catch defects before handoff | Bugs, regressions, security, maintainability, missing tests | Rewriting the feature unless asked |

## Default Routing

Use this routing for most feature or fix work:

1. **Architecture Agent**
   - Clarify the user outcome.
   - Identify affected bounded contexts: frontend, backend API, domain model, database migration, deployment, observability, or docs.
   - Define acceptance gates and likely tests.
   - Output a compact architecture note.

2. **Exploration Agent**
   - Inspect only the relevant code and docs.
   - Find existing patterns to reuse.
   - List key files, current behavior, and risk points.
   - Output concrete findings with file paths.

3. **Implementation Agent**
   - Work from the architecture note and exploration findings.
   - Keep the patch scoped to assigned ownership.
   - Update docs or progress notes when the change affects workflow, runtime, or handoff state.
   - Output changed files and any assumptions.

4. **Testing Agent**
   - Choose verification based on blast radius.
   - Prefer existing project commands:
     - Backend: `cd backend; mvn test`
     - Frontend: `cd frontend; npm run lint; npm test; npm run build`
     - Observability: `.\scripts\verify-observability-stack.ps1`
   - Output exact commands, result summary, and untested risks.

5. **Review Agent**
   - Review the final patch and verification evidence.
   - Lead with bugs, behavioral regressions, security issues, and missing tests.
   - Cite file paths and line numbers when possible.
   - Output either findings or an explicit "no blocking findings" statement.

## Handoff Template

Each Agent should hand off in this shape:

```markdown
## Role
<Architecture | Exploration | Implementation | Testing | Review>

## Summary
<What changed or what was learned>

## Evidence
<Files inspected, commands run, tests passed/failed, screenshots if relevant>

## Risks
<Known gaps, assumptions, blockers>

## Next
<Specific next action for the next Agent>
```

## Architecture Agent Checklist

- Confirm whether this is a backend, frontend, deployment, observability, docs, or cross-cutting change.
- Name the domain objects and API contracts affected.
- Decide whether database migration is required.
- Decide whether permission, audit, masking, or data-scope behavior is affected.
- Define the smallest acceptance test set.
- Call out rollback or migration risk when production data shape changes.

## Exploration Agent Checklist

- Start from `project/PROJECT_PROGRESS.md`.
- For backend work, inspect the relevant controller, service, repository, domain model, and tests.
- For frontend work, inspect API service functions, route wiring, domain types, page components, mocks, and tests.
- For docs or process work, inspect `README.md`, `project/`, and the relevant `docs/pre-implementation/` index.
- Report exact existing patterns to reuse.
- Avoid speculative implementation advice unless backed by file evidence.

## Implementation Agent Checklist

- Preserve unrelated local changes.
- Keep changes in the agreed module or file set.
- Prefer established project patterns:
  - Backend controllers return project API response conventions.
  - Services enforce permission and data-scope behavior near existing services.
  - Repositories keep in-memory and JDBC behavior aligned when both exist.
  - Frontend pages use existing API client, domain types, Ant Design patterns, and mock fallback style.
- Update tests near the behavior being changed.
- Update `project/PROJECT_PROGRESS.md` for meaningful progress or verification.

## Testing Agent Checklist

- Run focused tests first when available.
- Run broader project commands when shared behavior, routing, repository contracts, build config, or type contracts change.
- Record failures exactly enough for the next Agent to reproduce.
- If a command cannot run because of environment or sandbox limits, record the attempted command and blocker.
- Never claim verification without command evidence or a clearly marked manual inspection.

## Review Agent Checklist

- Findings first, ordered by severity.
- Focus on defects, regressions, security, data loss, permission leaks, flaky behavior, missing tests, and maintainability risks.
- Use file and line references.
- Distinguish confirmed issues from questions.
- If there are no blocking issues, say so and list residual test gaps.

## Parallel Work Rules

- Split parallel work by disjoint ownership, such as:
  - Backend API/service/repository slice
  - Frontend page/API/types slice
  - Test-only slice
  - Documentation/process slice
- Do not assign two Agents to edit the same file unless one clearly owns the final integration.
- Exploration tasks may run in parallel when they answer different questions.
- Testing can run in parallel with documentation or review preparation, but final review should see the final patch.

## Definition Of Done

A pipeline run is done when:

- The requested behavior or artifact exists in the repository.
- Relevant tests or checks have been run, or blockers are documented.
- Review has either no blocking findings or has named remaining issues clearly.
- `project/PROJECT_PROGRESS.md` is updated when the work changes project state.
- The final handoff names changed files, verification evidence, and next actions.
