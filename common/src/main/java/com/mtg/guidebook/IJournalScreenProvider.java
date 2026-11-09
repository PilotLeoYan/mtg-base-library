package com.mtg.guidebook;

/**
 * ServiceLoader SPI interface for opening the Planeswalker Journal GUI.
 * Decoupled from Minecraft Screen classes to guarantee dedicated server safety.
 */
public interface IJournalScreenProvider {
    /**
     * Opens the client-side journal GUI screen for the player.
     *
     * @param bookModel The populated JournalBookModel containing chapters and entries
     * @param initialChapterId Optional chapter ID to open to, or null
     * @param initialEntryId Optional entry ID to focus, or null
     * @param initialPageNumber 1-based page number to display
     */
    void openJournalScreen(JournalBookModel bookModel, String initialChapterId, String initialEntryId, int initialPageNumber);

    /**
     * Whether a graphical screen provider is available in the current runtime environment.
     *
     * @return true on physical clients with GUI support, false on headless/dedicated servers
     */
    boolean isAvailable();
}
