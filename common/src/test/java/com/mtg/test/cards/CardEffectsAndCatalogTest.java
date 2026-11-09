package com.mtg.test.cards;

import com.mtg.cards.catalog.BasicLandsCatalog;
import com.mtg.cards.catalog.BasicLandsExpansion;
import com.mtg.cards.effects.*;
import com.mtg.domain.*;
import com.mtg.entity.SummonManager;
import com.mtg.entity.SummonedCreature;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.progression.ProgressionManager;
import com.mtg.test.fixtures.TestCardFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone 4 — Card Catalogs & Strategy Effects Test Suite")
public class CardEffectsAndCatalogTest {

    private UUID playerId;
    private ManaPool manaPool;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        manaPool = ManaManager.getInstance().getOrCreatePlayerMana(playerId);
        manaPool.clear();
        manaPool.setMaxMana(20);

        BasicLandsExpansion expansion = new BasicLandsExpansion();
        expansion.registerCards(ProgressionManager.getInstance()::registerCard);
        expansion.registerEffects();
        TestCardFixtures.registerAllCards(ProgressionManager.getInstance()::registerCard);
        TestCardFixtures.registerAllEffects();
        SummonManager.getInstance().clear();
        EnchantmentManager.getInstance().clearAll();
    }

    @Test
    @DisplayName("1. Basic Lands Catalog & Expansion Integrity: 6 basic lands with valid contracts")
    void testBasicLandsCatalogAndExpansionIntegrity() {
        BasicLandsExpansion expansion = new BasicLandsExpansion();
        assertEquals("base", expansion.getSetCode());
        assertEquals("Magic: The Gathering Base Library (Basic Lands)", expansion.getSetName());
        assertEquals(100, expansion.getPriority());
        assertTrue(expansion.isEnabled());

        List<CardDefinition> lands = BasicLandsCatalog.getAllCards();
        assertEquals(6, lands.size(), "Basic lands catalog must contain exactly 6 lands");

        for (CardDefinition land : lands) {
            assertNotNull(land.id());
            assertNotNull(land.name());
            assertTrue(land.isLand());
            assertEquals(ManaCost.ZERO, land.manaCost());
            assertEquals(CardRarity.COMMON, land.rarity());
            assertEquals(1200, land.baseCooldownTicks());
            assertEquals(0, land.windupDelayTicks());
        }

        assertTrue(BasicLandsCatalog.PLAINS.isLand());
        assertTrue(BasicLandsCatalog.ISLAND.isLand());
        assertTrue(BasicLandsCatalog.SWAMP.isLand());
        assertTrue(BasicLandsCatalog.MOUNTAIN.isLand());
        assertTrue(BasicLandsCatalog.FOREST.isLand());
        assertTrue(BasicLandsCatalog.WASTES.isLand());
    }

    @Test
    @DisplayName("2. Synthetic Fixture Catalog Integrity: Instants, Sorceries, Enchantments, and Creatures")
    void testSyntheticFixtureCatalogCardIntegrity() {
        List<CardDefinition> cards = TestCardFixtures.getAllCards();
        assertFalse(cards.isEmpty(), "Test fixture card catalog must not be empty");

        for (CardDefinition card : cards) {
            assertNotNull(card.id());
            assertNotNull(card.name());
            assertNotNull(card.manaCost());
            assertNotNull(card.types());
            assertFalse(card.types().isEmpty());
            assertTrue(card.baseCooldownTicks() >= 0);
            assertTrue(card.windupDelayTicks() >= 0);
        }

        // Instants
        assertTrue(TestCardFixtures.LIGHTNING_BOLT.isInstant());
        assertTrue(TestCardFixtures.GIANT_GROWTH.isInstant());
        assertTrue(TestCardFixtures.COUNTERSPELL.isInstant());
        assertTrue(TestCardFixtures.UNSUMMON.isInstant());
        assertTrue(TestCardFixtures.DARK_RITUAL.isInstant());
        assertTrue(TestCardFixtures.HEALING_SALVE.isInstant());

        // Sorceries
        assertTrue(TestCardFixtures.WRATH_OF_GOD.isSorcery());
        assertTrue(TestCardFixtures.DIVINATION.isSorcery());
        assertTrue(TestCardFixtures.MIND_ROT.isSorcery());
        assertTrue(TestCardFixtures.PYROCLASM.isSorcery());

        // Enchantments
        assertTrue(TestCardFixtures.GLORIOUS_ANTHEM.isEnchantment());
        assertTrue(TestCardFixtures.PHYREXIAN_ARENA.isEnchantment());
        assertTrue(TestCardFixtures.RANCOR.isEnchantment());
        assertTrue(TestCardFixtures.LEVITATION.isEnchantment());

        // Creatures
        List<CardDefinition> creatures = TestCardFixtures.getAllCreatures();
        assertEquals(9, creatures.size());
        for (CardDefinition creature : creatures) {
            assertTrue(creature.isCreature());
            assertTrue(creature.combatStats().isPresent());
        }
    }

    @Test
    @DisplayName("3. Basic Land Effects: Attunes respective color mana surge and increases max mana cap")
    void testLandEffects() {
        // Plains -> White
        ICardEffectHandler plainsHandler = CardEffectRegistry.get(BasicLandsCatalog.PLAINS.id()).orElseThrow();
        plainsHandler.apply(CastContext.of(playerId, BasicLandsCatalog.PLAINS, 0));
        assertEquals(1, manaPool.get(ManaColor.WHITE));
        assertEquals(21, manaPool.getMaxMana());

        // Island -> Blue
        ICardEffectHandler islandHandler = CardEffectRegistry.get(BasicLandsCatalog.ISLAND.id()).orElseThrow();
        islandHandler.apply(CastContext.of(playerId, BasicLandsCatalog.ISLAND, 0));
        assertEquals(1, manaPool.get(ManaColor.BLUE));
        assertEquals(22, manaPool.getMaxMana());

        // Swamp -> Black
        ICardEffectHandler swampHandler = CardEffectRegistry.get(BasicLandsCatalog.SWAMP.id()).orElseThrow();
        swampHandler.apply(CastContext.of(playerId, BasicLandsCatalog.SWAMP, 0));
        assertEquals(1, manaPool.get(ManaColor.BLACK));
        assertEquals(23, manaPool.getMaxMana());

        // Mountain -> Red
        ICardEffectHandler mountainHandler = CardEffectRegistry.get(BasicLandsCatalog.MOUNTAIN.id()).orElseThrow();
        mountainHandler.apply(CastContext.of(playerId, BasicLandsCatalog.MOUNTAIN, 0));
        assertEquals(1, manaPool.get(ManaColor.RED));
        assertEquals(24, manaPool.getMaxMana());

        // Forest -> Green
        ICardEffectHandler forestHandler = CardEffectRegistry.get(BasicLandsCatalog.FOREST.id()).orElseThrow();
        forestHandler.apply(CastContext.of(playerId, BasicLandsCatalog.FOREST, 0));
        assertEquals(1, manaPool.get(ManaColor.GREEN));
        assertEquals(25, manaPool.getMaxMana());

        // Wastes -> Colorless
        ICardEffectHandler wastesHandler = CardEffectRegistry.get(BasicLandsCatalog.WASTES.id()).orElseThrow();
        wastesHandler.apply(CastContext.of(playerId, BasicLandsCatalog.WASTES, 0));
        assertEquals(1, manaPool.get(ManaColor.COLORLESS));
        assertEquals(26, manaPool.getMaxMana());
    }

    @Test
    @DisplayName("4. Instant Effects: Dark Ritual adds 3 Black mana; Giant Growth buffs summon; Healing Salve heals")
    void testInstantEffects() {
        // Dark Ritual
        ICardEffectHandler ritual = CardEffectRegistry.get(TestCardFixtures.DARK_RITUAL.id()).orElseThrow();
        CardEffectResult res = ritual.apply(CastContext.of(playerId, TestCardFixtures.DARK_RITUAL, 0));
        assertTrue(res.isSuccess());
        assertEquals(3, manaPool.get(ManaColor.BLACK));

        // Spawn a summon for Giant Growth
        SummonedCreature elf = new SummonedCreature(playerId, TestCardFixtures.LLANOWAR_ELVES);
        SummonManager.getInstance().registerSummon(elf);
        assertEquals(1, elf.getPower());
        assertEquals(1, elf.getToughness());

        ICardEffectHandler growth = CardEffectRegistry.get(TestCardFixtures.GIANT_GROWTH.id()).orElseThrow();
        growth.apply(CastContext.of(playerId, TestCardFixtures.GIANT_GROWTH, 0));
        assertEquals(4, elf.getPower(), "Power should be buffed by +3 (1 -> 4)");
        assertEquals(4, elf.getToughness(), "Toughness should be buffed by +3 (1 -> 4)");

        // Damage elf, then apply Healing Salve
        elf.takeDamage(10.0f);
        assertTrue(elf.getHealth() < elf.getMaxHealth());
        float preHealth = elf.getHealth();

        ICardEffectHandler heal = CardEffectRegistry.get(TestCardFixtures.HEALING_SALVE.id()).orElseThrow();
        heal.apply(CastContext.of(playerId, TestCardFixtures.HEALING_SALVE, 0));
        assertEquals(preHealth + 6.0f, elf.getHealth(), 0.01f, "Healing Salve should restore 6.0 health");
    }

    @Test
    @DisplayName("5. Sorcery Effects: Wrath of God destroys all active summons")
    void testWrathOfGodEffect() {
        SummonedCreature s1 = new SummonedCreature(playerId, TestCardFixtures.LLANOWAR_ELVES);
        SummonedCreature s2 = new SummonedCreature(playerId, TestCardFixtures.SERRA_ANGEL);
        SummonManager.getInstance().registerSummon(s1);
        SummonManager.getInstance().registerSummon(s2);
        assertEquals(2, SummonManager.getInstance().getActiveSummonCount(playerId));

        ICardEffectHandler wrath = CardEffectRegistry.get(TestCardFixtures.WRATH_OF_GOD.id()).orElseThrow();
        wrath.apply(CastContext.of(playerId, TestCardFixtures.WRATH_OF_GOD, 0));

        assertEquals(0, SummonManager.getInstance().getActiveSummonCount(playerId), "Wrath of God should purge all active summons");
        assertTrue(s1.isDead());
        assertTrue(s2.isDead());
    }

    @Test
    @DisplayName("6. Enchantment Effects: Glorious Anthem grants +1/+1 and expires cleanly")
    void testGloriousAnthemEffect() {
        SummonedCreature angel = new SummonedCreature(playerId, TestCardFixtures.SERRA_ANGEL);
        SummonManager.getInstance().registerSummon(angel);
        assertEquals(4, angel.getPower());
        assertEquals(4, angel.getToughness());

        ICardEffectHandler anthem = CardEffectRegistry.get(TestCardFixtures.GLORIOUS_ANTHEM.id()).orElseThrow();
        anthem.apply(CastContext.of(playerId, TestCardFixtures.GLORIOUS_ANTHEM, 0));

        // Verify +1/+1 applied
        assertEquals(5, angel.getPower(), "Glorious Anthem should increase power to 5");
        assertEquals(5, angel.getToughness(), "Glorious Anthem should increase toughness to 5");

        // Expire enchantment
        EnchantmentManager.getInstance().clearAll();
        assertEquals(4, angel.getPower(), "Power should revert to 4 upon enchantment expiration");
        assertEquals(4, angel.getToughness(), "Toughness should revert to 4 upon enchantment expiration");
    }

    @Test
    @DisplayName("7. Enchantment Effects: Phyrexian Arena triggers periodic mana generation on upkeep")
    void testPhyrexianArenaUpkeep() {
        ICardEffectHandler arena = CardEffectRegistry.get(TestCardFixtures.PHYREXIAN_ARENA.id()).orElseThrow();
        arena.apply(CastContext.of(playerId, TestCardFixtures.PHYREXIAN_ARENA, 0));

        assertEquals(0, manaPool.get(ManaColor.BLACK));

        // Trigger upkeep
        EnchantmentManager.getInstance().triggerUpkeep(playerId);
        assertEquals(1, manaPool.get(ManaColor.BLACK), "Upkeep should generate 1 Black mana");

        EnchantmentManager.getInstance().triggerUpkeep(playerId);
        assertEquals(2, manaPool.get(ManaColor.BLACK), "Second upkeep should generate another Black mana");
    }

    @Test
    @DisplayName("8. Creature Summoning: Invoking creature card creates and registers SummonedCreature")
    void testCreatureSummonRegistration() {
        ICardEffectHandler creatureHandler = CardEffectRegistry.get(TestCardFixtures.GOBLIN_GUIDE.id()).orElseThrow();
        CardEffectResult res = creatureHandler.apply(CastContext.of(playerId, TestCardFixtures.GOBLIN_GUIDE, 0));

        assertTrue(res.isSuccess());
        assertEquals(1, SummonManager.getInstance().getActiveSummonCount(playerId));
        List<SummonedCreature> summons = SummonManager.getInstance().getPlayerSummons(playerId);
        assertEquals(1, summons.size());
        assertEquals(TestCardFixtures.GOBLIN_GUIDE.id(), summons.getFirst().getCardDefinition().id());
    }
}
