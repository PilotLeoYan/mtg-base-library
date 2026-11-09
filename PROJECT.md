# Current project status

## Scope

Maintain MTG Base Library as the shared framework for independent expansions on Minecraft Java Edition 26.3. The six Basic Lands are the only built-in cards. The Core Set, Zendikar Rising, and Phyrexia snapshots are preserved in [`archived-expansions/`](archived-expansions/README.md).

## What the evidence shows

| Area | Status and evidence |
| --- | --- |
| Shared Java engines | Headless code and tests cover mana, casting, the dedicated hand model, progression, wildcards, summons, world models, configuration, and guidebook content. |
| Expansion startup | `MtgLibrary` loads configuration and registers the base lands and independent providers. Direct JVM checks passed with the base alone and with two separate add-on JARs on both classpath and module path. |
| Automated tests | On November 9, 2026, 591 JUnit tests passed in a direct Java 21 run using cached dependencies. Three test classes that depend on built loader artifacts were excluded. |
| Loader adapters | Fabric, Forge, and NeoForge entry points call the shared startup in source. The current source has yet to pass a full Gradle build or a Minecraft client or dedicated-server run here. |
| Minecraft gameplay | Packet transport, player events and persistence, the HUD and journal UI, actual entity registration, and world generation still need Minecraft bindings. |
| Release | The planned first CurseForge file is for Forge. Listing copy, file description, and logo are available in the [release handoff](docs/release/README.md). Publication status has not been confirmed. |
| Local binaries | The four pre-existing JARs found in module build folders on November 9 predate `MtgLibrary` and omit its class. They do not represent the current source. |

The full Gradle build was blocked before dependency resolution because this environment could not download Gradle 9.6.0 and only had Java 21. See [the validation guide](TEST_INFRA.md) for the exact evidence and next checks.

## Resume here

1. Use JDK 25 in an environment that can fetch Gradle and Maven dependencies. Build the current Forge module and run the complete tests. Inspect the freshly built JAR.
2. Verify Forge startup in a clean Minecraft 26.3 client and dedicated server. Test with the base alone and then with two independent fixture expansions. Record exact versions, logs, and outcomes in this file. Follow the [release handoff](docs/release/README.md) for the initial upload.
3. Repeat the build and startup matrix for Fabric and NeoForge before advertising their runtime support.
4. Bind the shared engines to the Minecraft lifecycle, persistence, and network, then validate the in-game hand, journal, entities, and world generation on the supported loaders.
