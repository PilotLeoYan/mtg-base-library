package com.mtg.progression;

import com.mtg.config.ProgressionConfig;
import com.mtg.domain.CardDefinition;
import com.mtg.domain.CardRarity;
import com.mtg.domain.ManaColor;
import com.mtg.domain.ManaCost;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluates gameplay unlock sources (mining ores, harvesting crops, killing mobs/bosses, opening dungeon chests)
 * against configurable drop chances, color weighting affinities, and rarity distributions.
 */
public class ProgressionDropEngine {
    private final List<CardDefinition> allCards;
    private final Random random;

    public ProgressionDropEngine(Collection<CardDefinition> cards) {
        this(cards, new Random());
    }

    public ProgressionDropEngine(Collection<CardDefinition> cards, Random random) {
        this.allCards = (cards != null) ? List.copyOf(cards) : List.of();
        this.random = (random != null) ? random : new Random();
    }

    public record DropRoll(boolean success, Optional<String> cardId, CardUnlockSource source) {}

    public DropRoll evaluateMiningDrop(String blockId, ProgressionConfig config) {
        if (config == null) config = ProgressionConfig.defaults();
        if (!isOre(blockId)) {
            return new DropRoll(false, Optional.empty(), CardUnlockSource.MINING);
        }

        double chance = config.miningCardDropChance();
        if (isHighTierOre(blockId)) {
            chance *= 2.5; // High tier ore boost
        }
        if (chance <= 0.0 || random.nextDouble() > chance) {
            return new DropRoll(false, Optional.empty(), CardUnlockSource.MINING);
        }

        CardRarity targetRarity = rollOreRarity(blockId);
        Set<ManaColor> targetColors = getOreColorAffinities(blockId);
        Optional<CardDefinition> picked = pickCard(targetRarity, targetColors);
        return new DropRoll(picked.isPresent(), picked.map(CardDefinition::id), CardUnlockSource.MINING);
    }

    public DropRoll evaluateFarmingDrop(String cropId, ProgressionConfig config) {
        if (config == null) config = ProgressionConfig.defaults();
        if (!isCrop(cropId)) {
            return new DropRoll(false, Optional.empty(), CardUnlockSource.FARMING);
        }

        double chance = config.farmingCardDropChance();
        if (chance <= 0.0 || random.nextDouble() > chance) {
            return new DropRoll(false, Optional.empty(), CardUnlockSource.FARMING);
        }

        CardRarity targetRarity = cropId.contains("nether_wart") ? rollNetherWartRarity() : rollCropRarity();
        Set<ManaColor> targetColors = getCropColorAffinities(cropId);
        Optional<CardDefinition> picked = pickCard(targetRarity, targetColors);
        return new DropRoll(picked.isPresent(), picked.map(CardDefinition::id), CardUnlockSource.FARMING);
    }

    public DropRoll evaluateMobDrop(String entityId, ProgressionConfig config) {
        if (config == null) config = ProgressionConfig.defaults();
        boolean isBoss = isBossMob(entityId);
        boolean isMini = isMiniBoss(entityId);

        double chance;
        if (isBoss) {
            chance = config.bossCardDropChance();
        } else if (isMini) {
            chance = 0.30;
        } else {
            chance = config.mobCardDropChance();
        }

        CardUnlockSource src = isBoss ? CardUnlockSource.BOSS_DROP : CardUnlockSource.MOB_DROP;
        if (chance <= 0.0 || random.nextDouble() > chance) {
            return new DropRoll(false, Optional.empty(), src);
        }

        CardRarity targetRarity;
        if (isBoss) {
            targetRarity = random.nextDouble() < 0.5 ? CardRarity.RARE : CardRarity.MYTHIC;
        } else if (isMini) {
            targetRarity = random.nextDouble() < 0.75 ? CardRarity.RARE : CardRarity.MYTHIC;
        } else {
            targetRarity = rollMobRarity();
        }

        Set<ManaColor> targetColors = getMobColorAffinities(entityId);
        Optional<CardDefinition> picked = pickCard(targetRarity, targetColors);
        return new DropRoll(picked.isPresent(), picked.map(CardDefinition::id), src);
    }

    public List<String> generateDungeonLoot(String structureId, ProgressionConfig config) {
        if (config == null) config = ProgressionConfig.defaults();
        int count = config.dungeonChestCardCount();
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CardRarity rarity = rollDungeonRarity(structureId);
            Set<ManaColor> colors = getDungeonColors(structureId);
            pickCard(rarity, colors).ifPresent(c -> result.add(c.id()));
        }
        return result;
    }

    public static boolean isOre(String blockId) {
        if (blockId == null) return false;
        String lower = blockId.toLowerCase(Locale.ROOT);
        return lower.contains("ore") || lower.contains("debris") || lower.contains("netherite");
    }

    public static boolean isHighTierOre(String blockId) {
        if (blockId == null) return false;
        String lower = blockId.toLowerCase(Locale.ROOT);
        return lower.contains("diamond") || lower.contains("debris") || lower.contains("emerald");
    }

    public static boolean isCrop(String cropId) {
        if (cropId == null) return false;
        String lower = cropId.toLowerCase(Locale.ROOT);
        return lower.contains("wheat") || lower.contains("carrot") || lower.contains("potato")
                || lower.contains("beetroot") || lower.contains("nether_wart")
                || lower.contains("melon") || lower.contains("pumpkin");
    }

    private static final Map<String, Set<ManaColor>> CUSTOM_BOSS_AFFINITIES = new ConcurrentHashMap<>();

    public static void registerBossAffinity(String entityIdSubstring, Set<ManaColor> colors) {
        if (entityIdSubstring != null && colors != null) {
            CUSTOM_BOSS_AFFINITIES.put(entityIdSubstring.toLowerCase(Locale.ROOT), Set.copyOf(colors));
        }
    }

    public static void clearBossAffinities() {
        CUSTOM_BOSS_AFFINITIES.clear();
    }

    public static boolean isBossMob(String entityId) {
        if (entityId == null) return false;
        String lower = entityId.toLowerCase(Locale.ROOT);
        if (lower.contains("avatar") || lower.contains("elemental")
                || lower.contains("ender_dragon") || lower.contains("wither")) {
            return true;
        }
        for (String key : CUSTOM_BOSS_AFFINITIES.keySet()) {
            if (lower.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMiniBoss(String entityId) {
        if (entityId == null) return false;
        String lower = entityId.toLowerCase(Locale.ROOT);
        return lower.contains("warden") || lower.contains("elder_guardian")
                || lower.contains("evoker") || lower.contains("ravager");
    }

    public CardRarity rollOreRarity(String blockId) {
        double r = random.nextDouble();
        String lower = (blockId != null) ? blockId.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("debris")) {
            return r < 0.65 ? CardRarity.RARE : CardRarity.MYTHIC;
        }
        if (lower.contains("diamond")) {
            return r < 0.40 ? CardRarity.UNCOMMON : (r < 0.90 ? CardRarity.RARE : CardRarity.MYTHIC);
        }
        if (lower.contains("gold") || lower.contains("emerald")) {
            return r < 0.60 ? CardRarity.COMMON : (r < 0.95 ? CardRarity.UNCOMMON : CardRarity.RARE);
        }
        return r < 0.85 ? CardRarity.COMMON : CardRarity.UNCOMMON;
    }

    public CardRarity rollCropRarity() {
        return random.nextDouble() < 0.85 ? CardRarity.COMMON : CardRarity.UNCOMMON;
    }

    public CardRarity rollNetherWartRarity() {
        double r = random.nextDouble();
        return r < 0.60 ? CardRarity.UNCOMMON : (r < 0.95 ? CardRarity.RARE : CardRarity.MYTHIC);
    }

    public CardRarity rollMobRarity() {
        double r = random.nextDouble();
        return r < 0.80 ? CardRarity.COMMON : (r < 0.98 ? CardRarity.UNCOMMON : CardRarity.RARE);
    }

    public CardRarity rollDungeonRarity(String structureId) {
        double r = random.nextDouble();
        String lower = (structureId != null) ? structureId.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("ancient_city")) return r < 0.65 ? CardRarity.RARE : CardRarity.MYTHIC;
        if (lower.contains("end_city")) return r < 0.60 ? CardRarity.RARE : CardRarity.MYTHIC;
        if (lower.contains("nether")) return r < 0.60 ? CardRarity.UNCOMMON : (r < 0.95 ? CardRarity.RARE : CardRarity.MYTHIC);
        return r < 0.70 ? CardRarity.COMMON : (r < 0.95 ? CardRarity.UNCOMMON : CardRarity.RARE);
    }

    public Set<ManaColor> getOreColorAffinities(String blockId) {
        String lower = (blockId != null) ? blockId.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("redstone")) return Set.of(ManaColor.RED);
        if (lower.contains("lapis")) return Set.of(ManaColor.BLUE);
        if (lower.contains("coal")) return Set.of(ManaColor.BLACK, ManaColor.RED);
        if (lower.contains("iron")) return Set.of(ManaColor.WHITE, ManaColor.COLORLESS);
        if (lower.contains("copper")) return Set.of(ManaColor.RED, ManaColor.BLUE);
        if (lower.contains("gold")) return Set.of(ManaColor.WHITE, ManaColor.COLORLESS);
        if (lower.contains("emerald")) return Set.of(ManaColor.GREEN);
        if (lower.contains("debris") || lower.contains("netherite")) return Set.of(ManaColor.BLACK, ManaColor.RED, ManaColor.COLORLESS);
        return EnumSet.allOf(ManaColor.class);
    }

    public Set<ManaColor> getCropColorAffinities(String cropId) {
        String lower = (cropId != null) ? cropId.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("nether_wart")) return Set.of(ManaColor.BLACK, ManaColor.RED);
        return Set.of(ManaColor.GREEN, ManaColor.WHITE);
    }

    public Set<ManaColor> getMobColorAffinities(String entityId) {
        String lower = (entityId != null) ? entityId.toLowerCase(Locale.ROOT) : "";
        for (Map.Entry<String, Set<ManaColor>> entry : CUSTOM_BOSS_AFFINITIES.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        if (lower.contains("white")) return Set.of(ManaColor.WHITE);
        if (lower.contains("blue")) return Set.of(ManaColor.BLUE);
        if (lower.contains("black")) return Set.of(ManaColor.BLACK);
        if (lower.contains("red")) return Set.of(ManaColor.RED);
        if (lower.contains("green")) return Set.of(ManaColor.GREEN);
        if (lower.contains("colorless")) return Set.of(ManaColor.COLORLESS);
        if (lower.contains("drowned")) return Set.of(ManaColor.BLUE, ManaColor.BLACK);
        if (lower.contains("enderman")) return Set.of(ManaColor.BLUE, ManaColor.COLORLESS);
        if (lower.contains("creeper")) return Set.of(ManaColor.RED, ManaColor.GREEN);
        if (lower.contains("blaze") || lower.contains("magma")) return Set.of(ManaColor.RED);
        if (lower.contains("wither")) return Set.of(ManaColor.BLACK, ManaColor.COLORLESS);
        if (lower.contains("ender_dragon")) return Set.of(ManaColor.BLACK, ManaColor.COLORLESS);
        return Set.of(ManaColor.BLACK, ManaColor.RED);
    }

    public DropRoll evaluateAltarDrop(ManaColor color, ProgressionConfig config) {
        if (config == null) config = ProgressionConfig.defaults();
        CardRarity targetRarity = random.nextDouble() < 0.5 ? CardRarity.RARE : CardRarity.MYTHIC;
        Set<ManaColor> colors = color != null ? Set.of(color) : EnumSet.allOf(ManaColor.class);
        Optional<CardDefinition> picked = pickCard(targetRarity, colors);
        return new DropRoll(picked.isPresent(), picked.map(CardDefinition::id), CardUnlockSource.BOSS_DROP);
    }

    public Set<ManaColor> getDungeonColors(String structureId) {
        String lower = (structureId != null) ? structureId.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("nether")) return Set.of(ManaColor.RED, ManaColor.BLACK);
        if (lower.contains("stronghold") || lower.contains("ocean")) return Set.of(ManaColor.BLUE, ManaColor.WHITE);
        return EnumSet.allOf(ManaColor.class);
    }

    private Optional<CardDefinition> pickCard(CardRarity rarity, Set<ManaColor> colors) {
        List<CardDefinition> candidates = allCards.stream()
                .filter(c -> c.rarity() == rarity)
                .filter(c -> matchesAnyColor(c, colors))
                .toList();

        if (candidates.isEmpty()) {
            candidates = allCards.stream().filter(c -> c.rarity() == rarity).toList();
        }
        if (candidates.isEmpty()) {
            candidates = allCards;
        }
        if (candidates.isEmpty()) return Optional.empty();
        return Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    private boolean matchesAnyColor(CardDefinition card, Set<ManaColor> colors) {
        if (colors == null || colors.isEmpty() || colors.size() == ManaColor.values().length) return true;
        ManaCost cost = card.manaCost();
        if (cost.isColorless() && colors.contains(ManaColor.COLORLESS)) return true;
        if (cost.white() > 0 && colors.contains(ManaColor.WHITE)) return true;
        if (cost.blue() > 0 && colors.contains(ManaColor.BLUE)) return true;
        if (cost.black() > 0 && colors.contains(ManaColor.BLACK)) return true;
        if (cost.red() > 0 && colors.contains(ManaColor.RED)) return true;
        if (cost.green() > 0 && colors.contains(ManaColor.GREEN)) return true;
        return false;
    }
}
