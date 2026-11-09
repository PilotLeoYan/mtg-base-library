package com.mtg.domain;

public class CardParseException extends RuntimeException {
    public CardParseException(String message) {
        super(message);
    }

    public CardParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
