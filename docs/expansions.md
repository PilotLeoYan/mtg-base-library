# Independent expansion development

An expansion is a separate mod that uses the base library at runtime. Keep its mod ID, Java packages, set code, card IDs, keywords, guidebook IDs, and asset namespace distinct from other projects. The base library owns the `mtg` namespace and the case-insensitive set code `base`.

## Set up a project

1. Create the add-on's own loader project. Build this repository with JDK 25 and put a copy of the resulting common JAR in the add-on's compile-time dependencies. Keep the common classes out of the add-on JAR.
2. Declare a required dependency on the base mod ID `mtg` in the add-on's loader metadata. Install the matching base loader JAR and the add-on JAR in your test profile.
3. Implement [IExpansionSet](../common/src/main/java/com/mtg/api/IExpansionSet.java) in a public provider with a public no-argument constructor. Register content in the hooks. Keep the constructor free of registrations.

Example Gradle dependency for a local development build

```groovy
dependencies {
    compileOnly files('libs/mtg-common-1.0.0.jar')
}
```

Use the actual version built from [gradle.properties](../gradle.properties).

## Minimal provider

```java
package example.wayfarers;

import com.mtg.api.IExpansionSet;
import com.mtg.api.keyword.KeywordRegistry;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.cards.effects.CardEffectRegistry;
import com.mtg.cards.effects.CardEffectResult;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;
import com.mtg.domain.CardType;
import com.mtg.domain.ManaCost;
import java.util.function.Consumer;

public final class WayfarersSet implements IExpansionSet {
    private static final String SET_CODE = "wayfarers";
    private static final String CARD_ID = "wayfarers:trail_marker";

    @Override
    public String getSetCode() {
        return SET_CODE;
    }

    @Override
    public String getSetName() {
        return "Wayfarers";
    }

    @Override
    public void registerCards(Consumer<CardDefinition> registry) {
        registry.accept(CardDefinition.builder(CARD_ID, SET_CODE, "Trail Marker")
                .manaCost(ManaCost.ZERO)
                .addType(CardType.INSTANT)
                .rarity(CardRarity.COMMON)
                .baseCooldownTicks(40)
                .build());
    }

    @Override
    public void registerKeywords(KeywordRegistry registry) {}

    @Override
    public void registerTriggers(TriggerBus bus) {}

    @Override
    public void registerEffects() {
        CardEffectRegistry.register(CARD_ID,
                context -> CardEffectResult.success("wayfarers.cast.trail_marker"));
    }
}
```

Put the provider's fully qualified class name in `src/main/resources/META-INF/services/com.mtg.api.IExpansionSet` on its own line.

```text
example.wayfarers.WayfarersSet
```

If the add-on has an explicit Java module descriptor, also declare the provider with `provides com.mtg.api.IExpansionSet with example.wayfarers.WayfarersSet`. Put the effect message in the add-on's `assets/wayfarers/lang/en_us.json`.

## Compatibility and checks

- Enabled sets run by descending priority and then by ascending set code. Priority alone cannot make two sets with overlapping IDs compatible. Duplicate set codes and duplicate card IDs cause startup to fail.
- Register effects for the card IDs the add-on owns. Some registries accept replacement entries. The current boss factory registry has one factory slot per mana color, so two add-ons that register the same color can replace one another's factories.
- Use `registerGuidebookContent` for journal models and `registerBosses` for domain boss contributions. See [project status](../PROJECT.md) for the remaining in-game UI and entity work.
- Verify the base by itself, then each add-on individually, then two together. Check that cards, effects, triggers, and journal content are available on the target loader.
- To revive a concrete set, read the [archive notes](../archived-expansions/README.md) before moving or compiling any archived code.
