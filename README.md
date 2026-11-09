# MTG Base Library

MTG Base Library is a framework for independent Magic: The Gathering-inspired expansions for Minecraft Java Edition 26.3. The base catalog contains Plains, Island, Swamp, Mountain, Forest, and Wastes. Expansions supply their own cards, rules, effects, bosses, and journal content through a shared Java API.

The shared engines and expansion startup have passed headless tests. The Fabric, Forge, and NeoForge entry points exist in source. Builds and Minecraft runtime checks for the current source are pending. See [project status](PROJECT.md) before using or publishing a JAR.

## Repository map

| Path | Purpose |
| --- | --- |
| [`common/`](common/) | Loader-neutral expansion API, configuration, localization, and gameplay models |
| [`fabric/`](fabric/), [`forge/`](forge/), [`neoforge/`](neoforge/) | Loader entry points, metadata, and platform adapters |
| [`archived-expansions/`](archived-expansions/README.md) | Preserved Core Set, Zendikar Rising, and Phyrexia source snapshots outside the build |
| [`gradle.properties`](gradle.properties) | Mod version and Minecraft, Java, and loader pins |

## Start here

| Task | Read |
| --- | --- |
| Continue work as an agent | [AGENTS.md](AGENTS.md) and [current status](PROJECT.md) |
| Work on the shared API or loaders | [Architecture](docs/architecture.md) |
| Author an independent expansion | [Expansion guide](docs/expansions.md) |
| Build or validate a change | [Testing and verification](TEST_INFRA.md) |
| Prepare a CurseForge upload | [Release handoff](docs/release/README.md) and [listing copy](CURSEFORGE.md) |

## Build

Use JDK 25 with the checked-in Gradle wrapper. Java sources target Java 21 bytecode. Minecraft 26.3 runs on Java 25.

```sh
java -version
make build
```

Use `make test` for tests, `make forge` (or `make fabric` or `make neoforge`) for one loader, and `make clean` to remove build outputs. The Gradle wrapper also works directly. GitHub Actions runs the full build and test suite on pushes to `main` and pull requests.

The `mtg-common` JAR is the compile-time API artifact. The three loader builds bundle the common classes. Install the JAR for the loader in use. See [the test guide](TEST_INFRA.md) for validation and artifact checks.
