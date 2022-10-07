package com.pingCode.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.Mandatory;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Collections;
import java.util.List;

@RestModel
public class PingCodeErrorMessage {
    @Nullable
    public String message;

    private List<Error> errors;

    @Nullable
    public String error;

    @Nullable
    public String errorDescription;

    @Nullable
    public String getMessage() {
        return message;
    }

    @NotNull
    public List<Error> getErrors() {
        if (errors == null) return Collections.emptyList();
        return errors;
    }

    @Nullable
    public String getPresentableError() {
        if (errors == null) {
            return "[" + error + "] " + errorDescription;
        } else {
            StringBuilder s = new StringBuilder();
            s.append(message);
            for (Error e : errors) {
                s.append(String.format("<br/>[%s; %s]%s: %s", e.getResource(), e.getField(), e.getCode(), e.getMessage()));
            }
            return s.toString();
        }
    }

    @RestModel
    public static class Error {
        @Mandatory
        private String resource;
        private String field;
        @Mandatory
        private String code;
        private String message;

        @NotNull
        public String getResource() {
            return resource;
        }

        @Nullable
        public String getField() {
            return field;
        }

        @NotNull
        public String getCode() {
            return code;
        }

        @Nullable
        public String getMessage() {
            return message;
        }
    }

    public boolean containsReasonMessage(@NotNull String reason) {
        if (message == null) return false;
        return message.contains(reason);
    }

    public boolean containsErrorCode(@NotNull String code) {
        if (errors == null) return false;
        for (Error error : errors) {
            if (error.getCode().contains(code)) return true;
        }
        return false;
    }

    public boolean containsErrorMessage(@NotNull String message) {
        if (errors == null) {
            if(error != null && error.contains(message))
                return true;

            return errorDescription != null && errorDescription.contains(message);
        } else {
            for (Error error : errors) {
                if (error.getCode().contains(message)) return true;
            }
        }

        return false;
    }
}
