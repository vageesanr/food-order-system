package com.cloudkitchens.model;

/**
 * Represents the different storage locations in the kitchen.
 */
public enum StorageType {
    HEATER("heater"),
    COOLER("cooler"),
    SHELF("shelf");

    private final String value;

    StorageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StorageType fromString(String value) {
        for (StorageType type : StorageType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown storage type: " + value);
    }
}
