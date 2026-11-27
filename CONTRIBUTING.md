# Contributing

1. Pick an issue and say so in a comment. One person per issue.
2. Create a branch from `main`: `git checkout -b feature/short-name` (or `fix/...`, `docs/...`).
3. Keep the layers: `ui`/`web` -> `service` -> `domain`; `infra` implements the interfaces. `ArchitectureTest` fails the build otherwise.
4. Write a test for every new rule. Run `mvn verify` before pushing (it must stay green, coverage gate 60%).
5. Never commit `.env`, `data/` or `target/` (they are in `.gitignore`). Run `git status` before every commit.
6. Commit messages: short, present tense, e.g. `Add FloorStrategy for room allocation`.
7. Push the branch and open a pull request. Another team member reviews it; merge only when CI is green.
8. Adding a pattern or rejecting one: write a short ADR in `docs/adr/` and update `docs/PATTERN_REGISTER.md`.

## Extension points
* New notification channel: subclass `ChannelFactory`, add one line in `App`.
* New room allocation rule: implement `RoomAllocationStrategy`.
* New certificate format: implement `CertificateFormat`, add one line in `App`.
