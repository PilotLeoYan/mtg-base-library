package com.mtg.api.keyword;

public enum KeywordCategory {
    STATIC,     // Continuous modifier (Flying, Haste, Vigilance, Trample)
    TRIGGERED,  // Event-triggered (Landfall, Lifelink)
    COMBAT,     // Combat calculation (First Strike, Deathtouch, Toxic)
    ACTION,     // Spell verb action (Proliferate, Scry)
    MODIFIER    // Casting cost / threshold (Kicker, Corrupted)
}
