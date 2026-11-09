package com.mtg.guidebook;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.UUID;

/**
 * Domain specification for the Planeswalker's Journal item (mtg:planeswalker_journal).
 * Handles headless and multiplayer use interactions without client coupling.
 */
public final class JournalItemDefinition {
    public static final String ITEM_ID = "mtg:planeswalker_journal";
    public static final String TRANSLATION_KEY = "item.mtg.planeswalker_journal";
    public static final int MAX_STACK_SIZE = 1;

    private static IJournalScreenProvider screenProvider;

    private JournalItemDefinition() {}

    /**
     * Resolves the active IJournalScreenProvider via Java ServiceLoader,
     * falling back to NoopJournalScreenProvider if none is registered.
     */
    public static synchronized IJournalScreenProvider getScreenProvider() {
        if (screenProvider == null) {
            ServiceLoader<IJournalScreenProvider> loader = ServiceLoader.load(IJournalScreenProvider.class);
            Iterator<IJournalScreenProvider> it = loader.iterator();
            if (it.hasNext()) {
                screenProvider = it.next();
            } else {
                screenProvider = new NoopJournalScreenProvider();
            }
        }
        return screenProvider;
    }

    public static synchronized void setScreenProvider(IJournalScreenProvider provider) {
        screenProvider = (provider != null) ? provider : new NoopJournalScreenProvider();
    }

    /**
     * Handles item use action by player.
     *
     * @param bookModel The journal model
     * @param playerId UUID of using player
     * @return true if screen was opened or available, false otherwise
     */
    public static boolean useItem(JournalBookModel bookModel, UUID playerId) {
        IJournalScreenProvider provider = getScreenProvider();
        if (provider != null && provider.isAvailable()) {
            provider.openJournalScreen(bookModel, null, null, 1);
            return true;
        }
        return false;
    }
}
