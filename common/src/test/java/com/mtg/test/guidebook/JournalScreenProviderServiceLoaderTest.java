package com.mtg.test.guidebook;

import com.mtg.guidebook.*;
import com.mtg.guidebook.catalog.DefaultJournalContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IJournalScreenProvider ServiceLoader Contract & Dedicated Server Safety Suite")
public class JournalScreenProviderServiceLoaderTest {

    @AfterEach
    void tearDown() {
        JournalItemDefinition.setScreenProvider(null);
    }

    @Test
    @DisplayName("Default screen provider in headless environment is NoopJournalScreenProvider")
    void testDefaultNoopScreenProvider() {
        IJournalScreenProvider provider = JournalItemDefinition.getScreenProvider();
        assertNotNull(provider);
        assertFalse(provider.isAvailable(), "In headless environment, GUI screen provider must report isAvailable() == false");

        // Calling openJournalScreen on Noop provider does not throw any exception
        JournalBookModel model = DefaultJournalContent.createDefaultJournal();
        assertDoesNotThrow(() -> provider.openJournalScreen(model, null, null, 1));
    }

    @Test
    @DisplayName("JournalItemDefinition.useItem returns false in headless environment without crashing")
    void testUseItemHeadless() {
        JournalBookModel model = DefaultJournalContent.createDefaultJournal();
        UUID playerId = UUID.randomUUID();

        boolean opened = JournalItemDefinition.useItem(model, playerId);
        assertFalse(opened, "Should return false when screen provider is not available");
    }

    @Test
    @DisplayName("Custom screen provider injection correctly receives openJournalScreen calls")
    void testCustomScreenProviderInjection() {
        AtomicBoolean screenOpened = new AtomicBoolean(false);

        IJournalScreenProvider mockProvider = new IJournalScreenProvider() {
            @Override
            public void openJournalScreen(JournalBookModel bookModel, String initialChapterId, String initialEntryId, int initialPageNumber) {
                screenOpened.set(true);
            }

            @Override
            public boolean isAvailable() {
                return true;
            }
        };

        JournalItemDefinition.setScreenProvider(mockProvider);
        assertTrue(JournalItemDefinition.getScreenProvider().isAvailable());

        JournalBookModel model = DefaultJournalContent.createDefaultJournal();
        boolean success = JournalItemDefinition.useItem(model, UUID.randomUUID());

        assertTrue(success);
        assertTrue(screenOpened.get(), "Custom screen provider must receive open call");
    }
}
