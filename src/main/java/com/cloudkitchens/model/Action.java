package com.cloudkitchens.model;

import com.fasterxml.jackson.annotation.JsonProperty;


public class Action {
    @JsonProperty("timestamp")
    private long timestampMicros;
    
    @JsonProperty("id")
    private String orderId;
    
    @JsonProperty("action")
    private ActionType actionType;
    
    @JsonProperty("target")
    private StorageType target;

    public Action() {}

    public Action(long timestampMicros, String orderId, ActionType actionType, StorageType target) {
        this.timestampMicros = timestampMicros;
        this.orderId = orderId;
        this.actionType = actionType;
        this.target = target;
    }

    // Getters and setters
    public long getTimestampMicros() {
        return timestampMicros;
    }

    public void setTimestampMicros(long timestampMicros) {
        this.timestampMicros = timestampMicros;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public StorageType getTarget() {
        return target;
    }

    public void setTarget(StorageType target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s %s -> %s", 
                           timestampMicros, actionType.getValue(), orderId, target.getValue());
    }
}
