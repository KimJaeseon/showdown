package com.showdown.backend.domain;

import jakarta.persistence.Converter;

public enum MatchStatus implements DbEnum {
    SCHEDULED("scheduled"),
    RUNNING("running"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    WALKOVER("walkover");

    private final String dbValue;

    MatchStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    @Converter(autoApply = true)
    public static class JpaConverter extends AbstractDbEnumConverter<MatchStatus> {
        public JpaConverter() {
            super(MatchStatus.class);
        }
    }
}
