package com.mtg.test.e2e.tier1;

import com.mtg.config.ManaConfig;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaManager;
import com.mtg.mana.ManaPool;
import com.mtg.network.S2COpenManaPromptPacket;
import com.mtg.network.SelectInitialManaPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tier 1 - Feature 06: Mana Pool Initial Selection Prompt E2E Test")
public class Feature06_InitialManaPromptTest {

    private UUID playerId;
    private ManaManager manaManager;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        manaManager = ManaManager.getInstance();
        manaManager.reset();
    }

    @AfterEach
    void tearDown() {
        manaManager.reset();
    }

    @Test
    @DisplayName("1. S2COpenManaPromptPacket encodes and decodes prompt for initial selection")
    void testOpenPromptPacketSerialization() {
        S2COpenManaPromptPacket packet = new S2COpenManaPromptPacket();
        byte[] bytes = packet.encode();
        assertNotNull(bytes);

        S2COpenManaPromptPacket decoded = S2COpenManaPromptPacket.decode(bytes);
        assertNotNull(decoded);
        assertEquals(S2COpenManaPromptPacket.CHANNEL, decoded.getChannelId());
    }

    @Test
    @DisplayName("2. SelectInitialManaPacket serializes and deserializes selected ManaColor")
    void testSelectInitialManaPacketSerialization() {
        SelectInitialManaPacket packet = new SelectInitialManaPacket(ManaColor.WHITE);
        byte[] bytes = packet.encode();
        assertNotNull(bytes);

        SelectInitialManaPacket decoded = SelectInitialManaPacket.decode(bytes);
        assertEquals(ManaColor.WHITE, decoded.selectedColor());
    }

    @Test
    @DisplayName("3. Selecting initial White mana initializes player pool with White starting mana")
    void testSelectingInitialWhiteMana() {
        boolean selected = manaManager.selectInitialMana(playerId, ManaColor.WHITE);
        assertTrue(selected, "Initial mana selection should succeed");

        ManaPool pool = manaManager.getPlayerMana(playerId);
        assertNotNull(pool);
        assertTrue(pool.get(ManaColor.WHITE) > 0, "White mana should be initialized");
        assertEquals(0, pool.get(ManaColor.BLUE), "Unselected colors should remain 0");
        assertEquals(0, pool.get(ManaColor.BLACK));
        assertEquals(0, pool.get(ManaColor.RED));
        assertEquals(0, pool.get(ManaColor.GREEN));
    }

    @Test
    @DisplayName("4. Selecting initial Green mana sets initial max mana cap from config")
    void testInitialManaPoolCapInitialization() {
        manaManager.selectInitialMana(playerId, ManaColor.GREEN);
        ManaPool pool = manaManager.getPlayerMana(playerId);

        assertEquals(ManaConfig.defaults().initialMaxMana(), pool.getMaxMana(),
                "Initial max mana should match configuration default");
        assertEquals(pool.getMaxMana(), pool.getTotalMana(), "Initial total mana matches max mana");
    }

    @Test
    @DisplayName("5. Initial mana selection status flag marks player as completed")
    void testInitialManaStatusFlag() {
        assertFalse(manaManager.hasChosenInitialMana(playerId), "Player must initially have unselected status");
        manaManager.selectInitialMana(playerId, ManaColor.RED);
        assertTrue(manaManager.hasChosenInitialMana(playerId), "Status must report selection completed");
    }
}
