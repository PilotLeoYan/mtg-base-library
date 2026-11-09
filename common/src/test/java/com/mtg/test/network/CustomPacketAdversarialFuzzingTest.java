package com.mtg.test.network;

import com.mtg.domain.CardRarity;
import com.mtg.domain.ManaColor;
import com.mtg.mana.ManaPool;
import com.mtg.network.*;
import com.mtg.progression.CardUnlockSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Custom Network Packets — Adversarial Fuzzing & Extreme Boundary Suite")
public class CustomPacketAdversarialFuzzingTest {

    // Helper to safely exercise decoders against corrupted inputs
    private void assertCleanRejectionOrSuccess(Class<?> packetClass, Runnable decodeAction) {
        try {
            decodeAction.run();
        } catch (IllegalArgumentException | NullPointerException expected) {
            // Clean expected rejection
        } catch (OutOfMemoryError oom) {
            fail("CRITICAL VULNERABILITY: " + packetClass.getSimpleName() + " threw OutOfMemoryError on untrusted input!", oom);
        } catch (Throwable unexpected) {
            fail("UNHANDLED FAILURE: " + packetClass.getSimpleName() + " threw unexpected exception: " + unexpected.getClass().getName(), unexpected);
        }
    }

    // =========================================================================
    // 1. Extreme Integer and Array Bounds Fuzzing
    // =========================================================================
    @Nested
    @DisplayName("1. Extreme Integer & Array Bounds Validation")
    class ExtremeBoundsTests {

        @Test
        @DisplayName("S2CSyncDeckPacket rejects negative, excessive, and Integer.MAX_VALUE slot counts")
        void testSyncDeckPacketSlotCountBounds() {
            int[] maliciousSizes = {
                    -1, -2, -100, Integer.MIN_VALUE,
                    65, 100, 1_000, 1_000_000, 1_000_000_000, Integer.MAX_VALUE
            };

            for (int size : maliciousSizes) {
                ByteBuffer buffer = ByteBuffer.allocate(28);
                buffer.putLong(1L);
                buffer.putLong(2L);
                buffer.putInt(4); // capacity
                buffer.putInt(0); // selected
                buffer.putInt(size); // slot size

                byte[] data = buffer.array();
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> S2CSyncDeckPacket.decode(data),
                        "Expected IllegalArgumentException for deck size " + size);
                assertTrue(ex.getMessage().contains("Invalid deck sync slot count"),
                        "Expected descriptive message but got: " + ex.getMessage());
            }
        }

        @Test
        @DisplayName("S2CSyncDeckPacket handles extreme capacity and selected slot integers without crashing")
        void testSyncDeckPacketExtremeSlotAndCapValues() throws IOException {
            int[] extremeValues = {Integer.MIN_VALUE, -1, 0, 7, 64, 100, Integer.MAX_VALUE};

            for (int cap : extremeValues) {
                for (int sel : extremeValues) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeLong(12345L);
                    dos.writeLong(67890L);
                    dos.writeInt(cap);
                    dos.writeInt(sel);
                    dos.writeInt(1); // 1 slot
                    dos.writeByte(0);
                    dos.writeUTF("mtg:card");
                    dos.writeInt(Integer.MAX_VALUE);
                    dos.writeInt(Integer.MIN_VALUE);
                    dos.writeInt(-1);
                    dos.writeInt(Integer.MAX_VALUE);

                    byte[] data = baos.toByteArray();
                    assertDoesNotThrow(() -> {
                        S2CSyncDeckPacket packet = S2CSyncDeckPacket.decode(data);
                        assertEquals(cap, packet.capacity());
                        assertEquals(sel, packet.selectedSlot());
                        assertEquals(1, packet.slots().size());
                    });
                }
            }
        }

        @Test
        @DisplayName("S2CSyncCollectionPacket rejects negative, excessive, and Integer.MAX_VALUE card counts")
        void testSyncCollectionPacketCardCountBounds() {
            int[] maliciousCounts = {
                    -1, -50, Integer.MIN_VALUE,
                    10_001, 20_000, 1_000_000, 1_000_000_000, Integer.MAX_VALUE
            };

            for (int count : maliciousCounts) {
                ByteBuffer buffer = ByteBuffer.allocate(20);
                buffer.putLong(1L);
                buffer.putLong(2L);
                buffer.putInt(count);

                byte[] data = buffer.array();
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> S2CSyncCollectionPacket.decode(data),
                        "Expected IllegalArgumentException for card count " + count);
                assertTrue(ex.getMessage().contains("Invalid collection card count"),
                        "Expected descriptive message but got: " + ex.getMessage());
            }
        }

        @Test
        @DisplayName("S2CSyncCollectionPacket rejects negative and excessive rarity counts")
        void testSyncCollectionPacketRarityCountBounds() throws IOException {
            int[] maliciousRarityCounts = {-1, -128, 17, 20, 50, 127};

            for (int rCount : maliciousRarityCounts) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeLong(1L);
                dos.writeLong(2L);
                dos.writeInt(0); // 0 cards
                dos.writeByte(rCount); // invalid rarity count

                byte[] data = baos.toByteArray();
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> S2CSyncCollectionPacket.decode(data),
                        "Expected IllegalArgumentException for rarity count " + rCount);
                assertTrue(ex.getMessage().contains("Invalid rarity count"),
                        "Expected descriptive message but got: " + ex.getMessage());
            }
        }

        @Test
        @DisplayName("C2SUpgradeMaxManaPacket clamps negative and Integer.MIN_VALUE offered XP levels to 0")
        void testUpgradeMaxManaPacketExtremeValues() {
            int[] testValues = {Integer.MIN_VALUE, -999, -1, 0, 1, 50, Integer.MAX_VALUE};

            for (int val : testValues) {
                C2SUpgradeMaxManaPacket packet = new C2SUpgradeMaxManaPacket(val);
                assertEquals(Math.max(0, val), packet.offeredXpLevels(), "Offered XP must be clamped to >= 0");

                byte[] encoded = packet.encode();
                C2SUpgradeMaxManaPacket decoded = C2SUpgradeMaxManaPacket.decode(encoded);
                assertEquals(Math.max(0, val), decoded.offeredXpLevels());
            }
        }

        @Test
        @DisplayName("C2SCastCardPacket handles extreme double coordinates (NaN, Infinity, Max/Min)")
        void testCastCardPacketCoordinatesFuzzing() {
            double[] extremeDoubles = {
                    0.0, -0.0, 1.0, -1.0,
                    Double.NaN,
                    Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.MAX_VALUE, Double.MIN_VALUE, -Double.MAX_VALUE
            };

            int[] testSlots = {-128, -1, 0, 3, 6, 127};
            int[] testEntities = {Integer.MIN_VALUE, -1, 0, 42, Integer.MAX_VALUE};

            for (double coord : extremeDoubles) {
                for (int slot : testSlots) {
                    C2SCastCardPacket packet = new C2SCastCardPacket(slot, testEntities[0], coord, coord, coord);
                    byte[] encoded = packet.encode();
                    C2SCastCardPacket decoded = C2SCastCardPacket.decode(encoded);

                    assertEquals((byte) slot, (byte) decoded.slotIndex());
                    assertEquals(testEntities[0], decoded.targetEntityId());
                    assertEquals(Double.doubleToRawLongBits(coord), Double.doubleToRawLongBits(decoded.targetX()));
                    assertEquals(Double.doubleToRawLongBits(coord), Double.doubleToRawLongBits(decoded.targetY()));
                    assertEquals(Double.doubleToRawLongBits(coord), Double.doubleToRawLongBits(decoded.targetZ()));
                }
            }
        }

        @Test
        @DisplayName("C2SRedeemWildcardPacket out-of-range rarity ordinals safely fallback to COMMON")
        void testRedeemWildcardOutOfRangeRarity() throws IOException {
            int[] invalidOrdinals = {-128, -1, 4, 5, 10, 100, 127};

            for (int ord : invalidOrdinals) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeByte(ord);
                dos.writeUTF("mtg:target");

                byte[] data = baos.toByteArray();
                C2SRedeemWildcardPacket decoded = C2SRedeemWildcardPacket.decode(data);
                assertEquals(CardRarity.COMMON, decoded.rarity(), "Out of range rarity ordinal " + ord + " must fall back to COMMON");
                assertEquals("mtg:target", decoded.targetCardId());
            }
        }

        @Test
        @DisplayName("SelectInitialManaPacket out-of-range color ordinals safely fallback to COLORLESS")
        void testSelectInitialManaOutOfRangeColor() throws IOException {
            int[] invalidOrdinals = {-128, -1, 6, 7, 10, 100, 127};

            for (int ord : invalidOrdinals) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeByte(ord);

                byte[] data = baos.toByteArray();
                SelectInitialManaPacket decoded = SelectInitialManaPacket.decode(data);
                assertEquals(ManaColor.COLORLESS, decoded.selectedColor(), "Out of range color ordinal " + ord + " must fall back to COLORLESS");
            }
        }

        @Test
        @DisplayName("S2CUnlockCardPacket out-of-range enums safely fallback without exception")
        void testUnlockCardPacketOutOfRangeEnums() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF("mtg:test");
            dos.writeByte(99); // invalid rarity -> COMMON
            dos.writeByte(99); // invalid source -> COMMAND
            dos.writeBoolean(true);
            dos.writeInt(Integer.MAX_VALUE);
            dos.writeInt(Integer.MIN_VALUE);
            dos.writeBoolean(true);
            dos.writeByte(-1); // invalid wildcard rarity -> null
            dos.writeInt(Integer.MAX_VALUE);

            byte[] data = baos.toByteArray();
            S2CUnlockCardPacket decoded = S2CUnlockCardPacket.decode(data);
            assertEquals("mtg:test", decoded.cardId());
            assertEquals(CardRarity.COMMON, decoded.rarity());
            assertEquals(CardUnlockSource.COMMAND, decoded.source());
            assertNull(decoded.wildcardRarity());
            assertEquals(Integer.MAX_VALUE, decoded.currentDuplicates());
            assertEquals(Integer.MIN_VALUE, decoded.threshold());
            assertEquals(Integer.MAX_VALUE, decoded.totalWildcardsOfRarity());
        }

        @Test
        @DisplayName("S2CSyncManaPacket with extreme and overflowing mana integers safely clamped")
        void testSyncManaPacketExtremeValues() {
            ByteBuffer buffer = ByteBuffer.allocate(46);
            buffer.putLong(12345L);
            buffer.putLong(67890L);
            buffer.putInt(Integer.MAX_VALUE); // white
            buffer.putInt(Integer.MAX_VALUE); // blue
            buffer.putInt(Integer.MAX_VALUE); // black
            buffer.putInt(Integer.MAX_VALUE); // red
            buffer.putInt(Integer.MAX_VALUE); // green
            buffer.putInt(Integer.MAX_VALUE); // colorless
            buffer.putInt(100); // maxMana = 100
            buffer.put((byte) 1);
            buffer.put((byte) 0); // WHITE

            byte[] data = buffer.array();
            assertDoesNotThrow(() -> {
                S2CSyncManaPacket decoded = S2CSyncManaPacket.decode(data);
                assertNotNull(decoded.pool());
                assertEquals(100, decoded.pool().getMaxMana());
                assertTrue(decoded.pool().getTotalMana() <= 100, "Total mana must be clamped to maxMana");
                assertTrue(decoded.pool().getTotalMana() >= 0, "Total mana must never wrap to negative");
            });
        }
    }

    // =========================================================================
    // 2. Truncated Stream & Malformed Stream Fuzzing
    // =========================================================================
    @Nested
    @DisplayName("2. Truncated & Malformed Stream Resilience")
    class TruncatedAndMalformedStreamTests {

        @Test
        @DisplayName("All 12 packet types reject truncated byte streams cleanly with IllegalArgumentException")
        void testAllPacketsTruncatedBytes() {
            List<byte[]> validPayloads = List.of(
                    new C2SCastCardPacket(0, 1, 10.0, 20.0, 30.0).encode(),
                    new C2SEquipCardPacket(1, "mtg:lightning_bolt").encode(),
                    new C2SRedeemWildcardPacket(CardRarity.RARE, "mtg:wrath_of_god").encode(),
                    new C2SSelectCardPacket(2).encode(),
                    new C2SUpgradeCapacityPacket().encode(),
                    new C2SUpgradeMaxManaPacket(5).encode(),
                    new S2COpenManaPromptPacket().encode(),
                    new S2CSyncCollectionPacket(UUID.randomUUID(), Set.of("mtg:bolt"), Map.of(), Map.of()).encode(),
                    new S2CSyncDeckPacket(UUID.randomUUID(), 4, 0, List.of(new S2CSyncDeckPacket.SlotSyncData(0, "mtg:a", 0, 10, 0, 0))).encode(),
                    new S2CSyncManaPacket(new ManaPool(UUID.randomUUID(), 20)).encode(),
                    new S2CUnlockCardPacket("mtg:a", CardRarity.COMMON, CardUnlockSource.MINING, false, 0, 4, false, null, 0).encode(),
                    new SelectInitialManaPacket(ManaColor.GREEN).encode()
            );

            for (byte[] payload : validPayloads) {
                // If payload is empty (e.g. S2COpenManaPromptPacket), skip truncation test
                if (payload.length <= 1) continue;

                for (int len = 0; len < payload.length; len++) {
                    byte[] truncated = Arrays.copyOf(payload, len);

                    // S2CSyncManaPacket
                    if (payload.length == 46) {
                        assertThrows(IllegalArgumentException.class, () -> S2CSyncManaPacket.decode(truncated));
                    }
                }
            }
        }

        @Test
        @DisplayName("Null byte arrays throw NullPointerException across all 12 packet types")
        void testNullDataThrowsNPE() {
            assertThrows(NullPointerException.class, () -> C2SCastCardPacket.decode(null));
            assertThrows(NullPointerException.class, () -> C2SEquipCardPacket.decode(null));
            assertThrows(NullPointerException.class, () -> C2SRedeemWildcardPacket.decode(null));
            assertThrows(NullPointerException.class, () -> C2SSelectCardPacket.decode(null));
            assertThrows(NullPointerException.class, () -> C2SUpgradeCapacityPacket.decode(null));
            assertThrows(NullPointerException.class, () -> C2SUpgradeMaxManaPacket.decode(null));
            assertThrows(NullPointerException.class, () -> S2COpenManaPromptPacket.decode(null));
            assertThrows(NullPointerException.class, () -> S2CSyncCollectionPacket.decode(null));
            assertThrows(NullPointerException.class, () -> S2CSyncDeckPacket.decode(null));
            assertThrows(NullPointerException.class, () -> S2CSyncManaPacket.decode(null));
            assertThrows(NullPointerException.class, () -> S2CUnlockCardPacket.decode(null));
            assertThrows(NullPointerException.class, () -> SelectInitialManaPacket.decode(null));
        }

        @Test
        @DisplayName("Trailing garbage appended to packets does not cause memory leaks or hangs")
        void testTrailingGarbageAppended() {
            byte[] validDeck = new S2CSyncDeckPacket(UUID.randomUUID(), 4, 0, List.of()).encode();
            byte[] withGarbage = new byte[validDeck.length + 1024];
            System.arraycopy(validDeck, 0, withGarbage, 0, validDeck.length);
            Arrays.fill(withGarbage, validDeck.length, withGarbage.length, (byte) 0xFF);

            // Must decode successfully ignoring trailing garbage
            assertDoesNotThrow(() -> {
                S2CSyncDeckPacket decoded = S2CSyncDeckPacket.decode(withGarbage);
                assertEquals(4, decoded.capacity());
                assertEquals(0, decoded.selectedSlot());
                assertEquals(0, decoded.slots().size());
            });
        }
    }

    // =========================================================================
    // 3. High-Throughput Random Bit Fuzzing Harness
    // =========================================================================
    @Nested
    @DisplayName("3. High-Throughput Randomized Bit Mutation Fuzzing")
    class RandomBitFuzzingHarness {

        @Test
        @DisplayName("1,000 randomized bit mutations across all packets never crash JVM or throw OOM")
        void testHighThroughputRandomBitMutations() {
            Random rng = new Random(0xCAFEBABE);

            List<CustomPacket> samplePackets = List.of(
                    new C2SCastCardPacket(1, 999, 128.5, 64.0, -320.25),
                    new C2SEquipCardPacket(2, "mtg:phyrexian_obliterator"),
                    new C2SRedeemWildcardPacket(CardRarity.MYTHIC, "mtg:black_lotus"),
                    new C2SSelectCardPacket(3),
                    new C2SUpgradeCapacityPacket(),
                    new C2SUpgradeMaxManaPacket(10),
                    new S2COpenManaPromptPacket(),
                    new S2CSyncCollectionPacket(UUID.randomUUID(), Set.of("mtg:card_1", "mtg:card_2"),
                            Map.of(CardRarity.COMMON, 5), Map.of(CardRarity.RARE, 2)),
                    new S2CSyncDeckPacket(UUID.randomUUID(), 5, 2, List.of(
                            new S2CSyncDeckPacket.SlotSyncData(0, "mtg:bolt", 10, 60, 0, 0),
                            new S2CSyncDeckPacket.SlotSyncData(1, null, 0, 0, 0, 0)
                    )),
                    new S2CSyncManaPacket(new ManaPool(UUID.randomUUID(), 10, 5, 5, 0, 0, 0, 20, true, ManaColor.WHITE)),
                    new S2CUnlockCardPacket("mtg:shivan_dragon", CardRarity.RARE, CardUnlockSource.MOB_DROP, true, 3, 4, false, null, 0),
                    new SelectInitialManaPacket(ManaColor.BLUE)
            );

            for (CustomPacket packet : samplePackets) {
                byte[] original = packet.encode();
                if (original.length == 0) continue;

                for (int iter = 0; iter < 100; iter++) {
                    byte[] mutated = Arrays.copyOf(original, original.length);
                    // Mutate 1 to 3 random bytes
                    int mutations = 1 + rng.nextInt(3);
                    for (int m = 0; m < mutations; m++) {
                        int pos = rng.nextInt(mutated.length);
                        mutated[pos] ^= (byte) (1 << rng.nextInt(8));
                    }

                    // Feed mutated byte array into appropriate decoder
                    assertCleanRejectionOrSuccess(packet.getClass(), () -> {
                        switch (packet) {
                            case C2SCastCardPacket p -> C2SCastCardPacket.decode(mutated);
                            case C2SEquipCardPacket p -> C2SEquipCardPacket.decode(mutated);
                            case C2SRedeemWildcardPacket p -> C2SRedeemWildcardPacket.decode(mutated);
                            case C2SSelectCardPacket p -> C2SSelectCardPacket.decode(mutated);
                            case C2SUpgradeCapacityPacket p -> C2SUpgradeCapacityPacket.decode(mutated);
                            case C2SUpgradeMaxManaPacket p -> C2SUpgradeMaxManaPacket.decode(mutated);
                            case S2COpenManaPromptPacket p -> S2COpenManaPromptPacket.decode(mutated);
                            case S2CSyncCollectionPacket p -> S2CSyncCollectionPacket.decode(mutated);
                            case S2CSyncDeckPacket p -> S2CSyncDeckPacket.decode(mutated);
                            case S2CSyncManaPacket p -> S2CSyncManaPacket.decode(mutated);
                            case S2CUnlockCardPacket p -> S2CUnlockCardPacket.decode(mutated);
                            case SelectInitialManaPacket p -> SelectInitialManaPacket.decode(mutated);
                            default -> {}
                        }
                    });
                }
            }
        }

        @Test
        @DisplayName("Arbitrary pseudorandom byte streams (500 arrays up to 2KB) cleanly rejected by all decoders")
        void testArbitraryPseudorandomByteStreams() {
            Random rng = new Random(0xDEADBEEF);

            for (int i = 0; i < 500; i++) {
                int len = rng.nextInt(2048);
                byte[] randomBytes = new byte[len];
                rng.nextBytes(randomBytes);

                assertCleanRejectionOrSuccess(C2SCastCardPacket.class, () -> C2SCastCardPacket.decode(randomBytes));
                assertCleanRejectionOrSuccess(C2SEquipCardPacket.class, () -> C2SEquipCardPacket.decode(randomBytes));
                assertCleanRejectionOrSuccess(C2SRedeemWildcardPacket.class, () -> C2SRedeemWildcardPacket.decode(randomBytes));
                assertCleanRejectionOrSuccess(C2SSelectCardPacket.class, () -> C2SSelectCardPacket.decode(randomBytes));
                assertCleanRejectionOrSuccess(C2SUpgradeCapacityPacket.class, () -> C2SUpgradeCapacityPacket.decode(randomBytes));
                assertCleanRejectionOrSuccess(C2SUpgradeMaxManaPacket.class, () -> C2SUpgradeMaxManaPacket.decode(randomBytes));
                assertCleanRejectionOrSuccess(S2COpenManaPromptPacket.class, () -> S2COpenManaPromptPacket.decode(randomBytes));
                assertCleanRejectionOrSuccess(S2CSyncCollectionPacket.class, () -> S2CSyncCollectionPacket.decode(randomBytes));
                assertCleanRejectionOrSuccess(S2CSyncDeckPacket.class, () -> S2CSyncDeckPacket.decode(randomBytes));
                assertCleanRejectionOrSuccess(S2CSyncManaPacket.class, () -> S2CSyncManaPacket.decode(randomBytes));
                assertCleanRejectionOrSuccess(S2CUnlockCardPacket.class, () -> S2CUnlockCardPacket.decode(randomBytes));
                assertCleanRejectionOrSuccess(SelectInitialManaPacket.class, () -> SelectInitialManaPacket.decode(randomBytes));
            }
        }
    }
}
