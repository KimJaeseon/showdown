package com.showdown.backend.domain;

import jakarta.persistence.Converter;

public enum TournamentStatus implements DbEnum {
    DRAFT("draft"),
    PUBLISHED("published"),
    RUNNING("running"),
    FINISHED("finished"),
    ARCHIVED("archived");

    private final String dbValue;

    TournamentStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    @Converter(autoApply = true)
    public static class JpaConverter extends AbstractDbEnumConverter<TournamentStatus> {
        public JpaConverter() {
            super(TournamentStatus.class);
        }
    }
}
