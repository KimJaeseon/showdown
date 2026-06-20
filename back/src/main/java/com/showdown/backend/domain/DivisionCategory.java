package com.showdown.backend.domain;

import jakarta.persistence.Converter;

public enum DivisionCategory implements DbEnum {
    MALE("male"),
    FEMALE("female"),
    MIXED("mixed"),
    YOUTH("youth"),
    OPEN("open"),
    CUSTOM("custom");

    private final String dbValue;

    DivisionCategory(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    @Converter(autoApply = true)
    public static class JpaConverter extends AbstractDbEnumConverter<DivisionCategory> {
        public JpaConverter() {
            super(DivisionCategory.class);
        }
    }
}
