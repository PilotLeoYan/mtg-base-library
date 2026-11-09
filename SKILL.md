---
name: mtg-expansion-framework
description: Use when maintaining the MTG Base Library, connecting it to a Minecraft loader, or developing an independent expansion for Minecraft Java Edition 26.3.
version: 2.1.0
---

# MTG Base Library development

Read [AGENTS.md](AGENTS.md) and [PROJECT.md](PROJECT.md) before changing the project. Use the [architecture guide](docs/architecture.md) to locate code and the [expansion guide](docs/expansions.md) to author or review an add-on.

## SOLID Principles

- **Single Responsibility Principle**. Keep immutable card definitions, mutable player state, and Minecraft transport in their respective layers.
- **Open/Closed Principle**. Extend the library through `IExpansionSet` and namespaced registries. Put set-specific rules and content in the add-on.
- **Liskov Substitution Principle**. Preserve the interface contracts when implementing effects, event listeners, and loader adapters.
- **Interface Segregation Principle**. Use the smallest relevant contract for a card effect, event hook, or platform operation.
- **Dependency Inversion Principle**. Keep the shared rules independent of Fabric, Forge, NeoForge, and client rendering. Adapt loader and game events at the boundary.

## Workflows

1. For library changes, start from the normal entry point and confirm that the base starts with the six Basic Lands. Preserve namespaced IDs and fail visibly on conflicts.
2. For expansions, reserve `base` and the `mtg` namespace for the library. Use the service descriptor and a unique package, set code, and mod ID. Verify two add-ons can coexist.
3. For Minecraft bindings, start with the matching loader module. Keep gameplay validation server-side and test both physical sides.
4. For tests, use [TEST_INFRA.md](TEST_INFRA.md). Separate headless results from loader builds and game runs. For releases, use the [release handoff](docs/release/README.md).

Use JDK 25 for builds and runtime testing. Keep the common source Java 21-compatible and externalize translatable text. Preserve archived expansions outside the base build.
