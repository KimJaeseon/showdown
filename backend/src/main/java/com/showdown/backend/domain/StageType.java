package com.showdown.backend.domain;

import jakarta.persistence.Converter;

public enum StageType implements DbEnum {
    ROUND_ROBIN("round_robin"),
    KNOCKOUT("knockout"),
    PLACEMENT("placement");

    private final String dbValue;

    StageType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    @Converter(autoApply = true)
    public static class JpaConverter extends AbstractDbEnumConverter<StageType> {
        public JpaConverter() {
            super(StageType.class);
        }
    }
}
