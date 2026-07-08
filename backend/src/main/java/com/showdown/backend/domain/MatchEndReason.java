package com.showdown.backend.domain;

import jakarta.persistence.Converter;

public enum MatchEndReason implements DbEnum {
    NORMAL("normal"),
    GIVING_UP("giving_up"),
    DEFAULT_LOSS("default_loss"),
    BYE("bye");

    private final String dbValue;

    MatchEndReason(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    @Converter(autoApply = true)
    public static class JpaConverter extends AbstractDbEnumConverter<MatchEndReason> {
        public JpaConverter() {
            super(MatchEndReason.class);
        }
    }
}
