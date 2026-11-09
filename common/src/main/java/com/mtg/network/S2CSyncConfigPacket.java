package com.mtg.network;

import com.mtg.config.*;
import com.mtg.config.toml.TomlDocument;
import com.mtg.config.toml.TomlParser;
import com.mtg.config.toml.TomlWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Server-to-Client packet that synchronizes authoritative game balance configuration
 * (Mana, Cards, Progression, WorldGen) to clients upon login or configuration reload.
 */
public record S2CSyncConfigPacket(
    ManaConfig manaConfig,
    CardsConfig cardsConfig,
    ProgressionConfig progressionConfig,
    WorldGenConfig worldGenConfig
) implements CustomPacket {

    public static final String CHANNEL = "mtg:sync_config";

    public S2CSyncConfigPacket {
        manaConfig = (manaConfig != null) ? manaConfig.sanitize() : ManaConfig.defaults();
        cardsConfig = (cardsConfig != null) ? cardsConfig.sanitize() : CardsConfig.defaults();
        progressionConfig = (progressionConfig != null) ? progressionConfig.sanitize() : ProgressionConfig.defaults();
        worldGenConfig = (worldGenConfig != null) ? worldGenConfig.sanitize() : WorldGenConfig.defaults();
    }

    public static S2CSyncConfigPacket fromCurrent() {
        return new S2CSyncConfigPacket(
            MtgConfig.getMana(),
            MtgConfig.getCards(),
            MtgConfig.getProgression(),
            MtgConfig.getWorldGen()
        );
    }

    @Override
    public String getChannelId() {
        return CHANNEL;
    }

    @Override
    public byte[] encode() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            writeTomlString(dos, TomlWriter.write(manaConfig.toTomlDocument()));
            writeTomlString(dos, TomlWriter.write(cardsConfig.toTomlDocument()));
            writeTomlString(dos, TomlWriter.write(progressionConfig.toTomlDocument()));
            writeTomlString(dos, TomlWriter.write(worldGenConfig.toTomlDocument()));

            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode S2CSyncConfigPacket", e);
        }
    }

    public static S2CSyncConfigPacket decode(byte[] data) {
        Objects.requireNonNull(data, "Data cannot be null");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {

            String manaToml = readTomlString(dis);
            String cardsToml = readTomlString(dis);
            String progToml = readTomlString(dis);
            String worldToml = readTomlString(dis);

            ManaConfig mana = parseMana(manaToml);
            CardsConfig cards = parseCards(cardsToml);
            ProgressionConfig prog = parseProgression(progToml);
            WorldGenConfig world = parseWorldGen(worldToml);

            return new S2CSyncConfigPacket(mana, cards, prog, world);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to decode S2CSyncConfigPacket", e);
        }
    }

    @Override
    public void handle(Object context) {
        MtgConfig.setMana(manaConfig);
        MtgConfig.setCards(cardsConfig);
        MtgConfig.setProgression(progressionConfig);
        MtgConfig.setWorldGen(worldGenConfig);
    }

    private static void writeTomlString(DataOutputStream dos, String toml) throws IOException {
        byte[] bytes = toml.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(bytes.length);
        dos.write(bytes);
    }

    private static String readTomlString(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (length < 0 || length > 1_000_000) {
            throw new IOException("Invalid TOML payload length: " + length);
        }
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static ManaConfig parseMana(String toml) {
        try {
            TomlDocument doc = TomlParser.parse(toml);
            return ManaConfig.fromTomlDocument(doc);
        } catch (Exception e) {
            return ManaConfig.defaults();
        }
    }

    private static CardsConfig parseCards(String toml) {
        try {
            TomlDocument doc = TomlParser.parse(toml);
            return CardsConfig.fromTomlDocument(doc);
        } catch (Exception e) {
            return CardsConfig.defaults();
        }
    }

    private static ProgressionConfig parseProgression(String toml) {
        try {
            TomlDocument doc = TomlParser.parse(toml);
            return ProgressionConfig.fromTomlDocument(doc);
        } catch (Exception e) {
            return ProgressionConfig.defaults();
        }
    }

    private static WorldGenConfig parseWorldGen(String toml) {
        try {
            TomlDocument doc = TomlParser.parse(toml);
            return WorldGenConfig.fromTomlDocument(doc);
        } catch (Exception e) {
            return WorldGenConfig.defaults();
        }
    }
}
