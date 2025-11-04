package com.cloudkitchens.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request DTO for submitting a solution to the challenge server.
 */
public class ChallengeRequest {
    @JsonProperty("options")
    private ChallengeOptions options;
    
    @JsonProperty("actions")
    private List<ChallengeAction> actions;

    public ChallengeRequest() {}

    public ChallengeRequest(ChallengeOptions options, List<ChallengeAction> actions) {
        this.options = options;
        this.actions = actions;
    }

    public ChallengeOptions getOptions() {
        return options;
    }

    public void setOptions(ChallengeOptions options) {
        this.options = options;
    }

    public List<ChallengeAction> getActions() {
        return actions;
    }

    public void setActions(List<ChallengeAction> actions) {
        this.actions = actions;
    }
}

