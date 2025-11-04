package com.cloudkitchens.model;

/**
 * Enum representing the types of kitchen actions.
 */
public enum ActionType {
    PLACE("place"),
    MOVE("move"),
    PICKUP("pickup"),
    DISCARD("discard");

    private final String value;

    ActionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ActionType fromString(String value) {
        for (ActionType type : ActionType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown action type: " + value);
    }
}

