package com.showdown.backend.domain;

import jakarta.persistence.Converter;

public enum StageStatus implements DbEnum {
    DRAFT("draft"),
    PUBLISHED("published"),
    RUNNING("running"),
    FINISHED("finished");

    private final String dbValue;

    StageStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    @Converter(autoApply = true)
    public static class JpaConverter extends AbstractDbEnumConverter<StageStatus> {
        public JpaConverter() {
            super(StageStatus.class);
        }
    }
}
