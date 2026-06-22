package com.showdown.backend.domain;

import jakarta.persistence.Converter;

public enum MatchSide implements DbEnum {
    PLAYER1("player1"),
    PLAYER2("player2");

    private final String dbValue;

    MatchSide(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    @Converter(autoApply = true)
    public static class JpaConverter extends AbstractDbEnumConverter<MatchSide> {
        public JpaConverter() {
            super(MatchSide.class);
        }
    }
}
