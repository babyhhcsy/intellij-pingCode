package com.pingCode.exceptions;

import org.jetbrains.annotations.NotNull;

public class PingCodeParseException extends RuntimeException {
    public PingCodeParseException(@NotNull String message) {
        super(message);
    }
}