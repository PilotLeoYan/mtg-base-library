package com.mtg.domain;

public enum ManaColor {
    WHITE("white", "{W}"),
    BLUE("blue", "{U}"),
    BLACK("black", "{B}"),
    RED("red", "{R}"),
    GREEN("green", "{G}"),
    COLORLESS("colorless", "{C}");

    private final String id;
    private final String symbol;

    ManaColor(String id, String symbol) {
        this.id = id;
        this.symbol = symbol;
    }

    public String getId() { return id; }
    public String getSymbol() { return symbol; }
}
