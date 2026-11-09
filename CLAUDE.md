# spring-ai-damage-detector

## Development Paradigm

This project follows the **object-oriented** paradigm. Use @nw-software-crafter for implementation.

## Mutation Testing Strategy

This project uses **per-feature** mutation testing. Runs after refactoring during each delivery, scoped to modified files. Kill rate gate: >= 80%.

PIT is limited to classes with direct tests (domain, `jev/`, `cli/` logic, `OutputFolder`); classes covered only by Cucumber scenarios are outside the gate, because PIT does not count Cucumber (user decision 2026-11-06).
