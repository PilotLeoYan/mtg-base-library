# CurseForge release handoff

The planned first upload is the Forge build. Use the [CurseForge project copy](../../CURSEFORGE.md) for the listing, changelog draft, and loader matrix. Use the project’s square PNG logo from the local release assets as the project icon.

## Forge file description

> First Forge release of MTG Base Library for Minecraft 26.3. Includes six basic lands and a shared API for independent expansions to add cards, effects, and mechanics.

## Check the actual upload file

The pre-existing `forge/build/libs/mtg-forge-1.0.0.jar` inspected on November 9, 2026 lacked `com/mtg/api/MtgLibrary.class`. Build a fresh file from the current source with JDK 25 and verify that it contains the shared startup and Forge metadata. Use the actual version from [gradle.properties](../../gradle.properties) if it changes.

```sh
./gradlew :forge:clean :forge:build
jar --list --file forge/build/libs/mtg-forge-1.0.0.jar | grep -Fx 'com/mtg/api/MtgLibrary.class'
jar --list --file forge/build/libs/mtg-forge-1.0.0.jar | grep -Fx 'META-INF/mods.toml'
sha256sum forge/build/libs/mtg-forge-1.0.0.jar
```

Run the client and dedicated-server checks in [TEST_INFRA.md](../../TEST_INFRA.md). For a verified Forge file, use the **Forge**, **Minecraft 26.3**, and **Alpha** labels. Record the tested loader and file checksum in [PROJECT.md](../../PROJECT.md). Add the published file link once the upload is confirmed.
