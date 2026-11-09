# Testing and verification

## Full build

Use JDK 25 and allow the wrapper and Maven dependencies to download. [gradle.properties](gradle.properties) defines the target versions.

```sh
java -version
./gradlew clean build
```

Use `./gradlew :forge:build` for a Forge-only build. Use `./gradlew test` for the complete headless suite. The `:common:test` task also requests the three loader JARs for artifact tests.

| Check | What it establishes |
| --- | --- |
| JUnit domain tests | Shared rules, packet encoding and decoding, configuration, registry behavior, and headless feature scenarios |
| Gradle packaging tests | Loader metadata, service descriptors, and shared class packaging in fresh JARs |
| Minecraft client and dedicated server | Actual loader startup, add-on discovery in its runtime, and game integration |

Look under [common/src/test/java](common/src/test/java/) for unit tests, stress tests, and headless scenarios. Add a regression case in the relevant suite when existing tests miss a behavior change. Reports from Gradle are available under each module's `build/reports/tests/test/`.

## Minecraft startup matrix

For each loader build you intend to support, use Minecraft 26.3 with its pinned loader and JDK 25. Use a fresh client profile and dedicated server, then run these scenarios.

1. Launch with only the matching base JAR. Verify startup and creation of the four TOML files under `config/mtg/`.
2. Launch with one independent fixture add-on, then with two. Verify the catalog contains all expected definitions and effects and that each add-on contributes through its hooks. Use a fixture or startup diagnostics to inspect registry state while the in-game UI is pending.
3. Join the dedicated server from a matching client. Record which features you actually exercised, along with game and loader versions, the JAR checksum, and the result.

## Recorded baseline

On November 9, 2026, direct Java 21 compilation and JUnit Platform execution with cached JUnit 5.10.2 dependencies passed 591 tests. The run excluded the following classes because they inspect built loader JARs.

- `com.mtg.build.CrossLoaderJarArtifactTest`
- `com.mtg.adversarial.AdversarialM1StressTest`
- `com.mtg.test.e2e.tier1.Feature01_MultiLoaderScaffoldingTest`

Separate Java smoke checks exercised the base alone and two independent SPI provider JARs on the classpath and module path. The full build stopped at the Gradle distribution download.
