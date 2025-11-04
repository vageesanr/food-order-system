package com.cloudkitchens.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Options for the challenge execution.
 */
public class ChallengeOptions {
    @JsonProperty("rate")
    private long rateMicros;
    
    @JsonProperty("min")
    private long minPickupMicros;
    
    @JsonProperty("max")
    private long maxPickupMicros;

    public ChallengeOptions() {}

    public ChallengeOptions(long rateMicros, long minPickupMicros, long maxPickupMicros) {
        this.rateMicros = rateMicros;
        this.minPickupMicros = minPickupMicros;
        this.maxPickupMicros = maxPickupMicros;
    }

    public long getRateMicros() {
        return rateMicros;
    }

    public void setRateMicros(long rateMicros) {
        this.rateMicros = rateMicros;
    }

    public long getMinPickupMicros() {
        return minPickupMicros;
    }

    public void setMinPickupMicros(long minPickupMicros) {
        this.minPickupMicros = minPickupMicros;
    }

    public long getMaxPickupMicros() {
        return maxPickupMicros;
    }

    public void setMaxPickupMicros(long maxPickupMicros) {
        this.maxPickupMicros = maxPickupMicros;
    }
}

