package com.mtg.api;

import com.mtg.api.keyword.KeywordRegistry;
import com.mtg.api.trigger.TriggerBus;
import com.mtg.cards.casting.CardCastingService;
import com.mtg.cards.catalog.BasicLandsExpansion;
import com.mtg.cards.effects.EnchantmentManager;
import com.mtg.config.MtgConfig;
import com.mtg.domain.CardDefinition;
import com.mtg.guidebook.JournalBookModel;
import com.mtg.guidebook.catalog.DefaultJournalContent;
import com.mtg.progression.ProgressionDropEngine;
import com.mtg.progression.ProgressionManager;
import com.mtg.world.boss.BossManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared startup and access to the base library's expansion-aware services. */
public final class MtgLibrary {
    private static MtgLibrary instance;
    private static boolean startupAttempted;

    private final KeywordRegistry keywords = new KeywordRegistry();
    private final TriggerBus triggers = new TriggerBus();
    private final JournalBookModel guidebook = DefaultJournalContent.createDefaultJournal();
    private final List<CardDefinition> cards;

    private MtgLibrary(Path configDirectory) {
        MtgConfig.loadAll(configDirectory.resolve("mtg"));
        ExpansionRegistry.registerExpansion(new BasicLandsExpansion());
        ExpansionRegistry.discoverAndRegisterViaServiceLoader();

        List<CardDefinition> catalog = new ArrayList<>();
        BossManager bosses = BossManager.getInstance();
        bosses.setTriggerBus(triggers);
        ExpansionRegistry.initializeAll(catalog::add, keywords, triggers, bosses, guidebook);
        cards = List.copyOf(catalog);

        ProgressionManager.getInstance().registerCards(cards);
        CardCastingService.getInstance().setTriggerBus(triggers);
        EnchantmentManager.getInstance().setTriggerBus(triggers);
    }

    /**
     * Starts the base library once per process. The path is the loader's config root.
     * Restart the process after a failed startup because addon hooks may have registered effects.
     */
    public static synchronized MtgLibrary initialize(Path configDirectory) {
        Objects.requireNonNull(configDirectory, "Config directory cannot be null");
        if (instance != null) {
            return instance;
        }
        if (startupAttempted) {
            throw new IllegalStateException("MTG library startup has already been attempted");
        }
        startupAttempted = true;
        instance = new MtgLibrary(configDirectory);
        return instance;
    }

    public static synchronized MtgLibrary getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MTG library has not finished starting");
        }
        return instance;
    }

    public List<CardDefinition> getCards() {
        return cards;
    }

    public KeywordRegistry getKeywords() {
        return keywords;
    }

    public TriggerBus getTriggers() {
        return triggers;
    }

    public JournalBookModel getGuidebook() {
        return guidebook;
    }

    public ProgressionDropEngine createDropEngine() {
        return new ProgressionDropEngine(cards);
    }
}
