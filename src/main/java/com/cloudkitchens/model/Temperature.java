package com.cloudkitchens.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Temperature {
    HOT("hot"),
    COLD("cold"),
    ROOM("room");

    private final String value;

    Temperature(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Temperature fromString(String value) {
        for (Temperature temp : Temperature.values()) {
            if (temp.value.equalsIgnoreCase(value)) {
                return temp;
            }
        }
        throw new IllegalArgumentException("Unknown temperature: " + value);
    }
}
