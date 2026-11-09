package com.mtg.guidebook;

/**
 * Headless and dedicated server fallback provider for IJournalScreenProvider.
 */
public class NoopJournalScreenProvider implements IJournalScreenProvider {
    @Override
    public void openJournalScreen(JournalBookModel bookModel, String initialChapterId, String initialEntryId, int initialPageNumber) {
        // No-op on headless or dedicated server environments
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
