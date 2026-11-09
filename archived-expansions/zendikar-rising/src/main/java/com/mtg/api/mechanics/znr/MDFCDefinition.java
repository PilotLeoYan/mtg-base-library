package com.mtg.api.mechanics.znr;

import com.mtg.domain.CardDefinition;
import java.util.Objects;

public class MDFCDefinition {
    private final String mdfcId;
    private final CardDefinition frontFace;
    private final CardDefinition backFace;

    public MDFCDefinition(String mdfcId, CardDefinition frontFace, CardDefinition backFace) {
        this.mdfcId = Objects.requireNonNull(mdfcId, "mdfcId cannot be null");
        this.frontFace = Objects.requireNonNull(frontFace, "frontFace cannot be null");
        this.backFace = Objects.requireNonNull(backFace, "backFace cannot be null");
    }

    public String getMdfcId() { return mdfcId; }
    public CardDefinition getFrontFace() { return frontFace; }
    public CardDefinition getBackFace() { return backFace; }

    public CardDefinition getFace(MDFCFace face) {
        return face == MDFCFace.FRONT ? frontFace : backFace;
    }
}
