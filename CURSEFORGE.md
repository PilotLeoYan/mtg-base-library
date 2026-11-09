# MTG Base Library for CurseForge

## Project fields

| Field | Value |
| --- | --- |
| Project name | MTG Base Library |
| Suggested slug | `mtg-base-library` |
| Game and class | Minecraft · Mods |
| Main category | API and Library |
| Summary | Shared card rules, mana and progression systems, and an expansion API for independent MTG add-ons. |

## Project description

**MTG Base Library** is a framework for independent Magic: The Gathering-inspired add-ons for Minecraft Java Edition. Add-ons can supply their own card sets and rules while sharing the same mana, casting, and progression systems.

The base catalog contains six basic lands: Plains, Island, Swamp, Mountain, Forest, and Wastes. Expansion authors can contribute cards, effects, keywords, event listeners, boss definitions, and chapters for the Planeswalker's Journal.

**Development status.** The rules and extension API are exercised in headless Java tests. The Minecraft client and dedicated-server integration is in development. The catalog, hand, progression, journal, summons, and world features are currently library systems. Check the changelog for the capabilities verified in each build.

**For add-on authors.** Declare a dependency on the `mtg` mod and compile against `mtg-common`. Implement `com.mtg.api.IExpansionSet` and list the provider in `META-INF/services/com.mtg.api.IExpansionSet`. Use your own mod ID, Java package, set code, and resource namespace. The `base` set code and `mtg` namespace belong to the base library.

This is an unofficial fan project. Minecraft and Magic: The Gathering are associated with their respective owners.

## First alpha changelog draft

- Added a common startup path for the expansion registry, TOML configuration, and shared services.
- Kept the six basic lands in the base catalog and preserved the Core Set, Zendikar Rising, and Phyrexia: All Will Be One sources outside the base build.
- Added stable expansion initialization order and duplicate card ID detection.
- Connected the extension hooks for keywords, effects, triggers, bosses, and journal content.

## Upload checklist

1. Use JDK 25 and run `./gradlew clean build`. The wrapper targets Gradle 9.6.0. Test each intended file on Minecraft Java Edition 26.3 with its corresponding loader. Use a fresh client profile and a dedicated server. Check base-only startup, config creation, and startup with two independent test add-ons.
2. Confirm the code license and supply the corresponding license file. The current loader metadata declares MIT. Prepare an original project icon and use only assets you have permission to distribute.
3. Create a Minecraft Mods project in the CurseForge Authors Dashboard and paste the summary and description above. Provide project links when you have confirmed their destinations.
4. Upload each verified loader build separately as **Alpha** with Minecraft version **26.3** and the matching loader tag. Copy the changelog draft and add the tests and capabilities verified for that file.

| Loader tag | Build output | Initial test target |
| --- | --- | --- |
| Fabric | `fabric/build/libs/mtg-fabric-1.0.0.jar` | Fabric Loader 0.19.5 |
| Forge | `forge/build/libs/mtg-forge-1.0.0.jar` | Forge 66.1.0 |
| NeoForge | `neoforge/build/libs/mtg-neoforge-1.0.0.jar` | NeoForge 26.3.0.101 |

`common/build/libs/mtg-common-1.0.0.jar` is the developer API artifact. Each loader build bundles the common classes. When an independent add-on is published, record MTG Base Library as a required dependency on that add-on's CurseForge file.

[CurseForge project submission guide](https://support.curseforge.com/support/solutions/articles/9000197241-creating-and-submitting-a-project) · [Minecraft 26.3 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3) · [Fabric 26.3 notes](https://fabricmc.net/2026/09/15/263.html) · [Forge 26.3 downloads](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html) · [NeoForge downloads](https://projects.neoforged.net/neoforged/neoforge)
