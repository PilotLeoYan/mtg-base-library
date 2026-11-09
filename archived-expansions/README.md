# Archived expansions

These source snapshots preserve the work on concrete sets outside the base build. The active library contains the six Basic Lands and the common extension API. See the [independent expansion guide](../docs/expansions.md) when starting a standalone add-on.

| Snapshot | Preserved material | Work before repackaging |
| --- | --- | --- |
| [Core Set](core-set/) | Card and creature catalogs, expansion provider, and three themed bosses | Replace the reserved `base` set code and `mtg:base/*` IDs. Use the library's six Basic Lands and migrate any persisted IDs. Move the classes to a unique package tree. |
| [Zendikar Rising](zendikar-rising/) | Landfall, Kicker, modal double-faced cards, and the `znr` provider | Check card and asset namespaces and add separate loader metadata and the SPI descriptor. |
| [Phyrexia: All Will Be One](phyrexia-one/) | Poison, Toxic, Corrupted, Proliferate, the `one` provider, boss source, and tests | Relocate the boss from `com.mtg.world.boss` to the add-on's package tree and check identifiers, loader metadata, and service discovery. |

The Core Set directory reflects where the snapshot was stored. Its provider actually declares `com.mtg.cards.catalog.BaseSetExpansion`, and other classes declare `com.mtg.entity.catalog` or `com.mtg.world.boss`. Read the Java package declarations before writing a service descriptor. Publish the provider under its new package name after the migration.

Keep the snapshots outside `settings.gradle`. Resume a set in its own project with a dependency on `mtg`, then verify that it works alongside another independent add-on on the target loader.
