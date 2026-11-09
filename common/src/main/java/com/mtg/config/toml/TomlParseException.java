package com.mtg.config.toml;

public class TomlParseException extends RuntimeException {
    public TomlParseException(String message) {
        super(message);
    }

    public TomlParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
