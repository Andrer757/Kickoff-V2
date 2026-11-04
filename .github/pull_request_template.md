## Description

<!-- Provide a concise description of the changes made and the motivation behind them. -->

## Related Issue

<!-- Link related issue if applicable, e.g. Fixes #12 or Closes #34 -->

## Type of Change

- [ ] `feat`: New feature or scaffolding capability
- [ ] `fix`: Bug fix
- [ ] `docs`: Documentation improvement
- [ ] `refactor`: Code refactoring without behavioral change
- [ ] `test`: New tests or test improvements
- [ ] `chore`: Build logic, dependency upgrades, or CI workflow updates

## Pre-Merge Verification Checklist

- [ ] Monorepo unit and integration tests pass: `./gradlew test`
- [ ] Clean Architecture boundaries pass: `./kickoff arch-check`
- [ ] Spotless / Ktlint formatting applied: `./gradlew spotlessApply`
- [ ] Scaffolding verified against both archetypes (`--type app` and `--type sdk`)
- [ ] Documentation updated if relevant (`README.md`, `docs/`, `CONTRIBUTING.md`)
