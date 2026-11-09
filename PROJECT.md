# Current project status

## Scope

Maintain MTG Base Library as the shared framework for independent expansions on Minecraft Java Edition 26.3. The six Basic Lands are the only built-in cards. The Core Set, Zendikar Rising, and Phyrexia snapshots are preserved in [`archived-expansions/`](archived-expansions/README.md).

## What the evidence shows

| Area | Status and evidence |
| --- | --- |
| Shared Java engines | Headless code and tests cover mana, casting, the dedicated hand model, progression, wildcards, summons, world models, configuration, and guidebook content. |
| Expansion startup | `MtgLibrary` loads configuration and registers the base lands and independent providers. Direct JVM checks passed with the base alone and with two separate add-on JARs on both classpath and module path. |
| Headless and packaging tests | On November 9, 2026, 591 JUnit tests passed in a direct Java 21 run using cached dependencies. The [CI baseline](https://github.com/PilotLeoYan/mtg-base-library/actions/runs/42695785475) and the fresh local Gradle build each passed all 618 tests, including the JAR packaging checks. |
| Loader builds | The fresh local Gradle build compiled and packaged the Fabric Loader 0.19.5, Forge 66.1.0, and NeoForge 26.3.0.101 adapters for Minecraft 26.3. |
| Minecraft runtime | Client and dedicated-server startup with the freshly built JARs is pending for all three loaders. |
| Minecraft gameplay | Packet transport, player events and persistence, the HUD and journal UI, actual entity registration, and world generation still need Minecraft bindings. |
| Release | The planned first CurseForge file is for Forge. Listing copy, file description, and logo are available in the [release handoff](docs/release/README.md). Publication status has not been confirmed. |
| Local binaries | The four fresh JARs in the module `build/libs/` directories contain `MtgLibrary` at Java 21 bytecode. Each loader JAR contains its Java 25 entry point and expanded loader metadata. |

The [first CI run](https://github.com/PilotLeoYan/mtg-base-library/actions/runs/42695536370) failed during dependency resolution because Gradle targeted Java 21 for the Forge and NeoForge modules, whose dependencies require Java 25. Commit [`6f69f76`](https://github.com/PilotLeoYan/mtg-base-library/commit/6f69f764caa171d906a3f23335fe1946743c4ce1) limited `options.release = 21` to `common`, so the loader modules use the Java 25 toolchain target. CI then passed `make build GRADLE_FLAGS="--no-daemon --console=plain"` with Gradle 9.6.0 and Temurin JDK 25.0.5+8. The earlier local build was blocked at the Gradle distribution download with only Java 21 installed. See [the validation guide](TEST_INFRA.md) for the local baseline and runtime checks.

The local `make build` failure reported on November 9 came from a missing Java 25 toolchain. `settings.gradle` now applies Gradle's Foojay resolver convention 1.0.0 for automatic provisioning. A fresh checkout with Gradle 9.6.0 and Ubuntu OpenJDK 21.0.13.0.3 failed at `FileLockContentionHandler` initialization both before and after the change because the execution environment denied socket creation. The post-change command was `make build GRADLE_FLAGS="--no-daemon --console=plain --stacktrace"`. Retrying with OpenJDK 25.0.2 and `--rerun-tasks --no-build-cache` hit the same restriction.

Direct `javac` and JUnit runs with OpenJDK 25.0.2 and JUnit 5.10.2 passed 591 shared headless tests and 2 Forge service-loading tests while Gradle was blocked in the restricted session. Local Git metadata was read-only in that session.

The regular local terminal then passed `make build GRADLE_FLAGS="--no-daemon --console=plain --rerun-tasks --no-build-cache"` with Gradle 9.6.0 in 34 seconds, executing all 21 tasks. The Gradle cache contained a newly provisioned Temurin 25.0.5 toolchain, and the generated JUnit XML recorded 618 passed tests with zero failures or skips. The fresh Forge JAR SHA-256 is `0c526b4dd33b834f97a7820bf40f893ee4b2c3d045c88f8400db43761a33d8ff`.

## Resume here

1. Verify Forge startup in a clean Minecraft 26.3 client and dedicated server. Test with the base alone and then with two independent fixture expansions. Record exact versions, logs, and outcomes in this file. Follow the [release handoff](docs/release/README.md) for the initial upload.
2. Run the Minecraft startup matrix for Fabric and NeoForge before advertising their runtime support.
3. Bind the shared engines to the Minecraft lifecycle, persistence, and network, then validate the in-game hand, journal, entities, and world generation on the supported loaders.
