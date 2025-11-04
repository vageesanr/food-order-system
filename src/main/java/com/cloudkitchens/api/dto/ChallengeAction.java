package com.cloudkitchens.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Action DTO for challenge server communication.
 */
public class ChallengeAction {
    @JsonProperty("timestamp")
    private long timestampMicros;
    
    @JsonProperty("id")
    private String orderId;
    
    @JsonProperty("action")
    private String actionType;
    
    @JsonProperty("target")
    private String target;

    public ChallengeAction() {}

    public ChallengeAction(long timestampMicros, String orderId, String actionType, String target) {
        this.timestampMicros = timestampMicros;
        this.orderId = orderId;
        this.actionType = actionType;
        this.target = target;
    }

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

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}

