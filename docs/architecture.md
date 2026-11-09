# Architecture

## Module boundaries

[settings.gradle](../settings.gradle) includes four Gradle projects. The [shared source](../common/src/main/java/com/mtg/) contains Java-only rules and data. Each loader bundles those classes with its own entry point and metadata.

| Loader | Entry point | Metadata |
| --- | --- | --- |
| Fabric | [FabricMtgMod](../fabric/src/main/java/com/mtg/fabric/FabricMtgMod.java) | [fabric.mod.json](../fabric/src/main/resources/fabric.mod.json) |
| Forge | [ForgeMtgMod](../forge/src/main/java/com/mtg/forge/ForgeMtgMod.java) | [mods.toml](../forge/src/main/resources/META-INF/mods.toml) |
| NeoForge | [NeoForgeMtgMod](../neoforge/src/main/java/com/mtg/neoforge/NeoForgeMtgMod.java) | [neoforge.mods.toml](../neoforge/src/main/resources/META-INF/neoforge.mods.toml) |

## Startup and extension lifecycle

1. The loader entry point passes its configuration directory to [MtgLibrary.initialize](../common/src/main/java/com/mtg/api/MtgLibrary.java).
2. The library loads `mana.toml`, `cards.toml`, `progression.toml`, and `world_gen.toml` under `config/mtg/` relative to the loader's game directory.
3. The library registers the built-in Basic Lands and discovers add-ons through [ExpansionRegistry](../common/src/main/java/com/mtg/api/ExpansionRegistry.java) and Java `ServiceLoader`.
4. Enabled sets contribute keywords, triggers, card definitions, effects, boss hooks, and journal content. The library populates the progression catalog and shares a trigger bus with casting, enchantments, and bosses.
5. Consumers can retrieve the initialized library with `MtgLibrary.getInstance()`. If startup fails, restart the process after fixing the cause. A failed hook may already have modified a registry.

## Where to work

| Area | Shared code |
| --- | --- |
| Card definitions, types, costs, and rarity | [`domain/`](../common/src/main/java/com/mtg/domain/) |
| Initial affinity, biome regeneration, and capacity | [`mana/`](../common/src/main/java/com/mtg/mana/) |
| Timed spells, cooldowns, and effects | [`cards/`](../common/src/main/java/com/mtg/cards/) |
| Dedicated hand, drops, unlocks, and wildcards | [`deck/`](../common/src/main/java/com/mtg/deck/) and [`progression/`](../common/src/main/java/com/mtg/progression/) |
| Summons, bosses, rituals, and mana nodes | [`entity/`](../common/src/main/java/com/mtg/entity/) and [`world/`](../common/src/main/java/com/mtg/world/) |
| Journal, locale strings, and HUD state | [`guidebook/`](../common/src/main/java/com/mtg/guidebook/), [`en_us.json`](../common/src/main/resources/assets/mtg/lang/en_us.json), and [`client/hud/`](../common/src/main/java/com/mtg/client/hud/) |
| Configuration, packet codecs, and service interfaces | [`config/`](../common/src/main/java/com/mtg/config/), [`network/`](../common/src/main/java/com/mtg/network/), and [`platform/`](../common/src/main/java/com/mtg/platform/) |

Treat the code as the interface reference. Keep player and world state authoritative on the server when adding bindings. Resolve client requests against server-side card, player, target, and cost state. Use locale keys for in-game text. See [current status](../PROJECT.md) for the boundary between Java models and Minecraft integration.
