# MTG Base Library contributor instructions

Start with [PROJECT.md](PROJECT.md) for the current evidence and next work. Use [README.md](README.md) for navigation. Use [SKILL.md](SKILL.md) as the project-specific implementation guide.

## Scope

- Keep the base library generic. Its built-in catalog is limited to the six Basic Lands. Develop concrete sets as independent add-ons with their own identifiers and loader metadata.
- Preserve the source snapshots under `archived-expansions/` and keep them outside `settings.gradle` and the base JARs. Read the [archive notes](archived-expansions/README.md) before resuming a snapshot.
- Write project code, comments, documentation, and default player-facing text in English. Keep text intended for translation in locale resources.
- Treat existing Git changes as work to preserve. Inspect status before editing. Use small Git changes and record any environment limitations that prevent staging or committing.

## Architecture and validation

- Keep Minecraft and loader imports in loader modules. Keep client-only code off dedicated-server class loading paths.
- Preserve existing API behavior and stable IDs. Add extension hooks in the shared module and put the corresponding Minecraft integration in each supported loader.
- Treat [`gradle.properties`](gradle.properties) as the version reference and source code as the interface reference. Build with JDK 25 and target Java 21 bytecode.
- Label verification by layer. Report headless domain tests, loader build results, and actual Minecraft client and server tests separately. Packaging checks and platform mocks cannot establish runtime support.
- Validate release binaries from a fresh build. See [TEST_INFRA.md](TEST_INFRA.md) and the [Forge release handoff](docs/release/README.md).
- Update [PROJECT.md](PROJECT.md) when implementation or verification status changes. Record commands, versions, and outcomes that the next agent needs. Keep old agent handoffs as historical references.
