package com.showdown.backend.domain;

import jakarta.persistence.AttributeConverter;
import java.util.Arrays;

abstract class AbstractDbEnumConverter<E extends Enum<E> & DbEnum> implements AttributeConverter<E, String> {
    private final Class<E> enumType;

    AbstractDbEnumConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Arrays.stream(enumType.getEnumConstants())
                .filter(value -> value.getDbValue().equals(dbData))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown " + enumType.getSimpleName() + " value: " + dbData));
    }
}
