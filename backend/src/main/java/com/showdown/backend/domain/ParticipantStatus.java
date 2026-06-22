package com.showdown.backend.domain;

import jakarta.persistence.Converter;

public enum ParticipantStatus implements DbEnum {
    ACTIVE("active"),
    WITHDRAWN("withdrawn"),
    DISQUALIFIED("disqualified");

    private final String dbValue;

    ParticipantStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    @Converter(autoApply = true)
    public static class JpaConverter extends AbstractDbEnumConverter<ParticipantStatus> {
        public JpaConverter() {
            super(ParticipantStatus.class);
        }
    }
}
