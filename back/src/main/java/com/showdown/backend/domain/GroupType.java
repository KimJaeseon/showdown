package com.showdown.backend.domain;

import jakarta.persistence.Converter;

public enum GroupType implements DbEnum {
    LEAGUE("league"),
    KNOCKOUT("knockout"),
    PLACEMENT("placement");

    private final String dbValue;

    GroupType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    @Converter(autoApply = true)
    public static class JpaConverter extends AbstractDbEnumConverter<GroupType> {
        public JpaConverter() {
            super(GroupType.class);
        }
    }
}
