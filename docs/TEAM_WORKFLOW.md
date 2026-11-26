# Team workflow and milestone plan

## Week 1 setup (do this on GitHub yourselves)
1. Create a private repo from the starter, copy this project's `src/`, `docs/`, `pom.xml`, `README.md` into it, invite teammates and the teacher.
2. Protect `main`: require pull request, 1 approval, CI check.
3. Create a Project board (Backlog / In progress / In review / Done) and milestones M1-M5.
4. Roles rotate each milestone: Architect, Maintainer, Reviewer/QA.

## Suggested issues per milestone (one issue = one branch = one PR)
| Milestone | Issues |
|---|---|
| M1 (wk 1-2) | #1 repo + CI green · #2 user stories · #3 ADR-0001 layers · tag v0.1 |
| M2 (wk 3-6) | domain + `Repository` + in-memory · Factory Method + Decorator channels · Adapter (SMS) · Observer · Builder · console UI · tag v0.2 |
| Checkpoint wk 7 | pattern audit -> ADR-0010 + register update |
| M3 (wk 8-11) | State (ticket) · Strategy (rooms) · Iterator (visitors) · Proxy · extension-point test · tag v0.3 |
| M4 (wk 12-14) | Command + Memento undo · `FileRepository` + contract test · ArchUnit · coverage >= 60% · tag v0.4 |
| M5 (wk 15) | rejected-pattern ADRs · README polish · reflections · slides · tag v1.0 |

Build the project in this order so the git history shows real growth; do not commit everything at once.

## Per task
```bash
git switch main && git pull
git switch -c feature/12-ticket-state
# code, test, small commits: feat: add TicketState machine (#12)
mvn verify
git push -u origin feature/12-ticket-state   # open PR: "Closes #12", name the pattern, ask for review
```
Each member needs >= 10 meaningful commits and >= 2 reviewed PRs. Review with at least one real question.
